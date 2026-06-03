package com.travelplatform.service.media;

import org.springframework.web.multipart.MultipartFile;

public interface MediaUploadService {

    String uploadImage(String category, MultipartFile file);
}
