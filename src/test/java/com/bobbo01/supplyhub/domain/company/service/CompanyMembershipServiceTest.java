package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyMembershipServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyMembershipService companyMembershipService;

    @Test
    void detachesUserFromCompanyWhenLeaving() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .creatorUserId(1L)
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User user = User.createOAuthUser(company, role, "alice@example.com", "Alice", null);
        user.grantCompanyAdmin();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        companyMembershipService.leaveCompany(1L);

        assertThat(user.isActive()).isTrue();
        assertThat(user.getCompany()).isNull();
        assertThat(user.hasCompanyAdminRole()).isFalse();
        verify(userRepository, never()).delete(user);
    }

    @Test
    void rejectsLeaveCompanyForPlatformAdmin() {
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        User user = User.createPlatformAdmin(role, "alice@example.com", "Alice", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> companyMembershipService.leaveCompany(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PLATFORM_ADMIN account cannot leave a company.");

        verify(userRepository, never()).delete(user);
    }
}
