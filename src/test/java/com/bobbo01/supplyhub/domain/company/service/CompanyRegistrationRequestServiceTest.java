package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.CompanyRegistrationRequest;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRegistrationRequestRepository;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserIdentityRepository;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyRegistrationRequestServiceTest {

    @Mock
    private CompanyRegistrationRequestRepository companyRegistrationRequestRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyInviteCodeService companyInviteCodeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CompanyRegistrationRequestService companyRegistrationRequestService;

    @Test
    void submitsPendingRequest() {
        FirstLoginCompanySetupState state = new FirstLoginCompanySetupState(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                "example.com",
                false,
                "Alice",
                "010-1111-2222",
                true,
                Instant.now(),
                Instant.now().plusSeconds(300),
                false
        );

        when(companyRegistrationRequestRepository.findByProviderAndProviderUserIdAndStatus(
                "workspace",
                "provider-user-1",
                CompanyRegistrationRequest.STATUS_PENDING
        )).thenReturn(Optional.empty());
        when(companyRegistrationRequestRepository.findByRequestedCompanyDomainIgnoreCaseAndStatus(
                "portal.example.com",
                CompanyRegistrationRequest.STATUS_PENDING
        )).thenReturn(Optional.empty());
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(false);
        when(companyRegistrationRequestRepository.save(any(CompanyRegistrationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyRegistrationRequest request = companyRegistrationRequestService.submitRequest(
                state,
                "Example",
                "portal.example.com"
        );

        assertThat(request.getStatus()).isEqualTo(CompanyRegistrationRequest.STATUS_PENDING);
        assertThat(request.getRequestedCompanyDomain()).isEqualTo("portal.example.com");
    }

    @Test
    void approvesRequestAndCreatesCompanyUserAndIdentity() {
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(reviewer, "id", 2L);
        CompanyRegistrationRequest request = CompanyRegistrationRequest.createPending(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                "010-1111-2222",
                "Example",
                "portal.example.com"
        );
        Company savedCompany = Company.builder()
                .companyName("Example")
                .companyDomain("portal.example.com")
                .inviteCode("INVITE1234")
                .status("ACTIVE")
                .build();
        Role cartUserRole = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User savedUser = User.createOAuthUser(savedCompany, cartUserRole, "alice@example.com", "Alice", "010-1111-2222");
        ReflectionTestUtils.setField(savedUser, "id", 10L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(companyRegistrationRequestRepository.findById(20L)).thenReturn(Optional.of(request));
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(false);
        when(userIdentityRepository.findByProviderAndProviderUserId("workspace", "provider-user-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.CART_USER)).thenReturn(Optional.of(cartUserRole));
        when(companyInviteCodeService.generateUniqueInviteCode()).thenReturn("INVITE1234");
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userIdentityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(request, "id", 20L);

        companyRegistrationRequestService.approveRequest(2L, 20L, "looks good");

        assertThat(request.getStatus()).isEqualTo(CompanyRegistrationRequest.STATUS_APPROVED);
        assertThat(request.getReviewMemo()).isEqualTo("looks good");
        verify(userIdentityRepository).save(any());
    }

    @Test
    void rejectsApprovalWhenDomainAlreadyExists() {
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        CompanyRegistrationRequest request = CompanyRegistrationRequest.createPending(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "Example",
                "portal.example.com"
        );
        ReflectionTestUtils.setField(reviewer, "id", 2L);
        ReflectionTestUtils.setField(request, "id", 20L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(companyRegistrationRequestRepository.findById(20L)).thenReturn(Optional.of(request));
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(true);

        assertThatThrownBy(() -> companyRegistrationRequestService.approveRequest(2L, 20L, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresRejectionReasonWhenRejectingRequest() {
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        CompanyRegistrationRequest request = CompanyRegistrationRequest.createPending(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "Example",
                "portal.example.com"
        );
        ReflectionTestUtils.setField(reviewer, "id", 2L);
        ReflectionTestUtils.setField(request, "id", 20L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(companyRegistrationRequestRepository.findById(20L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> companyRegistrationRequestService.rejectRequest(2L, 20L, "memo", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }
}
