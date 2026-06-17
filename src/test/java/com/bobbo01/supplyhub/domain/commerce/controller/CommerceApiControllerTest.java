package com.bobbo01.supplyhub.domain.commerce.controller;

import com.bobbo01.supplyhub.domain.commerce.dto.CommerceActionResponse;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderItemDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderProgressStepView;
import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommerceApiControllerTest {

    @Mock
    private CommerceWorkflowService commerceWorkflowService;

    @InjectMocks
    private CommerceApiController commerceApiController;

    @Test
    void returnsPurchaseOrderDetailJsonPayload() {
        AuthenticatedUserPrincipal principal = principal(7L);
        PurchaseOrderDetailView detailView = purchaseOrderDetailView(66L);
        when(commerceWorkflowService.getPurchaseOrderDetail(7L, 66L)).thenReturn(detailView);

        PurchaseOrderDetailView response = commerceApiController.purchaseOrderDetail(principal, 66L);

        assertThat(response.purchaseOrderId()).isEqualTo(66L);
        assertThat(response.items()).hasSize(1);
        verify(commerceWorkflowService).getPurchaseOrderDetail(7L, 66L);
    }

    @Test
    void submitsPurchaseOrderAndReturnsActionResponse() {
        AuthenticatedUserPrincipal principal = principal(7L);

        CommerceActionResponse response = commerceApiController
                .submitPurchaseOrderForPlatformApproval(principal, 66L)
                .getBody();

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.purchaseOrderId()).isEqualTo(66L);
        verify(commerceWorkflowService).submitPurchaseOrderForPlatformApproval(7L, 66L);
    }

    @Test
    void cancelsPurchaseOrderAndReturnsActionResponse() {
        AuthenticatedUserPrincipal principal = principal(7L);

        CommerceActionResponse response = commerceApiController.cancelPurchaseOrder(principal, 66L).getBody();

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.purchaseOrderId()).isEqualTo(66L);
        verify(commerceWorkflowService).cancelPurchaseOrder(7L, 66L);
    }

    @Test
    void paysPurchaseOrderAndReturnsActionResponse() {
        AuthenticatedUserPrincipal principal = principal(7L);

        CommerceActionResponse response = commerceApiController.payPurchaseOrder(principal, 66L).getBody();

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.purchaseOrderId()).isEqualTo(66L);
        verify(commerceWorkflowService).payPurchaseOrder(7L, 66L);
    }

    private AuthenticatedUserPrincipal principal(Long userId) {
        User user = User.createOAuthUser(
                Company.builder().companyName("Example").status("ACTIVE").build(),
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return AuthenticatedUserPrincipal.from(user, List.of());
    }

    private PurchaseOrderDetailView purchaseOrderDetailView(Long purchaseOrderId) {
        return new PurchaseOrderDetailView(
                purchaseOrderId,
                33L,
                "DRAFT",
                "Draft",
                "Review the draft",
                "Check items before submitting.",
                true,
                null,
                null,
                false,
                1,
                new BigDecimal("10.00"),
                true,
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(new PurchaseOrderProgressStepView("DRAFT", "Draft", false, true)),
                List.of(new PurchaseOrderStatusHistoryView(
                        "DRAFT",
                        "Draft",
                        "DRAFT",
                        "Draft",
                        "User",
                        null,
                        null,
                        "Draft created",
                        "User",
                        "-"
                )),
                List.of(new PurchaseOrderItemDetailView(
                        1L,
                        3L,
                        "Keyboard",
                        1,
                        new BigDecimal("10.00"),
                        "KRW",
                        new BigDecimal("10.00")
                ))
        );
    }
}
