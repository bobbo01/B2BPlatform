package com.bobbo01.supplyhub.domain.purchase.repository;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findAllByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<PurchaseRequest> findBySourceCartId(Long sourceCartId);
}
