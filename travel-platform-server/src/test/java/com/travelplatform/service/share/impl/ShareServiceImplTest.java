package com.travelplatform.service.share.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.share.SharePostCreateRequest;
import com.travelplatform.entity.ShareImage;
import com.travelplatform.entity.SharePost;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.ShareImageMapper;
import com.travelplatform.mapper.SharePostMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.media.MediaUploadService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.share.SharePostDetailVO;
import com.travelplatform.vo.share.SharePostListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareServiceImplTest {

    @Mock SharePostMapper sharePostMapper;
    @Mock ShareImageMapper shareImageMapper;
    @Mock UserMapper userMapper;
    @Mock MediaUploadService mediaUploadService;
    @InjectMocks ShareServiceImpl service;

    @Test
    void uploadShareImageShouldDelegateToMediaService() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2});
        when(mediaUploadService.uploadImage("share", file)).thenReturn("/uploads/share/photo.jpg");

        String result = service.uploadShareImage(file);

        assertThat(result).isEqualTo("/uploads/share/photo.jpg");
    }

    @Test
    void createSharePostShouldRejectEmptyImageListAfterFiltering() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            SharePostCreateRequest request = baseRequest();
            request.setImageUrls(List.of(" ", ""));

            assertThatThrownBy(() -> service.createSharePost(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void createSharePostShouldTrimDeduplicateAndPersistImages() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            SharePostCreateRequest request = baseRequest();
            request.setImageUrls(List.of(" /img/1.jpg ", "/img/2.jpg", "/img/1.jpg"));

            ArgumentCaptor<SharePost> postCaptor = ArgumentCaptor.forClass(SharePost.class);
            when(sharePostMapper.insert(postCaptor.capture())).thenAnswer(invocation -> {
                postCaptor.getValue().setId(10L);
                return 1;
            });
            ArgumentCaptor<ShareImage> imageCaptor = ArgumentCaptor.forClass(ShareImage.class);

            Long result = service.createSharePost(request);

            assertThat(result).isEqualTo(10L);
            assertThat(postCaptor.getValue().getTitle()).isEqualTo("Trip Title");
            assertThat(postCaptor.getValue().getCoverImage()).isEqualTo("/img/1.jpg");
            verify(shareImageMapper, times(2)).insert(imageCaptor.capture());
            assertThat(imageCaptor.getAllValues()).extracting(ShareImage::getImageUrl)
                    .containsExactly("/img/1.jpg", "/img/2.jpg");
        }
    }

    @Test
    void listPublicSharesShouldAssembleListItems() {
        SharePost post = post(10L, 1L, 1, null);
        Page<SharePost> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(post));
        when(sharePostMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L, "demo", "Demo", "/avatar.png")));
        when(shareImageMapper.selectList(any())).thenReturn(List.of(image(10L, "/img/1.jpg"), image(10L, "/img/2.jpg")));

        PageResult<SharePostListItemVO> result = service.listPublicShares(1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getAuthorNickname()).isEqualTo("Demo");
        assertThat(result.getRecords().get(0).getImageCount()).isEqualTo(2);
        assertThat(result.getRecords().get(0).getViewCount()).isEqualTo(0);
    }

    @Test
    void listCurrentUserSharesShouldUseCurrentUserContext() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Page<SharePost> page = new Page<>(1, 10, 0);
            page.setRecords(List.of());
            when(sharePostMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<SharePostListItemVO> result = service.listCurrentUserShares(1, 10);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Test
    void getPublicShareDetailShouldIncreaseViewCountAndReturnImages() {
        SharePost post = post(10L, 1L, 1, 2);
        when(sharePostMapper.selectById(10L)).thenReturn(post);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "", "/avatar.png"));
        when(shareImageMapper.selectList(any())).thenReturn(List.of(image(10L, "/img/1.jpg"), image(10L, "/img/2.jpg")));

        SharePostDetailVO result = service.getPublicShareDetail(10L);

        verify(sharePostMapper).updateById(post);
        assertThat(post.getViewCount()).isEqualTo(3);
        assertThat(result.getAuthorNickname()).isEqualTo("demo");
        assertThat(result.getImageUrls()).containsExactly("/img/1.jpg", "/img/2.jpg");
    }

    @Test
    void getPublicShareDetailShouldRejectInvisiblePost() {
        when(sharePostMapper.selectById(10L)).thenReturn(post(10L, 1L, 0, 2));

        assertThatThrownBy(() -> service.getPublicShareDetail(10L)).isInstanceOf(BusinessException.class);
    }

    private SharePostCreateRequest baseRequest() {
        SharePostCreateRequest request = new SharePostCreateRequest();
        request.setTitle(" Trip Title ");
        request.setSummary(" Summary ");
        request.setContent(" Content ");
        request.setImageUrls(List.of("/img/1.jpg"));
        return request;
    }

    private SharePost post(Long id, Long userId, Integer status, Integer viewCount) {
        SharePost post = new SharePost();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle("Trip Title");
        post.setSummary("Summary");
        post.setContent("Content");
        post.setCoverImage("/img/1.jpg");
        post.setStatus(status);
        post.setViewCount(viewCount);
        post.setLikeCount(0);
        post.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return post;
    }

    private ShareImage image(Long postId, String imageUrl) {
        ShareImage image = new ShareImage();
        image.setPostId(postId);
        image.setImageUrl(imageUrl);
        return image;
    }

    private User user(Long id, String username, String nickname, String avatar) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }
}
