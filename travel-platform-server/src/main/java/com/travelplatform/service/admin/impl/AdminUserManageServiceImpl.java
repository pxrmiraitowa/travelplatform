package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.admin.user.AdminUserRoleUpdateRequest;
import com.travelplatform.dto.admin.user.AdminUserStatusUpdateRequest;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserRole;
import com.travelplatform.mapper.RoleMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.mapper.UserRoleMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.admin.AdminUserManageService;
import com.travelplatform.vo.admin.user.AdminRoleOptionVO;
import com.travelplatform.vo.admin.user.AdminUserDetailVO;
import com.travelplatform.vo.admin.user.AdminUserListItemVO;
import com.travelplatform.vo.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AdminUserManageServiceImpl implements AdminUserManageService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserManageServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResult<AdminUserListItemVO> listUsers(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId));
        List<AdminUserListItemVO> records = users.stream()
                .filter(user -> matchKeyword(user, keyword))
                .map(this::toListVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminUserDetailVO getUserDetail(Long id) {
        User user = getUser(id);
        AdminUserDetailVO vo = new AdminUserDetailVO();
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
        vo.setCreateTime(user.getCreateTime());
        vo.setRoleCodes(getRoleCodes(user.getId()));
        return vo;
    }

    @Override
    public void updateUserStatus(Long id, AdminUserStatusUpdateRequest request) {
        User user = getUser(id);
        if (!List.of(0, 1).contains(request.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户状态不合法");
        }
        if (SecurityUtils.getCurrentUserId().equals(id) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能禁用当前登录管理员");
        }
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserRoles(Long id, AdminUserRoleUpdateRequest request) {
        User user = getUser(id);
        LinkedHashSet<String> uniqueCodes = new LinkedHashSet<>(request.getRoleCodes());
        uniqueCodes.add(ROLE_USER);
        if (SecurityUtils.getCurrentUserId().equals(id) && !uniqueCodes.contains(ROLE_ADMIN)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能移除当前登录管理员的后台角色");
        }
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleCode, uniqueCodes)
                .eq(Role::getStatus, 1));
        if (roles.size() != uniqueCodes.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色配置不合法");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public List<AdminRoleOptionVO> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .eq(Role::getStatus, 1)
                        .orderByAsc(Role::getId))
                .stream()
                .map(role -> {
                    AdminRoleOptionVO vo = new AdminRoleOptionVO();
                    vo.setId(role.getId());
                    vo.setRoleCode(role.getRoleCode());
                    vo.setRoleName(role.getRoleName());
                    return vo;
                })
                .toList();
    }

    private boolean matchKeyword(User user, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(user.getUsername(), normalized)
                || contains(user.getNickname(), normalized)
                || contains(user.getPhone(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return user;
    }

    private List<String> getRoleCodes(Long userId) {
        return roleMapper.selectRolesByUserId(userId).stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(Role::getRoleCode)
                .toList();
    }

    private AdminUserListItemVO toListVO(User user) {
        AdminUserListItemVO vo = new AdminUserListItemVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setRoleCodes(getRoleCodes(user.getId()));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private <T> PageResult<T> paginate(List<T> source, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, source.size());
        int toIndex = Math.min(fromIndex + safePageSize, source.size());
        PageResult<T> result = new PageResult<>();
        result.setRecords(new ArrayList<>(source.subList(fromIndex, toIndex)));
        result.setTotal((long) source.size());
        result.setPageNum(safePageNum);
        result.setPageSize(safePageSize);
        return result;
    }
}
