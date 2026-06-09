package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.SharePost;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.SharePostMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.vo.admin.content.AdminReviewListItemVO;
import com.travelplatform.vo.admin.content.AdminShareListItemVO;
import com.travelplatform.vo.common.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContentManageServiceImplTest {

    @Mock SharePostMapper sharePostMapper;
    @Mock ReviewMapper reviewMapper;
    @Mock UserMapper userMapper;
    @Mock OrdersMapper ordersMapper;
    @InjectMocks AdminContentManageServiceImpl service;

    @Test
    void listSharesShouldMapAuthorAndFilterByKeyword() {
        SharePost post = sharePost(1L, 1L, "Great Trip", "Summary", 1);
        Page<SharePost> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(post));
        when(sharePostMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L, "demo", "Demo")));

        PageResult<AdminShareListItemVO> result = service.listShares("Great", null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getAuthorNickname()).isEqualTo("Demo");
        assertThat(result.getRecords().get(0).getTitle()).isEqualTo("Great Trip");
    }

    @Test
    void deleteShareShouldHidePost() {
        SharePost post = sharePost(1L, 1L, "Great Trip", "Summary", 1);
        when(sharePostMapper.selectById(1L)).thenReturn(post);

        service.deleteShare(1L);

        assertThat(post.getStatus()).isEqualTo(2);
        verify(sharePostMapper).updateById(post);
    }

    @Test
    void deleteShareShouldRejectMissingPost() {
        when(sharePostMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteShare(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void listReviewsShouldMapOrderNoAndAuthor() {
        Review review = review(1L, 11L, 1L, "Nice review", 1);
        Page<Review> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(review));
        when(reviewMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L, "demo", "Demo")));
        when(ordersMapper.selectList(any())).thenReturn(List.of(order(11L, "ORD-11")));

        PageResult<AdminReviewListItemVO> result = service.listReviews("Nice", null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getOrderNo()).isEqualTo("ORD-11");
        assertThat(result.getRecords().get(0).getAuthorNickname()).isEqualTo("Demo");
    }

    @Test
    void deleteReviewShouldHideReview() {
        Review review = review(1L, 11L, 1L, "Nice review", 1);
        when(reviewMapper.selectById(1L)).thenReturn(review);

        service.deleteReview(1L);

        assertThat(review.getStatus()).isEqualTo(2);
        verify(reviewMapper).updateById(review);
    }

    @Test
    void deleteReviewShouldRejectMissingReview() {
        when(reviewMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteReview(1L)).isInstanceOf(BusinessException.class);
    }

    private User user(Long id, String username, String nickname) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }

    private SharePost sharePost(Long id, Long userId, String title, String summary, Integer status) {
        SharePost post = new SharePost();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setSummary(summary);
        post.setStatus(status);
        post.setViewCount(8);
        post.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return post;
    }

    private Review review(Long id, Long orderId, Long userId, String content, Integer status) {
        Review review = new Review();
        review.setId(id);
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setBizType("FLIGHT");
        review.setRating(5);
        review.setContent(content);
        review.setStatus(status);
        review.setCreateTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        return review;
    }

    private Orders order(Long id, String orderNo) {
        Orders order = new Orders();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }
}
