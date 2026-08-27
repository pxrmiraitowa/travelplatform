package com.travelplatform.contenttrip.service.share.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.entity.ShareImage;
import com.travelplatform.contenttrip.entity.SharePost;
import com.travelplatform.contenttrip.entity.User;
import com.travelplatform.contenttrip.mapper.ShareImageMapper;
import com.travelplatform.contenttrip.mapper.SharePostMapper;
import com.travelplatform.contenttrip.mapper.UserMapper;
import com.travelplatform.contenttrip.service.share.ShareService;
import com.travelplatform.contenttrip.vo.share.SharePostDetailVO;
import com.travelplatform.contenttrip.vo.share.SharePostListItemVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShareServiceImpl implements ShareService {

    private static final int STATUS_VISIBLE = 1;

    private final SharePostMapper sharePostMapper;
    private final ShareImageMapper shareImageMapper;
    private final UserMapper userMapper;

    public ShareServiceImpl(SharePostMapper sharePostMapper,
                            ShareImageMapper shareImageMapper,
                            UserMapper userMapper) {
        this.sharePostMapper = sharePostMapper;
        this.shareImageMapper = shareImageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<SharePostListItemVO> listPublicShares(Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);

        Page<SharePost> page = sharePostMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<SharePost>()
                        .eq(SharePost::getStatus, STATUS_VISIBLE)
                        .orderByDesc(SharePost::getId));
        List<SharePost> posts = page.getRecords();
        Map<Long, User> userMap = loadUserMap(posts);
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

    private SharePostListItemVO toListItemVO(SharePost post, User user, Integer imageCount) {
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
        User user = userMapper.selectById(post.getUserId());
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

    private Map<Long, User> loadUserMap(List<SharePost> posts) {
        List<Long> userIds = posts.stream().map(SharePost::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
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

    private String resolveNickname(User user) {
        if (user == null) {
            return "未知用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
