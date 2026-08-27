package com.travelplatform.contenttrip.security;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUserProvider {

    private static final String USER_ID_HEADER = "X-User-Id";

    public Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        String userId = request == null ? null : request.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户身份无效");
        }
    }
}
