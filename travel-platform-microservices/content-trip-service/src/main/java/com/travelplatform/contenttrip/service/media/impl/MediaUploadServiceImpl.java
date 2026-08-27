package com.travelplatform.contenttrip.service.media.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.service.media.MediaUploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final Path uploadDir;

    public MediaUploadServiceImpl(@Value("${travel.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String uploadImage(String category, MultipartFile file) {
        validateCategory(category);
        validateImage(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String dateFolder = LocalDate.now().toString().replace("-", "");
        Path targetDirectory = uploadDir.resolve(Paths.get(category, dateFolder)).normalize();
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetFile = targetDirectory.resolve(fileName).normalize();
        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetFile);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "图片上传失败");
        }
        return "/api/public/uploads/" + category + "/" + dateFolder + "/" + fileName;
    }

    private void validateCategory(String category) {
        if (!StringUtils.hasText(category) || category.contains("..") || category.contains("/") || category.contains("\\")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传目录不合法");
        }
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
}
