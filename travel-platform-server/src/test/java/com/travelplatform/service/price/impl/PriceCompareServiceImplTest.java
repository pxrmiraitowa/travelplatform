package com.travelplatform.service.price.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.mapper.CouponMapper;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.vo.price.PriceCompareVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceCompareServiceImplTest {

    @Mock
    private HotelMapper hotelMapper;
    @Mock
    private HotelRoomMapper hotelRoomMapper;
    @Mock
    private FlightMapper flightMapper;
    @Mock
    private TourPackageMapper tourPackageMapper;
    @Mock
    private CouponMapper couponMapper;

    @InjectMocks
    private PriceCompareServiceImpl service;

    @Test
    void shouldNormalizeSupportedProductTypes() {
        assertThat(service.normalizeProductType(" hotel ")).isEqualTo("HOTEL");
        assertThat(service.normalizeProductType("flight")).isEqualTo("FLIGHT");
        assertThat(service.normalizeProductType("TOUR")).isEqualTo("TOUR");
    }

    @Test
    void shouldRejectUnsupportedProductType() {
        assertThatThrownBy(() -> service.normalizeProductType("car"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldKeepCurrentHotelInCompareListWhenMoreThanSixMatchesExist() {
        Long currentHotelId = 7L;
        when(hotelMapper.selectById(currentHotelId)).thenReturn(buildHotel(currentHotelId, "Current", "Shanghai", 5, 1));
        when(hotelMapper.selectList(any())).thenReturn(List.of(
                buildHotel(1L, "A", "Shanghai", 5, 1),
                buildHotel(2L, "B", "Shanghai", 5, 1),
                buildHotel(3L, "C", "Shanghai", 5, 1),
                buildHotel(4L, "D", "Shanghai", 5, 1),
                buildHotel(5L, "E", "Shanghai", 5, 1),
                buildHotel(6L, "F", "Shanghai", 5, 1),
                buildHotel(currentHotelId, "Current", "Shanghai", 5, 1)
        ));
        when(hotelRoomMapper.selectList(any())).thenReturn(List.of(
                buildRoom(1L, new BigDecimal("300")),
                buildRoom(2L, new BigDecimal("320")),
                buildRoom(3L, new BigDecimal("340")),
                buildRoom(4L, new BigDecimal("360")),
                buildRoom(5L, new BigDecimal("380")),
                buildRoom(6L, new BigDecimal("400")),
                buildRoom(currentHotelId, new BigDecimal("520"))
        ));
        when(couponMapper.selectList(any())).thenReturn(List.of(buildCoupon()));

        PriceCompareVO result = service.getHotelCompare(currentHotelId);

        assertThat(result.getCompareItems()).hasSize(6);
        assertThat(result.getCompareItems())
                .extracting("productId")
                .contains(currentHotelId);
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("520");
        assertThat(result.getLowestPrice()).isEqualByComparingTo("300");
        assertThat(result.getCouponList()).hasSize(1);
    }

    private Hotel buildHotel(Long id, String name, String city, Integer starLevel, Integer status) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setHotelName(name);
        hotel.setCity(city);
        hotel.setDistrict("Pudong");
        hotel.setStarLevel(starLevel);
        hotel.setStatus(status);
        return hotel;
    }

    private HotelRoom buildRoom(Long hotelId, BigDecimal price) {
        HotelRoom room = new HotelRoom();
        room.setHotelId(hotelId);
        room.setPrice(price);
        room.setStatus(1);
        room.setStock(3);
        return room;
    }

    private Coupon buildCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCouponName("Hotel 50 Off");
        coupon.setProductType(PriceCompareServiceImpl.PRODUCT_TYPE_HOTEL);
        coupon.setThresholdAmount(new BigDecimal("500"));
        coupon.setDiscountAmount(new BigDecimal("50"));
        coupon.setDiscountType("FULL_REDUCTION");
        coupon.setDescription("Demo");
        coupon.setStatus(1);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(1));
        return coupon;
    }
}
