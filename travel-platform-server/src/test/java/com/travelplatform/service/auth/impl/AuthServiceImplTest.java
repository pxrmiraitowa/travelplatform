package com.travelplatform.service.auth.impl;

import com.travelplatform.common.exception.BusinessException;
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
import com.travelplatform.service.user.UserService;
import com.travelplatform.vo.user.CurrentUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock UserService userService;

    private AuthServiceImpl service;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        org.mockito.MockitoAnnotations.openMocks(this);
        jwtTokenProvider = new JwtTokenProvider(
                "travelplatform-demo-secret-key-for-jwt-signing-2026",
                3600
        );
        service = new AuthServiceImpl(
                userMapper,
                roleMapper,
                userRoleMapper,
                passwordEncoder,
                new SecurityUserService(userMapper, roleMapper),
                jwtTokenProvider,
                tokenBlacklistService,
                userService
        );
    }

    @Test
    void registerShouldCreateUserAssignRoleAndReturnLoginResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("demo");
        request.setNickname("Demo");
        request.setPhone("13900000001");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "ROLE_USER"));
        User storedUser = new User();
        storedUser.setId(1L);
        storedUser.setUsername("demo");
        storedUser.setPassword("encoded");
        storedUser.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(storedUser);
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role(2L, "ROLE_USER")));
        when(userService.buildCurrentUserVO(1L)).thenReturn(currentUser(1L, "demo"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userMapper.insert(userCaptor.capture())).thenAnswer(invocation -> {
            User user = userCaptor.getValue();
            user.setId(1L);
            return 1;
        });

        LoginResponse response = service.register(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserInfo().getId()).isEqualTo(1L);
        assertThat(jwtTokenProvider.getUserId(response.getToken())).isEqualTo(1L);
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void registerShouldRejectMismatchedPasswords() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("123");
        request.setConfirmPassword("456");

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void loginShouldReturnTokenForEnabledUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("123456");
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setPassword("encoded");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role(2L, "ROLE_USER")));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userService.buildCurrentUserVO(1L)).thenReturn(currentUser(1L, "demo"));

        LoginResponse response = service.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(response.getToken())).isEqualTo(1L);
        verify(userMapper).updateById(user);
    }

    @Test
    void logoutShouldIgnoreBlankTokenAndBlacklistNormalToken() {
        service.logout(" ");
        verify(tokenBlacklistService, org.mockito.Mockito.never()).blacklist(any(), any());

        LoginUser loginUser = new LoginUser(1L, "demo", "encoded", 1, List.of("ROLE_USER"), List.of());
        String token = jwtTokenProvider.generateToken(loginUser);

        service.logout(token);

        verify(tokenBlacklistService).blacklist(token, jwtTokenProvider.getExpiration(token));
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
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
