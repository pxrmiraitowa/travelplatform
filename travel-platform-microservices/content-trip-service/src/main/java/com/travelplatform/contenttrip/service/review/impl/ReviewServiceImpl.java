package com.travelplatform.contenttrip.service.review.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.dto.review.ReviewCreateRequest;
import com.travelplatform.contenttrip.entity.Review;
import com.travelplatform.contenttrip.mapper.ReviewMapper;
import com.travelplatform.contenttrip.security.CurrentUserProvider;
import com.travelplatform.contenttrip.service.order.OrderReviewClient;
import com.travelplatform.contenttrip.service.order.OrderReviewContext;
import com.travelplatform.contenttrip.service.review.ReviewService;
import com.travelplatform.contenttrip.service.user.UserBasicClient;
import com.travelplatform.contenttrip.service.user.UserBasicInfo;
import com.travelplatform.contenttrip.vo.review.ReviewVO;
import com.travelplatform.contenttrip.vo.review.ReviewableOrderVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int STATUS_VISIBLE = 1;

    private final ReviewMapper reviewMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OrderReviewClient orderReviewClient;
    private final UserBasicClient userBasicClient;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             CurrentUserProvider currentUserProvider,
                             OrderReviewClient orderReviewClient,
                             UserBasicClient userBasicClient) {
        this.reviewMapper = reviewMapper;
        this.currentUserProvider = currentUserProvider;
        this.orderReviewClient = orderReviewClient;
        this.userBasicClient = userBasicClient;
    }

    @Override
    @Transactional
    public ReviewVO createReview(ReviewCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        OrderReviewContext order = getOwnedCompletedOrder(request.getOrderId(), userId);
        Review existing = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getOrderId())
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单已评价");
        }

        Review review = new Review();
        review.setOrderId(order.getOrderId());
        review.setUserId(order.getUserId());
        review.setBizType(order.getBizType());
        review.setBizId(order.getBizId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        review.setStatus(STATUS_VISIBLE);
        reviewMapper.insert(review);
        return toReviewVO(review, loadUser(review.getUserId()));
    }

    @Override
    public PageResult<ReviewableOrderVO> listReviewableOrders(Integer pageNum, Integer pageSize) {
        Long userId = currentUserProvider.getCurrentUserId();
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);

        PageResult<OrderReviewContext> page = orderReviewClient.listReviewableOrders(userId, safePageNum, safePageSize);
        List<Long> orderIds = page.getRecords().stream().map(OrderReviewContext::getOrderId).toList();
        List<Long> reviewedOrderIds = orderIds.isEmpty()
                ? List.of()
                : reviewMapper.selectList(new LambdaQueryWrapper<Review>().in(Review::getOrderId, orderIds))
                        .stream()
                        .map(Review::getOrderId)
                        .toList();

        PageResult<ReviewableOrderVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .filter(order -> !reviewedOrderIds.contains(order.getOrderId()))
                .map(this::toReviewableOrderVO)
                .toList());
        Long reviewedCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId));
        result.setTotal(Math.max(0, page.getTotal() - reviewedCount));
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        return result;
    }

    @Override
    public ReviewVO getCurrentUserOrderReview(Long orderId) {
        OrderReviewContext order = getOwnedOrder(orderId, currentUserProvider.getCurrentUserId());
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getOrderId())
                .last("limit 1"));
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "当前订单暂无评价");
        }
        return toReviewVO(review, loadUser(review.getUserId()));
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
        return toReviewVO(review, loadUser(review.getUserId()));
    }

    private ReviewableOrderVO toReviewableOrderVO(OrderReviewContext order) {
        ReviewableOrderVO vo = new ReviewableOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setTravelDate(order.getTravelDate());
        vo.setSummaryTitle(order.getSummaryTitle());
        vo.setSummarySubtitle(order.getSummarySubtitle());
        return vo;
    }

    private OrderReviewContext getOwnedOrder(Long orderId, Long userId) {
        OrderReviewContext order = orderReviewClient.getReviewContext(orderId, userId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private OrderReviewContext getOwnedCompletedOrder(Long orderId, Long userId) {
        OrderReviewContext order = getOwnedOrder(orderId, userId);
        if (!order.isCompleted()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持对已完成订单进行评价");
        }
        return order;
    }

    private ReviewVO toReviewVO(Review review, UserBasicInfo user) {
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

    private UserBasicInfo loadUser(Long userId) {
        return userBasicClient.listBasicUsers(List.of(userId)).get(userId);
    }
}
