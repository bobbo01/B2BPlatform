package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CompanyMembershipService {

    private final UserRepository userRepository;

    public CompanyMembershipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User leaveCompany(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User to leave company was not found."));
        if (user.isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN account cannot leave a company.");
        }
        if (user.getCompany() == null) {
            throw new IllegalStateException("User is not currently linked to a company.");
        }

        user.leaveCompany();
        return user;
    }
}
