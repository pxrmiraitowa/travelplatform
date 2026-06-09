package com.travelplatform.service.hotel.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.hotel.HotelQueryRequest;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.hotel.HotelDetailVO;
import com.travelplatform.vo.hotel.HotelListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {
    @Mock HotelMapper hotelMapper;
    @Mock HotelRoomMapper roomMapper;
    @InjectMocks HotelServiceImpl service;

    @Test
    void searchHotelsShouldExcludeHotelsWithoutAvailableRooms() {
        when(hotelMapper.selectList(any())).thenReturn(List.of(hotel(1L), hotel(2L)));
        when(roomMapper.selectList(any())).thenReturn(List.of(room(1L, new BigDecimal("500"), 2), room(2L, new BigDecimal("300"), 0)));

        PageResult<HotelListItemVO> result = service.searchHotels(new HotelQueryRequest());

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(1L);
        assertThat(result.getRecords().get(0).getMinPrice()).isEqualByComparingTo("500");
    }

    @Test
    void getHotelDetailShouldIncludeRoomList() {
        when(hotelMapper.selectById(1L)).thenReturn(hotel(1L));
        when(roomMapper.selectList(any())).thenReturn(List.of(room(1L, new BigDecimal("500"), 2)));

        HotelDetailVO result = service.getHotelDetail(1L);

        assertThat(result.getRoomList()).hasSize(1);
        assertThat(result.getDetailImages()).isNotEmpty();
    }

    @Test
    void getHotelDetailShouldRejectMissingHotel() {
        when(hotelMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getHotelDetail(1L)).isInstanceOf(BusinessException.class);
    }

    private Hotel hotel(Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setHotelName("Hotel " + id);
        hotel.setCity("Shanghai");
        hotel.setDistrict("Pudong");
        hotel.setAddress("Address");
        hotel.setDescription("Desc");
        hotel.setStarLevel(5);
        hotel.setCoverImage("/cover.jpg");
        hotel.setDetailImages("/a.jpg,/b.jpg");
        hotel.setCheckInTime("14:00");
        hotel.setCheckOutTime("12:00");
        hotel.setStatus(1);
        return hotel;
    }

    private HotelRoom room(Long hotelId, BigDecimal price, Integer stock) {
        HotelRoom room = new HotelRoom();
        room.setHotelId(hotelId);
        room.setRoomName("Deluxe");
        room.setPrice(price);
        room.setStock(stock);
        room.setStatus(1);
        return room;
    }
}
