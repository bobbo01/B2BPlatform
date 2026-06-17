package com.bobbo01.supplyhub.domain.commerce.service;

import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequest;
import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequestStatus;
import com.bobbo01.supplyhub.domain.approval.repository.ApprovalRequestRepository;
import com.bobbo01.supplyhub.domain.approval.service.ApprovalRequestService;
import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.cart.service.CartService;
import com.bobbo01.supplyhub.domain.commerce.dto.ApprovalInboxItemView;
import com.bobbo01.supplyhub.domain.commerce.dto.CartItemView;
import com.bobbo01.supplyhub.domain.commerce.dto.CartSummaryView;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderItemDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderProgressStepView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderSummaryView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseRequestItemView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseRequestSummaryView;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseOrderService;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseRequestService;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommerceWorkflowService {
    private static final List<PurchaseOrderStatus> NORMAL_ORDER_FLOW = List.of(
            PurchaseOrderStatus.DRAFT,
            PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL,
            PurchaseOrderStatus.PAYMENT_PENDING,
            PurchaseOrderStatus.PAID,
            PurchaseOrderStatus.READY_TO_SHIP,
            PurchaseOrderStatus.SHIPPED,
            PurchaseOrderStatus.DELIVERED
    );

    private final CartService cartService;
    private final PurchaseRequestService purchaseRequestService;
    private final ApprovalRequestService approvalRequestService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderViewAssembler purchaseOrderViewAssembler;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;
    private final UserRepository userRepository;

    public CommerceWorkflowService(
            CartService cartService,
            PurchaseRequestService purchaseRequestService,
            ApprovalRequestService approvalRequestService,
            PurchaseOrderService purchaseOrderService,
            PurchaseOrderViewAssembler purchaseOrderViewAssembler,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            ApprovalRequestRepository approvalRequestRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository,
            UserRepository userRepository
    ) {
        this.cartService = cartService;
        this.purchaseRequestService = purchaseRequestService;
        this.approvalRequestService = approvalRequestService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderViewAssembler = purchaseOrderViewAssembler;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderStatusHistoryRepository = purchaseOrderStatusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CommerceWorkspaceView getWorkspaceView(Long userId) {
        User user = getActiveCompanyUser(userId);
        boolean canUseCart = true;
        boolean canCreatePurchaseRequest = hasAnyRole(user, RoleNames.PURCHASER, RoleNames.APPROVER);
        boolean canApprove = hasAnyRole(user, RoleNames.APPROVER);

        CartSummaryView cartView = cartRepository.findByOwnerIdAndStatus(userId, com.bobbo01.supplyhub.domain.cart.entity.CartStatus.OPEN)
                .map(this::toCartView)
                .orElse(null);

        List<PurchaseRequest> purchaseRequests = purchaseRequestRepository.findAllByRequesterIdOrderByCreatedAtDesc(userId)
                .stream()
                .toList();

        List<ApprovalRequest> pendingApprovals = canApprove
                ? approvalRequestRepository.findAllByApproverIdAndStatusOrderByCreatedAtAsc(userId, ApprovalRequestStatus.PENDING)
                .stream()
                .toList()
                : List.of();

        Map<Long, List<PurchaseRequestItem>> purchaseRequestItemsByRequestId = loadPurchaseRequestItemsByRequestId(
                purchaseRequests,
                pendingApprovals.stream()
                        .map(ApprovalRequest::getPurchaseRequest)
                        .toList()
        );

        List<PurchaseRequestSummaryView> purchaseRequestViews = purchaseRequests.stream()
                .map(purchaseRequest -> toPurchaseRequestView(
                        purchaseRequest,
                        purchaseRequestItemsByRequestId.getOrDefault(purchaseRequest.getId(), List.of())
                ))
                .toList();

        List<ApprovalInboxItemView> pendingApprovalViews = pendingApprovals.stream()
                .map(approvalRequest -> toApprovalInboxItemView(
                        approvalRequest,
                        purchaseRequestItemsByRequestId.getOrDefault(approvalRequest.getPurchaseRequest().getId(), List.of())
                ))
                .toList();

        List<PurchaseOrder> purchaseOrderEntities = purchaseOrderRepository.findAllWithPurchaseRequestByBuyerIdOrderByCreatedAtDesc(userId);
        List<Long> purchaseOrderIds = purchaseOrderEntities.stream()
                .map(PurchaseOrder::getId)
                .toList();
        Map<Long, List<PurchaseOrderItem>> purchaseOrderItemsByOrderId = purchaseOrderIds.isEmpty()
                ? Map.of()
                : purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(purchaseOrderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getPurchaseOrder().getId()));
        List<PurchaseOrderSummaryView> purchaseOrders = purchaseOrderEntities.stream()
                .map(purchaseOrder -> purchaseOrderViewAssembler.toPurchaseOrderSummaryView(
                        purchaseOrder,
                        purchaseOrderItemsByOrderId.getOrDefault(purchaseOrder.getId(), List.of()),
                        purchaseOrderViewAssembler.toOrderStatusLabel(purchaseOrder.getStatus())
                ))
                .toList();

        return new CommerceWorkspaceView(
                canUseCart,
                canCreatePurchaseRequest,
                canApprove,
                cartView,
                purchaseRequestViews,
                pendingApprovalViews,
                purchaseOrders
        );
    }

    @Transactional
    public void addProductToCart(Long userId, Long productId, Integer quantity) {
        getActiveCompanyUser(userId);
        Cart cart = cartService.openCart(userId);
        cartService.addItem(cart.getId(), productId, quantity);
    }

    @Transactional
    public void updateCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        getActiveCompanyUser(userId);
        cartService.updateItemQuantity(userId, cartItemId, quantity);
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        getActiveCompanyUser(userId);
        cartService.removeItem(userId, cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        getActiveCompanyUser(userId);
        cartService.clearOpenCart(userId);
    }

    @Transactional
    public void createPurchaseRequestDraft(Long userId) {
        User user = getActiveCompanyUser(userId);
        ensureRequesterRole(user);

        Cart cart = cartRepository.findByOwnerIdAndStatus(userId, com.bobbo01.supplyhub.domain.cart.entity.CartStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("Open cart was not found."));
        if (purchaseRequestRepository.findBySourceCartId(cart.getId()).isPresent()) {
            throw new IllegalStateException("A purchase request already exists for the current cart.");
        }

        purchaseRequestService.createDraftFromCart(cart.getId(), userId);
        cartService.checkout(cart.getId());
    }

    @Transactional
    public void submitPurchaseRequest(Long userId, Long purchaseRequestId) {
        User user = getActiveCompanyUser(userId);
        ensureRequesterRole(user);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        if (!purchaseRequest.getRequester().getId().equals(userId)) {
            throw new IllegalStateException("Only the requester can submit this purchase request.");
        }

        purchaseRequestService.submit(purchaseRequestId);
        User approver = resolveApprover(purchaseRequest);
        approvalRequestService.createPendingApproval(purchaseRequestId, approver.getId());
    }

    @Transactional
    public void cancelPurchaseRequest(Long userId, Long purchaseRequestId) {
        User user = getActiveCompanyUser(userId);
        ensureRequesterRole(user);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        if (!purchaseRequest.getRequester().getId().equals(userId)) {
            throw new IllegalStateException("Only the requester can cancel this purchase request.");
        }

        purchaseRequestService.cancel(purchaseRequestId);
    }

    @Transactional
    public void approveApproval(Long userId, Long approvalRequestId) {
        approveApproval(userId, approvalRequestId, null);
    }

    @Transactional
    public void approveApproval(Long userId, Long approvalRequestId, String decisionNote) {
        User approver = getActiveCompanyUser(userId);
        ensureApproverRole(approver);

        ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new IllegalStateException("Approval request was not found."));
        approvalRequestService.approve(approvalRequestId, userId, decisionNote);

        PurchaseRequest purchaseRequest = approvalRequest.getPurchaseRequest();
        if (!purchaseOrderRepository.existsByPurchaseRequestId(purchaseRequest.getId())) {
            purchaseOrderService.createDraftFromApprovedRequest(
                    purchaseRequest.getId(),
                    approver.getId()
            );
        }
    }

    @Transactional
    public void rejectApproval(Long userId, Long approvalRequestId) {
        rejectApproval(userId, approvalRequestId, null);
    }

    @Transactional
    public void rejectApproval(Long userId, Long approvalRequestId, String decisionNote) {
        User approver = getActiveCompanyUser(userId);
        ensureApproverRole(approver);
        approvalRequestService.reject(approvalRequestId, userId, decisionNote);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDetailView getPurchaseOrderDetail(Long userId, Long purchaseOrderId) {
        User user = getActiveCompanyUser(userId);
        ensureOrderManagerRole(user);
        PurchaseOrder purchaseOrder = getRequiredPurchaseOrderDetail(purchaseOrderId);
        ensureOrderBuyer(user, purchaseOrder);
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findAllByPurchaseOrderIdOrderByCreatedAtAsc(purchaseOrderId);
        List<PurchaseOrderItemDetailView> itemViews = purchaseOrderViewAssembler.toPurchaseOrderItemDetailViews(items);
        BigDecimal totalAmount = itemViews.stream()
                .map(PurchaseOrderItemDetailView::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PurchaseOrderStatus status = purchaseOrder.getStatus();
        List<PurchaseOrderStatusHistoryView> statusHistory = purchaseOrderStatusHistoryRepository
                .findAllByPurchaseOrderIdOrderByChangedAtAsc(purchaseOrderId)
                .stream()
                .map(history -> purchaseOrderViewAssembler.toPurchaseOrderStatusHistoryView(
                        history,
                        purchaseOrderViewAssembler.toOrderStatusLabel(history.getFromStatus()),
                        purchaseOrderViewAssembler.toOrderStatusLabel(history.getToStatus()),
                        purchaseOrderViewAssembler.toHistoryNoteLabel(history.getChangeNote())
                ))
                .toList();
        return purchaseOrderViewAssembler.toPurchaseOrderDetailView(
                purchaseOrder,
                itemViews,
                totalAmount,
                purchaseOrderViewAssembler.toOrderStatusLabel(status),
                isTerminalStatus(status),
                buildProgressSteps(purchaseOrder),
                statusHistory
        );
    }

    @Transactional
    public void submitPurchaseOrderForPlatformApproval(Long userId, Long purchaseOrderId) {
        User user = getActiveCompanyUser(userId);
        ensureOrderManagerRole(user);
        ensureOrderBuyer(user, purchaseOrderId);
        purchaseOrderService.submitForPlatformApproval(purchaseOrderId, userId);
    }

    @Transactional
    public void cancelPurchaseOrder(Long userId, Long purchaseOrderId) {
        User user = getActiveCompanyUser(userId);
        ensureOrderManagerRole(user);
        ensureOrderBuyer(user, purchaseOrderId);
        purchaseOrderService.cancel(purchaseOrderId, userId);
    }

    @Transactional
    public void payPurchaseOrder(Long userId, Long purchaseOrderId) {
        User user = getActiveCompanyUser(userId);
        ensureRequesterRole(user);
        ensureOrderBuyer(user, purchaseOrderId);
        purchaseOrderService.markPaid(purchaseOrderId, userId);
    }

    private CartSummaryView toCartView(Cart cart) {
        List<CartItemView> items = cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(cart.getId())
                .stream()
                .map(item -> new CartItemView(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getProductName(),
                        item.getQuantity(),
                        item.getProduct().getMinOrderQty(),
                        item.getUnitPrice(),
                        item.getCurrencyCode(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        BigDecimal totalAmount = items.stream()
                .map(CartItemView::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartSummaryView(cart.getId(), items.size(), totalAmount, items);
    }

    private PurchaseRequestSummaryView toPurchaseRequestView(
            PurchaseRequest purchaseRequest,
            List<PurchaseRequestItem> items
    ) {
        List<PurchaseRequestItemView> itemViews = items.stream()
                .map(this::toPurchaseRequestItemView)
                .toList();
        BigDecimal totalAmount = calculatePurchaseRequestTotalAmount(items);
        return new PurchaseRequestSummaryView(
                purchaseRequest.getId(),
                toPurchaseRequestStatusLabel(purchaseRequest.getStatus()),
                items.size(),
                totalAmount,
                purchaseRequest.getStatus() == com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus.DRAFT,
                purchaseRequest.getStatus() == com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus.DRAFT,
                itemViews
        );
    }

    private ApprovalInboxItemView toApprovalInboxItemView(
            ApprovalRequest approvalRequest,
            List<PurchaseRequestItem> items
    ) {
        List<PurchaseRequestItemView> itemViews = items.stream()
                .map(this::toPurchaseRequestItemView)
                .toList();
        BigDecimal totalAmount = calculatePurchaseRequestTotalAmount(items);
        return new ApprovalInboxItemView(
                approvalRequest.getId(),
                approvalRequest.getPurchaseRequest().getId(),
                approvalRequest.getRequester().getFullName(),
                approvalRequest.getRequester().getEmail(),
                items.size(),
                totalAmount,
                itemViews
        );
    }

    private Map<Long, List<PurchaseRequestItem>> loadPurchaseRequestItemsByRequestId(
            Collection<PurchaseRequest> purchaseRequests,
            Collection<PurchaseRequest> approvalPurchaseRequests
    ) {
        List<Long> purchaseRequestIds = java.util.stream.Stream.concat(
                        purchaseRequests.stream(),
                        approvalPurchaseRequests.stream()
                )
                .map(PurchaseRequest::getId)
                .distinct()
                .toList();
        if (purchaseRequestIds.isEmpty()) {
            return Map.of();
        }
        return purchaseRequestItemRepository.findAllByPurchaseRequestIdInOrderByCreatedAtAsc(purchaseRequestIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getPurchaseRequest().getId()));
    }

    private PurchaseRequestItemView toPurchaseRequestItemView(PurchaseRequestItem item) {
        return new PurchaseRequestItemView(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getCurrencyCode(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    private BigDecimal calculatePurchaseRequestTotalAmount(List<PurchaseRequestItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String toPurchaseRequestStatusLabel(
            com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus status
    ) {
        return switch (status) {
            case DRAFT -> "구매 요청 초안";
            case SUBMITTED -> "승인 요청 대기";
            case APPROVED -> "승인 완료";
            case REJECTED -> "반려";
            case CANCELLED -> "취소";
        };
    }

    private boolean isTerminalStatus(PurchaseOrderStatus status) {
        return status == PurchaseOrderStatus.REJECTED || status == PurchaseOrderStatus.CANCELLED;
    }

    private List<PurchaseOrderProgressStepView> buildProgressSteps(PurchaseOrder purchaseOrder) {
        PurchaseOrderStatus activeStatus = resolveProgressStatus(purchaseOrder);
        int activeIndex = NORMAL_ORDER_FLOW.indexOf(activeStatus);
        List<PurchaseOrderProgressStepView> progressSteps = new ArrayList<>(NORMAL_ORDER_FLOW.size());

        for (int index = 0; index < NORMAL_ORDER_FLOW.size(); index++) {
            PurchaseOrderStatus stepStatus = NORMAL_ORDER_FLOW.get(index);
            progressSteps.add(new PurchaseOrderProgressStepView(
                    stepStatus.name(),
                    purchaseOrderViewAssembler.toOrderStatusLabel(stepStatus),
                    index < activeIndex,
                    index == activeIndex
            ));
        }

        return progressSteps;
    }

    private PurchaseOrderStatus resolveProgressStatus(PurchaseOrder purchaseOrder) {
        PurchaseOrderStatus status = purchaseOrder.getStatus();
        if (status == PurchaseOrderStatus.REJECTED) {
            return PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL;
        }
        if (status == PurchaseOrderStatus.CANCELLED) {
            return purchaseOrder.getSubmittedForPlatformApprovalAt() != null
                    ? PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL
                    : PurchaseOrderStatus.DRAFT;
        }
        return status;
    }

    private void ensureOrderBuyer(User user, PurchaseOrder purchaseOrder) {
        if (!purchaseOrder.getBuyer().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the buyer can manage this purchase order.");
        }
    }

    private void ensureOrderBuyer(User user, Long purchaseOrderId) {
        ensureOrderBuyer(user, getRequiredPurchaseOrder(purchaseOrderId));
    }

    private PurchaseOrder getRequiredPurchaseOrderDetail(Long purchaseOrderId) {
        return purchaseOrderRepository.findDetailById(purchaseOrderId)
                .orElseThrow(() -> new IllegalStateException("Purchase order was not found."));
    }

    private PurchaseOrder getRequiredPurchaseOrder(Long purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new IllegalStateException("Purchase order was not found."));
    }

    private User resolveApprover(PurchaseRequest purchaseRequest) {
        List<User> approvers = userRepository.findAllByCompanyIdAndStatusIgnoreCaseAndRoleRoleNameOrderByIdAsc(
                purchaseRequest.getCompany().getId(),
                "ACTIVE",
                RoleNames.APPROVER
        );
        Optional<User> preferred = approvers.stream()
                .filter(candidate -> !candidate.getId().equals(purchaseRequest.getRequester().getId()))
                .min(Comparator.comparing(User::getId));
        return preferred.or(() -> approvers.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("No active approver is available for this company."));
    }

    private User getActiveCompanyUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (user.isPlatformAdmin() || user.getCompany() == null) {
            throw new IllegalStateException("Only company users can use commerce workflows.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Only active company users can use commerce workflows.");
        }
        return user;
    }

    private void ensureRequesterRole(User user) {
        if (!user.hasAnyPurchasingRole(RoleNames.PURCHASER, RoleNames.APPROVER)) {
            throw new IllegalStateException("Only PURCHASER or APPROVER can create and submit purchase requests.");
        }
    }

    private void ensureApproverRole(User user) {
        if (!user.hasPurchasingRole(RoleNames.APPROVER)) {
            throw new IllegalStateException("Only APPROVER can review purchase approvals.");
        }
    }

    private void ensureOrderManagerRole(User user) {
        if (!user.hasPurchasingRole(RoleNames.APPROVER)) {
            throw new IllegalStateException("Only APPROVER can submit or cancel purchase orders.");
        }
    }

    private boolean hasAnyRole(User user, String... roleNames) {
        return user.hasAnyPurchasingRole(roleNames);
    }
}
