package com.travelplatform.service.admin;

import com.travelplatform.dto.admin.user.AdminUserRoleUpdateRequest;
import com.travelplatform.dto.admin.user.AdminUserStatusUpdateRequest;
import com.travelplatform.vo.admin.user.AdminRoleOptionVO;
import com.travelplatform.vo.admin.user.AdminUserDetailVO;
import com.travelplatform.vo.admin.user.AdminUserListItemVO;
import com.travelplatform.vo.common.PageResult;

import java.util.List;

public interface AdminUserManageService {

    PageResult<AdminUserListItemVO> listUsers(String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminUserDetailVO getUserDetail(Long id);

    void updateUserStatus(Long id, AdminUserStatusUpdateRequest request);

    void updateUserRoles(Long id, AdminUserRoleUpdateRequest request);

    List<AdminRoleOptionVO> listRoles();
}
