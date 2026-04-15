package com.travelplatform.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.auth.LoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.dto.auth.RegisterRequest;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserRole;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.mapper.UserRoleMapper;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.security.LoginUser;
import com.travelplatform.security.SecurityUserService;
import com.travelplatform.security.TokenBlacklistService;
import com.travelplatform.service.auth.AuthService;
import com.travelplatform.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_CODE = "ROLE_USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUserService securityUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;

    public AuthServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           SecurityUserService securityUserService,
                           JwtTokenProvider jwtTokenProvider,
                           TokenBlacklistService tokenBlacklistService,
                           UserService userService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.securityUserService = securityUserService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两次输入的密码不一致");
        }

        User usernameExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (usernameExists != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        User phoneExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())
                .last("LIMIT 1"));
        if (phoneExists != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "手机号已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, DEFAULT_ROLE_CODE)
                .eq(Role::getStatus, 1)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "默认角色未初始化");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        LoginUser loginUser = securityUserService.loadUserById(user.getId());
        String token = jwtTokenProvider.generateToken(loginUser);
        return new LoginResponse(token, userService.buildCurrentUserVO(user.getId()));
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        LoginUser loginUser = securityUserService.loadUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (!loginUser.isEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号已被禁用");
        }

        User user = userMapper.selectById(loginUser.getUserId());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(loginUser);
        return new LoginResponse(token, userService.buildCurrentUserVO(loginUser.getUserId()));
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokenBlacklistService.blacklist(token, jwtTokenProvider.getExpiration(token));
    }
}
