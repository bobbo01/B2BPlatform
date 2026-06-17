package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.service.CompanyJoinRequestService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/workspace/company-join-request")
public class WorkspaceCompanyJoinReviewController {

    private final CompanyJoinRequestService companyJoinRequestService;

    public WorkspaceCompanyJoinReviewController(CompanyJoinRequestService companyJoinRequestService) {
        this.companyJoinRequestService = companyJoinRequestService;
    }

    @PostMapping("/approve")
    public String approveCompanyJoinRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyJoinRequestService.approveRequest(principal.getUserId(), requestId, reviewMemo);
        return "redirect:/workspace";
    }

    @PostMapping("/reject")
    public String rejectCompanyJoinRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo,
            @RequestParam("rejectionReason") String rejectionReason
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyJoinRequestService.rejectRequest(principal.getUserId(), requestId, reviewMemo, rejectionReason);
        return "redirect:/workspace";
    }
}
