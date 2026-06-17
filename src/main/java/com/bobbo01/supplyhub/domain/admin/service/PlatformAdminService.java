package com.bobbo01.supplyhub.domain.admin.service;

import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminCompanyView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminUserView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformApprovalOrderView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformSettlementOrderView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformSettlementSummaryView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.commerce.service.PurchaseOrderViewAssembler;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderSettlementStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseOrderService;
import com.bobbo01.supplyhub.domain.role.RoleLabeler;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlatformAdminService {

    public static final String ORDER_FILTER_PENDING = "pending";
    public static final String ORDER_FILTER_APPROVED = "approved";
    public static final String ORDER_FILTER_DELIVERY = "delivery";
    public static final String ORDER_FILTER_REJECTED = "rejected";
    public static final String ORDER_FILTER_ALL = "all";
    private static final List<PurchaseOrderStatus> CONFIRMED_ORDER_STATUSES = List.of(
            PurchaseOrderStatus.PAYMENT_PENDING,
            PurchaseOrderStatus.PAID,
            PurchaseOrderStatus.READY_TO_SHIP,
            PurchaseOrderStatus.SHIPPED,
            PurchaseOrderStatus.DELIVERED
    );

    private static final Set<String> PURCHASING_ROLE_NAMES = Set.of(
            RoleNames.CART_USER,
            RoleNames.PURCHASER,
            RoleNames.APPROVER
    );

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderViewAssembler purchaseOrderViewAssembler;

    public PlatformAdminService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RoleRepository roleRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository,
            PurchaseOrderService purchaseOrderService,
            PurchaseOrderViewAssembler purchaseOrderViewAssembler
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderStatusHistoryRepository = purchaseOrderStatusHistoryRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderViewAssembler = purchaseOrderViewAssembler;
    }

    @Transactional(readOnly = true)
    public List<PlatformAdminUserView> getUserViews(Long reviewerUserId) {
        assertPlatformAdmin(reviewerUserId);
        return userRepository.findAllWithCompanyOrderByEmailAsc().stream()
                .map(this::toUserView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformAdminCompanyView> getCompanyViews(Long reviewerUserId) {
        assertPlatformAdmin(reviewerUserId);
        return companyRepository.findAllOrderByCompanyNameAsc().stream()
                .map(company -> new PlatformAdminCompanyView(
                        company.getId(),
                        company.getCompanyName(),
                        company.getCompanyDomain(),
                        company.getStatus(),
                        company.getCreatorUserId()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformApprovalOrderView> getPlatformApprovalOrders(Long reviewerUserId, String orderFilter) {
        assertPlatformAdmin(reviewerUserId);
        List<PurchaseOrder> purchaseOrders = loadOrders(orderFilter);
        if (purchaseOrders.isEmpty()) {
            return List.of();
        }

        List<Long> purchaseOrderIds = purchaseOrders.stream()
                .map(PurchaseOrder::getId)
                .toList();
        Map<Long, List<PurchaseOrderItem>> itemsByPurchaseOrderId = purchaseOrderItemRepository
                .findAllByPurchaseOrderIdInOrderByCreatedAtAsc(purchaseOrderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getPurchaseOrder().getId()
                ));
        Map<Long, List<PurchaseOrderStatusHistory>> statusHistoryByPurchaseOrderId = purchaseOrderStatusHistoryRepository
                .findAllByPurchaseOrderIdInOrderByChangedAtAsc(purchaseOrderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        history -> history.getPurchaseOrder().getId()
                ));

        return purchaseOrders.stream()
                .map(purchaseOrder -> toPendingOrderView(
                        purchaseOrder,
                        itemsByPurchaseOrderId.getOrDefault(purchaseOrder.getId(), List.of()),
                        statusHistoryByPurchaseOrderId.getOrDefault(purchaseOrder.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformSettlementSummaryView getSettlementSummary(Long reviewerUserId) {
        assertPlatformAdmin(reviewerUserId);
        List<PurchaseOrder> confirmedOrders = purchaseOrderRepository
                .findAllForAdminListByStatusInOrderByCreatedAtDesc(CONFIRMED_ORDER_STATUSES);
        List<PurchaseOrder> deliveredOrders = purchaseOrderRepository
                .findAllForSettlementListByStatusOrderByDeliveredAtDesc(PurchaseOrderStatus.DELIVERED);

        return new PlatformSettlementSummaryView(
                sumOrderAmounts(confirmedOrders),
                sumOrderAmounts(deliveredOrders.stream()
                        .filter(order -> order.getSettlementStatus() == PurchaseOrderSettlementStatus.UNSETTLED)
                        .toList()),
                sumOrderAmounts(deliveredOrders.stream()
                        .filter(order -> order.getSettlementStatus() == PurchaseOrderSettlementStatus.SETTLED)
                        .toList()),
                confirmedOrders.size(),
                deliveredOrders.stream()
                        .filter(order -> order.getSettlementStatus() == PurchaseOrderSettlementStatus.UNSETTLED)
                        .count(),
                deliveredOrders.stream()
                        .filter(order -> order.getSettlementStatus() == PurchaseOrderSettlementStatus.SETTLED)
                        .count()
        );
    }

    @Transactional(readOnly = true)
    public List<PlatformSettlementOrderView> getSettlementOrders(Long reviewerUserId) {
        assertPlatformAdmin(reviewerUserId);
        return purchaseOrderRepository.findAllForSettlementListByStatusOrderByDeliveredAtDesc(PurchaseOrderStatus.DELIVERED)
                .stream()
                .map(this::toSettlementOrderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public String normalizeOrderFilter(String orderFilter) {
        if (ORDER_FILTER_APPROVED.equalsIgnoreCase(orderFilter)) {
            return ORDER_FILTER_APPROVED;
        }
        if (ORDER_FILTER_DELIVERY.equalsIgnoreCase(orderFilter)) {
            return ORDER_FILTER_DELIVERY;
        }
        if (ORDER_FILTER_REJECTED.equalsIgnoreCase(orderFilter)) {
            return ORDER_FILTER_REJECTED;
        }
        if (ORDER_FILTER_ALL.equalsIgnoreCase(orderFilter)) {
            return ORDER_FILTER_ALL;
        }
        return ORDER_FILTER_PENDING;
    }

    @Transactional
    public void updateUserStatus(Long reviewerUserId, Long targetUserId, String status) {
        assertPlatformAdmin(reviewerUserId);
        User user = getRequiredUser(targetUserId);
        if ("ACTIVE".equalsIgnoreCase(status)) {
            user.activate();
            return;
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            assertNotLastActiveCompanyAdmin(user, true, user.hasCompanyAdminRole());
            user.inactivate();
            return;
        }
        throw new IllegalArgumentException("Unsupported user status: " + status);
    }

    @Transactional
    public void updateUserPurchasingRole(Long reviewerUserId, Long targetUserId, String roleName) {
        assertPlatformAdmin(reviewerUserId);
        User user = getRequiredUser(targetUserId);
        if (user.isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN user does not have a purchasing role.");
        }

        Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role was not found: " + roleName));
        assertPurchasingRole(role);
        user.changePurchasingRole(role);
    }

    @Transactional
    public void updateUserCompanyAdmin(Long reviewerUserId, Long targetUserId, boolean companyAdmin) {
        assertPlatformAdmin(reviewerUserId);
        User user = getRequiredUser(targetUserId);
        if (user.isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN user cannot hold COMPANY_ADMIN.");
        }

        if (companyAdmin) {
            user.grantCompanyAdmin();
            return;
        }

        assertNotLastActiveCompanyAdmin(user, "INACTIVE".equalsIgnoreCase(user.getStatus()), false);
        user.revokeCompanyAdmin();
    }

    @Transactional
    public void updateCompanyStatus(Long reviewerUserId, Long companyId, String status) {
        assertPlatformAdmin(reviewerUserId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Company was not found."));
        if ("ACTIVE".equalsIgnoreCase(status)) {
            company.activate();
            return;
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            company.inactivate();
            return;
        }
        throw new IllegalArgumentException("Unsupported company status: " + status);
    }

    @Transactional
    public void approvePurchaseOrder(Long reviewerUserId, Long purchaseOrderId, String reviewMemo) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.approveByPlatform(purchaseOrderId, reviewerUserId, reviewMemo);
    }

    @Transactional
    public void rejectPurchaseOrder(Long reviewerUserId, Long purchaseOrderId, String reviewMemo, String rejectionReason) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.rejectByPlatform(purchaseOrderId, reviewerUserId, reviewMemo, rejectionReason);
    }

    @Transactional
    public void markOrderReadyToShip(Long reviewerUserId, Long purchaseOrderId) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.markReadyToShip(purchaseOrderId, reviewerUserId);
    }

    @Transactional
    public void markOrderShipped(Long reviewerUserId, Long purchaseOrderId) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.markShipped(purchaseOrderId, reviewerUserId);
    }

    @Transactional
    public void markOrderDelivered(Long reviewerUserId, Long purchaseOrderId) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.markDelivered(purchaseOrderId, reviewerUserId);
    }

    @Transactional
    public void markOrderSettled(Long reviewerUserId, Long purchaseOrderId) {
        assertPlatformAdmin(reviewerUserId);
        purchaseOrderService.markSettled(purchaseOrderId, reviewerUserId);
    }

    @Transactional
    public void markOrdersSettled(Long reviewerUserId, List<Long> purchaseOrderIds) {
        assertPlatformAdmin(reviewerUserId);
        if (purchaseOrderIds == null || purchaseOrderIds.isEmpty()) {
            throw new IllegalArgumentException("At least one purchase order is required for settlement.");
        }
        purchaseOrderIds.forEach(purchaseOrderId -> purchaseOrderService.markSettled(purchaseOrderId, reviewerUserId));
    }

    @Transactional(readOnly = true)
    public List<String> getPurchasingRoleOptions() {
        return List.of(RoleNames.CART_USER, RoleNames.PURCHASER, RoleNames.APPROVER);
    }

    private PlatformApprovalOrderView toPendingOrderView(
            PurchaseOrder purchaseOrder,
            List<PurchaseOrderItem> items,
            List<PurchaseOrderStatusHistory> statusHistory
    ) {
        BigDecimal calculatedTotalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean canApprove = purchaseOrder.getStatus() == PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL;
        boolean canReject = purchaseOrder.getStatus() == PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL;
        boolean canMarkReadyToShip = purchaseOrder.canMarkReadyToShip();
        boolean canMarkShipped = purchaseOrder.canShip();
        boolean canMarkDelivered = purchaseOrder.canDeliver();
        boolean hasAvailableAction = canApprove || canReject || canMarkReadyToShip || canMarkShipped || canMarkDelivered;
        return new PlatformApprovalOrderView(
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseRequest().getId(),
                purchaseOrder.getCompany().getId(),
                purchaseOrder.getCompany().getCompanyName(),
                purchaseOrder.getBuyer().getFullName(),
                purchaseOrder.getBuyer().getEmail(),
                items.size(),
                purchaseOrder.getTotalAmount() != null ? purchaseOrder.getTotalAmount() : calculatedTotalAmount,
                purchaseOrder.getStatus().name(),
                purchaseOrderViewAssembler.toOrderStatusLabel(purchaseOrder.getStatus()),
                purchaseOrder.getSubmittedForPlatformApprovalAt(),
                purchaseOrder.getPlatformReviewedAt(),
                purchaseOrder.getPaidAt(),
                purchaseOrder.getReadyToShipAt(),
                purchaseOrder.getShippedAt(),
                purchaseOrder.getDeliveredAt(),
                purchaseOrder.getPlatformReviewedBy() != null ? purchaseOrder.getPlatformReviewedBy().getFullName() : null,
                purchaseOrder.getPlatformReviewMemo(),
                purchaseOrder.getPlatformRejectionReason(),
                canApprove,
                canReject,
                canMarkReadyToShip,
                canMarkShipped,
                canMarkDelivered,
                hasAvailableAction,
                hasAvailableAction ? null : toAdminOrderActionGuideMessage(purchaseOrder.getStatus()),
                statusHistory.stream()
                        .map(this::toPurchaseOrderStatusHistoryView)
                        .toList()
        );
    }

    private String toAdminOrderActionGuideMessage(PurchaseOrderStatus status) {
        return switch (status) {
            case PAYMENT_PENDING -> "구매자의 결제를 기다리는 상태입니다.";
            case DELIVERED -> "배송이 완료된 주문입니다. 이 화면에서는 추가 액션 없이 이력만 확인할 수 있습니다.";
            case REJECTED -> "반려가 완료된 종료 상태입니다. 반려 사유와 상태 이력만 확인할 수 있습니다.";
            case CANCELLED -> "취소된 종료 상태입니다. 이 화면에서는 추가 액션이 없습니다.";
            default -> null;
        };
    }

    private PlatformSettlementOrderView toSettlementOrderView(PurchaseOrder purchaseOrder) {
        PurchaseOrderSettlementStatus settlementStatus = purchaseOrder.getSettlementStatus();
        return new PlatformSettlementOrderView(
                purchaseOrder.getId(),
                purchaseOrder.getPurchaseRequest().getId(),
                purchaseOrder.getCompany().getId(),
                purchaseOrder.getCompany().getCompanyName(),
                purchaseOrder.getBuyer().getFullName(),
                purchaseOrder.getBuyer().getEmail(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getStatus().name(),
                purchaseOrderViewAssembler.toOrderStatusLabel(purchaseOrder.getStatus()),
                settlementStatus.name(),
                toSettlementStatusLabel(settlementStatus),
                purchaseOrder.getDeliveredAt(),
                purchaseOrder.getSettledAt(),
                purchaseOrder.getSettledBy() != null ? purchaseOrder.getSettledBy().getFullName() : null
        );
    }

    private PurchaseOrderStatusHistoryView toPurchaseOrderStatusHistoryView(PurchaseOrderStatusHistory history) {
        return purchaseOrderViewAssembler.toPurchaseOrderStatusHistoryView(
                history,
                purchaseOrderViewAssembler.toOrderStatusLabel(history.getFromStatus()),
                purchaseOrderViewAssembler.toOrderStatusLabel(history.getToStatus()),
                purchaseOrderViewAssembler.toHistoryNoteLabel(history.getChangeNote())
        );
    }

    private String toSettlementStatusLabel(PurchaseOrderSettlementStatus settlementStatus) {
        return switch (settlementStatus) {
            case UNSETTLED -> "정산 대기";
            case SETTLED -> "정산 완료";
        };
    }

    private List<PurchaseOrder> loadOrders(String orderFilter) {
        String normalizedFilter = normalizeOrderFilter(orderFilter);
        if (ORDER_FILTER_APPROVED.equals(normalizedFilter)) {
            return purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                    PurchaseOrderStatus.PAYMENT_PENDING
            ));
        }
        if (ORDER_FILTER_DELIVERY.equals(normalizedFilter)) {
            return purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                    PurchaseOrderStatus.PAID,
                    PurchaseOrderStatus.READY_TO_SHIP,
                    PurchaseOrderStatus.SHIPPED,
                    PurchaseOrderStatus.DELIVERED
            ));
        }
        if (ORDER_FILTER_REJECTED.equals(normalizedFilter)) {
            return purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(PurchaseOrderStatus.REJECTED));
        }
        if (ORDER_FILTER_ALL.equals(normalizedFilter)) {
            return purchaseOrderRepository.findAllForAdminListOrderByCreatedAtDesc();
        }
        return switch (normalizedFilter) {
            case ORDER_FILTER_APPROVED -> purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                    PurchaseOrderStatus.PAYMENT_PENDING
            ));
            case ORDER_FILTER_DELIVERY -> purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                    PurchaseOrderStatus.PAID,
                    PurchaseOrderStatus.READY_TO_SHIP,
                    PurchaseOrderStatus.SHIPPED,
                    PurchaseOrderStatus.DELIVERED
            ));
            default -> purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                    PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL
            ));
        };
    }

    private BigDecimal sumOrderAmounts(List<PurchaseOrder> purchaseOrders) {
        return purchaseOrders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PlatformAdminUserView toUserView(User user) {
        return new PlatformAdminUserView(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isPlatformAdmin() ? RoleNames.PLATFORM_ADMIN : "COMPANY_USER",
                RoleLabeler.toRoleLabel(user.isPlatformAdmin() ? RoleNames.PLATFORM_ADMIN : "COMPANY_USER"),
                user.getCompany() != null ? user.getCompany().getCompanyName() : null,
                user.getStatus(),
                user.isPlatformAdmin() ? null : user.getPurchasingRoleName(),
                user.isPlatformAdmin() ? null : RoleLabeler.toRoleLabel(user.getPurchasingRoleName()),
                user.hasCompanyAdminRole()
        );
    }

    private void assertPlatformAdmin(Long reviewerUserId) {
        User reviewer = getRequiredUser(reviewerUserId);
        if (!reviewer.isPlatformAdmin()) {
            throw new IllegalStateException("Only PLATFORM_ADMIN can use this operation.");
        }
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
    }

    private void assertNotLastActiveCompanyAdmin(User user, boolean targetInactive, boolean targetCompanyAdmin) {
        if (user.getCompany() == null || !user.hasCompanyAdminRole() || user.isPlatformAdmin()) {
            return;
        }
        boolean removesActiveAdmin = targetInactive || !targetCompanyAdmin;
        if (!removesActiveAdmin || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return;
        }
        long activeCompanyAdminCount = userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(
                user.getCompany().getId(),
                "ACTIVE"
        );
        if (activeCompanyAdminCount <= 1) {
            throw new IllegalStateException("The last active COMPANY_ADMIN in a company cannot be removed or inactivated.");
        }
    }

    private void assertPurchasingRole(Role role) {
        if (!PURCHASING_ROLE_NAMES.contains(role.getRoleName())) {
            throw new IllegalArgumentException("Only purchasing roles can be assigned in this operation.");
        }
    }
}
