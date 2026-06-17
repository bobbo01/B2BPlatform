package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.CompanyUserAdminView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyUserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CompanyUserAdminService companyUserAdminService;

    @Test
    void listsOnlyUsersInOwnCompanySortedByEmail() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User approver = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "b@example.com",
                "Bravo",
                null
        );
        ReflectionTestUtils.setField(approver, "id", 2L);

        User purchaser = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "a@example.com",
                "Alpha",
                null
        );
        ReflectionTestUtils.setField(purchaser, "id", 3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findAllByCompanyIdOrderByEmailAsc(10L)).thenReturn(List.of(purchaser, reviewer, approver));

        var views = companyUserAdminService.getCompanyUserViews(1L);

        assertThat(views).hasSize(3);
        assertThat(views).extracting(CompanyUserAdminView::email)
                .containsExactly("a@example.com", "admin@example.com", "b@example.com");
        assertThat(views).extracting(CompanyUserAdminView::purchasingRoleName)
                .containsExactly(RoleNames.PURCHASER, RoleNames.CART_USER, RoleNames.APPROVER);
        assertThat(views).extracting(CompanyUserAdminView::selfManagedCompanyAdminLocked)
                .containsExactly(false, true, false);
    }

    @Test
    void allowsCompanyAdminToChangeOwnPurchasingRole() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        Role approverRole = Role.builder().roleName(RoleNames.APPROVER).description("approver").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.APPROVER)).thenReturn(Optional.of(approverRole));

        companyUserAdminService.updateUserPurchasingRole(1L, 1L, RoleNames.APPROVER);

        assertThat(reviewer.getPurchasingRole().getRoleName()).isEqualTo(RoleNames.APPROVER);
    }

    @Test
    void rejectsAssigningNonPurchasingRole() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        Role platformRole = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.PLATFORM_ADMIN)).thenReturn(Optional.of(platformRole));

        assertThatThrownBy(() -> companyUserAdminService.updateUserPurchasingRole(1L, 1L, RoleNames.PLATFORM_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only purchasing roles can be assigned in this operation.");
    }

    @Test
    void rejectsNonCompanyAdminFromViewingCompanyUsers() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> companyUserAdminService.getCompanyUserViews(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPANY_ADMIN");
    }

    @Test
    void rejectsManagingUserFromAnotherCompany() {
        Company reviewerCompany = Company.builder().companyName("A").status("ACTIVE").build();
        ReflectionTestUtils.setField(reviewerCompany, "id", 10L);
        Company otherCompany = Company.builder().companyName("B").status("ACTIVE").build();
        ReflectionTestUtils.setField(otherCompany, "id", 20L);

        User reviewer = User.createOAuthUser(
                reviewerCompany,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                otherCompany,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> companyUserAdminService.updateUserPurchasingRole(1L, 2L, RoleNames.APPROVER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own company");
    }

    @Test
    void allowsCompanyAdminToInactivateUserInOwnCompany() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        companyUserAdminService.updateUserStatus(1L, 2L, "INACTIVE");

        assertThat(target.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void rejectsInactivatingOwnCompanyAdminAccount() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> companyUserAdminService.updateUserStatus(1L, 1L, "INACTIVE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot revoke or inactivate their own COMPANY_ADMIN account");
    }

    @Test
    void rejectsUnsupportedStatusChange() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> companyUserAdminService.updateUserStatus(1L, 2L, "PAUSED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported user status");
    }

    @Test
    void allowsCompanyAdminToGrantCompanyAdminInOwnCompany() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        companyUserAdminService.updateUserCompanyAdmin(1L, 2L, true);

        assertThat(target.hasCompanyAdminRole()).isTrue();
    }

    @Test
    void rejectsRevokingOwnCompanyAdminRole() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> companyUserAdminService.updateUserCompanyAdmin(1L, 1L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot revoke or inactivate their own COMPANY_ADMIN account");
    }

    @Test
    void rejectsInactivatingLastActiveOtherCompanyAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "other-admin@example.com",
                "Other Admin",
                null
        );
        target.grantCompanyAdmin();
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(1L);

        assertThatThrownBy(() -> companyUserAdminService.updateUserStatus(1L, 2L, "INACTIVE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active COMPANY_ADMIN");
    }

    @Test
    void rejectsRevokingLastActiveOtherCompanyAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);

        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "other-admin@example.com",
                "Other Admin",
                null
        );
        target.grantCompanyAdmin();
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(1L);

        assertThatThrownBy(() -> companyUserAdminService.updateUserCompanyAdmin(1L, 2L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active COMPANY_ADMIN");
    }
}
