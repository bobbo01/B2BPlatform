package com.bobbo01.supplyhub.domain.purchase.service;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

@Service
public class PurchaseOrderService {

    private static final String ORDER_CURRENCY_CODE = "KRW";
    private static final String PAYMENT_ACTION_NOTE = "PAYMENT_COMPLETED";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final UserRepository userRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            UserRepository userRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderStatusHistoryRepository = purchaseOrderStatusHistoryRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PurchaseOrder createDraftFromApprovedRequest(Long purchaseRequestId, Long buyerUserId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved purchase requests can create order drafts.");
        }

        User buyer = getRequiredApproverBuyer(purchaseRequest, buyerUserId);
        PurchaseOrder purchaseOrder = purchaseOrderRepository.save(
                PurchaseOrder.createDraft(purchaseRequest.getCompany(), purchaseRequest, buyer)
        );

        List<PurchaseRequestItem> requestItems = purchaseRequestItemRepository
                .findAllByPurchaseRequestIdOrderByCreatedAtAsc(purchaseRequestId);
        validateOrderCurrency(requestItems);
        BigDecimal subtotalAmount = calculateSubtotalAmount(requestItems);
        purchaseOrder.applyPricingSnapshot(subtotalAmount, subtotalAmount, ORDER_CURRENCY_CODE);
        purchaseOrderItemRepository.saveAll(
                requestItems.stream()
                        .map(item -> PurchaseOrderItem.fromPurchaseRequestItem(purchaseOrder, item))
                        .toList()
        );
        return purchaseOrder;
    }

    @Transactional
    public void submitForPlatformApproval(Long purchaseOrderId, Long actorUserId) {
        User actor = getRequiredActiveBuyer(purchaseOrderId, actorUserId, RoleNames.APPROVER);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::submitForPlatformApproval,
                purchaseOrder -> actor,
                purchaseOrder -> null
        );
    }

    @Transactional
    public void cancel(Long purchaseOrderId, Long actorUserId) {
        User actor = getRequiredActiveBuyer(purchaseOrderId, actorUserId, RoleNames.APPROVER);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::cancel,
                purchaseOrder -> actor,
                purchaseOrder -> null
        );
    }

    @Transactional
    public void approveByPlatform(Long purchaseOrderId, Long reviewerUserId, String reviewMemo) {
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        String normalizedReviewMemo = normalizeOptionalText(reviewMemo);
        transitionOrder(
                purchaseOrderId,
                purchaseOrder -> purchaseOrder.approveByPlatform(reviewer, normalizedReviewMemo),
                purchaseOrder -> reviewer,
                purchaseOrder -> normalizedReviewMemo
        );
    }

    @Transactional
    public void markPaid(Long purchaseOrderId, Long actorUserId) {
        User actor = getRequiredActiveBuyer(purchaseOrderId, actorUserId, RoleNames.PURCHASER, RoleNames.APPROVER);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::markPaid,
                purchaseOrder -> actor,
                purchaseOrder -> PAYMENT_ACTION_NOTE
        );
    }

    @Transactional
    public void markReadyToShip(Long purchaseOrderId, Long reviewerUserId) {
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::markReadyToShip,
                purchaseOrder -> reviewer,
                purchaseOrder -> null
        );
    }

    @Transactional
    public void markShipped(Long purchaseOrderId, Long reviewerUserId) {
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::markShipped,
                purchaseOrder -> reviewer,
                purchaseOrder -> null
        );
    }

    @Transactional
    public void markDelivered(Long purchaseOrderId, Long reviewerUserId) {
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        transitionOrder(
                purchaseOrderId,
                PurchaseOrder::markDelivered,
                purchaseOrder -> reviewer,
                purchaseOrder -> null
        );
    }

    @Transactional
    public void markSettled(Long purchaseOrderId, Long reviewerUserId) {
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        PurchaseOrder purchaseOrder = getRequiredPurchaseOrder(purchaseOrderId);
        purchaseOrder.markSettled(reviewer);
    }

    @Transactional
    public void rejectByPlatform(Long purchaseOrderId, Long reviewerUserId, String reviewMemo, String rejectionReason) {
        String normalizedReviewMemo = normalizeOptionalText(reviewMemo);
        String normalizedRejectionReason = requireRejectionReason(rejectionReason);
        User reviewer = getRequiredPlatformAdmin(reviewerUserId);
        transitionOrder(
                purchaseOrderId,
                purchaseOrder -> purchaseOrder.rejectByPlatform(
                        reviewer,
                        normalizedReviewMemo,
                        normalizedRejectionReason
                ),
                purchaseOrder -> reviewer,
                purchaseOrder -> normalizedRejectionReason
        );
    }

    private void transitionOrder(
            Long purchaseOrderId,
            Consumer<PurchaseOrder> transitionAction,
            ActorResolver actorResolver,
            NoteResolver noteResolver
    ) {
        PurchaseOrder purchaseOrder = getRequiredPurchaseOrder(purchaseOrderId);
        PurchaseOrderStatus fromStatus = purchaseOrder.getStatus();
        transitionAction.accept(purchaseOrder);
        recordStatusHistory(
                purchaseOrder,
                fromStatus,
                actorResolver.resolve(purchaseOrder),
                noteResolver.resolve(purchaseOrder)
        );
    }

    private PurchaseOrder getRequiredPurchaseOrder(Long purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new IllegalStateException("Purchase order was not found."));
    }

    private User getRequiredApproverBuyer(PurchaseRequest purchaseRequest, Long buyerUserId) {
        User buyer = getRequiredCompanyUser(buyerUserId);
        if (!purchaseRequest.getCompany().getId().equals(buyer.getCompany().getId())) {
            throw new IllegalStateException("Only a buyer in the same company can create this purchase order.");
        }
        assertHasAnyPurchasingRole(buyer, RoleNames.APPROVER);
        return buyer;
    }

    private User getRequiredActiveBuyer(Long purchaseOrderId, Long actorUserId, String... roleNames) {
        PurchaseOrder purchaseOrder = getRequiredPurchaseOrder(purchaseOrderId);
        User actor = getRequiredCompanyUser(actorUserId);
        if (!purchaseOrder.getBuyer().getId().equals(actor.getId())) {
            throw new IllegalStateException("Only the buyer can change this purchase order.");
        }
        assertHasAnyPurchasingRole(actor, roleNames);
        return actor;
    }

    private User getRequiredPlatformAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!user.isPlatformAdmin()) {
            throw new IllegalStateException("Only PLATFORM_ADMIN can change platform-managed order states.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Only active PLATFORM_ADMIN can change platform-managed order states.");
        }
        return user;
    }

    private User getRequiredCompanyUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!user.isCompanyUser()) {
            throw new IllegalStateException("Only active company users can change buyer-managed order states.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Only active company users can change buyer-managed order states.");
        }
        return user;
    }

    private void assertHasAnyPurchasingRole(User user, String... roleNames) {
        if (user.hasAnyPurchasingRole(roleNames)) {
            return;
        }
        throw new IllegalStateException("User does not have permission to change this purchase order.");
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requireRejectionReason(String rejectionReason) {
        String normalized = normalizeOptionalText(rejectionReason);
        if (normalized == null) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        return normalized;
    }

    private void validateOrderCurrency(List<PurchaseRequestItem> requestItems) {
        boolean hasUnsupportedCurrency = requestItems.stream()
                .map(PurchaseRequestItem::getCurrencyCode)
                .anyMatch(currencyCode -> !ORDER_CURRENCY_CODE.equalsIgnoreCase(currencyCode));
        if (hasUnsupportedCurrency) {
            throw new IllegalStateException("Only KRW purchase orders are supported.");
        }
    }

    private BigDecimal calculateSubtotalAmount(List<PurchaseRequestItem> requestItems) {
        return requestItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recordStatusHistory(
            PurchaseOrder purchaseOrder,
            PurchaseOrderStatus fromStatus,
            User changedByUser,
            String changeNote
    ) {
        purchaseOrderStatusHistoryRepository.save(
                PurchaseOrderStatusHistory.record(
                        purchaseOrder,
                        fromStatus,
                        purchaseOrder.getStatus(),
                        changedByUser,
                        changeNote
                )
        );
    }

    @FunctionalInterface
    private interface ActorResolver {
        User resolve(PurchaseOrder purchaseOrder);
    }

    @FunctionalInterface
    private interface NoteResolver {
        String resolve(PurchaseOrder purchaseOrder);
    }
}
