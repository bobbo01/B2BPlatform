package com.bobbo01.supplyhub.domain.commerce.service;

import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderItemDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderProgressStepView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderSummaryView;
import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PurchaseOrderViewAssembler {

    public PurchaseOrderDetailView toPurchaseOrderDetailView(
            PurchaseOrder purchaseOrder,
            List<PurchaseOrderItemDetailView> itemViews,
            BigDecimal totalAmount,
            String statusLabel,
            boolean terminalStatus,
            List<PurchaseOrderProgressStepView> progressSteps,
            List<PurchaseOrderStatusHistoryView> statusHistoryViews
    ) {
        PurchaseOrderStatus status = purchaseOrder.getStatus();
        boolean canSubmitForPlatformApproval = purchaseOrder.canSubmitForPlatformApproval();
        boolean canCancel = purchaseOrder.canCancel();
        boolean canPay = purchaseOrder.canPay();
        boolean hasAvailableAction = canSubmitForPlatformApproval || canCancel || canPay;
        return new PurchaseOrderDetailView(
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseRequest().getId(),
                status.name(),
                statusLabel,
                toOrderStatusGuideTitle(status),
                toOrderStatusGuideMessage(status),
                hasAvailableAction,
                hasAvailableAction ? null : toOrderActionGuideTitle(status),
                hasAvailableAction ? null : toOrderActionGuideMessage(status),
                terminalStatus,
                itemViews.size(),
                resolveOrderTotalAmount(purchaseOrder, totalAmount),
                canSubmitForPlatformApproval,
                canCancel,
                canPay,
                purchaseOrder.getSubmittedForPlatformApprovalAt(),
                purchaseOrder.getPlacedAt(),
                purchaseOrder.getPaidAt(),
                purchaseOrder.getCancelledAt(),
                purchaseOrder.getPlatformReviewedBy() != null ? purchaseOrder.getPlatformReviewedBy().getFullName() : null,
                purchaseOrder.getPlatformReviewedAt(),
                purchaseOrder.getPlatformReviewMemo(),
                purchaseOrder.getPlatformRejectionReason(),
                progressSteps,
                statusHistoryViews,
                itemViews
        );
    }

    public PurchaseOrderSummaryView toPurchaseOrderSummaryView(
            PurchaseOrder purchaseOrder,
            List<PurchaseOrderItem> items,
            String statusLabel
    ) {
        return new PurchaseOrderSummaryView(
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseRequest().getId(),
                purchaseOrder.getStatus().name(),
                statusLabel,
                items.size(),
                resolveOrderTotalAmount(purchaseOrder, calculateOrderTotalAmount(items))
        );
    }

    public List<PurchaseOrderItemDetailView> toPurchaseOrderItemDetailViews(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(this::toPurchaseOrderItemDetailView)
                .toList();
    }

    public BigDecimal calculateOrderTotalAmount(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PurchaseOrderStatusHistoryView toPurchaseOrderStatusHistoryView(
            PurchaseOrderStatusHistory history,
            String fromStatusLabel,
            String toStatusLabel,
            String historyNoteLabel
    ) {
        String changedByDisplayName = history.getChangedByUser() != null
                ? history.getChangedByUser().getFullName()
                : "시스템";
        String changeNoteDisplay = historyNoteLabel != null ? historyNoteLabel : "-";
        return new PurchaseOrderStatusHistoryView(
                history.getFromStatus().name(),
                fromStatusLabel,
                history.getToStatus().name(),
                toStatusLabel,
                history.getChangedByUser() != null ? history.getChangedByUser().getFullName() : null,
                historyNoteLabel,
                history.getChangedAt(),
                fromStatusLabel + " -> " + toStatusLabel,
                changedByDisplayName,
                changeNoteDisplay
        );
    }

    public String toOrderStatusLabel(PurchaseOrderStatus status) {
        return switch (status) {
            case DRAFT -> "\uC8FC\uBB38 \uCD08\uC548";
            case PENDING_PLATFORM_APPROVAL -> "\uC8FC\uBB38 \uC2B9\uC778 \uB300\uAE30";
            case PAYMENT_PENDING -> "\uACB0\uC81C \uB300\uAE30";
            case PAID -> "\uACB0\uC81C \uC644\uB8CC";
            case READY_TO_SHIP -> "\uBC30\uC1A1 \uC900\uBE44";
            case SHIPPED -> "\uBC30\uC1A1 \uC911";
            case DELIVERED -> "\uBC30\uC1A1 \uC644\uB8CC";
            case REJECTED -> "\uBC18\uB824";
            case CANCELLED -> "\uCDE8\uC18C";
        };
    }

    public String toOrderStatusGuideTitle(PurchaseOrderStatus status) {
        return switch (status) {
            case DRAFT -> "주문 초안을 검토해 주세요";
            case PENDING_PLATFORM_APPROVAL -> "플랫폼 승인 대기 중입니다";
            case PAYMENT_PENDING -> "주문이 확정되어 결제를 기다리고 있습니다";
            case PAID -> "결제가 완료되어 배송 준비를 기다리고 있습니다";
            case READY_TO_SHIP -> "배송 준비가 완료되었습니다";
            case SHIPPED -> "주문이 배송 중입니다";
            case DELIVERED -> "주문 배송이 완료되었습니다";
            case REJECTED -> "주문이 반려되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
        };
    }

    public String toOrderStatusGuideMessage(PurchaseOrderStatus status) {
        return switch (status) {
            case DRAFT -> "품목과 금액을 확인한 뒤 주문 확정으로 플랫폼 승인 대기로 제출할 수 있습니다.";
            case PENDING_PLATFORM_APPROVAL -> "플랫폼 관리자가 주문 승인 또는 반려를 처리할 때까지 이 주문은 수정 없이 대기합니다.";
            case PAYMENT_PENDING -> "플랫폼 승인이 끝난 주문입니다. 이제 구매자가 결제를 완료하면 배송 단계로 넘어갑니다.";
            case PAID -> "결제가 끝난 주문입니다. 다음 배송 준비와 발송 처리는 플랫폼 관리자 화면에서 진행됩니다.";
            case READY_TO_SHIP -> "플랫폼에서 배송 준비를 마친 상태입니다. 발송 처리 후 배송 중 상태로 전이됩니다.";
            case SHIPPED -> "발송이 완료되었습니다. 배송 완료 처리 전까지는 진행 상황만 확인할 수 있습니다.";
            case DELIVERED -> "배송이 끝난 주문입니다. 이 화면에서는 완료 이력만 확인할 수 있습니다.";
            case REJECTED -> "플랫폼 승인 단계에서 주문이 종료되었습니다. 반려 사유와 상태 이력을 확인해 주세요.";
            case CANCELLED -> "승인 전 단계에서 취소된 주문입니다. 이 주문은 다시 진행되지 않습니다.";
        };
    }

    public String toOrderActionGuideTitle(PurchaseOrderStatus status) {
        return switch (status) {
            case PAID -> "다음 배송 준비를 기다려 주세요";
            case READY_TO_SHIP -> "배송 준비가 완료되었습니다";
            case SHIPPED -> "배송 진행 상태를 확인해 주세요";
            case DELIVERED -> "배송이 완료된 주문입니다";
            case REJECTED -> "반려된 주문입니다";
            case CANCELLED -> "취소된 주문입니다";
            default -> null;
        };
    }

    public String toOrderActionGuideMessage(PurchaseOrderStatus status) {
        return switch (status) {
            case PAID -> "이 주문은 결제가 완료되었습니다. 다음 배송 준비와 발송 처리는 플랫폼 관리자 화면에서 진행됩니다.";
            case READY_TO_SHIP -> "배송 준비가 끝난 상태입니다. 구매자는 추가 액션 없이 발송 처리 전까지 상태 이력만 확인할 수 있습니다.";
            case SHIPPED -> "이미 발송이 시작된 주문입니다. 구매자는 배송 완료 전까지 진행 상태와 이력만 확인할 수 있습니다.";
            case DELIVERED -> "배송이 완료된 주문입니다. 이 화면에서는 추가 처리 없이 완료 이력만 확인할 수 있습니다.";
            case REJECTED -> "반려 사유와 상태 이력을 확인한 뒤 필요한 경우 새 주문 초안을 다시 진행해 주세요.";
            case CANCELLED -> "취소된 주문은 다시 진행되지 않습니다. 필요하면 새 주문 초안을 만들어야 합니다.";
            default -> null;
        };
    }

    public String toHistoryNoteLabel(String changeNote) {
        if (changeNote == null || changeNote.isBlank()) {
            return null;
        }
        return switch (changeNote) {
            case "PAYMENT_COMPLETED" -> "\uACB0\uC81C \uC644\uB8CC \uCC98\uB9AC";
            default -> changeNote;
        };
    }

    private PurchaseOrderItemDetailView toPurchaseOrderItemDetailView(PurchaseOrderItem item) {
        return new PurchaseOrderItemDetailView(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getCurrencyCode(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    private BigDecimal resolveOrderTotalAmount(PurchaseOrder purchaseOrder, BigDecimal fallbackAmount) {
        return purchaseOrder.getTotalAmount() != null ? purchaseOrder.getTotalAmount() : fallbackAmount;
    }
}
