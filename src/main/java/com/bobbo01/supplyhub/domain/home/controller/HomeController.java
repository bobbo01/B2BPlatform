package com.bobbo01.supplyhub.domain.home.controller;

import com.bobbo01.supplyhub.domain.home.service.HomeService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import com.bobbo01.supplyhub.global.auth.oauth.CustomOAuth2User;
import com.bobbo01.supplyhub.global.auth.oauth.CustomOidcUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/")
    public String home(Authentication authentication,
                       @AuthenticationPrincipal Object principal,
                       @RequestParam(name = "loginError", required = false) String loginError,
                       Model model) {
        HomeSessionUser sessionUser = resolveSessionUser(authentication, principal);
        model.addAttribute("isLoggedIn", sessionUser.loggedIn());
        model.addAttribute("user", sessionUser);
        model.addAttribute("ssoProviders", homeService.getSsoProviders());
        model.addAttribute("featuredProducts", homeService.getFeaturedProducts());
        model.addAttribute("loginErrorMessage", resolveLoginErrorMessage(loginError));
        return "pages/home";
    }

    private String resolveLoginErrorMessage(String loginError) {
        if (loginError == null || loginError.isBlank()) {
            return null;
        }
        return switch (loginError) {
            case "user_inactive" -> "비활성 사용자 계정입니다.";
            case "company_inactive" -> "비활성 회사 계정입니다.";
            default -> "로그인 처리 중 오류가 발생했습니다.";
        };
    }

    private HomeSessionUser resolveSessionUser(Authentication authentication, Object principal) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return HomeSessionUser.anonymous();
        }

        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            boolean linkedUser = homeService.isLinkedUser(authenticatedUserPrincipal.getUserId());
            boolean platformAdmin = isPlatformAdmin(authenticatedUserPrincipal);
            String companyName = linkedUser
                    ? homeService.getLinkedCompanyName(authenticatedUserPrincipal.getUserId())
                    : null;
            return linkedUser
                    ? HomeSessionUser.linked(
                    authenticatedUserPrincipal.getDisplayName(),
                    authenticatedUserPrincipal.getEmail(),
                    platformAdmin,
                    companyName
            )
                    : HomeSessionUser.pending(
                    authenticatedUserPrincipal.getDisplayName(),
                    authenticatedUserPrincipal.getEmail(),
                    platformAdmin,
                    null
            );
        }

        if (principal instanceof CustomOidcUser customOidcUser) {
            return HomeSessionUser.pending(
                    customOidcUser.getOauth2Attributes().resolvedName(),
                    customOidcUser.getEmail(),
                    false,
                    null
            );
        }

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            return HomeSessionUser.pending(
                    customOAuth2User.getOauth2Attributes().resolvedName(),
                    customOAuth2User.getEmail(),
                    false,
                    null
            );
        }

        return HomeSessionUser.pending(authentication.getName(), authentication.getName(), false, null);
    }

    private boolean isPlatformAdmin(AuthenticatedUserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals);
    }

    public record HomeSessionUser(
            boolean loggedIn,
            boolean linkedUser,
            String displayName,
            String email,
            boolean platformAdmin,
            String companyName
    ) {
        private static HomeSessionUser anonymous() {
            return new HomeSessionUser(false, false, null, null, false, null);
        }

        private static HomeSessionUser linked(
                String displayName,
                String email,
                boolean platformAdmin,
                String companyName
        ) {
            return new HomeSessionUser(true, true, displayName, email, platformAdmin, companyName);
        }

        private static HomeSessionUser pending(
                String displayName,
                String email,
                boolean platformAdmin,
                String companyName
        ) {
            return new HomeSessionUser(true, false, displayName, email, platformAdmin, companyName);
        }
    }
}

