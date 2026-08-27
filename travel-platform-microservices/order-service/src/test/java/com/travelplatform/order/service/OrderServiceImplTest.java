package com.travelplatform.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.order.dto.OrderCreateRequest;
import com.travelplatform.order.dto.OrderRefundRequest;
import com.travelplatform.order.entity.Order;
import com.travelplatform.order.integration.ProductSnapshot;
import com.travelplatform.order.integration.ProductSnapshotClient;
import com.travelplatform.order.mapper.OrderMapper;
import com.travelplatform.order.service.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrderServiceImplTest {
    @Mock private OrderMapper orderMapper;
    @Mock private ProductSnapshotClient productClient;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OrderServiceImpl(orderMapper, productClient);
    }

    @Test
    void createsOrderFromServerSideProductSnapshot() {
        OrderCreateRequest request = request();
        when(productClient.getSnapshot(request)).thenReturn(new ProductSnapshot(8L, "测试航班", "广州 → 上海",
                new BigDecimal("680.00"), true, 5));

        service.create(3L, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order saved = captor.getValue();
        assertEquals(new BigDecimal("1360.00"), saved.getTotalAmount());
        assertEquals(OrderStatusConstant.PENDING_PAYMENT, saved.getOrderStatus());
        assertEquals(3L, saved.getUserId());
    }

    @Test
    void rejectsClientWhenStockIsInsufficient() {
        OrderCreateRequest request = request();
        when(productClient.getSnapshot(request)).thenReturn(new ProductSnapshot(8L, "测试航班", "广州 → 上海",
                new BigDecimal("680.00"), true, 1));
        assertThrows(BusinessException.class, () -> service.create(3L, request));
    }

    @Test
    void paysOnlyOwnedPendingOrder() {
        Order order = new Order(); order.setId(1L); order.setUserId(3L);
        order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
        when(orderMapper.selectById(1L)).thenReturn(order);

        service.pay(3L, 1L);

        assertEquals(OrderStatusConstant.PAID_PENDING_TRAVEL, order.getOrderStatus());
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    void rejectsCrossUserAccess() {
        Order order = new Order(); order.setId(1L); order.setUserId(9L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> service.detail(3L, 1L));
    }

    @Test
    void refundsOnlyPaidOrder() {
        Order order = new Order(); order.setId(2L); order.setUserId(3L);
        order.setOrderStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
        when(orderMapper.selectById(2L)).thenReturn(order);
        OrderRefundRequest request = new OrderRefundRequest(); request.setReason("行程变更");

        service.refund(3L, 2L, request);

        assertEquals(OrderStatusConstant.REFUNDED, order.getOrderStatus());
        assertEquals("行程变更", order.getRefundReason());
    }

    private OrderCreateRequest request() {
        OrderCreateRequest request = new OrderCreateRequest(); request.setProductType("FLIGHT");
        request.setProductId(8L); request.setQuantity(2); request.setContactName("张三");
        request.setContactPhone("13800000000"); request.setTravelDate(LocalDate.now().plusDays(7)); return request;
    }
}
