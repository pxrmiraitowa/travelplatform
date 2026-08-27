package com.travelplatform.product.service.price.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.product.entity.Coupon;
import com.travelplatform.product.entity.Flight;
import com.travelplatform.product.entity.Hotel;
import com.travelplatform.product.entity.HotelRoom;
import com.travelplatform.product.entity.TourPackage;
import com.travelplatform.product.mapper.CouponMapper;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.mapper.HotelMapper;
import com.travelplatform.product.mapper.HotelRoomMapper;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.service.price.PriceCompareService;
import com.travelplatform.product.vo.price.CompareItemVO;
import com.travelplatform.product.vo.price.CouponVO;
import com.travelplatform.product.vo.price.PriceCompareVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PriceCompareServiceImpl implements PriceCompareService {

    public static final String PRODUCT_TYPE_HOTEL = "HOTEL";
    public static final String PRODUCT_TYPE_FLIGHT = "FLIGHT";
    public static final String PRODUCT_TYPE_TOUR = "TOUR";

    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final FlightMapper flightMapper;
    private final TourPackageMapper tourPackageMapper;
    private final CouponMapper couponMapper;

    public PriceCompareServiceImpl(HotelMapper hotelMapper,
                                   HotelRoomMapper hotelRoomMapper,
                                   FlightMapper flightMapper,
                                   TourPackageMapper tourPackageMapper,
                                   CouponMapper couponMapper) {
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.flightMapper = flightMapper;
        this.tourPackageMapper = tourPackageMapper;
        this.couponMapper = couponMapper;
    }

    @Override
    public PriceCompareVO getHotelCompare(Long hotelId) {
        Hotel currentHotel = hotelMapper.selectById(hotelId);
        if (currentHotel == null || !Integer.valueOf(1).equals(currentHotel.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "酒店不存在");
        }

        List<Hotel> hotels = hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getStatus, 1)
                .eq(Hotel::getCity, currentHotel.getCity())
                .eq(Hotel::getStarLevel, currentHotel.getStarLevel())
                .orderByAsc(Hotel::getId));

        List<Long> hotelIds = hotels.stream().map(Hotel::getId).toList();
        Map<Long, BigDecimal> minPriceMap = hotelIds.isEmpty()
                ? Map.of()
                : hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                        .in(HotelRoom::getHotelId, hotelIds)
                        .eq(HotelRoom::getStatus, 1)
                        .gt(HotelRoom::getStock, 0))
                .stream()
                .collect(Collectors.groupingBy(HotelRoom::getHotelId,
                        Collectors.mapping(HotelRoom::getPrice,
                                Collectors.collectingAndThen(Collectors.minBy(BigDecimal::compareTo), item -> item.orElse(null)))));

        List<CompareItemVO> hotelCompareItems = hotels.stream()
                .filter(hotel -> minPriceMap.get(hotel.getId()) != null)
                .map(hotel -> {
                    CompareItemVO item = new CompareItemVO();
                    item.setProductId(hotel.getId());
                    item.setProductName(hotel.getHotelName());
                    item.setSubTitle(buildHotelSubtitle(hotel));
                    item.setPrice(minPriceMap.get(hotel.getId()));
                    item.setCurrentProduct(hotel.getId().equals(hotelId));
                    return item;
                })
                .sorted(Comparator.comparing(CompareItemVO::getPrice).thenComparing(CompareItemVO::getProductId))
                .toList();

        List<CompareItemVO> compareItems = keepCurrentHotelInCompareList(hotelCompareItems, hotelId);

        return buildCompareVO(PRODUCT_TYPE_HOTEL, hotelId, resolveCurrentPrice(hotelCompareItems, hotelId),
                compareItems, loadCoupons(PRODUCT_TYPE_HOTEL));
    }

    @Override
    public PriceCompareVO getFlightCompare(Long flightId) {
        Flight currentFlight = flightMapper.selectById(flightId);
        if (currentFlight == null || !Integer.valueOf(1).equals(currentFlight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "航班不存在");
        }

        LocalDate departureDate = currentFlight.getDepartureTime().toLocalDate();
        List<Flight> flights = flightMapper.selectList(new LambdaQueryWrapper<Flight>()
                .eq(Flight::getStatus, 1)
                .eq(Flight::getDepartureCity, currentFlight.getDepartureCity())
                .eq(Flight::getArrivalCity, currentFlight.getArrivalCity())
                .ge(Flight::getDepartureTime, departureDate.atStartOfDay())
                .lt(Flight::getDepartureTime, departureDate.plusDays(1).atStartOfDay())
                .orderByAsc(Flight::getPrice)
                .orderByAsc(Flight::getDepartureTime));

        List<CompareItemVO> compareItems = flights.stream()
                .map(flight -> {
                    CompareItemVO item = new CompareItemVO();
                    item.setProductId(flight.getId());
                    item.setProductName(flight.getFlightNo());
                    item.setSubTitle(flight.getAirlineName() + " | " + formatDateTimeText(flight.getDepartureTime()));
                    item.setPrice(flight.getPrice());
                    item.setCurrentProduct(flight.getId().equals(flightId));
                    return item;
                })
                .limit(6)
                .toList();

        return buildCompareVO(PRODUCT_TYPE_FLIGHT, flightId, currentFlight.getPrice(),
                compareItems, loadCoupons(PRODUCT_TYPE_FLIGHT));
    }

    @Override
    public PriceCompareVO getTourCompare(Long tourId) {
        TourPackage currentTour = tourPackageMapper.selectById(tourId);
        if (currentTour == null || !Integer.valueOf(1).equals(currentTour.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "旅游产品不存在");
        }

        List<TourPackage> tours = tourPackageMapper.selectList(new LambdaQueryWrapper<TourPackage>()
                .eq(TourPackage::getStatus, 1)
                .eq(TourPackage::getDestination, currentTour.getDestination())
                .orderByAsc(TourPackage::getPrice)
                .orderByAsc(TourPackage::getDays)
                .orderByAsc(TourPackage::getId));

        List<CompareItemVO> compareItems = tours.stream()
                .filter(item -> currentTour.getDays() == null || item.getDays() == null || Math.abs(item.getDays() - currentTour.getDays()) <= 2)
                .map(tour -> {
                    CompareItemVO item = new CompareItemVO();
                    item.setProductId(tour.getId());
                    item.setProductName(tour.getPackageName());
                    item.setSubTitle((StringUtils.hasText(tour.getDepartureCity()) ? tour.getDepartureCity() : "全国出发")
                            + " | " + tour.getDays() + "天");
                    item.setPrice(tour.getPrice());
                    item.setCurrentProduct(tour.getId().equals(tourId));
                    return item;
                })
                .limit(6)
                .toList();

        return buildCompareVO(PRODUCT_TYPE_TOUR, tourId, currentTour.getPrice(),
                compareItems, loadCoupons(PRODUCT_TYPE_TOUR));
    }

    private PriceCompareVO buildCompareVO(String productType,
                                          Long productId,
                                          BigDecimal currentPrice,
                                          List<CompareItemVO> compareItems,
                                          List<CouponVO> couponList) {
        List<CompareItemVO> mutableItems = new ArrayList<>(compareItems);
        if (mutableItems.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前产品暂无可比价样例");
        }

        BigDecimal lowestPrice = mutableItems.stream()
                .map(CompareItemVO::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(currentPrice);

        for (CompareItemVO item : mutableItems) {
            boolean isLowest = item.getPrice() != null && item.getPrice().compareTo(lowestPrice) == 0;
            item.setLowestPrice(isLowest);
            if (isLowest) {
                item.setHighlightText("当前分组低价");
            } else if (item.getPrice() != null) {
                item.setHighlightText("高出低价 " + item.getPrice().subtract(lowestPrice).stripTrailingZeros().toPlainString() + " 元");
            }
        }

        PriceCompareVO vo = new PriceCompareVO();
        vo.setProductType(productType);
        vo.setProductId(productId);
        vo.setCurrentPrice(currentPrice);
        vo.setLowestPrice(lowestPrice);
        vo.setLowest(currentPrice.compareTo(lowestPrice) == 0);
        vo.setPriceDiff(currentPrice.subtract(lowestPrice));
        vo.setLowPriceLabel(vo.isLowest() ? "当前低价" : "比最低价高 " + vo.getPriceDiff().stripTrailingZeros().toPlainString() + " 元");
        vo.setCompareItems(mutableItems);
        vo.setCouponList(couponList);
        return vo;
    }

    private List<CouponVO> loadCoupons(String productType) {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, 1)
                        .eq(Coupon::getProductType, productType)
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now)
                        .orderByAsc(Coupon::getThresholdAmount)
                        .orderByDesc(Coupon::getDiscountAmount))
                .stream()
                .map(this::toCouponVO)
                .toList();
    }

    private CouponVO toCouponVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCouponName(coupon.getCouponName());
        vo.setDiscountType(coupon.getDiscountType());
        vo.setThresholdAmount(coupon.getThresholdAmount());
        vo.setDiscountAmount(coupon.getDiscountAmount());
        vo.setDescription(coupon.getDescription());
        return vo;
    }

    private List<CompareItemVO> keepCurrentHotelInCompareList(List<CompareItemVO> compareItems, Long hotelId) {
        if (compareItems.size() <= 6) {
            return compareItems;
        }

        CompareItemVO currentItem = compareItems.stream()
                .filter(item -> hotelId.equals(item.getProductId()))
                .findFirst()
                .orElse(null);
        if (currentItem == null) {
            return compareItems.stream().limit(6).toList();
        }

        List<CompareItemVO> limitedItems = compareItems.stream()
                .filter(item -> !hotelId.equals(item.getProductId()))
                .limit(5)
                .collect(Collectors.toCollection(ArrayList::new));
        limitedItems.add(currentItem);
        limitedItems.sort(Comparator.comparing(CompareItemVO::getPrice).thenComparing(CompareItemVO::getProductId));
        return limitedItems;
    }

    private BigDecimal resolveCurrentPrice(List<CompareItemVO> compareItems, Long productId) {
        return compareItems.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .map(CompareItemVO::getPrice)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前酒店暂无可比价房型"));
    }

    private String buildHotelSubtitle(Hotel hotel) {
        return hotel.getCity() + " " + (hotel.getDistrict() == null ? "" : hotel.getDistrict()) + " | " + hotel.getStarLevel() + "星";
    }

    private String formatDateTimeText(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        return date + " " + time.toLocalTime().withSecond(0).withNano(0);
    }
}
