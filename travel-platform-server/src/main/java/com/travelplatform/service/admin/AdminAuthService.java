package com.travelplatform.service.admin;

import com.travelplatform.dto.admin.auth.AdminLoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.vo.user.CurrentUserVO;

public interface AdminAuthService {

    LoginResponse login(AdminLoginRequest request);

    CurrentUserVO getCurrentAdmin();
}
