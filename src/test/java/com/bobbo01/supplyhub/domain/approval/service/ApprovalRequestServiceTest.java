package com.bobbo01.supplyhub.domain.approval.service;

import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequest;
import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequestStatus;
import com.bobbo01.supplyhub.domain.approval.repository.ApprovalRequestRepository;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApprovalRequestService approvalRequestService;

    @Test
    void createsPendingApprovalForSubmittedRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User approver = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "approver@example.com",
                "Approver",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(approvalRequestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequest approvalRequest = approvalRequestService.createPendingApproval(10L, 2L);

        assertThat(approvalRequest.getStatus()).isEqualTo(ApprovalRequestStatus.PENDING);
        assertThat(approvalRequest.getApprover()).isEqualTo(approver);
    }

    @Test
    void approvingApprovalAlsoApprovesPurchaseRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User approver = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "approver@example.com",
                "Approver",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        ApprovalRequest approvalRequest = ApprovalRequest.createPending(company, purchaseRequest, requester, approver);

        ReflectionTestUtils.setField(approver, "id", 2L);
        when(approvalRequestRepository.findById(20L)).thenReturn(Optional.of(approvalRequest));

        approvalRequestService.approve(20L, 2L, "approved");

        assertThat(approvalRequest.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
    }

    @Test
    void rejectsApprovalOnlyWithDecisionNote() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User approver = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "approver@example.com",
                "Approver",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        ApprovalRequest approvalRequest = ApprovalRequest.createPending(company, purchaseRequest, requester, approver);

        ReflectionTestUtils.setField(approver, "id", 2L);
        when(approvalRequestRepository.findById(20L)).thenReturn(Optional.of(approvalRequest));

        assertThatThrownBy(() -> approvalRequestService.reject(20L, 2L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rejection reason is required.");
    }
}
