package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AccountServiceTest {

    @Mock
    private UserOAuthAccountService userOAuthAccountService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private OAuth2LoginPolicy loginPolicy;

    @InjectMocks
    private OAuth2AccountService oauth2AccountService;

    @Test
    void skipsDomainCompanyLookupForPublicEmailDomains() {
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@gmail.com",
                true,
                "Alice",
                null,
                "alice@gmail.com",
                Map.of(),
                "sub"
        );

        when(loginPolicy.isEmailDomainAllowed("alice@gmail.com")).thenReturn(true);
        when(loginPolicy.extractDomain("alice@gmail.com")).thenReturn("gmail.com");
        when(loginPolicy.isPublicEmailDomain("gmail.com")).thenReturn(true);

        assertThat(oauth2AccountService.findActiveCompanyByEmailDomain(attributes)).isEmpty();
        verify(companyRepository, never()).findByCompanyDomainIgnoreCase("gmail.com");
    }
}
