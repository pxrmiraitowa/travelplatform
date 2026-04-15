package com.travelplatform.service.admin.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.admin.auth.AdminLoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.security.LoginUser;
import com.travelplatform.security.SecurityUserService;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.admin.AdminAuthService;
import com.travelplatform.service.user.UserService;
import com.travelplatform.vo.user.CurrentUserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

    private final SecurityUserService securityUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final UserService userService;

    public AdminAuthServiceImpl(SecurityUserService securityUserService,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider jwtTokenProvider,
                                UserMapper userMapper,
                                UserService userService) {
        this.securityUserService = securityUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @Override
    public LoginResponse login(AdminLoginRequest request) {
        LoginUser loginUser = securityUserService.loadUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (!loginUser.isEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号已被禁用");
        }
        if (loginUser.getRoleCodes().stream().noneMatch(ADMIN_ROLE_CODE::equals)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号没有后台访问权限");
        }
        User user = userMapper.selectById(loginUser.getUserId());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return new LoginResponse(jwtTokenProvider.generateToken(loginUser), userService.buildCurrentUserVO(loginUser.getUserId()));
    }

    @Override
    public CurrentUserVO getCurrentAdmin() {
        if (!SecurityUtils.hasRole(ADMIN_ROLE_CODE)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号没有后台访问权限");
        }
        return userService.buildCurrentUserVO(SecurityUtils.getCurrentUserId());
    }
}
