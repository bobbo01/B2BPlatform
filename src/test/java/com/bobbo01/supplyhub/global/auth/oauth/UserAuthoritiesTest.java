package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthoritiesTest {

    @Test
    void returnsPurchasingRoleAuthorityForCompanyUser() {
        User user = User.createOAuthUser(
                Company.builder().companyName("Example").status("ACTIVE").build(),
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );

        assertThat(UserAuthorities.authorityNames(user))
                .containsExactly("ROLE_CART_USER");
    }

    @Test
    void addsCompanyAdminAuthorityWhenCompanyAdminFlagIsSet() {
        User user = User.builder()
                .company(Company.builder().companyName("Example").status("ACTIVE").build())
                .role(Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build())
                .companyAdmin(true)
                .email("alice@example.com")
                .fullName("Alice")
                .status("ACTIVE")
                .build();

        assertThat(UserAuthorities.authorityNames(user))
                .containsExactly("ROLE_PURCHASER", "ROLE_COMPANY_ADMIN");
    }

    @Test
    void returnsPlatformAdminOnlyForPlatformAdminUser() {
        User user = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "alice@example.com",
                "Alice",
                null
        );

        assertThat(UserAuthorities.authorityNames(user))
                .containsExactly("ROLE_PLATFORM_ADMIN");
    }

    @Test
    void ignoresUnknownPersistedCompanyUserRoleForAuthorities() {
        User user = User.builder()
                .company(Company.builder().companyName("Example").status("ACTIVE").build())
                .role(Role.builder().roleName("LEGACY_COMPANY_ADMIN").description("legacy").build())
                .companyAdmin(false)
                .email("legacy@example.com")
                .fullName("Legacy")
                .status("ACTIVE")
                .build();

        assertThat(UserAuthorities.authorityNames(user)).isEmpty();
    }
}
