package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.CompanyProfileUpdateForm;
import com.bobbo01.supplyhub.domain.company.service.CompanyProfileService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/workspace/company")
public class WorkspaceCompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public WorkspaceCompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @PostMapping("/profile")
    public String updateCompanyProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @ModelAttribute CompanyProfileUpdateForm form,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }

        try {
            companyProfileService.updateCompanyProfile(
                    principal.getUserId(),
                    form.getCompanyName(),
                    form.getCompanyDomain()
            );
            redirectAttributes.addFlashAttribute("workspaceMessage", "회사 정보를 업데이트했습니다.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("workspaceError", ex.getMessage());
            redirectAttributes.addFlashAttribute("companyProfileForm", form);
        }

        return "redirect:/workspace?section=overview";
    }
}
