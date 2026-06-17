package com.bobbo01.supplyhub.domain.user.entity;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleInterpretationTest {

    @Test
    void separatesPurchasingRoleAndCompanyAdminFlagForCompanyUser() {
        User user = User.createOAuthUser(
                Company.builder().companyName("Example").status("ACTIVE").build(),
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "user@example.com",
                "User",
                null
        );
        user.grantCompanyAdmin();

        assertThat(user.isCompanyUser()).isTrue();
        assertThat(user.getPurchasingRoleName()).isEqualTo(RoleNames.PURCHASER);
        assertThat(user.hasPurchasingRole(RoleNames.PURCHASER)).isTrue();
        assertThat(user.hasAnyPurchasingRole(RoleNames.CART_USER, RoleNames.PURCHASER)).isTrue();
        assertThat(user.hasCompanyAdminRole()).isTrue();
    }

    @Test
    void treatsPlatformAdminAsNonCompanyUserWithoutPurchasingRoleMatch() {
        User user = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );

        assertThat(user.isCompanyUser()).isFalse();
        assertThat(user.getPurchasingRoleName()).isEqualTo(RoleNames.PLATFORM_ADMIN);
        assertThat(user.hasAnyPurchasingRole(RoleNames.CART_USER, RoleNames.PURCHASER, RoleNames.APPROVER)).isFalse();
        assertThat(user.isPlatformAdmin()).isTrue();
    }

    @Test
    void rejectsPersistedCompanyAdminRoleForCompanyUserConstruction() {
        assertThatThrownBy(() -> User.builder()
                .company(Company.builder().companyName("Example").status("ACTIVE").build())
                .role(Role.builder().roleName(RoleNames.COMPANY_ADMIN).description("company admin").build())
                .companyAdmin(false)
                .email("user@example.com")
                .fullName("User")
                .status("ACTIVE")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational flag");
    }
}
