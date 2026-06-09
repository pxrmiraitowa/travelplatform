package com.travelplatform.service.admin.impl;

import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.admin.order.AdminOrderStatusUpdateRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderHotel;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.vo.admin.order.AdminOrderDetailVO;
import com.travelplatform.vo.admin.order.AdminOrderListItemVO;
import com.travelplatform.vo.common.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderManageServiceImplTest {

    @Mock OrdersMapper ordersMapper;
    @Mock OrderFlightMapper orderFlightMapper;
    @Mock OrderTrainMapper orderTrainMapper;
    @Mock OrderHotelMapper orderHotelMapper;
    @Mock OrderTourMapper orderTourMapper;
    @Mock UserMapper userMapper;
    @Mock FlightMapper flightMapper;
    @Mock TrainTicketMapper trainTicketMapper;
    @Mock HotelRoomMapper hotelRoomMapper;
    @Mock TourPackageMapper tourPackageMapper;
    @InjectMocks AdminOrderManageServiceImpl service;

    @Test
    void listOrdersShouldFilterByKeywordAfterMappingFlightSummary() {
        Orders flightOrder = order(1L, OrderBizTypeConstant.FLIGHT, 1L);
        Orders hotelOrder = order(2L, OrderBizTypeConstant.HOTEL, 2L);
        when(ordersMapper.selectList(any())).thenReturn(List.of(flightOrder, hotelOrder));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "Demo"));
        when(userMapper.selectById(2L)).thenReturn(user(2L, "alice", "Alice"));
        when(orderFlightMapper.selectOne(any())).thenReturn(flightDetail(1L));
        when(orderHotelMapper.selectOne(any())).thenReturn(hotelDetail(2L));

        PageResult<AdminOrderListItemVO> result = service.listOrders("MU1001", null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getOrderNo()).isEqualTo("ORD-1");
        assertThat(result.getRecords().get(0).getSummaryTitle()).isEqualTo("Shanghai -> Beijing");
    }

    @Test
    void getOrderDetailShouldPopulateFlightAndUserInfo() {
        Orders order = order(1L, OrderBizTypeConstant.FLIGHT, 1L);
        when(ordersMapper.selectById(1L)).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "Demo"));
        when(orderFlightMapper.selectOne(any())).thenReturn(flightDetail(1L));

        AdminOrderDetailVO result = service.getOrderDetail(1L);

        assertThat(result.getUsername()).isEqualTo("demo");
        assertThat(result.getFlightInfo()).isNotNull();
        assertThat(result.getFlightInfo().getFlightNo()).isEqualTo("MU1001");
        assertThat(result.getHotelInfo()).isNull();
    }

    @Test
    void updateOrderStatusShouldSyncFlightDetailStatus() {
        Orders order = order(1L, OrderBizTypeConstant.FLIGHT, 1L);
        when(ordersMapper.selectById(1L)).thenReturn(order);
        OrderFlight detail = flightDetail(1L);
        when(orderFlightMapper.selectOne(any())).thenReturn(detail);

        AdminOrderStatusUpdateRequest request = new AdminOrderStatusUpdateRequest();
        request.setOrderStatus(OrderStatusConstant.COMPLETED);

        service.updateOrderStatus(1L, request);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatusConstant.COMPLETED);
        assertThat(detail.getStatus()).isEqualTo(OrderStatusConstant.COMPLETED);
        verify(ordersMapper).updateById(order);
        verify(orderFlightMapper).updateById(detail);
    }

    @Test
    void updateOrderStatusShouldRejectRecoveryFromCancelled() {
        Orders order = order(1L, OrderBizTypeConstant.FLIGHT, 1L);
        order.setOrderStatus(OrderStatusConstant.CANCELLED);
        when(ordersMapper.selectById(1L)).thenReturn(order);

        AdminOrderStatusUpdateRequest request = new AdminOrderStatusUpdateRequest();
        request.setOrderStatus(OrderStatusConstant.COMPLETED);

        assertThatThrownBy(() -> service.updateOrderStatus(1L, request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelOrderShouldCancelHotelOrderAndRestoreRoomStock() {
        Orders order = order(2L, OrderBizTypeConstant.HOTEL, 2L);
        order.setOrderStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
        OrderHotel detail = hotelDetail(2L);
        HotelRoom room = new HotelRoom();
        room.setId(31L);
        room.setStock(2);
        when(ordersMapper.selectById(2L)).thenReturn(order);
        when(orderHotelMapper.selectOne(any())).thenReturn(detail);
        when(hotelRoomMapper.selectById(31L)).thenReturn(room);

        service.cancelOrder(2L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatusConstant.CANCELLED);
        assertThat(detail.getStatus()).isEqualTo(OrderStatusConstant.CANCELLED);
        assertThat(room.getStock()).isEqualTo(3);
        verify(hotelRoomMapper).updateById(room);
    }

    @Test
    void cancelOrderShouldRejectMissingOrder() {
        when(ordersMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelOrder(99L)).isInstanceOf(BusinessException.class);
    }

    private Orders order(Long id, String bizType, Long userId) {
        Orders order = new Orders();
        order.setId(id);
        order.setOrderNo("ORD-" + id);
        order.setUserId(userId);
        order.setBizType(bizType);
        order.setBizId(11L);
        order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("600"));
        order.setContactName("Tom");
        order.setContactPhone("13900000000");
        order.setTravelDate(LocalDate.now().plusDays(3));
        order.setRemark("remark");
        order.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return order;
    }

    private User user(Long id, String username, String nickname) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }

    private OrderFlight flightDetail(Long orderId) {
        OrderFlight detail = new OrderFlight();
        detail.setOrderId(orderId);
        detail.setFlightId(11L);
        detail.setFlightNo("MU1001");
        detail.setAirlineName("China Eastern");
        detail.setDepartureCity("Shanghai");
        detail.setArrivalCity("Beijing");
        detail.setDepartureAirport("PVG");
        detail.setArrivalAirport("PEK");
        detail.setDepartureTime(LocalDateTime.now().plusDays(5));
        detail.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
        detail.setPassengerName("Tom");
        detail.setPassengerPhone("13900000000");
        detail.setPassengerIdCard("110101199001011234");
        detail.setTicketPrice(new BigDecimal("600"));
        detail.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        return detail;
    }

    private OrderHotel hotelDetail(Long orderId) {
        OrderHotel detail = new OrderHotel();
        detail.setOrderId(orderId);
        detail.setHotelId(21L);
        detail.setHotelRoomId(31L);
        detail.setHotelName("Lake Hotel");
        detail.setCity("Hangzhou");
        detail.setAddress("West Lake");
        detail.setRoomName("Deluxe");
        detail.setBedType("King");
        detail.setBreakfast("Included");
        detail.setCheckInDate(LocalDate.now().plusDays(2));
        detail.setCheckOutDate(LocalDate.now().plusDays(3));
        detail.setGuestName("Tom");
        detail.setGuestPhone("13900000000");
        detail.setGuestIdCard("110101199001011234");
        detail.setRoomPrice(new BigDecimal("500"));
        detail.setNightCount(1);
        detail.setStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
        return detail;
    }
}
