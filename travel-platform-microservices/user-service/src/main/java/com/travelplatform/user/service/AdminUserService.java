package com.travelplatform.user.service;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.user.dto.admin.AdminUserRolesRequest;
import com.travelplatform.user.dto.admin.AdminUserStatusRequest;
import com.travelplatform.user.vo.admin.AdminRoleOptionVO;
import com.travelplatform.user.vo.admin.AdminUserVO;
import java.util.List;

public interface AdminUserService {
    PageResult<AdminUserVO> page(String keyword, Integer status, int pageNum, int pageSize);
    AdminUserVO detail(Long id);
    void updateStatus(Long id, AdminUserStatusRequest request);
    void updateRoles(Long id, AdminUserRolesRequest request);
    List<AdminRoleOptionVO> roles();
}
