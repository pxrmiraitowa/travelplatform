package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.service.media.MediaUploadService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaUploadService mediaUploadService;

    public AdminMediaController(MediaUploadService mediaUploadService) {
        this.mediaUploadService = mediaUploadService;
    }

    @Operation(summary = "后台上传商品图片")
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("url", mediaUploadService.uploadImage("product", file)));
    }
}
