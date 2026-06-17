package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.WorkspacePageView;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.company.service.WorkspaceViewService;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderSummaryView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceCompanyView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspacePageController.class)
class WorkspacePageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirstLoginCompanySetupService firstLoginCompanySetupService;

    @MockBean
    private WorkspaceViewService workspaceViewService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void rendersAsyncOrderDetailHooksForSelectedOrder() throws Exception {
        AuthenticatedUserPrincipal principal = principal("buyer@example.com", "Buyer");
        when(workspaceViewService.getWorkspacePage(eq(principal.getUserId()), eq("commerce"), eq("order-drafts"), eq(99L)))
                .thenReturn(workspacePageView(99L));

        String content = mockMvc.perform(get("/workspace")
                        .param("section", "commerce")
                        .param("commerceSection", "order-drafts")
                        .param("orderId", "99")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                authenticatedToken(principal, "ROLE_USER")
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-selected-order-id=\"99\"");
        assertThat(content).contains("data-order-toggle=\"99\"");
        assertThat(content).contains("data-order-detail-panel=\"99\"");
        assertThat(content).contains("/api/commerce/purchase-orders/99");
        assertThat(content).contains("/js/workspace-order-drafts.js");
        assertThat(content).contains("hidden");
        assertThat(content).doesNotContain("/commerce/purchase-orders/submit-for-platform-approval");
    }

    @Test
    void rendersAsyncOrderDetailHooksWithoutSelectedOrder() throws Exception {
        AuthenticatedUserPrincipal principal = principal("buyer@example.com", "Buyer");
        when(workspaceViewService.getWorkspacePage(eq(principal.getUserId()), eq("commerce"), eq("order-drafts"), eq(null)))
                .thenReturn(workspacePageView(null));

        String content = mockMvc.perform(get("/workspace")
                        .param("section", "commerce")
                        .param("commerceSection", "order-drafts")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                authenticatedToken(principal, "ROLE_USER")
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-order-toggle=\"99\"");
        assertThat(content).contains("data-order-detail-panel=\"99\"");
        assertThat(content).contains("주문 상세 보기");
        assertThat(content).doesNotContain("data-selected-order-id=\"99\"");
    }

    private WorkspacePageView workspacePageView(Long selectedPurchaseOrderId) {
        return new WorkspacePageView(
                new WorkspaceUserView(
                        1L,
                        "Buyer",
                        "buyer@example.com",
                        false,
                        RoleNames.APPROVER,
                        false,
                        new WorkspaceCompanyView(1L, "Example", "example.com", "INVITE", "ACTIVE")
                ),
                "commerce",
                false,
                null,
                List.of(),
                List.of(),
                List.of(),
                new CommerceWorkspaceView(
                        true,
                        true,
                        true,
                        null,
                        List.of(),
                        List.of(),
                        List.of(new PurchaseOrderSummaryView(
                                99L,
                                33L,
                                "DRAFT",
                                "Draft",
                                1,
                                new BigDecimal("10.00")
                        ))
                ),
                "order-drafts",
                selectedPurchaseOrderId
        );
    }

    private AuthenticatedUserPrincipal principal(String email, String name) {
        User user = User.createOAuthUser(
                com.bobbo01.supplyhub.domain.company.entity.Company.builder()
                        .companyName("Example")
                        .status("ACTIVE")
                        .build(),
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                email,
                name,
                null
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return AuthenticatedUserPrincipal.from(user, List.of());
    }

    private TestingAuthenticationToken authenticatedToken(AuthenticatedUserPrincipal principal, String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
        authentication.setAuthenticated(true);
        return authentication;
    }
}
