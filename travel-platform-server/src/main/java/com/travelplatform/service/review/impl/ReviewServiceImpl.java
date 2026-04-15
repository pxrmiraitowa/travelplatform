package com.travelplatform.service.review.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.review.ReviewCreateRequest;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderHotel;
import com.travelplatform.entity.OrderTour;
import com.travelplatform.entity.OrderTrain;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.review.ReviewService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.review.ReviewVO;
import com.travelplatform.vo.review.ReviewableOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int STATUS_VISIBLE = 1;

    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;
    private final OrderFlightMapper orderFlightMapper;
    private final OrderTrainMapper orderTrainMapper;
    private final OrderHotelMapper orderHotelMapper;
    private final OrderTourMapper orderTourMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             OrdersMapper ordersMapper,
                             UserMapper userMapper,
                             OrderFlightMapper orderFlightMapper,
                             OrderTrainMapper orderTrainMapper,
                             OrderHotelMapper orderHotelMapper,
                             OrderTourMapper orderTourMapper) {
        this.reviewMapper = reviewMapper;
        this.ordersMapper = ordersMapper;
        this.userMapper = userMapper;
        this.orderFlightMapper = orderFlightMapper;
        this.orderTrainMapper = orderTrainMapper;
        this.orderHotelMapper = orderHotelMapper;
        this.orderTourMapper = orderTourMapper;
    }

    @Override
    @Transactional
    public ReviewVO createReview(ReviewCreateRequest request) {
        Orders order = getOwnedCompletedOrder(request.getOrderId());
        Review existing = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId())
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单已评价");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setUserId(order.getUserId());
        review.setBizType(order.getBizType());
        review.setBizId(order.getBizId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        review.setStatus(STATUS_VISIBLE);
        reviewMapper.insert(review);
        return toReviewVO(review, userMapper.selectById(review.getUserId()));
    }

    @Override
    public PageResult<ReviewableOrderVO> listReviewableOrders(Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);

        Page<Orders> page = ordersMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .eq(Orders::getOrderStatus, OrderStatusConstant.COMPLETED)
                        .notInSql(Orders::getId, "select order_id from review")
                        .orderByDesc(Orders::getId));

        PageResult<ReviewableOrderVO> result = new PageResult<>();
        result.setRecords(buildReviewableOrderVOs(page.getRecords()));
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @Override
    public ReviewVO getCurrentUserOrderReview(Long orderId) {
        Orders order = getOwnedOrder(orderId);
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId())
                .last("limit 1"));
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "当前订单暂无评价");
        }
        return toReviewVO(review, userMapper.selectById(review.getUserId()));
    }

    @Override
    public ReviewVO getVisibleOrderReview(Long orderId) {
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getStatus, STATUS_VISIBLE)
                .last("limit 1"));
        if (review == null) {
            return null;
        }
        return toReviewVO(review, userMapper.selectById(review.getUserId()));
    }

    private List<ReviewableOrderVO> buildReviewableOrderVOs(List<Orders> orders) {
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, OrderFlight> flightMap = loadMap(orderFlightMapper.selectList(new LambdaQueryWrapper<OrderFlight>().in(!orderIds.isEmpty(), OrderFlight::getOrderId, orderIds)), OrderFlight::getOrderId);
        Map<Long, OrderTrain> trainMap = loadMap(orderTrainMapper.selectList(new LambdaQueryWrapper<OrderTrain>().in(!orderIds.isEmpty(), OrderTrain::getOrderId, orderIds)), OrderTrain::getOrderId);
        Map<Long, OrderHotel> hotelMap = loadMap(orderHotelMapper.selectList(new LambdaQueryWrapper<OrderHotel>().in(!orderIds.isEmpty(), OrderHotel::getOrderId, orderIds)), OrderHotel::getOrderId);
        Map<Long, OrderTour> tourMap = loadMap(orderTourMapper.selectList(new LambdaQueryWrapper<OrderTour>().in(!orderIds.isEmpty(), OrderTour::getOrderId, orderIds)), OrderTour::getOrderId);

        return orders.stream().map(order -> {
            ReviewableOrderVO vo = new ReviewableOrderVO();
            vo.setOrderId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setBizType(order.getBizType());
            vo.setTravelDate(order.getTravelDate());
            fillSummary(vo, order, flightMap.get(order.getId()), trainMap.get(order.getId()), hotelMap.get(order.getId()), tourMap.get(order.getId()));
            return vo;
        }).toList();
    }

    private void fillSummary(ReviewableOrderVO vo,
                             Orders order,
                             OrderFlight flight,
                             OrderTrain train,
                             OrderHotel hotel,
                             OrderTour tour) {
        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType()) && flight != null) {
            vo.setSummaryTitle(flight.getDepartureCity() + " -> " + flight.getArrivalCity());
            vo.setSummarySubtitle(flight.getFlightNo() + " | " + flight.getAirlineName());
        } else if (OrderBizTypeConstant.TRAIN.equals(order.getBizType()) && train != null) {
            vo.setSummaryTitle(train.getDepartureCity() + " -> " + train.getArrivalCity());
            vo.setSummarySubtitle(train.getTrainNo() + " | " + train.getSeatType());
        } else if (OrderBizTypeConstant.HOTEL.equals(order.getBizType()) && hotel != null) {
            vo.setSummaryTitle(hotel.getHotelName());
            vo.setSummarySubtitle(hotel.getRoomName() + " | " + hotel.getCheckInDate());
        } else if (OrderBizTypeConstant.TOUR.equals(order.getBizType()) && tour != null) {
            vo.setSummaryTitle(tour.getPackageName());
            vo.setSummarySubtitle(tour.getDestination() + " | " + tour.getTravelDate());
        } else {
            vo.setSummaryTitle("订单信息");
            vo.setSummarySubtitle(order.getBizType());
        }
    }

    private Orders getOwnedOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !SecurityUtils.getCurrentUserId().equals(order.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private Orders getOwnedCompletedOrder(Long orderId) {
        Orders order = getOwnedOrder(orderId);
        if (!Integer.valueOf(OrderStatusConstant.COMPLETED).equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持对已完成订单进行评价");
        }
        return order;
    }

    private ReviewVO toReviewVO(Review review, User user) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setBizType(review.getBizType());
        vo.setBizId(review.getBizId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setAuthorNickname(user == null ? "未知用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername()));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private <T> Map<Long, T> loadMap(List<T> source, Function<T, Long> keyMapper) {
        return source.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }
}
