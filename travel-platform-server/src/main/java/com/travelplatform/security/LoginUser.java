package com.travelplatform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Integer status;
    private final List<String> roleCodes;
    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUser(Long userId,
                     String username,
                     String password,
                     Integer status,
                     List<String> roleCodes,
                     Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.roleCodes = roleCodes;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
