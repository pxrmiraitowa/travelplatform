package com.travelplatform.contenttrip.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.entity.Review;
import com.travelplatform.contenttrip.entity.SharePost;
import com.travelplatform.contenttrip.mapper.ReviewMapper;
import com.travelplatform.contenttrip.mapper.SharePostMapper;
import com.travelplatform.contenttrip.service.admin.AdminContentManageService;
import com.travelplatform.contenttrip.service.order.OrderReviewClient;
import com.travelplatform.contenttrip.service.order.OrderReviewContext;
import com.travelplatform.contenttrip.service.user.UserBasicClient;
import com.travelplatform.contenttrip.service.user.UserBasicInfo;
import com.travelplatform.contenttrip.vo.admin.AdminReviewListItemVO;
import com.travelplatform.contenttrip.vo.admin.AdminShareListItemVO;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminContentManageServiceImpl implements AdminContentManageService {

    private static final int STATUS_HIDDEN = 2;

    private final SharePostMapper sharePostMapper;
    private final ReviewMapper reviewMapper;
    private final UserBasicClient userBasicClient;
    private final OrderReviewClient orderReviewClient;

    public AdminContentManageServiceImpl(SharePostMapper sharePostMapper,
                                         ReviewMapper reviewMapper,
                                         UserBasicClient userBasicClient,
                                         OrderReviewClient orderReviewClient) {
        this.sharePostMapper = sharePostMapper;
        this.reviewMapper = reviewMapper;
        this.userBasicClient = userBasicClient;
        this.orderReviewClient = orderReviewClient;
    }

    @Override
    public PageResult<AdminShareListItemVO> listShares(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);
        Page<SharePost> page = sharePostMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<SharePost>()
                        .eq(status != null, SharePost::getStatus, status)
                        .orderByDesc(SharePost::getId));
        Map<Long, UserBasicInfo> users = loadUsers(page.getRecords().stream().map(SharePost::getUserId).toList());

        PageResult<AdminShareListItemVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .filter(post -> !StringUtils.hasText(keyword) || contains(post.getTitle(), keyword) || contains(post.getSummary(), keyword))
                .map(post -> toShareVO(post, users))
                .toList());
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
        Map<Long, UserBasicInfo> users = loadUsers(page.getRecords().stream().map(Review::getUserId).toList());

        PageResult<AdminReviewListItemVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .filter(review -> !StringUtils.hasText(keyword) || contains(review.getContent(), keyword))
                .map(review -> toReviewVO(review, users))
                .toList());
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

    private AdminShareListItemVO toShareVO(SharePost post, Map<Long, UserBasicInfo> users) {
        AdminShareListItemVO vo = new AdminShareListItemVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setAuthorNickname(authorName(post.getUserId(), users));
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }

    private AdminReviewListItemVO toReviewVO(Review review, Map<Long, UserBasicInfo> users) {
        AdminReviewListItemVO vo = new AdminReviewListItemVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setOrderNo(loadOrderNo(review));
        vo.setBizType(review.getBizType());
        vo.setAuthorNickname(authorName(review.getUserId(), users));
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setStatus(review.getStatus());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private Map<Long, UserBasicInfo> loadUsers(Collection<Long> userIds) {
        try {
            return userBasicClient.listBasicUsers(userIds.stream().filter(id -> id != null).collect(Collectors.toSet()));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private String authorName(Long userId, Map<Long, UserBasicInfo> users) {
        UserBasicInfo user = users.get(userId);
        if (user == null) {
            return "用户#" + userId;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return StringUtils.hasText(user.getUsername()) ? user.getUsername() : "用户#" + userId;
    }

    private String loadOrderNo(Review review) {
        try {
            OrderReviewContext context = orderReviewClient.getReviewContext(review.getOrderId(), review.getUserId());
            return StringUtils.hasText(context.getOrderNo()) ? context.getOrderNo() : "订单#" + review.getOrderId();
        } catch (RuntimeException exception) {
            return "订单#" + review.getOrderId();
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword.trim());
    }
}
