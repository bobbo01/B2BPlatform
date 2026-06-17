package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.service.CompanyInviteCodeService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/workspace/company/invite-code")
public class WorkspaceInviteCodeController {

    private final CompanyInviteCodeService companyInviteCodeService;

    public WorkspaceInviteCodeController(CompanyInviteCodeService companyInviteCodeService) {
        this.companyInviteCodeService = companyInviteCodeService;
    }

    @PostMapping("/regenerate")
    public String regenerateCompanyInviteCode(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/";
        }
        companyInviteCodeService.regenerateInviteCode(principal.getUserId());
        return "redirect:/workspace?section=overview";
    }

    @PostMapping("/revoke")
    public String revokeCompanyInviteCode(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/";
        }
        companyInviteCodeService.revokeInviteCode(principal.getUserId());
        return "redirect:/workspace?section=overview";
    }
}
