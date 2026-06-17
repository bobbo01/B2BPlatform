package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String POST_LOGIN_REDIRECT_PATH = "/";
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    private final OAuth2AccountService oauth2AccountService;
    private final FirstLoginCompanySetupService firstLoginCompanySetupService;

    public OAuth2LoginSuccessHandler(
            OAuth2AccountService oauth2AccountService,
            FirstLoginCompanySetupService firstLoginCompanySetupService
    ) {
        this.oauth2AccountService = oauth2AccountService;
        this.firstLoginCompanySetupService = firstLoginCompanySetupService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            OAuth2Attributes attributes = extractAttributes(authentication);
            Optional<User> existingUser = oauth2AccountService.resolveExistingUserByIdentity(attributes);
            if (existingUser.isPresent()) {
                authenticateUser(request, response, existingUser.get());
                firstLoginCompanySetupService.clearPendingState(request.getSession(true));
                response.sendRedirect(POST_LOGIN_REDIRECT_PATH);
                return;
            }

            if (oauth2AccountService.isPlatformAdminEmail(attributes)) {
                User platformAdmin = oauth2AccountService.resolvePlatformAdmin(attributes);
                authenticateUser(request, response, platformAdmin);
                firstLoginCompanySetupService.clearPendingState(request.getSession(true));
                response.sendRedirect(POST_LOGIN_REDIRECT_PATH);
                return;
            }

            Optional<Company> company = oauth2AccountService.findActiveCompanyByEmailDomain(attributes);

            if (company.isPresent()) {
                User user = oauth2AccountService.resolveUser(company.get(), attributes);
                authenticateUser(request, response, user);
                firstLoginCompanySetupService.clearPendingState(request.getSession(true));
                response.sendRedirect(POST_LOGIN_REDIRECT_PATH);
                return;
            }

            HttpSession session = request.getSession(true);
            firstLoginCompanySetupService.storePendingState(session, attributes);
            preserveAuthentication(request, response, authentication);
            response.sendRedirect(POST_LOGIN_REDIRECT_PATH);
        } catch (OAuth2AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            request.getSession(true).removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            response.sendRedirect("/?loginError=" + ex.getError().getErrorCode());
        }
    }

    private OAuth2Attributes extractAttributes(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOidcUser oidcUser) {
            return oidcUser.getOauth2Attributes();
        }
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getOauth2Attributes();
        }
        throw new IllegalStateException("지원하지 않는 OAuth principal 타입입니다.");
    }

    private void authenticateUser(HttpServletRequest request, HttpServletResponse response, User user) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(
                user,
                UserAuthorities.asSimpleGrantedAuthorities(user)
        );
        UsernamePasswordAuthenticationToken localAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(localAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void preserveAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
