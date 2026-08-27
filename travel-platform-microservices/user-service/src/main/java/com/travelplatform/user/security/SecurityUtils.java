package com.travelplatform.user.security;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录或登录已失效");
        }
        return loginUser;
    }

    public static Long getCurrentUserId() {
        return getLoginUser().getUserId();
    }
}
