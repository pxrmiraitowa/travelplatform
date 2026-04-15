package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.SharePost;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.SharePostMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.service.admin.AdminContentManageService;
import com.travelplatform.vo.admin.content.AdminReviewListItemVO;
import com.travelplatform.vo.admin.content.AdminShareListItemVO;
import com.travelplatform.vo.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminContentManageServiceImpl implements AdminContentManageService {

    private static final int STATUS_HIDDEN = 2;

    private final SharePostMapper sharePostMapper;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;

    public AdminContentManageServiceImpl(SharePostMapper sharePostMapper,
                                         ReviewMapper reviewMapper,
                                         UserMapper userMapper,
                                         OrdersMapper ordersMapper) {
        this.sharePostMapper = sharePostMapper;
        this.reviewMapper = reviewMapper;
        this.userMapper = userMapper;
        this.ordersMapper = ordersMapper;
    }

    @Override
    public PageResult<AdminShareListItemVO> listShares(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);
        Page<SharePost> page = sharePostMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<SharePost>()
                        .eq(status != null, SharePost::getStatus, status)
                        .orderByDesc(SharePost::getId));
        Map<Long, User> userMap = loadUsers(page.getRecords().stream().map(SharePost::getUserId).distinct().toList());

        PageResult<AdminShareListItemVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .filter(post -> !StringUtils.hasText(keyword) || contains(post.getTitle(), keyword) || contains(post.getSummary(), keyword))
                .map(post -> {
                    AdminShareListItemVO vo = new AdminShareListItemVO();
                    vo.setId(post.getId());
                    vo.setTitle(post.getTitle());
                    User user = userMap.get(post.getUserId());
                    vo.setAuthorNickname(user == null ? "未知用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername()));
                    vo.setStatus(post.getStatus());
                    vo.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
                    vo.setCreateTime(post.getCreateTime());
                    return vo;
                }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @Override
    public void deleteShare(Long id) {
        SharePost post = sharePostMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分享内容不存在");
        }
        post.setStatus(STATUS_HIDDEN);
        sharePostMapper.updateById(post);
    }

    @Override
    public PageResult<AdminReviewListItemVO> listReviews(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);
        Page<Review> page = reviewMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(StringUtils.hasText(bizType), Review::getBizType, bizType)
                        .eq(status != null, Review::getStatus, status)
                        .orderByDesc(Review::getId));
        Map<Long, User> userMap = loadUsers(page.getRecords().stream().map(Review::getUserId).distinct().toList());
        Map<Long, Orders> orderMap = loadOrders(page.getRecords().stream().map(Review::getOrderId).distinct().toList());

        PageResult<AdminReviewListItemVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .filter(review -> !StringUtils.hasText(keyword) || contains(review.getContent(), keyword))
                .map(review -> {
                    AdminReviewListItemVO vo = new AdminReviewListItemVO();
                    vo.setId(review.getId());
                    vo.setOrderId(review.getOrderId());
                    Orders order = orderMap.get(review.getOrderId());
                    vo.setOrderNo(order == null ? "" : order.getOrderNo());
                    vo.setBizType(review.getBizType());
                    User user = userMap.get(review.getUserId());
                    vo.setAuthorNickname(user == null ? "未知用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername()));
                    vo.setRating(review.getRating());
                    vo.setContent(review.getContent());
                    vo.setStatus(review.getStatus());
                    vo.setCreateTime(review.getCreateTime());
                    return vo;
                }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @Override
    public void deleteReview(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价内容不存在");
        }
        review.setStatus(STATUS_HIDDEN);
        reviewMapper.updateById(review);
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<Long, Orders> loadOrders(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>().in(Orders::getId, orderIds))
                .stream().collect(Collectors.toMap(Orders::getId, order -> order));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword.trim());
    }
}
