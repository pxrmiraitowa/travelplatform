package com.travelplatform.service.share.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.share.SharePostCreateRequest;
import com.travelplatform.entity.ShareImage;
import com.travelplatform.entity.SharePost;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.ShareImageMapper;
import com.travelplatform.mapper.SharePostMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.share.ShareService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.share.SharePostDetailVO;
import com.travelplatform.vo.share.SharePostListItemVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShareServiceImpl implements ShareService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int STATUS_VISIBLE = 1;

    private final SharePostMapper sharePostMapper;
    private final ShareImageMapper shareImageMapper;
    private final UserMapper userMapper;
    private final Path uploadDir;

    public ShareServiceImpl(SharePostMapper sharePostMapper,
                            ShareImageMapper shareImageMapper,
                            UserMapper userMapper,
                            @Value("${travel.upload-dir:./uploads}") String uploadDir) {
        this.sharePostMapper = sharePostMapper;
        this.shareImageMapper = shareImageMapper;
        this.userMapper = userMapper;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String uploadShareImage(MultipartFile file) {
        validateImage(file);
        String extension = resolveExtension(file.getOriginalFilename());
        String dateFolder = LocalDate.now().toString().replace("-", "");
        Path targetDirectory = uploadDir.resolve(Paths.get("share", dateFolder)).normalize();
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetFile = targetDirectory.resolve(fileName).normalize();
        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "图片上传失败");
        }
        return "/api/public/uploads/share/" + dateFolder + "/" + fileName;
    }

    @Override
    @Transactional
    public Long createSharePost(SharePostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
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
                .eq(SharePost::getUserId, SecurityUtils.getCurrentUserId())
                .orderByDesc(SharePost::getId), pageNum, pageSize);
    }

    @Override
    @Transactional
    public SharePostDetailVO getPublicShareDetail(Long id) {
        SharePost post = sharePostMapper.selectById(id);
        if (post == null || !Integer.valueOf(STATUS_VISIBLE).equals(post.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分享内容不存在");
        }
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        sharePostMapper.updateById(post);
        return toDetailVO(post);
    }

    private PageResult<SharePostListItemVO> listShares(LambdaQueryWrapper<SharePost> wrapper, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 20);
        Page<SharePost> page = sharePostMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        List<SharePost> posts = page.getRecords();
        Map<Long, User> userMap = loadUserMap(posts);
        Map<Long, Integer> imageCountMap = loadImageCountMap(posts);

        PageResult<SharePostListItemVO> result = new PageResult<>();
        result.setRecords(posts.stream().map(post -> {
            User user = userMap.get(post.getUserId());
            SharePostListItemVO vo = new SharePostListItemVO();
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setSummary(post.getSummary());
            vo.setCoverImage(post.getCoverImage());
            vo.setViewCount(defaultInt(post.getViewCount()));
            vo.setImageCount(imageCountMap.getOrDefault(post.getId(), 0));
            vo.setAuthorNickname(resolveNickname(user));
            vo.setAuthorAvatar(user == null ? null : user.getAvatar());
            vo.setCreateTime(post.getCreateTime());
            return vo;
        }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
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
                .stream().collect(Collectors.toMap(User::getId, user -> user));
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

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片大小不能超过5MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 jpg、png、webp、gif 图片");
        }
        resolveExtension(file.getOriginalFilename());
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片文件名不合法");
        }
        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片格式不支持");
        }
        return extension;
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
