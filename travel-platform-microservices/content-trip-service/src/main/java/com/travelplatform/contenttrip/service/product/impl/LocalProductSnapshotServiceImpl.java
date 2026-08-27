package com.travelplatform.contenttrip.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.entity.Flight;
import com.travelplatform.contenttrip.entity.Hotel;
import com.travelplatform.contenttrip.entity.HotelRoom;
import com.travelplatform.contenttrip.entity.TourPackage;
import com.travelplatform.contenttrip.mapper.FlightMapper;
import com.travelplatform.contenttrip.mapper.HotelMapper;
import com.travelplatform.contenttrip.mapper.HotelRoomMapper;
import com.travelplatform.contenttrip.mapper.TourPackageMapper;
import com.travelplatform.contenttrip.service.product.ProductSnapshot;
import com.travelplatform.contenttrip.service.product.ProductSnapshotService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalProductSnapshotServiceImpl implements ProductSnapshotService {

    public static final String PRODUCT_TYPE_HOTEL = "HOTEL";
    public static final String PRODUCT_TYPE_FLIGHT = "FLIGHT";
    public static final String PRODUCT_TYPE_TOUR = "TOUR";

    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final FlightMapper flightMapper;
    private final TourPackageMapper tourPackageMapper;

    public LocalProductSnapshotServiceImpl(HotelMapper hotelMapper,
                                           HotelRoomMapper hotelRoomMapper,
                                           FlightMapper flightMapper,
                                           TourPackageMapper tourPackageMapper) {
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.flightMapper = flightMapper;
        this.tourPackageMapper = tourPackageMapper;
    }

    @Override
    public ProductSnapshot getProductSnapshot(String productType, Long productId) {
        String normalizedType = normalizeProductType(productType);
        return switch (normalizedType) {
            case PRODUCT_TYPE_HOTEL -> buildHotelSnapshot(productId);
            case PRODUCT_TYPE_FLIGHT -> buildFlightSnapshot(productId);
            case PRODUCT_TYPE_TOUR -> buildTourSnapshot(productId);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的产品类型");
        };
    }

    @Override
    public String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "产品类型不能为空");
        }
        String value = productType.trim().toUpperCase();
        if (!PRODUCT_TYPE_HOTEL.equals(value) && !PRODUCT_TYPE_FLIGHT.equals(value) && !PRODUCT_TYPE_TOUR.equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的产品类型");
        }
        return value;
    }

    private ProductSnapshot buildHotelSnapshot(Long hotelId) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !Integer.valueOf(1).equals(hotel.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "酒店不存在");
        }
        BigDecimal currentPrice = hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                        .eq(HotelRoom::getHotelId, hotelId)
                        .eq(HotelRoom::getStatus, 1)
                        .gt(HotelRoom::getStock, 0))
                .stream()
                .map(HotelRoom::getPrice)
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前酒店暂无可售房型"));
        return new ProductSnapshot(PRODUCT_TYPE_HOTEL, hotelId, hotel.getHotelName(), currentPrice);
    }

    private ProductSnapshot buildFlightSnapshot(Long flightId) {
        Flight flight = flightMapper.selectById(flightId);
        if (flight == null || !Integer.valueOf(1).equals(flight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "航班不存在");
        }
        return new ProductSnapshot(PRODUCT_TYPE_FLIGHT, flightId, flight.getFlightNo(), flight.getPrice());
    }

    private ProductSnapshot buildTourSnapshot(Long tourId) {
        TourPackage tour = tourPackageMapper.selectById(tourId);
        if (tour == null || !Integer.valueOf(1).equals(tour.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "旅游产品不存在");
        }
        return new ProductSnapshot(PRODUCT_TYPE_TOUR, tourId, tour.getPackageName(), tour.getPrice());
    }
}
