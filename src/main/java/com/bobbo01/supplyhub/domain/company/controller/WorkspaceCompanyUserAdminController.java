package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.service.CompanyUserAdminService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/workspace/company-users")
public class WorkspaceCompanyUserAdminController {

    private final CompanyUserAdminService companyUserAdminService;

    public WorkspaceCompanyUserAdminController(CompanyUserAdminService companyUserAdminService) {
        this.companyUserAdminService = companyUserAdminService;
    }

    @PostMapping("/purchasing-role")
    public String updateCompanyUserPurchasingRole(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("roleName") String roleName
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyUserAdminService.updateUserPurchasingRole(principal.getUserId(), userId, roleName);
        return "redirect:/workspace?section=company-users";
    }

    @PostMapping("/status")
    public String updateCompanyUserStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("status") String status
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyUserAdminService.updateUserStatus(principal.getUserId(), userId, status);
        return "redirect:/workspace?section=company-users";
    }

    @PostMapping("/company-admin")
    public String updateCompanyUserCompanyAdmin(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("companyAdmin") boolean companyAdmin
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyUserAdminService.updateUserCompanyAdmin(principal.getUserId(), userId, companyAdmin);
        return "redirect:/workspace?section=company-users";
    }
}
