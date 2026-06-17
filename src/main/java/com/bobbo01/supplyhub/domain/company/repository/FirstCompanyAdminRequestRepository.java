package com.bobbo01.supplyhub.domain.company.repository;

import com.bobbo01.supplyhub.domain.company.entity.FirstCompanyAdminRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FirstCompanyAdminRequestRepository extends JpaRepository<FirstCompanyAdminRequest, Long> {

    Optional<FirstCompanyAdminRequest> findFirstByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<FirstCompanyAdminRequest> findByRequesterIdAndStatus(Long requesterId, String status);

    Optional<FirstCompanyAdminRequest> findByCompanyIdAndStatus(Long companyId, String status);

    List<FirstCompanyAdminRequest> findAllByStatusOrderByCreatedAtAsc(String status);
}
