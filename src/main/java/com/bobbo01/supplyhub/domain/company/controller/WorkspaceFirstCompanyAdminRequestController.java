package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.service.FirstCompanyAdminRequestService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/workspace/first-company-admin-request")
public class WorkspaceFirstCompanyAdminRequestController {

    private final FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    public WorkspaceFirstCompanyAdminRequestController(
            FirstCompanyAdminRequestService firstCompanyAdminRequestService
    ) {
        this.firstCompanyAdminRequestService = firstCompanyAdminRequestService;
    }

    @PostMapping
    public String firstCompanyAdminRequest(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/";
        }
        firstCompanyAdminRequestService.createRequest(principal.getUserId());
        return "redirect:/workspace";
    }

    @PostMapping("/cancel")
    public String cancelFirstCompanyAdminRequest(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/";
        }
        firstCompanyAdminRequestService.cancelOwnPendingRequest(principal.getUserId());
        return "redirect:/workspace";
    }
}
