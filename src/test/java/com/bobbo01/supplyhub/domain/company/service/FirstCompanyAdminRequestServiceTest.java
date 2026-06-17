package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.FirstCompanyAdminRequest;
import com.bobbo01.supplyhub.domain.company.repository.FirstCompanyAdminRequestRepository;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstCompanyAdminRequestServiceTest {

    @Mock
    private FirstCompanyAdminRequestRepository firstCompanyAdminRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    @Test
    void createsFirstCompanyAdminRequestForEligibleCreator() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .creatorUserId(1L)
                .build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(firstCompanyAdminRequestRepository.findByRequesterIdAndStatus(1L, FirstCompanyAdminRequest.STATUS_PENDING))
                .thenReturn(Optional.empty());
        when(firstCompanyAdminRequestRepository.findByCompanyIdAndStatus(company.getId(), FirstCompanyAdminRequest.STATUS_PENDING))
                .thenReturn(Optional.empty());

        firstCompanyAdminRequestService.createRequest(1L);

        verify(firstCompanyAdminRequestRepository).save(any(FirstCompanyAdminRequest.class));
    }

    @Test
    void approvesPendingRequestAndGrantsCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .creatorUserId(1L)
                .build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(reviewer, "id", 2L);
        FirstCompanyAdminRequest request = FirstCompanyAdminRequest.createPending(company, requester);

        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(firstCompanyAdminRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        firstCompanyAdminRequestService.approveRequest(2L, 10L);

        assertThat(requester.hasCompanyAdminRole()).isTrue();
        assertThat(request.getStatus()).isEqualTo(FirstCompanyAdminRequest.STATUS_APPROVED);
    }

    @Test
    void rejectsRequestWhenUserIsNotEligibleCreator() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .creatorUserId(99L)
                .build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> firstCompanyAdminRequestService.createRequest(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("회사 생성 완료를 최종 성공시킨 사용자만");

        verify(firstCompanyAdminRequestRepository, never()).save(any(FirstCompanyAdminRequest.class));
    }

    @Test
    void canCreateRequestReturnsFalseWhenPendingExists() {
        Company company = Company.builder()
                .companyName("Example")
                .status("ACTIVE")
                .creatorUserId(1L)
                .build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        FirstCompanyAdminRequest pendingRequest = FirstCompanyAdminRequest.createPending(company, requester);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(firstCompanyAdminRequestRepository.findByRequesterIdAndStatus(eq(1L), eq(FirstCompanyAdminRequest.STATUS_PENDING)))
                .thenReturn(Optional.of(pendingRequest));

        assertThat(firstCompanyAdminRequestService.canCreateRequest(1L)).isFalse();
    }
}
