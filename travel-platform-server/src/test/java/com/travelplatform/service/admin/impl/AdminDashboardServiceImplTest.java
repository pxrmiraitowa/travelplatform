package com.travelplatform.service.admin.impl;

import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.vo.admin.AdminDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock FlightMapper flightMapper;
    @Mock TrainTicketMapper trainTicketMapper;
    @Mock HotelMapper hotelMapper;
    @Mock HotelRoomMapper hotelRoomMapper;
    @Mock TourPackageMapper tourPackageMapper;
    @Mock OrdersMapper ordersMapper;
    @InjectMocks AdminDashboardServiceImpl service;

    @Test
    void getDashboardShouldAggregateCounts() {
        when(userMapper.selectCount(null)).thenReturn(10L);
        when(flightMapper.selectCount(null)).thenReturn(2L);
        when(trainTicketMapper.selectCount(null)).thenReturn(3L);
        when(hotelMapper.selectCount(null)).thenReturn(4L);
        when(hotelRoomMapper.selectCount(null)).thenReturn(5L);
        when(tourPackageMapper.selectCount(null)).thenReturn(6L);
        when(ordersMapper.selectCount(isNull())).thenReturn(7L);
        when(ordersMapper.selectCount(argThat(wrapper -> wrapper != null))).thenReturn(8L);

        AdminDashboardVO result = service.getDashboard();

        assertThat(result.getUserCount()).isEqualTo(10L);
        assertThat(result.getProductCount()).isEqualTo(20L);
        assertThat(result.getOrderCount()).isEqualTo(7L);
        assertThat(result.getRecentOrderCount()).isEqualTo(8L);
    }
}
