package com.travelplatform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityUserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public SecurityUserService(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public LoginUser loadUserByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        return buildLoginUser(user);
    }

    public LoginUser loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(User user) {
        List<String> roleCodes = roleMapper.selectRolesByUserId(user.getId()).stream()
                .map(Role::getRoleCode)
                .toList();
        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getStatus(), roleCodes, authorities);
    }
}
