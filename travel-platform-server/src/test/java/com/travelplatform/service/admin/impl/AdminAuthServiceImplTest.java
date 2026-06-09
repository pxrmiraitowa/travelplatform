package com.travelplatform.service.admin.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.admin.auth.AdminLoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.security.SecurityUserService;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.user.UserService;
import com.travelplatform.vo.user.CurrentUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserService userService;

    private AdminAuthServiceImpl service;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        org.mockito.MockitoAnnotations.openMocks(this);
        jwtTokenProvider = new JwtTokenProvider(
                "travelplatform-demo-secret-key-for-jwt-signing-2026",
                3600
        );
        service = new AdminAuthServiceImpl(
                new SecurityUserService(userMapper, roleMapper),
                passwordEncoder,
                jwtTokenProvider,
                userMapper,
                userService
        );
    }

    @Test
    void loginShouldRequireAdminRoleAndReturnToken() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        User user = user(1L, "admin", "encoded", 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role("ROLE_ADMIN")));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userService.buildCurrentUserVO(1L)).thenReturn(currentUser(1L, "admin"));

        LoginResponse result = service.login(request);

        assertThat(result.getToken()).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(result.getToken())).isEqualTo(1L);
        verify(userMapper).updateById(user);
    }

    @Test
    void loginShouldRejectNonAdminAccount() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("demo");
        request.setPassword("123456");
        when(userMapper.selectOne(any())).thenReturn(user(1L, "demo", "encoded", 1));
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role("ROLE_USER")));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void getCurrentAdminShouldRejectNonAdminRole() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> service.getCurrentAdmin()).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void getCurrentAdminShouldReturnCurrentUserInfo() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(userService.buildCurrentUserVO(1L)).thenReturn(currentUser(1L, "admin"));

            CurrentUserVO result = service.getCurrentAdmin();

            assertThat(result.getUsername()).isEqualTo("admin");
        }
    }

    private User user(Long id, String username, String password, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setStatus(status);
        return user;
    }

    private Role role(String code) {
        Role role = new Role();
        role.setRoleCode(code);
        role.setStatus(1);
        return role;
    }

    private CurrentUserVO currentUser(Long id, String username) {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(id);
        vo.setUsername(username);
        return vo;
    }
}
