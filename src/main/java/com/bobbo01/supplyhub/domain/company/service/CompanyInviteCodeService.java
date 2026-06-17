package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class CompanyInviteCodeService {

    private static final char[] INVITE_CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int INVITE_CODE_LENGTH = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 20;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CompanyInviteCodeService(
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company ensureInviteCode(Company company) {
        if (company.getInviteCode() != null && !company.getInviteCode().isBlank()) {
            return company;
        }
        company.updateInviteCode(generateUniqueInviteCode());
        return companyRepository.save(company);
    }

    @Transactional
    public Company regenerateInviteCode(Long reviewerUserId) {
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!reviewer.hasCompanyAdminRole()) {
            throw new IllegalStateException("Only COMPANY_ADMIN can regenerate invite codes.");
        }
        Company company = reviewer.getCompany();
        if (company == null) {
            throw new IllegalStateException("Company admin must belong to a company.");
        }
        company.updateInviteCode(generateUniqueInviteCode());
        return companyRepository.save(company);
    }

    @Transactional
    public Company revokeInviteCode(Long reviewerUserId) {
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!reviewer.hasCompanyAdminRole()) {
            throw new IllegalStateException("Only COMPANY_ADMIN can revoke invite codes.");
        }
        Company company = reviewer.getCompany();
        if (company == null) {
            throw new IllegalStateException("Company admin must belong to a company.");
        }
        company.revokeInviteCode();
        return companyRepository.save(company);
    }

    public String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String inviteCode = randomInviteCode();
            if (!companyRepository.existsByInviteCodeIgnoreCase(inviteCode)) {
                return inviteCode;
            }
        }
        throw new IllegalStateException("Failed to generate a unique invite code.");
    }

    private String randomInviteCode() {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
        for (int index = 0; index < INVITE_CODE_LENGTH; index++) {
            builder.append(INVITE_CODE_CHARSET[secureRandom.nextInt(INVITE_CODE_CHARSET.length)]);
        }
        return builder.toString();
    }
}
