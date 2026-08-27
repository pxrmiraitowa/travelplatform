package com.travelplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.user.dto.admin.AdminUserRolesRequest;
import com.travelplatform.user.dto.admin.AdminUserStatusRequest;
import com.travelplatform.user.entity.Role;
import com.travelplatform.user.entity.User;
import com.travelplatform.user.entity.UserRole;
import com.travelplatform.user.mapper.RoleMapper;
import com.travelplatform.user.mapper.UserMapper;
import com.travelplatform.user.mapper.UserRoleMapper;
import com.travelplatform.user.security.SecurityUtils;
import com.travelplatform.user.service.AdminUserService;
import com.travelplatform.user.vo.admin.AdminRoleOptionVO;
import com.travelplatform.user.vo.admin.AdminUserVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper; this.roleMapper = roleMapper; this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResult<AdminUserVO> page(String keyword, Integer status, int pageNum, int pageSize) {
        List<AdminUserVO> source = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(status != null, User::getStatus, status).orderByDesc(User::getId)).stream()
                .filter(user -> matches(user, keyword)).map(this::toVO).toList();
        int safePage = Math.max(1, pageNum); int safeSize = Math.min(100, Math.max(1, pageSize));
        int from = Math.min((safePage - 1) * safeSize, source.size());
        int to = Math.min(from + safeSize, source.size());
        PageResult<AdminUserVO> result = new PageResult<>();
        result.setRecords(new ArrayList<>(source.subList(from, to))); result.setTotal(source.size());
        result.setPageNum(safePage); result.setPageSize(safeSize); return result;
    }

    @Override public AdminUserVO detail(Long id) { return toVO(getUser(id)); }

    @Override
    @Transactional
    public void updateStatus(Long id, AdminUserStatusRequest request) {
        User user = getUser(id);
        if (SecurityUtils.getCurrentUserId().equals(id) && request.getStatus() == 0) {
            throw badRequest("不能禁用当前登录管理员");
        }
        user.setStatus(request.getStatus()); userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateRoles(Long id, AdminUserRolesRequest request) {
        User user = getUser(id);
        Set<String> codes = request.getRoleCodes().stream().filter(StringUtils::hasText)
                .map(String::trim).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        codes.add(ROLE_USER);
        if (SecurityUtils.getCurrentUserId().equals(id) && !codes.contains(ROLE_ADMIN)) {
            throw badRequest("不能移除当前登录管理员的后台角色");
        }
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleCode, codes).eq(Role::getStatus, 1));
        if (roles.size() != codes.size()) throw badRequest("角色配置不合法");
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        roles.forEach(role -> {
            UserRole relation = new UserRole(); relation.setUserId(user.getId()); relation.setRoleId(role.getId());
            userRoleMapper.insert(relation);
        });
    }

    @Override
    public List<AdminRoleOptionVO> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getStatus, 1).orderByAsc(Role::getId))
                .stream().map(role -> new AdminRoleOptionVO(role.getId(), role.getRoleCode(), role.getRoleName())).toList();
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        return user;
    }
    private AdminUserVO toVO(User user) {
        List<String> codes = roleMapper.selectRolesByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(Role::getId)).map(Role::getRoleCode).toList();
        return new AdminUserVO(user.getId(), user.getUsername(), user.getNickname(), user.getRealName(),
                user.getPhone(), user.getEmail(), user.getGender(), user.getAvatar(), user.getStatus(), codes,
                user.getLastLoginTime(), user.getCreateTime());
    }
    private boolean matches(User user, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(user.getUsername(), value) || contains(user.getNickname(), value)
                || contains(user.getPhone(), value) || contains(user.getEmail(), value);
    }
    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
    private BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
