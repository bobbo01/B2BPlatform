package com.bobbo01.supplyhub.domain.user.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.entity.UserIdentity;
import com.bobbo01.supplyhub.domain.user.repository.UserIdentityRepository;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2LoginPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOAuthAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OAuth2LoginPolicy loginPolicy;

    @InjectMocks
    private UserOAuthAccountService userOAuthAccountService;

    @Test
    void resolvesDefaultRoleByNameWhenConfiguredRoleIdIsMissing() {
        Company company = Company.builder().companyName("Example").companyDomain("example.com").status("ACTIVE").build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user role").build();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("workspace", "provider-user-1")).thenReturn(Optional.empty());
        when(userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), "alice@example.com")).thenReturn(Optional.empty());
        when(loginPolicy.defaultRoleName()).thenReturn(RoleNames.CART_USER);
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.CART_USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userOAuthAccountService.resolveUserAfterCompanySetup(company, attributes, loginPolicy);

        assertThat(user.getRole().getRoleName()).isEqualTo(RoleNames.CART_USER);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsInactiveUserResolvedByIdentityBeforeCompanySetup() {
        Company company = Company.builder().companyName("Example").companyDomain("example.com").status("ACTIVE").build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user role").build();
        User user = User.createOAuthUser(company, role, "alice@example.com", "Alice", null);
        user.inactivate();
        UserIdentity identity = UserIdentity.create(user, "workspace", "provider-user-1", "alice@example.com", true);
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("workspace", "provider-user-1"))
                .thenReturn(Optional.of(identity));

        assertThatThrownBy(() -> userOAuthAccountService.resolveExistingUserByIdentity(attributes))
                .hasMessageContaining("User is not active");
    }

    @Test
    void returnsEmptyWhenIdentityBelongsToActiveDetachedUser() {
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user role").build();
        User user = User.builder()
                .company(null)
                .role(role)
                .companyAdmin(false)
                .email("alice@example.com")
                .fullName("Alice")
                .phone(null)
                .status("ACTIVE")
                .lastLoginAt(null)
                .build();
        UserIdentity identity = UserIdentity.create(user, "workspace", "provider-user-1", "alice@example.com", true);
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("workspace", "provider-user-1"))
                .thenReturn(Optional.of(identity));

        assertThat(userOAuthAccountService.resolveExistingUserByIdentity(attributes)).isEmpty();
    }

    @Test
    void reattachesDetachedUserAfterCompanySetup() {
        Company company = Company.builder().companyName("Example").companyDomain("example.com").status("ACTIVE").build();
        Role currentRole = Role.builder().roleName(RoleNames.APPROVER).description("approver").build();
        Role defaultRole = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User detachedUser = User.builder()
                .company(null)
                .role(currentRole)
                .companyAdmin(false)
                .email("alice@example.com")
                .fullName("Alice")
                .phone(null)
                .status("ACTIVE")
                .lastLoginAt(null)
                .build();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice Updated",
                "010-0000-0000",
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("workspace", "provider-user-1")).thenReturn(Optional.empty());
        when(userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), "alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(detachedUser));
        when(loginPolicy.defaultRoleName()).thenReturn(RoleNames.CART_USER);
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.CART_USER)).thenReturn(Optional.of(defaultRole));

        User user = userOAuthAccountService.resolveUserAfterCompanySetup(company, attributes, loginPolicy);

        assertThat(user.getCompany()).isEqualTo(company);
        assertThat(user.getRole().getRoleName()).isEqualTo(RoleNames.CART_USER);
        assertThat(user.getFullName()).isEqualTo("Alice Updated");
    }
}
