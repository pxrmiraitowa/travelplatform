package com.travelplatform.product.service.snapshot.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.product.entity.Flight;
import com.travelplatform.product.entity.Hotel;
import com.travelplatform.product.entity.HotelRoom;
import com.travelplatform.product.entity.TourPackage;
import com.travelplatform.product.entity.TrainTicket;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.mapper.HotelMapper;
import com.travelplatform.product.mapper.HotelRoomMapper;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.mapper.TrainTicketMapper;
import com.travelplatform.product.service.snapshot.ProductSnapshotService;
import com.travelplatform.product.vo.snapshot.ProductSnapshotVO;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductSnapshotServiceImpl implements ProductSnapshotService {

    private static final String FLIGHT = "FLIGHT";
    private static final String TRAIN = "TRAIN";
    private static final String HOTEL = "HOTEL";
    private static final String TOUR = "TOUR";

    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;

    public ProductSnapshotServiceImpl(FlightMapper flightMapper,
                                      TrainTicketMapper trainTicketMapper,
                                      HotelMapper hotelMapper,
                                      HotelRoomMapper hotelRoomMapper,
                                      TourPackageMapper tourPackageMapper) {
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
    }

    @Override
    public ProductSnapshotVO getSnapshot(String productType, Long productId, Long variantId, String variantName) {
        String type = normalize(productType);
        if (productId == null || productId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品ID不能为空");
        }
        return switch (type) {
            case FLIGHT -> flightSnapshot(productId);
            case TRAIN -> trainSnapshot(productId, variantName);
            case HOTEL -> hotelSnapshot(productId, variantId, variantName);
            case TOUR -> tourSnapshot(productId);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的商品类型");
        };
    }

    private ProductSnapshotVO flightSnapshot(Long flightId) {
        Flight flight = flightMapper.selectById(flightId);
        if (flight == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "航班不存在");
        }
        String summary = flight.getDepartureCity() + " -> " + flight.getArrivalCity();
        return build(FLIGHT, flight.getId(), null, flight.getCabinClass(), flight.getFlightNo(), summary,
                flight.getPrice(), enabled(flight.getStatus()) && safe(flight.getStock()) > 0,
                safe(flight.getStock()), null);
    }

    private ProductSnapshotVO trainSnapshot(Long trainId, String seatType) {
        TrainTicket ticket = trainTicketMapper.selectById(trainId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "车次不存在");
        }
        SeatSnapshot seat = selectSeat(ticket, seatType);
        if (seat == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有可预订的席别");
        }
        String summary = ticket.getDepartureCity() + " -> " + ticket.getArrivalCity() + " / " + seat.name();
        return build(TRAIN, ticket.getId(), null, seat.name(), ticket.getTrainNo(), summary,
                seat.price(), enabled(ticket.getStatus()) && seat.stock() > 0,
                seat.stock(), null);
    }

    private ProductSnapshotVO hotelSnapshot(Long hotelId, Long roomId, String roomName) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "酒店不存在");
        }
        HotelRoom room = selectRoom(hotelId, roomId, roomName);
        if (room == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前酒店暂无可售房型");
        }
        return build(HOTEL, hotel.getId(), room.getId(), room.getRoomName(), hotel.getHotelName(), room.getRoomName(),
                room.getPrice(), enabled(hotel.getStatus()) && enabled(room.getStatus()) && safe(room.getStock()) > 0,
                safe(room.getStock()), hotel.getCoverImage());
    }

    private ProductSnapshotVO tourSnapshot(Long tourId) {
        TourPackage tour = tourPackageMapper.selectById(tourId);
        if (tour == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "旅游产品不存在");
        }
        String departure = StringUtils.hasText(tour.getDepartureCity()) ? tour.getDepartureCity() : "全国出发";
        String summary = departure + " -> " + tour.getDestination();
        return build(TOUR, tour.getId(), null, null, tour.getPackageName(), summary, tour.getPrice(),
                enabled(tour.getStatus()) && safe(tour.getStock()) > 0, safe(tour.getStock()), tour.getCoverImage());
    }

    private HotelRoom selectRoom(Long hotelId, Long roomId, String roomName) {
        List<HotelRoom> rooms = hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                .eq(HotelRoom::getHotelId, hotelId)
                .eq(HotelRoom::getStatus, 1)
                .gt(HotelRoom::getStock, 0));
        return rooms.stream()
                .filter(room -> roomId == null || roomId.equals(room.getId()))
                .filter(room -> !StringUtils.hasText(roomName) || roomName.equals(room.getRoomName()))
                .min(Comparator.comparing(HotelRoom::getPrice))
                .orElse(null);
    }

    private SeatSnapshot selectSeat(TrainTicket ticket, String expectedName) {
        List<SeatSnapshot> seats = List.of(
                new SeatSnapshot("商务座", ticket.getBusinessPrice(), safe(ticket.getBusinessStock())),
                new SeatSnapshot("一等座", ticket.getFirstClassPrice(), safe(ticket.getFirstClassStock())),
                new SeatSnapshot("二等座", ticket.getSecondClassPrice(), safe(ticket.getSecondClassStock()))
        );
        return seats.stream()
                .filter(seat -> seat.price() != null && seat.price().compareTo(BigDecimal.ZERO) > 0)
                .filter(seat -> seat.stock() > 0)
                .filter(seat -> !StringUtils.hasText(expectedName) || expectedName.equals(seat.name()))
                .min(Comparator.comparing(SeatSnapshot::price))
                .orElse(null);
    }

    private ProductSnapshotVO build(String productType,
                                    Long productId,
                                    Long variantId,
                                    String variantName,
                                    String productName,
                                    String summary,
                                    BigDecimal price,
                                    boolean available,
                                    Integer stock,
                                    String coverImage) {
        ProductSnapshotVO vo = new ProductSnapshotVO();
        vo.setProductType(productType);
        vo.setProductId(productId);
        vo.setVariantId(variantId);
        vo.setVariantName(variantName);
        vo.setProductName(productName);
        vo.setSummary(summary);
        vo.setCurrentPrice(price);
        vo.setAvailable(available);
        vo.setStock(stock);
        vo.setStockSummary(available ? "可预订，剩余" + safe(stock) + "份" : "暂不可预订");
        vo.setCoverImage(coverImage);
        return vo;
    }

    private String normalize(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品类型不能为空");
        }
        String value = productType.trim().toUpperCase(Locale.ROOT);
        if (!List.of(FLIGHT, TRAIN, HOTEL, TOUR).contains(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的商品类型");
        }
        return value;
    }

    private boolean enabled(Integer status) {
        return Integer.valueOf(1).equals(status);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private record SeatSnapshot(String name, BigDecimal price, int stock) {
    }
}
