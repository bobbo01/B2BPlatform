package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.CompanyJoinRequestView;
import com.bobbo01.supplyhub.domain.company.dto.CompanyRegistrationRequestView;
import com.bobbo01.supplyhub.domain.company.dto.FirstLoginCompanySetupForm;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/workspace")
public class WorkspaceOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceOnboardingController.class);

    private final FirstLoginCompanySetupService firstLoginCompanySetupService;

    public WorkspaceOnboardingController(
            FirstLoginCompanySetupService firstLoginCompanySetupService
    ) {
        this.firstLoginCompanySetupService = firstLoginCompanySetupService;
    }

    @PostMapping
    public String completeWorkspaceCompanySetup(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @ModelAttribute("form") FirstLoginCompanySetupForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model
    ) {
        if (principal != null) {
            return "redirect:/workspace";
        }

        FirstLoginCompanySetupState state;
        java.util.Optional<CompanyRegistrationRequestView> companyRegistrationRequest;
        java.util.Optional<CompanyJoinRequestView> companyJoinRequest;

        try {
            state = firstLoginCompanySetupService.getRequiredState(session);
            companyRegistrationRequest = firstLoginCompanySetupService.findLatestRegistrationRequest(session);
            companyJoinRequest = firstLoginCompanySetupService.findLatestJoinRequest(session);
        } catch (IllegalStateException ex) {
            return "redirect:/";
        }

        if (form.useInviteCode()) {
            if (isBlank(form.getInviteCode())) {
                populatePendingSetupModel(model, state, companyRegistrationRequest.orElse(null), companyJoinRequest.orElse(null), form, "Invite code is required.", "join");
                return "pages/workspace";
            }
        } else {
            if (isBlank(form.getCompanyName())) {
                bindingResult.rejectValue("companyName", "NotBlank", "Company name is required.");
            }
            if (isBlank(form.getCompanyDomain())) {
                bindingResult.rejectValue("companyDomain", "NotBlank", "Company domain is required.");
            }
            if (bindingResult.hasErrors()) {
                populatePendingSetupModel(model, state, companyRegistrationRequest.orElse(null), companyJoinRequest.orElse(null), form, null, "register");
                return "pages/workspace";
            }
        }

        try {
            if (!form.useInviteCode()) {
                firstLoginCompanySetupService.submitRegistrationRequest(session, form.getCompanyName(), form.getCompanyDomain());
                return "redirect:/workspace";
            }
            firstLoginCompanySetupService.submitJoinRequest(
                session,
                    form.getInviteCode()
            );
            return "redirect:/workspace?mode=join";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("First login company setup failed: email={}, action={}, companyName={}, existingCompanyId={}, reason={}",
                    state.email(), form.getAction(), form.getCompanyName(), null, ex.getMessage(), ex);
            populatePendingSetupModel(
                    model,
                    state,
                    companyRegistrationRequest.orElse(null),
                    companyJoinRequest.orElse(null),
                    form,
                    ex.getMessage(),
                    form.useInviteCode() ? "join" : "register"
            );
            return "pages/workspace";
        }
    }

    @PostMapping("/company-join-request/cancel")
    public String cancelCompanyJoinRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpSession session
    ) {
        if (principal != null) {
            return "redirect:/workspace";
        }
        firstLoginCompanySetupService.cancelOwnPendingJoinRequest(session);
        return "redirect:/workspace?mode=join";
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
