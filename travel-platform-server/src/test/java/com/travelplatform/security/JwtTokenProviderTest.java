package com.travelplatform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider =
            new JwtTokenProvider("travelplatform-demo-secret-key-for-jwt-signing-2026", 3600);

    @Test
    void shouldGenerateTokenWithReadableClaims() {
        LoginUser loginUser = new LoginUser(
                99L,
                "tester",
                "secret",
                1,
                List.of("ROLE_USER", "ROLE_ADMIN"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        String token = provider.generateToken(loginUser);

        assertThat(provider.getUserId(token)).isEqualTo(99L);
        assertThat(provider.getExpiration(token)).isAfter(new Date());
        assertThat(provider.isTokenValid(
                token,
                User.withUsername("tester").password("secret").authorities("ROLE_USER").build()
        )).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentUsername() {
        LoginUser loginUser = new LoginUser(
                100L,
                "tester",
                "secret",
                1,
                List.of("ROLE_USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = provider.generateToken(loginUser);

        assertThat(provider.isTokenValid(
                token,
                User.withUsername("another-user").password("secret").authorities("ROLE_USER").build()
        )).isFalse();
    }
}
