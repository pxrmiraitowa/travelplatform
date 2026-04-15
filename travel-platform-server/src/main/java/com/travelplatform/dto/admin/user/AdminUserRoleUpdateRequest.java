package com.travelplatform.dto.admin.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AdminUserRoleUpdateRequest {

    @NotEmpty(message = "角色不能为空")
    private List<String> roleCodes;

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
