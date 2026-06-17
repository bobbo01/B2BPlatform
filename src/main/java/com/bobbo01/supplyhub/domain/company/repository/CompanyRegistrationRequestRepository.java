package com.bobbo01.supplyhub.domain.company.repository;

import com.bobbo01.supplyhub.domain.company.entity.CompanyRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRegistrationRequestRepository extends JpaRepository<CompanyRegistrationRequest, Long> {

    Optional<CompanyRegistrationRequest> findFirstByProviderAndProviderUserIdOrderByCreatedAtDesc(
            String provider,
            String providerUserId
    );

    Optional<CompanyRegistrationRequest> findByProviderAndProviderUserIdAndStatus(
            String provider,
            String providerUserId,
            String status
    );

    Optional<CompanyRegistrationRequest> findByRequestedCompanyDomainIgnoreCaseAndStatus(
            String requestedCompanyDomain,
            String status
    );

    List<CompanyRegistrationRequest> findAllByStatusOrderByCreatedAtAsc(String status);
}
