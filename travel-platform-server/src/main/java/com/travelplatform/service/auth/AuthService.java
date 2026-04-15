package com.travelplatform.service.auth;

import com.travelplatform.dto.auth.LoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.dto.auth.RegisterRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);
}
