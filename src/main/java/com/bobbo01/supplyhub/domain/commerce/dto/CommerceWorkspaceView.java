package com.bobbo01.supplyhub.domain.commerce.dto;

import java.util.List;

public record CommerceWorkspaceView(
        boolean canUseCart,
        boolean canCreatePurchaseRequest,
        boolean canApprove,
        CartSummaryView cart,
        List<PurchaseRequestSummaryView> purchaseRequests,
        List<ApprovalInboxItemView> pendingApprovals,
        List<PurchaseOrderSummaryView> purchaseOrders
) {
}
