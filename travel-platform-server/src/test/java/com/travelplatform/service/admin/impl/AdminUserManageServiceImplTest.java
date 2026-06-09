package com.travelplatform.service.admin.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.admin.user.AdminUserRoleUpdateRequest;
import com.travelplatform.dto.admin.user.AdminUserStatusUpdateRequest;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserRole;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.mapper.UserRoleMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.admin.user.AdminRoleOptionVO;
import com.travelplatform.vo.admin.user.AdminUserDetailVO;
import com.travelplatform.vo.admin.user.AdminUserListItemVO;
import com.travelplatform.vo.common.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManageServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @Mock UserRoleMapper userRoleMapper;
    @InjectMocks AdminUserManageServiceImpl service;

    @Test
    void listUsersShouldFilterByKeywordAndMapRoles() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                user(1L, "demo", "Demo", "13900000000", 1),
                user(2L, "alice", "Alice", "13800000000", 1)
        ));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(role(1L, "ROLE_ADMIN", "Admin")));

        PageResult<AdminUserListItemVO> result = service.listUsers("alice", null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("alice");
        assertThat(result.getRecords().get(0).getRoleCodes()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void getUserDetailShouldReturnRoleCodes() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "Demo", "13900000000", 1));
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(
                role(1L, "ROLE_ADMIN", "Admin"),
                role(2L, "ROLE_USER", "User")
        ));

        AdminUserDetailVO result = service.getUserDetail(1L);

        assertThat(result.getUsername()).isEqualTo("demo");
        assertThat(result.getRoleCodes()).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void updateUserStatusShouldRejectDisablingCurrentAdmin() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "Demo", "13900000000", 1));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest();
            request.setStatus(0);

            assertThatThrownBy(() -> service.updateUserStatus(1L, request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void updateUserStatusShouldPersistValidStatus() {
        User user = user(2L, "alice", "Alice", "13800000000", 1);
        when(userMapper.selectById(2L)).thenReturn(user);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest();
            request.setStatus(0);

            service.updateUserStatus(2L, request);

            assertThat(user.getStatus()).isEqualTo(0);
            verify(userMapper).updateById(user);
        }
    }

    @Test
    void updateUserRolesShouldRejectRemovingAdminRoleFromCurrentUser() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "demo", "Demo", "13900000000", 1));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
            request.setRoleCodes(List.of("ROLE_USER"));

            assertThatThrownBy(() -> service.updateUserRoles(1L, request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void updateUserRolesShouldReplaceRolesAndKeepRoleUser() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "alice", "Alice", "13800000000", 1));
        when(roleMapper.selectList(any())).thenReturn(List.of(
                role(1L, "ROLE_ADMIN", "Admin"),
                role(2L, "ROLE_USER", "User")
        ));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
            request.setRoleCodes(List.of("ROLE_ADMIN"));

            service.updateUserRoles(2L, request);

            verify(userRoleMapper).delete(any());
            ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
            verify(userRoleMapper, times(2)).insert(captor.capture());
            assertThat(captor.getAllValues()).extracting(UserRole::getRoleId).containsExactly(1L, 2L);
        }
    }

    @Test
    void listRolesShouldMapEnabledRoles() {
        when(roleMapper.selectList(any())).thenReturn(List.of(
                role(1L, "ROLE_ADMIN", "Admin"),
                role(2L, "ROLE_USER", "User")
        ));

        List<AdminRoleOptionVO> result = service.listRoles();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoleCode()).isEqualTo("ROLE_ADMIN");
    }

    private User user(Long id, String username, String nickname, String phone, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setStatus(status);
        user.setLastLoginTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        return user;
    }

    private Role role(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setStatus(1);
        return role;
    }
}
