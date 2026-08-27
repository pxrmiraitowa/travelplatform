package com.travelplatform.user.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class AdminUserRolesRequest {
    @NotEmpty
    private List<String> roleCodes;

    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
