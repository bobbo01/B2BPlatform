package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.service.CompanyMembershipService;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import com.bobbo01.supplyhub.global.auth.oauth.UserAuthorities;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/workspace")
public class WorkspaceMembershipController {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final CompanyMembershipService companyMembershipService;

    public WorkspaceMembershipController(CompanyMembershipService companyMembershipService) {
        this.companyMembershipService = companyMembershipService;
    }

    @PostMapping("/leave")
    public String leave(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (principal == null) {
            return "redirect:/";
        }

        User updatedUser = companyMembershipService.leaveCompany(principal.getUserId());
        refreshAuthentication(request, response, updatedUser);
        return "redirect:/";
    }

    private void refreshAuthentication(HttpServletRequest request, HttpServletResponse response, User user) {
        AuthenticatedUserPrincipal refreshedPrincipal = AuthenticatedUserPrincipal.from(
                user,
                UserAuthorities.asSimpleGrantedAuthorities(user)
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                refreshedPrincipal,
                null,
                refreshedPrincipal.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
