package com.travelplatform.service.media.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaUploadServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadImageToExpectedCategoryFolder() throws IOException {
        MediaUploadServiceImpl service = new MediaUploadServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});

        String url = service.uploadImage("hotel", file);

        assertThat(url).startsWith("/api/public/uploads/hotel/");
        String relativePath = url.replace("/api/public/uploads/", "").replace("/", java.io.File.separator);
        Path storedFile = tempDir.resolve(relativePath);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).containsExactly(1, 2, 3);
    }

    @Test
    void shouldRejectInvalidCategoryTraversal() {
        MediaUploadServiceImpl service = new MediaUploadServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> service.uploadImage("../hotel", file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MediaUploadServiceImpl service = new MediaUploadServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cover.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.uploadImage("hotel", file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.BAD_REQUEST.getCode());
    }
}
