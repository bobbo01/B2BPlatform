package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInviteCodeServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyInviteCodeService companyInviteCodeService;

    @Test
    void returnsExistingInviteCodeWithoutSaving() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("INVITE1234")
                .status("ACTIVE")
                .build();

        Company resolvedCompany = companyInviteCodeService.ensureInviteCode(company);

        assertThat(resolvedCompany.getInviteCode()).isEqualTo("INVITE1234");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void generatesAndSavesInviteCodeWhenMissing() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .status("ACTIVE")
                .build();
        when(companyRepository.existsByInviteCodeIgnoreCase(any())).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);

        Company resolvedCompany = companyInviteCodeService.ensureInviteCode(company);

        assertThat(resolvedCompany.getInviteCode()).hasSize(10);
        verify(companyRepository).save(company);
    }

    @Test
    void regeneratesInviteCodeForCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("OLDCODE123")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyRepository.existsByInviteCodeIgnoreCase(any())).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);

        Company updatedCompany = companyInviteCodeService.regenerateInviteCode(1L);

        assertThat(updatedCompany.getInviteCode()).hasSize(10);
        assertThat(updatedCompany.getInviteCode()).isNotEqualTo("OLDCODE123");
        verify(companyRepository).save(company);
    }

    @Test
    void rejectsRegenerationForNonCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("OLDCODE123")
                .status("ACTIVE")
                .build();
        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> companyInviteCodeService.regenerateInviteCode(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPANY_ADMIN");
    }

    @Test
    void revokesInviteCodeForCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("OLDCODE123")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyRepository.save(company)).thenReturn(company);

        Company updatedCompany = companyInviteCodeService.revokeInviteCode(1L);

        assertThat(updatedCompany.getInviteCode()).isNull();
        verify(companyRepository).save(company);
    }
}
