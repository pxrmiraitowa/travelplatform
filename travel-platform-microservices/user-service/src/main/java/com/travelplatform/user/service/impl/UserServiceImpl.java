package com.travelplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.user.dto.user.UpdateUserProfileRequest;
import com.travelplatform.user.entity.Role;
import com.travelplatform.user.entity.User;
import com.travelplatform.user.mapper.RoleMapper;
import com.travelplatform.user.mapper.UserMapper;
import com.travelplatform.user.security.SecurityUtils;
import com.travelplatform.user.service.UserService;
import com.travelplatform.user.vo.BasicUserVO;
import com.travelplatform.user.vo.CurrentUserVO;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public List<BasicUserVO> listBasicUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinctIds = userIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty()) {
            return List.of();
        }

        List<User> users = userMapper.selectBatchIds(distinctIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));

        List<BasicUserVO> result = new ArrayList<>();
        for (Long userId : distinctIds) {
            User user = userMap.get(userId);
            if (user != null) {
                result.add(toBasicUserVO(user));
            }
        }
        return result;
    }

    private BasicUserVO toBasicUserVO(User user) {
        BasicUserVO vo = new BasicUserVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
