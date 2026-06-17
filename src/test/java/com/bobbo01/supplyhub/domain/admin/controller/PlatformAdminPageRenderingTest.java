package com.bobbo01.supplyhub.domain.admin.controller;

import com.bobbo01.supplyhub.domain.admin.dto.PlatformApprovalOrderView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminPageView;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminPageService;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminService;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyRegistrationRequestService;
import com.bobbo01.supplyhub.domain.company.service.FirstCompanyAdminRequestService;
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

@WebMvcTest(PlatformAdminController.class)
class PlatformAdminPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformAdminPageService platformAdminPageService;

    @MockBean
    private PlatformAdminService platformAdminService;

    @MockBean
    private CompanyRegistrationRequestService companyRegistrationRequestService;

    @MockBean
    private FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void rendersOrderActionsPerOrderStatusOnAllFilter() throws Exception {
        AuthenticatedUserPrincipal principal = principal();
        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(true);
        when(platformAdminPageService.getOrdersPage(eq(principal.getUserId()), eq("all"))).thenReturn(
                new PlatformAdminPageView(
                        new WorkspaceUserView(principal.getUserId(), "Admin", "admin@example.com", true, "PLATFORM_ADMIN", false, null),
                        "orders",
                        "all",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                pendingOrderView(30L),
                                paidOrderView(31L),
                                rejectedOrderView(32L)
                        ),
                        null,
                        List.of()
                )
        );

        String content = mockMvc.perform(get("/admin/orders")
                        .param("orderFilter", "all")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                authenticatedToken(principal)
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(countOccurrences(content, "/admin/orders/approve")).isEqualTo(1);
        assertThat(countOccurrences(content, "/admin/orders/reject")).isEqualTo(1);
        assertThat(countOccurrences(content, "/admin/orders/ready-to-ship")).isEqualTo(1);
        assertThat(content).doesNotContain("/admin/orders/ship");
        assertThat(content).doesNotContain("/admin/orders/deliver");
        assertThat(content).contains("Rejected orders are read-only.");
        assertThat(content).contains("/js/admin-navigation.js");
    }

    private PlatformApprovalOrderView pendingOrderView(Long purchaseOrderId) {
        return new PlatformApprovalOrderView(
                purchaseOrderId,
                10L,
                1L,
                "Example",
                "Buyer",
                "buyer@example.com",
                1,
                new BigDecimal("10.00"),
                "PENDING_PLATFORM_APPROVAL",
                "Pending Platform Approval",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                true,
                false,
                false,
                false,
                true,
                null,
                List.of()
        );
    }

    private PlatformApprovalOrderView paidOrderView(Long purchaseOrderId) {
        return new PlatformApprovalOrderView(
                purchaseOrderId,
                11L,
                1L,
                "Example",
                "Buyer",
                "buyer@example.com",
                1,
                new BigDecimal("20.00"),
                "PAID",
                "Paid",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                true,
                false,
                false,
                true,
                null,
                List.of()
        );
    }

    private PlatformApprovalOrderView rejectedOrderView(Long purchaseOrderId) {
        return new PlatformApprovalOrderView(
                purchaseOrderId,
                12L,
                1L,
                "Example",
                "Buyer",
                "buyer@example.com",
                1,
                new BigDecimal("30.00"),
                "REJECTED",
                "Rejected",
                null,
                null,
                null,
                null,
                null,
                null,
                "Admin",
                "memo",
                "policy mismatch",
                false,
                false,
                false,
                false,
                false,
                false,
                "Rejected orders are read-only.",
                List.of()
        );
    }

    private AuthenticatedUserPrincipal principal() {
        User user = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return AuthenticatedUserPrincipal.from(user, List.of());
    }

    private int countOccurrences(String content, String token) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private TestingAuthenticationToken authenticatedToken(AuthenticatedUserPrincipal principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );
        authentication.setAuthenticated(true);
        return authentication;
    }
}
