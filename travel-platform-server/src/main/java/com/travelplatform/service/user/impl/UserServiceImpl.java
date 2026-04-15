package com.travelplatform.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.user.UpdateUserProfileRequest;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.user.UserService;
import com.travelplatform.vo.user.CurrentUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public CurrentUserVO getCurrentUser() {
        return buildCurrentUserVO(SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public CurrentUserVO updateCurrentUser(UpdateUserProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (StringUtils.hasText(request.getPhone())) {
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getPhone())
                    .ne(User::getId, userId)
                    .last("LIMIT 1"));
            if (existingUser != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "手机号已被其他用户使用");
            }
        }
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : null);
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
        user.setGender(request.getGender());
        user.setAvatar(StringUtils.hasText(request.getAvatar()) ? request.getAvatar() : null);
        userMapper.updateById(user);
        return buildCurrentUserVO(userId);
    }

    @Override
    public CurrentUserVO buildCurrentUserVO(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        List<String> roleCodes = roleMapper.selectRolesByUserId(userId).stream()
                .map(Role::getRoleCode)
                .toList();

        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setRoleCodes(roleCodes);
        return vo;
    }
}
