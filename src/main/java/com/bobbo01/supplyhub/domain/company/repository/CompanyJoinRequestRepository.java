package com.bobbo01.supplyhub.domain.company.repository;

import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequest;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyJoinRequestRepository extends JpaRepository<CompanyJoinRequest, Long> {

    boolean existsByProviderAndProviderUserIdAndStatus(
            String provider,
            String providerUserId,
            CompanyJoinRequestStatus status
    );

    boolean existsByCompanyIdAndRequestedEmailIgnoreCaseAndStatus(
            Long companyId,
            String requestedEmail,
            CompanyJoinRequestStatus status
    );

    Optional<CompanyJoinRequest> findFirstByProviderAndProviderUserIdOrderByCreatedAtDesc(
            String provider,
            String providerUserId
    );

    Optional<CompanyJoinRequest> findByProviderAndProviderUserIdAndStatus(
            String provider,
            String providerUserId,
            CompanyJoinRequestStatus status
    );

    List<CompanyJoinRequest> findAllByCompanyIdAndStatusOrderByCreatedAtAsc(
            Long companyId,
            CompanyJoinRequestStatus status
    );
}
