package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.entity.Orders;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.service.admin.AdminDashboardService;
import com.travelplatform.vo.admin.AdminDashboardVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;
    private final OrdersMapper ordersMapper;

    public AdminDashboardServiceImpl(UserMapper userMapper,
                                     FlightMapper flightMapper,
                                     TrainTicketMapper trainTicketMapper,
                                     HotelMapper hotelMapper,
                                     HotelRoomMapper hotelRoomMapper,
                                     TourPackageMapper tourPackageMapper,
                                     OrdersMapper ordersMapper) {
        this.userMapper = userMapper;
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
        this.ordersMapper = ordersMapper;
    }

    @Override
    public AdminDashboardVO getDashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setUserCount(userMapper.selectCount(null));
        vo.setProductCount(flightMapper.selectCount(null)
                + trainTicketMapper.selectCount(null)
                + hotelMapper.selectCount(null)
                + hotelRoomMapper.selectCount(null)
                + tourPackageMapper.selectCount(null));
        vo.setOrderCount(ordersMapper.selectCount(null));
        vo.setRecentOrderCount(ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, LocalDateTime.now().minusDays(7))));
        return vo;
    }
}
