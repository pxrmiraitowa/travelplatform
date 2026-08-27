package com.travelplatform.order.security;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserProvider {

    public Long getCurrentUserId(HttpServletRequest request) {
        String value = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户身份无效");
        }
    }
}
