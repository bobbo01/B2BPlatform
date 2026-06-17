package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class CompanyProfileService {

    private static final Pattern COMPANY_DOMAIN_PATTERN =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$");

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CompanyProfileService(
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company updateCompanyProfile(Long reviewerUserId, String companyName, String companyDomain) {
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!reviewer.hasCompanyAdminRole()) {
            throw new IllegalStateException("Only COMPANY_ADMIN can update company information.");
        }

        Company company = reviewer.getCompany();
        if (company == null || company.getId() == null) {
            throw new IllegalStateException("Company admin must belong to a persisted company.");
        }

        String normalizedCompanyName = normalizeCompanyName(companyName);
        String normalizedCompanyDomain = normalizeCompanyDomain(companyDomain);
        validateCompanyName(normalizedCompanyName);
        validateCompanyDomain(normalizedCompanyDomain, company.getId());

        company.updateCompanyProfile(normalizedCompanyName, normalizedCompanyDomain);
        return companyRepository.save(company);
    }

    private String normalizeCompanyName(String companyName) {
        return companyName == null ? null : companyName.trim();
    }

    private String normalizeCompanyDomain(String companyDomain) {
        return companyDomain == null ? null : companyDomain.trim().toLowerCase();
    }

    private void validateCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
    }

    private void validateCompanyDomain(String companyDomain, Long companyId) {
        if (companyDomain == null || companyDomain.isBlank()) {
            throw new IllegalArgumentException("Company domain is required.");
        }
        if (!COMPANY_DOMAIN_PATTERN.matcher(companyDomain).matches()) {
            throw new IllegalArgumentException("Company domain must be a valid domain like example.com.");
        }
        if (companyRepository.existsByCompanyDomainIgnoreCaseAndIdNot(companyDomain, companyId)) {
            throw new IllegalStateException("The company domain is already in use.");
        }
    }
}
