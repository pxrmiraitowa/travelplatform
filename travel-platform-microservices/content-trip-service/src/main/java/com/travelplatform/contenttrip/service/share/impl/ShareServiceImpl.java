package com.travelplatform.contenttrip.service.share.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.dto.share.SharePostCreateRequest;
import com.travelplatform.contenttrip.entity.ShareImage;
import com.travelplatform.contenttrip.entity.SharePost;
import com.travelplatform.contenttrip.mapper.ShareImageMapper;
import com.travelplatform.contenttrip.mapper.SharePostMapper;
import com.travelplatform.contenttrip.security.CurrentUserProvider;
import com.travelplatform.contenttrip.service.media.MediaUploadService;
import com.travelplatform.contenttrip.service.share.ShareService;
import com.travelplatform.contenttrip.service.user.UserBasicClient;
import com.travelplatform.contenttrip.service.user.UserBasicInfo;
import com.travelplatform.contenttrip.vo.share.SharePostDetailVO;
import com.travelplatform.contenttrip.vo.share.SharePostListItemVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShareServiceImpl implements ShareService {

    private static final int STATUS_VISIBLE = 1;

    private final SharePostMapper sharePostMapper;
    private final ShareImageMapper shareImageMapper;
    private final CurrentUserProvider currentUserProvider;
    private final MediaUploadService mediaUploadService;
    private final UserBasicClient userBasicClient;

    public ShareServiceImpl(SharePostMapper sharePostMapper,
                            ShareImageMapper shareImageMapper,
                            CurrentUserProvider currentUserProvider,
                            MediaUploadService mediaUploadService,
                            UserBasicClient userBasicClient) {
        this.sharePostMapper = sharePostMapper;
        this.shareImageMapper = shareImageMapper;
        this.currentUserProvider = currentUserProvider;
        this.mediaUploadService = mediaUploadService;
        this.userBasicClient = userBasicClient;
    }

    @Override
    public String uploadShareImage(MultipartFile file) {
        return mediaUploadService.uploadImage("share", file);
    }

    @Override
    @Transactional
    public Long createSharePost(SharePostCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        List<String> imageUrls = request.getImageUrls().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (imageUrls.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请至少上传一张分享图片");
        }

        SharePost post = new SharePost();
        post.setUserId(userId);
        post.setTitle(request.getTitle().trim());
        post.setSummary(request.getSummary().trim());
        post.setContent(request.getContent().trim());
        post.setCoverImage(imageUrls.get(0));
        post.setStatus(STATUS_VISIBLE);
        post.setViewCount(0);
        post.setLikeCount(0);
        sharePostMapper.insert(post);

        for (int i = 0; i < imageUrls.size(); i++) {
            ShareImage image = new ShareImage();
            image.setPostId(post.getId());
            image.setImageUrl(imageUrls.get(i));
            image.setSortNo(i + 1);
            shareImageMapper.insert(image);
        }
        return post.getId();
    }

    @Override
    public PageResult<SharePostListItemVO> listPublicShares(Integer pageNum, Integer pageSize) {
        return listShares(new LambdaQueryWrapper<SharePost>()
                .eq(SharePost::getStatus, STATUS_VISIBLE)
                .orderByDesc(SharePost::getId), pageNum, pageSize);
    }

    @Override
    public PageResult<SharePostListItemVO> listCurrentUserShares(Integer pageNum, Integer pageSize) {
        return listShares(new LambdaQueryWrapper<SharePost>()
                .eq(SharePost::getUserId, currentUserProvider.getCurrentUserId())
                .orderByDesc(SharePost::getId), pageNum, pageSize);
    }

    private PageResult<SharePostListItemVO> listShares(LambdaQueryWrapper<SharePost> wrapper, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);
        Page<SharePost> page = sharePostMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        List<SharePost> posts = page.getRecords();
        Map<Long, UserBasicInfo> userMap = loadUserMap(posts);
        Map<Long, Integer> imageCountMap = loadImageCountMap(posts);

        PageResult<SharePostListItemVO> result = new PageResult<>();
        result.setRecords(posts.stream().map(post -> toListItemVO(post, userMap.get(post.getUserId()),
                imageCountMap.getOrDefault(post.getId(), 0))).toList());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @Override
    @Transactional
    public SharePostDetailVO getPublicShareDetail(Long id) {
        SharePost post = sharePostMapper.selectById(id);
        if (post == null || !Integer.valueOf(STATUS_VISIBLE).equals(post.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分享内容不存在");
        }
        post.setViewCount(defaultInt(post.getViewCount()) + 1);
        sharePostMapper.updateById(post);
        return toDetailVO(post);
    }

    private SharePostListItemVO toListItemVO(SharePost post, UserBasicInfo user, Integer imageCount) {
        SharePostListItemVO vo = new SharePostListItemVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setSummary(post.getSummary());
        vo.setCoverImage(post.getCoverImage());
        vo.setViewCount(defaultInt(post.getViewCount()));
        vo.setImageCount(imageCount);
        vo.setAuthorNickname(resolveNickname(user));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }

    private SharePostDetailVO toDetailVO(SharePost post) {
        UserBasicInfo user = userBasicClient.listBasicUsers(List.of(post.getUserId())).get(post.getUserId());
        List<String> imageUrls = shareImageMapper.selectList(new LambdaQueryWrapper<ShareImage>()
                        .eq(ShareImage::getPostId, post.getId())
                        .orderByAsc(ShareImage::getSortNo, ShareImage::getId))
                .stream()
                .map(ShareImage::getImageUrl)
                .toList();

        SharePostDetailVO vo = new SharePostDetailVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setSummary(post.getSummary());
        vo.setContent(post.getContent());
        vo.setCoverImage(post.getCoverImage());
        vo.setViewCount(defaultInt(post.getViewCount()));
        vo.setAuthorNickname(resolveNickname(user));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setCreateTime(post.getCreateTime());
        vo.setImageUrls(imageUrls);
        return vo;
    }

    private Map<Long, UserBasicInfo> loadUserMap(List<SharePost> posts) {
        List<Long> userIds = posts.stream().map(SharePost::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userBasicClient.listBasicUsers(userIds);
    }

    private Map<Long, Integer> loadImageCountMap(List<SharePost> posts) {
        List<Long> postIds = posts.stream().map(SharePost::getId).toList();
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> countMap = new HashMap<>();
        shareImageMapper.selectList(new LambdaQueryWrapper<ShareImage>().in(ShareImage::getPostId, postIds))
                .forEach(image -> countMap.merge(image.getPostId(), 1, Integer::sum));
        return countMap;
    }

    private String resolveNickname(UserBasicInfo user) {
        if (user == null) {
            return "未知用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
