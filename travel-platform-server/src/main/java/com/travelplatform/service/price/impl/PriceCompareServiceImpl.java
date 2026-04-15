package com.travelplatform.service.price.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.mapper.CouponMapper;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.service.price.PriceCompareService;
import com.travelplatform.vo.price.CompareItemVO;
import com.travelplatform.vo.price.CouponVO;
import com.travelplatform.vo.price.PriceCompareVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Hotel not found");
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

        List<CompareItemVO> compareItems = hotels.stream()
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
                .limit(6)
                .toList();

        return buildCompareVO(PRODUCT_TYPE_HOTEL, hotelId, resolveCurrentPrice(compareItems, hotelId),
                compareItems, loadCoupons(PRODUCT_TYPE_HOTEL));
    }

    @Override
    public PriceCompareVO getFlightCompare(Long flightId) {
        Flight currentFlight = flightMapper.selectById(flightId);
        if (currentFlight == null || !Integer.valueOf(1).equals(currentFlight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Flight not found");
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
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Tour not found");
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

    public ProductSnapshot getProductSnapshot(String productType, Long productId) {
        String normalizedType = normalizeProductType(productType);
        return switch (normalizedType) {
            case PRODUCT_TYPE_HOTEL -> buildHotelSnapshot(productId);
            case PRODUCT_TYPE_FLIGHT -> buildFlightSnapshot(productId);
            case PRODUCT_TYPE_TOUR -> buildTourSnapshot(productId);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Unsupported product type");
        };
    }

    private ProductSnapshot buildHotelSnapshot(Long hotelId) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !Integer.valueOf(1).equals(hotel.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Hotel not found");
        }
        BigDecimal currentPrice = hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                        .eq(HotelRoom::getHotelId, hotelId)
                        .eq(HotelRoom::getStatus, 1)
                        .gt(HotelRoom::getStock, 0))
                .stream()
                .map(HotelRoom::getPrice)
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Hotel has no available room"));
        return new ProductSnapshot(PRODUCT_TYPE_HOTEL, hotelId, hotel.getHotelName(), currentPrice);
    }

    private ProductSnapshot buildFlightSnapshot(Long flightId) {
        Flight flight = flightMapper.selectById(flightId);
        if (flight == null || !Integer.valueOf(1).equals(flight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Flight not found");
        }
        return new ProductSnapshot(PRODUCT_TYPE_FLIGHT, flightId, flight.getFlightNo(), flight.getPrice());
    }

    private ProductSnapshot buildTourSnapshot(Long tourId) {
        TourPackage tour = tourPackageMapper.selectById(tourId);
        if (tour == null || !Integer.valueOf(1).equals(tour.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Tour not found");
        }
        return new ProductSnapshot(PRODUCT_TYPE_TOUR, tourId, tour.getPackageName(), tour.getPrice());
    }

    public String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Product type is required");
        }
        String value = productType.trim().toUpperCase();
        if (!PRODUCT_TYPE_HOTEL.equals(value) && !PRODUCT_TYPE_FLIGHT.equals(value) && !PRODUCT_TYPE_TOUR.equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Unsupported product type");
        }
        return value;
    }

    private PriceCompareVO buildCompareVO(String productType,
                                          Long productId,
                                          BigDecimal currentPrice,
                                          List<CompareItemVO> compareItems,
                                          List<CouponVO> couponList) {
        List<CompareItemVO> mutableItems = new ArrayList<>(compareItems);
        if (mutableItems.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "No comparable products found");
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

    private BigDecimal resolveCurrentPrice(List<CompareItemVO> compareItems, Long productId) {
        return compareItems.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .map(CompareItemVO::getPrice)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Current product has no comparable price"));
    }

    private String buildHotelSubtitle(Hotel hotel) {
        return hotel.getCity() + " " + (hotel.getDistrict() == null ? "" : hotel.getDistrict()) + " | " + hotel.getStarLevel() + "星";
    }

    private String formatDateTimeText(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        return date + " " + time.toLocalTime().withSecond(0).withNano(0);
    }

    public static class ProductSnapshot {
        private final String productType;
        private final Long productId;
        private final String productName;
        private final BigDecimal currentPrice;

        public ProductSnapshot(String productType, Long productId, String productName, BigDecimal currentPrice) {
            this.productType = productType;
            this.productId = productId;
            this.productName = productName;
            this.currentPrice = currentPrice;
        }

        public String getProductType() {
            return productType;
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }
    }
}
