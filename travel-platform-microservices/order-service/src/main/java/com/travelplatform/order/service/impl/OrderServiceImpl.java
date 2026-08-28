package com.travelplatform.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.dto.OrderCreateRequest;
import com.travelplatform.order.dto.OrderRefundRequest;
import com.travelplatform.order.entity.Order;
import com.travelplatform.order.integration.CouponSettlement;
import com.travelplatform.order.integration.ProductSnapshot;
import com.travelplatform.order.integration.ProductSnapshotClient;
import com.travelplatform.order.mapper.OrderMapper;
import com.travelplatform.order.service.OrderService;
import com.travelplatform.order.vo.OrderVO;
import com.travelplatform.order.vo.ReviewContextVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final OrderMapper orderMapper;
    private final ProductSnapshotClient productClient;

    public OrderServiceImpl(OrderMapper orderMapper, ProductSnapshotClient productClient) {
        this.orderMapper = orderMapper;
        this.productClient = productClient;
    }

    @Override
    @Transactional
    public OrderVO create(Long userId, OrderCreateRequest request) {
        ProductSnapshot snapshot = productClient.getSnapshot(request);
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (!snapshot.available() || snapshot.stock() == null || snapshot.stock() < quantity) {
            throw badRequest("商品库存不足或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        String productType = request.getProductType().trim().toUpperCase(Locale.ROOT);
        BigDecimal originalAmount = snapshot.price().multiply(BigDecimal.valueOf(quantity));
        CouponSettlement settlement = productClient.settleCoupon(productType, request.getCouponId(), originalAmount);
        Order order = new Order();
        order.setOrderNo("TP" + now.format(ORDER_TIME) + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUserId(userId);
        order.setBizType(productType);
        order.setBizId(request.getProductId());
        order.setVariantId(request.getVariantId());
        order.setVariantName(request.getVariantName());
        order.setProductName(snapshot.productName());
        order.setProductSummary(snapshot.summary());
        order.setUnitPrice(snapshot.price());
        order.setQuantity(quantity);
        order.setOriginalAmount(settlement.originalAmount());
        order.setDiscountAmount(settlement.discountAmount());
        order.setTotalAmount(settlement.payableAmount());
        order.setCouponId(settlement.couponId());
        order.setCouponName(settlement.couponName());
        order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setTravelDate(request.getTravelDate());
        order.setContactName(request.getContactName().trim());
        order.setContactPhone(request.getContactPhone().trim());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        productClient.deductStock(productType, request.getProductId(), request.getVariantId(), request.getVariantName(), quantity);
        try {
            orderMapper.insert(order);
        } catch (RuntimeException exception) {
            try {
                productClient.restoreStock(productType, request.getProductId(), request.getVariantId(),
                        request.getVariantName(), quantity);
            } catch (RuntimeException compensationFailure) {
                LOGGER.error("Order creation and stock compensation both failed: productType={}, productId={}, quantity={}",
                        productType, request.getProductId(), quantity, compensationFailure);
                exception.addSuppressed(compensationFailure);
            }
            throw exception;
        }
        return OrderVO.from(order);
    }

    @Override
    public PageResult<OrderVO> page(Long userId, String bizType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                .eq(org.springframework.util.StringUtils.hasText(bizType), Order::getBizType,
                        org.springframework.util.StringUtils.hasText(bizType) ? bizType.trim().toUpperCase(Locale.ROOT) : null)
                .eq(status != null, Order::getOrderStatus, status).orderByDesc(Order::getId);
        IPage<Order> result = orderMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return pageResult(result, result.getRecords().stream().map(OrderVO::from).toList());
    }

    @Override public OrderVO detail(Long userId, Long orderId) { return OrderVO.from(owned(orderId, userId)); }

    @Override
    @Transactional
    public OrderVO pay(Long userId, Long orderId) {
        Order order = owned(orderId, userId);
        requireStatus(order, OrderStatusConstant.PENDING_PAYMENT, "只有待支付订单可以支付");
        order.setOrderStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
        order.setPaidAt(LocalDateTime.now());
        update(order);
        return OrderVO.from(order);
    }

    @Override
    @Transactional
    public OrderVO cancel(Long userId, Long orderId) {
        Order order = owned(orderId, userId);
        requireStatus(order, OrderStatusConstant.PENDING_PAYMENT, "只有待支付订单可以取消，已支付订单请申请退款");
        order.setOrderStatus(OrderStatusConstant.CANCELLED);
        update(order);
        restoreStock(order);
        return OrderVO.from(order);
    }

    @Override
    @Transactional
    public OrderVO refund(Long userId, Long orderId, OrderRefundRequest request) {
        Order order = owned(orderId, userId);
        requireStatus(order, OrderStatusConstant.PAID_PENDING_TRAVEL, "只有已支付待出行订单可以退款");
        order.setOrderStatus(OrderStatusConstant.REFUNDED);
        order.setRefundReason(request.getReason().trim());
        order.setRefundedAt(LocalDateTime.now());
        update(order);
        restoreStock(order);
        return OrderVO.from(order);
    }

    @Override
    @Transactional
    public OrderVO complete(Long userId, Long orderId) {
        Order order = owned(orderId, userId);
        requireStatus(order, OrderStatusConstant.PAID_PENDING_TRAVEL, "只有已支付订单可以完成");
        order.setOrderStatus(OrderStatusConstant.COMPLETED);
        update(order);
        return OrderVO.from(order);
    }

    @Override
    public ReviewContextVO reviewContext(Long orderId, Long userId) {
        return toReviewContext(owned(orderId, userId));
    }

    @Override
    public PageResult<ReviewContextVO> reviewable(Long userId, int pageNum, int pageSize) {
        IPage<Order> result = orderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                        .eq(Order::getOrderStatus, OrderStatusConstant.COMPLETED).orderByDesc(Order::getId));
        return pageResult(result, result.getRecords().stream().map(this::toReviewContext).toList());
    }

    private Order owned(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问该订单");
        return order;
    }

    private ReviewContextVO toReviewContext(Order order) {
        return new ReviewContextVO(order.getId(), order.getOrderNo(), order.getUserId(), order.getBizType(),
                order.getBizId(), order.getTravelDate(), order.getProductName(), order.getProductSummary(),
                order.getOrderStatus() == OrderStatusConstant.COMPLETED);
    }

    private void update(Order order) { order.setUpdatedAt(LocalDateTime.now()); orderMapper.updateById(order); }
    private void restoreStock(Order order) {
        int quantity = order.getQuantity() == null ? 1 : order.getQuantity();
        productClient.restoreStock(order.getBizType(), order.getBizId(), order.getVariantId(),
                order.getVariantName(), quantity);
    }
    private void requireStatus(Order order, int expected, String message) {
        if (order.getOrderStatus() != expected) throw badRequest(message);
    }
    private BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
    private <T> PageResult<T> pageResult(IPage<?> source, List<T> records) {
        PageResult<T> result = new PageResult<>(); result.setRecords(records); result.setTotal(source.getTotal());
        result.setPageNum((int) source.getCurrent()); result.setPageSize((int) source.getSize()); return result;
    }
}
