package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.CompanyProfileUpdateForm;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyProfileService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceCompanyProfileControllerTest {

    @Mock
    private CompanyProfileService companyProfileService;

    @InjectMocks
    private WorkspaceCompanyProfileController workspaceCompanyProfileController;

    @Test
    void updatesCompanyProfileAndRedirectsToWorkspaceOverview() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");
        CompanyProfileUpdateForm form = form("New Name", "new.example.com");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = workspaceCompanyProfileController.updateCompanyProfile(principal, form, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=overview");
        assertThat(redirectAttributes.getFlashAttributes().get("workspaceMessage")).isEqualTo("회사 정보를 업데이트했습니다.");
        verify(companyProfileService).updateCompanyProfile(principal.getUserId(), "New Name", "new.example.com");
    }

    @Test
    void redirectsBackWithFlashErrorWhenUpdateFails() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");
        CompanyProfileUpdateForm form = form("New Name", "dup.example.com");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("The company domain is already in use."))
                .when(companyProfileService)
                .updateCompanyProfile(principal.getUserId(), "New Name", "dup.example.com");

        String viewName = workspaceCompanyProfileController.updateCompanyProfile(principal, form, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=overview");
        assertThat(redirectAttributes.getFlashAttributes().get("workspaceError"))
                .isEqualTo("The company domain is already in use.");
        assertThat(redirectAttributes.getFlashAttributes().get("companyProfileForm")).isSameAs(form);
    }

    private CompanyProfileUpdateForm form(String companyName, String companyDomain) {
        CompanyProfileUpdateForm form = new CompanyProfileUpdateForm();
        form.setCompanyName(companyName);
        form.setCompanyDomain(companyDomain);
        return form;
    }

    private AuthenticatedUserPrincipal principal(String email, String name) {
        return AuthenticatedUserPrincipal.from(
                User.createOAuthUser(
                        Company.builder().companyName("Example").status("ACTIVE").build(),
                        Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                        email,
                        name,
                        null
                ),
                java.util.List.of()
        );
    }
}
