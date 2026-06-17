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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyProfileService companyProfileService;

    @Test
    void updatesCompanyProfileForCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Old Name")
                .companyDomain("old.example.com")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User reviewer = companyAdminUser(company, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyRepository.existsByCompanyDomainIgnoreCaseAndIdNot("next.example.com", 10L)).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);

        Company updatedCompany = companyProfileService.updateCompanyProfile(1L, "  New Name  ", " NEXT.EXAMPLE.COM ");

        assertThat(updatedCompany.getCompanyName()).isEqualTo("New Name");
        assertThat(updatedCompany.getCompanyDomain()).isEqualTo("next.example.com");
        verify(companyRepository).save(company);
    }

    @Test
    void rejectsUpdateForNonCompanyAdmin() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
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

        assertThatThrownBy(() -> companyProfileService.updateCompanyProfile(1L, "New Name", "new.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPANY_ADMIN");
        verify(companyRepository, never()).save(company);
    }

    @Test
    void rejectsDuplicateCompanyDomain() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User reviewer = companyAdminUser(company, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(companyRepository.existsByCompanyDomainIgnoreCaseAndIdNot("dup.example.com", 10L)).thenReturn(true);

        assertThatThrownBy(() -> companyProfileService.updateCompanyProfile(1L, "New Name", "dup.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in use");
        verify(companyRepository, never()).save(company);
    }

    @Test
    void rejectsInvalidCompanyDomain() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User reviewer = companyAdminUser(company, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> companyProfileService.updateCompanyProfile(1L, "New Name", "not a domain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid domain");
        verify(companyRepository, never()).save(company);
    }

    private User companyAdminUser(Company company, Long userId) {
        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.grantCompanyAdmin();
        ReflectionTestUtils.setField(reviewer, "id", userId);
        return reviewer;
    }
}
