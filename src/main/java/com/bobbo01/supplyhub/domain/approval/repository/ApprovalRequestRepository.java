package com.bobbo01.supplyhub.domain.approval.repository;

import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequest;
import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    @EntityGraph(attributePaths = {"purchaseRequest", "requester"})
    List<ApprovalRequest> findAllByApproverIdAndStatusOrderByCreatedAtAsc(Long approverId, ApprovalRequestStatus status);

    Optional<ApprovalRequest> findByPurchaseRequestIdAndStatus(Long purchaseRequestId, ApprovalRequestStatus status);
}
