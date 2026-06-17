package com.bobbo01.supplyhub.global.auth.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginPolicyTest {

    @Test
    void detectsPlatformAdminEmailFromAllowlist() {
        OAuth2LoginPolicy policy = new OAuth2LoginPolicy(
                1L,
                1L,
                "CART_USER",
                true,
                false,
                false,
                true,
                "",
                "gmail.com",
                "admin@example.com, ops@example.com"
        );

        assertThat(policy.isPlatformAdminEmail("admin@example.com")).isTrue();
        assertThat(policy.isPlatformAdminEmail("ADMIN@example.com")).isTrue();
        assertThat(policy.isPlatformAdminEmail("user@example.com")).isFalse();
    }
}
