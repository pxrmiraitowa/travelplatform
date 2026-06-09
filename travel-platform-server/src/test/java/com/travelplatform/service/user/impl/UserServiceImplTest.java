package com.travelplatform.service.user.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.user.UpdateUserProfileRequest;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.user.CurrentUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @InjectMocks UserServiceImpl service;

    @Test
    void buildCurrentUserVoShouldMapUserAndRoles() {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setNickname("Demo");
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);
        Role role = new Role();
        role.setRoleCode("ROLE_ADMIN");
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role));

        CurrentUserVO result = service.buildCurrentUserVO(1L);

        assertThat(result.getUsername()).isEqualTo("demo");
        assertThat(result.getRoleCodes()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void updateCurrentUserShouldRejectDuplicatePhone() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            User current = new User();
            current.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(current);
            when(userMapper.selectOne(any())).thenReturn(new User());

            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setPhone("13900000002");

            assertThatThrownBy(() -> service.updateCurrentUser(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void updateCurrentUserShouldPersistChangesAndReturnLatestUser() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            User current = new User();
            current.setId(1L);
            current.setUsername("demo");
            current.setStatus(1);
            when(userMapper.selectById(1L)).thenReturn(current);

            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setNickname("New");
            request.setPhone("");

            CurrentUserVO result = service.updateCurrentUser(request);

            verify(userMapper).updateById(current);
            assertThat(current.getPhone()).isNull();
            assertThat(result.getNickname()).isEqualTo("New");
        }
    }
}
