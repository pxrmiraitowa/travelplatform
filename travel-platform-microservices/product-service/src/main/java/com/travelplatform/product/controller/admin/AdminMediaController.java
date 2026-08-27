package com.travelplatform.product.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.service.media.MediaUploadService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaUploadService mediaUploadService;

    public AdminMediaController(MediaUploadService mediaUploadService) {
        this.mediaUploadService = mediaUploadService;
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("url", mediaUploadService.uploadImage("product", file)));
    }
}
