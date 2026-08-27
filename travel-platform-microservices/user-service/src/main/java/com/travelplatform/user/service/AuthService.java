package com.travelplatform.user.service;

import com.travelplatform.user.dto.auth.LoginRequest;
import com.travelplatform.user.dto.auth.LoginResponse;
import com.travelplatform.user.dto.auth.RegisterRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);
}
