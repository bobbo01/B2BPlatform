package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.FirstLoginCompanySetupForm;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceOnboardingControllerTest {

    @Mock
    private FirstLoginCompanySetupService firstLoginCompanySetupService;

    @InjectMocks
    private WorkspaceOnboardingController workspaceOnboardingController;

    @Test
    void redirectsToWorkspaceAfterSuccessfulCompanyRegistration() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        FirstLoginCompanySetupForm form = new FirstLoginCompanySetupForm();
        form.setCompanyName("Example");
        form.setCompanyDomain("portal.example.com");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        FirstLoginCompanySetupState state = pendingSetupState("alice@example.com", "example.com", false);
        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspaceOnboardingController.completeWorkspaceCompanySetup(
                null,
                form,
                bindingResult,
                request,
                response,
                session,
                new org.springframework.ui.ConcurrentModel()
        );

        assertThat(viewName).isEqualTo("redirect:/workspace");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(firstLoginCompanySetupService).submitRegistrationRequest(eq(session), eq("Example"), eq("portal.example.com"));
    }

    @Test
    void requiresCompanyNameWhenRegisteringWithPublicEmailDomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        FirstLoginCompanySetupForm form = new FirstLoginCompanySetupForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        FirstLoginCompanySetupState state = pendingSetupState("alice@gmail.com", "gmail.com", true);
        org.springframework.ui.ConcurrentModel model = new org.springframework.ui.ConcurrentModel();

        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspaceOnboardingController.completeWorkspaceCompanySetup(
                null,
                form,
                bindingResult,
                request,
                response,
                session,
                model
        );

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("setupMode")).isEqualTo("register");
        assertThat(bindingResult.hasFieldErrors("companyName")).isTrue();
        assertThat(bindingResult.hasFieldErrors("companyDomain")).isTrue();
    }

    @Test
    void requiresCompanyDomainWhenRegistering() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        FirstLoginCompanySetupForm form = new FirstLoginCompanySetupForm();
        form.setCompanyName("Example");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        FirstLoginCompanySetupState state = pendingSetupState("alice@example.com", "example.com", false);
        org.springframework.ui.ConcurrentModel model = new org.springframework.ui.ConcurrentModel();

        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspaceOnboardingController.completeWorkspaceCompanySetup(
                null,
                form,
                bindingResult,
                request,
                response,
                session,
                model
        );

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(bindingResult.hasFieldErrors("companyDomain")).isTrue();
    }

    @Test
    void submitsJoinRequestForInviteCodeInsteadOfAuthenticating() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        FirstLoginCompanySetupForm form = new FirstLoginCompanySetupForm();
        form.setAction("useInviteCode");
        form.setInviteCode("INVITE-123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        FirstLoginCompanySetupState state = pendingSetupState("alice@example.com", "example.com", false);

        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspaceOnboardingController.completeWorkspaceCompanySetup(
                null,
                form,
                bindingResult,
                request,
                response,
                session,
                new org.springframework.ui.ConcurrentModel()
        );

        assertThat(viewName).isEqualTo("redirect:/workspace?mode=join");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(firstLoginCompanySetupService).submitJoinRequest(eq(session), eq("INVITE-123"));
    }

    @Test
    void cancelsPendingJoinRequestFromPendingSetupSession() {
        MockHttpSession session = new MockHttpSession();

        String viewName = workspaceOnboardingController.cancelCompanyJoinRequest(null, session);

        assertThat(viewName).isEqualTo("redirect:/workspace?mode=join");
        verify(firstLoginCompanySetupService).cancelOwnPendingJoinRequest(session);
    }

    private FirstLoginCompanySetupState pendingSetupState(String email, String emailDomain, boolean publicDomain) {
        return new FirstLoginCompanySetupState(
                "workspace",
                "provider-user-1",
                email,
                emailDomain,
                publicDomain,
                "Alice",
                null,
                true,
                Instant.now(),
                Instant.now().plusSeconds(300),
                false
        );
    }

}
