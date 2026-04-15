package com.travelplatform.dto.auth;

import com.travelplatform.vo.user.CurrentUserVO;

public class LoginResponse {

    private String token;

    private CurrentUserVO userInfo;

    public LoginResponse() {
    }

    public LoginResponse(String token, CurrentUserVO userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CurrentUserVO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(CurrentUserVO userInfo) {
        this.userInfo = userInfo;
    }
}
