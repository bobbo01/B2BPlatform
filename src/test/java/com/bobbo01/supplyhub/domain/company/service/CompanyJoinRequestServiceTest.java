package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequest;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequestStatus;
import com.bobbo01.supplyhub.domain.company.repository.CompanyJoinRequestRepository;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyJoinRequestServiceTest {

    @Mock
    private CompanyJoinRequestRepository companyJoinRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOAuthAccountService userOAuthAccountService;

    @InjectMocks
    private CompanyJoinRequestService companyJoinRequestService;

    @Test
    void submitsPendingJoinRequestForActiveCompany() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);

        when(companyJoinRequestRepository.existsByProviderAndProviderUserIdAndStatus(
                "google",
                "provider-user-1",
                CompanyJoinRequestStatus.PENDING
        )).thenReturn(false);
        when(companyJoinRequestRepository.existsByCompanyIdAndRequestedEmailIgnoreCaseAndStatus(
                10L,
                "alice@example.com",
                CompanyJoinRequestStatus.PENDING
        )).thenReturn(false);
        when(companyJoinRequestRepository.save(any(CompanyJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyJoinRequest request = companyJoinRequestService.submitJoinRequest(
                company,
                "google",
                "provider-user-1",
                "alice@example.com",
                "Alice"
        );

        assertThat(request.getStatus()).isEqualTo(CompanyJoinRequestStatus.PENDING);
        assertThat(request.getRequestedRoleName()).isEqualTo(RoleNames.CART_USER);
    }

    @Test
    void approvesPendingJoinRequestWithinReviewerCompany() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User reviewer = User.createOAuthUser(company, role, "admin@example.com", "Admin", null);
        reviewer.grantCompanyAdmin();
        CompanyJoinRequest request = CompanyJoinRequest.createPending(
                company,
                "google",
                "provider-user-1",
                "alice@example.com",
                "Alice"
        );

        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyJoinRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        companyJoinRequestService.approveRequest(1L, 10L, "approved");

        verify(userOAuthAccountService).provisionApprovedCompanyJoinRequest(request);
        assertThat(request.getStatus()).isEqualTo(CompanyJoinRequestStatus.APPROVED);
        assertThat(request.getReviewedBy()).isEqualTo(reviewer);
        assertThat(request.getReviewMemo()).isEqualTo("approved");
    }

    @Test
    void rejectsDuplicatePendingJoinRequestForSameIdentity() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);

        when(companyJoinRequestRepository.existsByProviderAndProviderUserIdAndStatus(
                "google",
                "provider-user-1",
                CompanyJoinRequestStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> companyJoinRequestService.submitJoinRequest(
                company,
                "google",
                "provider-user-1",
                "alice@example.com",
                "Alice"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending join request");

        verify(companyJoinRequestRepository).existsByProviderAndProviderUserIdAndStatus(
                "google",
                "provider-user-1",
                CompanyJoinRequestStatus.PENDING
        );
    }

    @Test
    void requiresRejectionReasonWhenRejectingJoinRequest() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User reviewer = User.createOAuthUser(company, role, "admin@example.com", "Admin", null);
        reviewer.grantCompanyAdmin();
        CompanyJoinRequest request = CompanyJoinRequest.createPending(
                company,
                "google",
                "provider-user-1",
                "alice@example.com",
                "Alice"
        );

        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyJoinRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> companyJoinRequestService.rejectRequest(1L, 10L, "memo", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }
}
