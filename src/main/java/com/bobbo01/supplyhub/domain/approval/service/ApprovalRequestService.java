package com.bobbo01.supplyhub.domain.approval.service;

import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequest;
import com.bobbo01.supplyhub.domain.approval.repository.ApprovalRequestRepository;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;

    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            UserRepository userRepository
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApprovalRequest createPendingApproval(Long purchaseRequestId, Long approverUserId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted purchase requests can enter approval.");
        }
        if (approvalRequestRepository.findByPurchaseRequestIdAndStatus(purchaseRequestId, com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequestStatus.PENDING).isPresent()) {
            throw new IllegalStateException("A pending approval request already exists for this purchase request.");
        }

        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new IllegalStateException("Approver was not found."));
        return approvalRequestRepository.save(ApprovalRequest.createPending(
                purchaseRequest.getCompany(),
                purchaseRequest,
                purchaseRequest.getRequester(),
                approver
        ));
    }

    @Transactional
    public void approve(Long approvalRequestId, Long approverUserId, String decisionNote) {
        ApprovalRequest approvalRequest = getApprovalForApprover(approvalRequestId, approverUserId);
        approvalRequest.approve(normalizeOptionalText(decisionNote));
        approvalRequest.getPurchaseRequest().approve();
    }

    @Transactional
    public void reject(Long approvalRequestId, Long approverUserId, String decisionNote) {
        ApprovalRequest approvalRequest = getApprovalForApprover(approvalRequestId, approverUserId);
        approvalRequest.reject(requireDecisionNote(decisionNote));
        approvalRequest.getPurchaseRequest().reject();
    }

    @Transactional
    public void cancel(Long approvalRequestId, Long approverUserId, String decisionNote) {
        ApprovalRequest approvalRequest = getApprovalForApprover(approvalRequestId, approverUserId);
        approvalRequest.cancel(normalizeOptionalText(decisionNote));
    }

    private ApprovalRequest getApprovalForApprover(Long approvalRequestId, Long approverUserId) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new IllegalStateException("Approval request was not found."));
        if (!approvalRequest.getApprover().getId().equals(approverUserId)) {
            throw new IllegalStateException("Only the assigned approver can review this request.");
        }
        return approvalRequest;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String requireDecisionNote(String decisionNote) {
        String normalized = normalizeOptionalText(decisionNote);
        if (normalized == null) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        return normalized;
    }
}
