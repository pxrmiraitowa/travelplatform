package com.travelplatform.order.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.dto.admin.AdminOrderStatusUpdateRequest;
import com.travelplatform.order.entity.Order;
import com.travelplatform.order.integration.ProductSnapshotClient;
import com.travelplatform.order.mapper.OrderMapper;
import com.travelplatform.order.service.admin.AdminOrderManageService;
import com.travelplatform.order.vo.admin.AdminOrderDetailVO;
import com.travelplatform.order.vo.admin.AdminOrderListItemVO;
import com.travelplatform.order.vo.admin.OrderSnapshotVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderManageServiceImpl implements AdminOrderManageService {

    private final OrderMapper orderMapper;
    private final ProductSnapshotClient productClient;

    public AdminOrderManageServiceImpl(OrderMapper orderMapper, ProductSnapshotClient productClient) {
        this.orderMapper = orderMapper;
        this.productClient = productClient;
    }

    @Override
    public PageResult<AdminOrderListItemVO> listOrders(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<Order>()
                .eq(StringUtils.hasText(bizType), Order::getBizType,
                        StringUtils.hasText(bizType) ? bizType.trim().toUpperCase(Locale.ROOT) : null)
                .eq(status != null, Order::getOrderStatus, status)
                .orderByDesc(Order::getId);
        IPage<Order> page = orderMapper.selectPage(new Page<>(safePageNum, safePageSize), query);
        List<AdminOrderListItemVO> records = page.getRecords().stream()
                .map(this::toListVO)
                .filter(item -> matchKeyword(item, keyword))
                .toList();
        return pageResult(page, records);
    }

    @Override
    public AdminOrderDetailVO getOrderDetail(Long id) {
        Order order = getOrder(id);
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setBizId(order.getBizId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setTravelDate(order.getTravelDate());
        vo.setRemark(order.getRefundReason());
        vo.setCreateTime(order.getCreatedAt());
        vo.setUserId(order.getUserId());
        vo.setUsername("user-" + order.getUserId());
        vo.setNickname("用户#" + order.getUserId());

        OrderSnapshotVO snapshot = toSnapshot(order);
        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            vo.setFlightInfo(snapshot);
        } else if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            vo.setTrainInfo(snapshot);
        } else if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            vo.setHotelInfo(snapshot);
        } else if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            vo.setTourInfo(snapshot);
        }
        return vo;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long id, AdminOrderStatusUpdateRequest request) {
        Order order = getOrder(id);
        Integer targetStatus = request.getOrderStatus();
        if (!List.of(OrderStatusConstant.PENDING_PAYMENT, OrderStatusConstant.PAID_PENDING_TRAVEL,
                OrderStatusConstant.COMPLETED, OrderStatusConstant.CANCELLED, OrderStatusConstant.REFUNDED).contains(targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单状态不合法");
        }
        if (List.of(OrderStatusConstant.CANCELLED, OrderStatusConstant.REFUNDED).contains(order.getOrderStatus())
                && !order.getOrderStatus().equals(targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已取消或已退款订单不支持再次变更状态");
        }
        int previousStatus = order.getOrderStatus();
        order.setOrderStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        if (previousStatus != targetStatus
                && List.of(OrderStatusConstant.CANCELLED, OrderStatusConstant.REFUNDED).contains(targetStatus)) {
            restoreStock(order);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = getOrder(id);
        if (order.getOrderStatus() == OrderStatusConstant.CANCELLED) {
            return;
        }
        if (!List.of(OrderStatusConstant.PENDING_PAYMENT, OrderStatusConstant.PAID_PENDING_TRAVEL).contains(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单状态不允许取消");
        }
        order.setOrderStatus(OrderStatusConstant.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreStock(order);
    }

    private AdminOrderListItemVO toListVO(Order order) {
        AdminOrderListItemVO vo = new AdminOrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setTravelDate(order.getTravelDate());
        vo.setCreateTime(order.getCreatedAt());
        vo.setUserId(order.getUserId());
        vo.setUsername("user-" + order.getUserId());
        vo.setNickname("用户#" + order.getUserId());
        vo.setSummaryTitle(order.getProductName());
        vo.setSummarySubtitle(order.getProductSummary());
        return vo;
    }

    private OrderSnapshotVO toSnapshot(Order order) {
        OrderSnapshotVO vo = new OrderSnapshotVO();
        vo.setProductId(order.getBizId());
        vo.setProductName(order.getProductName());
        vo.setProductSummary(order.getProductSummary());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setQuantity(order.getQuantity());
        vo.setTravelDate(order.getTravelDate());
        return vo;
    }

    private boolean matchKeyword(AdminOrderListItemVO item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(item.getOrderNo(), normalized)
                || contains(item.getUsername(), normalized)
                || contains(item.getNickname(), normalized)
                || contains(item.getSummaryTitle(), normalized)
                || contains(item.getSummarySubtitle(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Order getOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private void restoreStock(Order order) {
        int quantity = order.getQuantity() == null ? 1 : order.getQuantity();
        productClient.restoreStock(order.getBizType(), order.getBizId(), order.getVariantId(),
                order.getVariantName(), quantity);
    }

    private <T> PageResult<T> pageResult(IPage<?> source, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(source.getTotal());
        result.setPageNum((int) source.getCurrent());
        result.setPageSize((int) source.getSize());
        return result;
    }
}
