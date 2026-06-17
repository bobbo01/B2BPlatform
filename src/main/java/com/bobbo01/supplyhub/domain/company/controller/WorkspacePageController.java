package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.CompanyJoinRequestView;
import com.bobbo01.supplyhub.domain.company.dto.CompanyProfileUpdateForm;
import com.bobbo01.supplyhub.domain.company.dto.CompanyRegistrationRequestView;
import com.bobbo01.supplyhub.domain.company.dto.FirstLoginCompanySetupForm;
import com.bobbo01.supplyhub.domain.company.dto.WorkspacePageView;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.company.service.WorkspaceViewService;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/workspace")
public class WorkspacePageController {

    private static final Logger log = LoggerFactory.getLogger(WorkspacePageController.class);

    private final FirstLoginCompanySetupService firstLoginCompanySetupService;
    private final WorkspaceViewService workspaceViewService;

    public WorkspacePageController(
            FirstLoginCompanySetupService firstLoginCompanySetupService,
            WorkspaceViewService workspaceViewService
    ) {
        this.firstLoginCompanySetupService = firstLoginCompanySetupService;
        this.workspaceViewService = workspaceViewService;
    }

    @GetMapping
    public String workspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "section", required = false) String section,
            @RequestParam(name = "commerceSection", required = false) String commerceSection,
            @RequestParam(name = "orderId", required = false) Long orderId,
            HttpSession session,
            Model model
    ) {
        if (principal != null) {
            WorkspacePageView workspacePageView = workspaceViewService.getWorkspacePage(
                    principal.getUserId(),
                    section,
                    commerceSection,
                    orderId
            );
            if (workspacePageView.workspaceUser().platformAdmin()) {
                return "redirect:/admin/companies";
            }
            model.addAttribute("user", principal);
            model.addAttribute("workspaceUser", workspacePageView.workspaceUser());
            model.addAttribute("workspaceCompany", workspacePageView.workspaceUser().company());
            model.addAttribute("activeSection", workspacePageView.activeSection());
            model.addAttribute("canCreateFirstCompanyAdminRequest", workspacePageView.canCreateFirstCompanyAdminRequest());
            model.addAttribute("firstCompanyAdminRequest", workspacePageView.firstCompanyAdminRequest());
            model.addAttribute("pendingCompanyJoinRequests", workspacePageView.pendingCompanyJoinRequests());
            model.addAttribute("companyUserAdminUsers", workspacePageView.companyUserAdminUsers());
            model.addAttribute("companyUserPurchasingRoleOptions", workspacePageView.companyUserPurchasingRoleOptions());
            model.addAttribute("commerceView", workspacePageView.commerceView());
            model.addAttribute("activeCommerceSection", workspacePageView.activeCommerceSection());
            model.addAttribute("selectedPurchaseOrderId", workspacePageView.selectedPurchaseOrderId());
            if (!model.containsAttribute("companyProfileForm") && workspacePageView.workspaceUser().company() != null) {
                CompanyProfileUpdateForm companyProfileForm = new CompanyProfileUpdateForm();
                companyProfileForm.setCompanyName(workspacePageView.workspaceUser().company().companyName());
                companyProfileForm.setCompanyDomain(workspacePageView.workspaceUser().company().companyDomain());
                model.addAttribute("companyProfileForm", companyProfileForm);
            }
            model.addAttribute("pendingSetup", false);
            return "pages/workspace";
        }

        try {
            FirstLoginCompanySetupState state = firstLoginCompanySetupService.getRequiredState(session);
            java.util.Optional<CompanyRegistrationRequestView> companyRegistrationRequest =
                    firstLoginCompanySetupService.findLatestRegistrationRequest(session);
            java.util.Optional<CompanyJoinRequestView> companyJoinRequest =
                    firstLoginCompanySetupService.findLatestJoinRequest(session);
            FirstLoginCompanySetupForm form = new FirstLoginCompanySetupForm();
            populatePendingSetupModel(
                    model,
                    state,
                    companyRegistrationRequest.orElse(null),
                    companyJoinRequest.orElse(null),
                    form,
                    null,
                    resolveSetupMode(mode)
            );
            return "pages/workspace";
        } catch (IllegalStateException ex) {
            log.warn("Workspace request had no valid auth or pending setup state: sessionId={}", session.getId(), ex);
            return "redirect:/";
        }
    }

    private void populatePendingSetupModel(
            Model model,
            FirstLoginCompanySetupState state,
            CompanyRegistrationRequestView companyRegistrationRequest,
            CompanyJoinRequestView companyJoinRequest,
            FirstLoginCompanySetupForm form,
            String errorMessage,
            String setupMode
    ) {
        if (!form.useInviteCode() && isBlank(form.getCompanyDomain())) {
            form.setCompanyDomain(state.emailDomain());
        }
        model.addAttribute("pendingSetup", true);
        model.addAttribute("state", state);
        model.addAttribute("companyRegistrationRequest", companyRegistrationRequest);
        model.addAttribute("companyJoinRequest", companyJoinRequest);
        model.addAttribute("form", form);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("setupMode", setupMode);
    }

    private String resolveSetupMode(String mode) {
        if ("join".equalsIgnoreCase(mode)) {
            return "join";
        }
        return "register";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
