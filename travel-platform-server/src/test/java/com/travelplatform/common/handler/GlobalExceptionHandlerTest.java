package com.travelplatform.common.handler;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.Result;
import com.travelplatform.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBusinessExceptionPayload() {
        Result<Void> result = handler.handleBusinessException(new BusinessException(418, "custom"));

        assertThat(result.getCode()).isEqualTo(418);
        assertThat(result.getMessage()).isEqualTo("custom");
    }

    @Test
    void shouldReturnUploadTooLargeMessage() {
        Result<Void> result = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(6L * 1024 * 1024)
        );

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo("图片大小不能超过5MB");
    }

    @Test
    void shouldReturnSystemErrorForUnexpectedException() {
        Result<Void> result = handler.handleException(new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.SYSTEM_ERROR.getMessage());
    }
}
