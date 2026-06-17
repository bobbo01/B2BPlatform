package com.bobbo01.supplyhub.domain.purchase.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderSettlementStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository purchaseRequestItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Test
    void createsDraftOrderFromApprovedPurchaseRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        Product product = Product.builder()
                .sku("SKU-1")
                .productName("Chair")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        purchaseRequest.approve();
        PurchaseRequestItem requestItem = PurchaseRequestItem.builder()
                .purchaseRequest(purchaseRequest)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();
        ReflectionTestUtils.setField(company, "id", 1L);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRequestItemRepository.findAllByPurchaseRequestIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(requestItem));

        PurchaseOrder purchaseOrder = purchaseOrderService.createDraftFromApprovedRequest(10L, 2L);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(purchaseOrder.getSubtotalAmount()).isEqualByComparingTo("25.00");
        assertThat(purchaseOrder.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(purchaseOrder.getCurrencyCode()).isEqualTo("KRW");

        ArgumentCaptor<List<PurchaseOrderItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(1);
        assertThat(itemCaptor.getValue().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsDraftOrderCreationForNonApproverBuyer() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        purchaseRequest.approve();
        ReflectionTestUtils.setField(company, "id", 1L);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> purchaseOrderService.createDraftFromApprovedRequest(10L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User does not have permission to change this purchase order.");
    }

    @Test
    void submitsDraftOrderForPlatformApproval() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        ReflectionTestUtils.setField(buyer, "id", 10L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        purchaseOrderService.submitForPlatformApproval(10L, 10L);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        assertThat(purchaseOrder.getSubmittedForPlatformApprovalAt()).isNotNull();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        assertThat(historyCaptor.getValue().getChangedByUser()).isEqualTo(buyer);
    }

    @Test
    void cancelsDraftOrderForApproverBuyer() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        ReflectionTestUtils.setField(buyer, "id", 10L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        purchaseOrderService.cancel(10L, 10L);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        assertThat(purchaseOrder.getCancelledAt()).isNotNull();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        assertThat(historyCaptor.getValue().getChangedByUser()).isEqualTo(buyer);
    }

    @Test
    void rejectsDraftOrderCreationForBuyerInDifferentCompany() {
        Company sourceCompany = Company.builder().companyName("Source").status("ACTIVE").build();
        Company otherCompany = Company.builder().companyName("Other").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                sourceCompany,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User buyer = User.createOAuthUser(
                otherCompany,
                Role.builder().roleName(RoleNames.APPROVER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(sourceCompany, requester, null);
        purchaseRequest.submit();
        purchaseRequest.approve();
        ReflectionTestUtils.setField(sourceCompany, "id", 1L);
        ReflectionTestUtils.setField(otherCompany, "id", 2L);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> purchaseOrderService.createDraftFromApprovedRequest(10L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a buyer in the same company can create this purchase order.");
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void approvesPendingPlatformOrder() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.submitForPlatformApproval();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        purchaseOrderService.approveByPlatform(10L, 20L, "looks good");

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PAYMENT_PENDING);
        assertThat(purchaseOrder.getPlatformReviewedBy()).isEqualTo(reviewer);
        assertThat(purchaseOrder.getPlatformReviewMemo()).isEqualTo("looks good");
        assertThat(purchaseOrder.getPlacedAt()).isNotNull();
        assertThat(purchaseOrder.canSubmitForPlatformApproval()).isFalse();
        assertThat(purchaseOrder.canCancel()).isFalse();
        assertThat(purchaseOrder.canPay()).isTrue();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(PurchaseOrderStatus.PAYMENT_PENDING);
        assertThat(historyCaptor.getValue().getChangedByUser()).isEqualTo(reviewer);
        assertThat(historyCaptor.getValue().getChangeNote()).isEqualTo("looks good");
    }

    @Test
    void rejectsPlatformApprovalFromNonPlatformAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "reviewer@example.com",
                "Reviewer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.submitForPlatformApproval();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> purchaseOrderService.approveByPlatform(10L, 20L, "looks good"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PLATFORM_ADMIN can change platform-managed order states.");
    }

    @Test
    void requiresRejectionReasonForPlatformRejection() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.submitForPlatformApproval();

        assertThatThrownBy(() -> purchaseOrderService.rejectByPlatform(10L, 20L, null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rejection reason is required.");
    }

    @Test
    void rejectsNonKrwPurchaseOrderSnapshot() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "requester@example.com",
                "Requester",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        Product product = Product.builder()
                .sku("SKU-USD")
                .productName("Chair")
                .unitPrice(new BigDecimal("10.00"))
                .currencyCode("USD")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        purchaseRequest.approve();
        PurchaseRequestItem requestItem = PurchaseRequestItem.builder()
                .purchaseRequest(purchaseRequest)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("10.00"))
                .currencyCode("USD")
                .build();
        ReflectionTestUtils.setField(company, "id", 1L);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRequestItemRepository.findAllByPurchaseRequestIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(requestItem));

        assertThatThrownBy(() -> purchaseOrderService.createDraftFromApprovedRequest(10L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only KRW purchase orders are supported.");
    }

    @Test
    void keepsRejectedOrderTerminal() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.submitForPlatformApproval();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        purchaseOrderService.rejectByPlatform(10L, 20L, "needs revision", "out of policy");

        assertThat(purchaseOrder.isRejected()).isTrue();
        assertThat(purchaseOrder.canSubmitForPlatformApproval()).isFalse();
        assertThat(purchaseOrder.canCancel()).isFalse();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(PurchaseOrderStatus.REJECTED);
        assertThat(historyCaptor.getValue().getChangedByUser()).isEqualTo(reviewer);
        assertThat(historyCaptor.getValue().getChangeNote()).isEqualTo("out of policy");
        assertThatThrownBy(purchaseOrder::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Purchase order can no longer be cancelled.");
    }

    @Test
    void marksPaymentPendingOrderAsPaid() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(reviewer, "approved");
        ReflectionTestUtils.setField(buyer, "id", 10L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));

        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        purchaseOrderService.markPaid(10L, 10L);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PAID);
        assertThat(purchaseOrder.getPaidAt()).isNotNull();
        assertThat(purchaseOrder.canPay()).isFalse();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(PurchaseOrderStatus.PAYMENT_PENDING);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(PurchaseOrderStatus.PAID);
        assertThat(historyCaptor.getValue().getChangedByUser()).isEqualTo(buyer);
        assertThat(historyCaptor.getValue().getChangeNote()).isEqualTo("PAYMENT_COMPLETED");
    }

    @Test
    void allowsApproverBuyerToMarkPaymentPendingOrderAsPaid() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(reviewer, "approved");
        ReflectionTestUtils.setField(buyer, "id", 10L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        purchaseOrderService.markPaid(10L, 10L);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PAID);
        verify(purchaseOrderStatusHistoryRepository).save(any(PurchaseOrderStatusHistory.class));
    }

    @Test
    void rejectsPaymentFromNonBuyer() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User otherUser = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("other").build(),
                "other@example.com",
                "Other",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(
                User.createPlatformAdmin(
                        Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                        "admin@example.com",
                        "Admin",
                        null
                ),
                "approved"
        );
        ReflectionTestUtils.setField(buyer, "id", 10L);
        ReflectionTestUtils.setField(otherUser, "id", 11L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(11L)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> purchaseOrderService.markPaid(10L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the buyer can change this purchase order.");
    }

    @Test
    void rejectsPaymentFromInactiveBuyer() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        buyer.inactivate();
        ReflectionTestUtils.setField(buyer, "id", 10L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> purchaseOrderService.markPaid(10L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active company users can change buyer-managed order states.");
    }

    @Test
    void advancesDeliveryStatesAfterPayment() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(reviewer, "approved");
        purchaseOrder.markPaid();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        purchaseOrderService.markReadyToShip(10L, 20L);
        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.READY_TO_SHIP);
        assertThat(purchaseOrder.getReadyToShipAt()).isNotNull();

        purchaseOrderService.markShipped(10L, 20L);
        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.SHIPPED);
        assertThat(purchaseOrder.getShippedAt()).isNotNull();

        purchaseOrderService.markDelivered(10L, 20L);
        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.DELIVERED);
        assertThat(purchaseOrder.getDeliveredAt()).isNotNull();
        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(purchaseOrderStatusHistoryRepository, org.mockito.Mockito.times(3)).save(historyCaptor.capture());
        assertThat(historyCaptor.getAllValues())
                .extracting(PurchaseOrderStatusHistory::getToStatus)
                .containsExactly(
                        PurchaseOrderStatus.READY_TO_SHIP,
                        PurchaseOrderStatus.SHIPPED,
                        PurchaseOrderStatus.DELIVERED
                );
        assertThat(historyCaptor.getAllValues())
                .extracting(PurchaseOrderStatusHistory::getChangedByUser)
                .containsOnly(reviewer);
    }

    @Test
    void marksDeliveredOrderAsSettled() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(reviewer, "approved");
        purchaseOrder.markPaid();
        purchaseOrder.markReadyToShip();
        purchaseOrder.markShipped();
        purchaseOrder.markDelivered();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        purchaseOrderService.markSettled(10L, 20L);

        assertThat(purchaseOrder.getSettlementStatus()).isEqualTo(PurchaseOrderSettlementStatus.SETTLED);
        assertThat(purchaseOrder.getSettledAt()).isNotNull();
        assertThat(purchaseOrder.getSettledBy()).isEqualTo(reviewer);
        verify(purchaseOrderStatusHistoryRepository, never()).save(any(PurchaseOrderStatusHistory.class));
    }

    @Test
    void rejectsSettlementBeforeDelivery() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("buyer").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(purchaseOrder));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> purchaseOrderService.markSettled(10L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Purchase order is not ready to be settled.");
    }

    @Test
    void rejectsDeliveryTransitionFromInactivePlatformAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.inactivate();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> purchaseOrderService.markReadyToShip(10L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active PLATFORM_ADMIN can change platform-managed order states.");
    }

    @Test
    void rejectsPlatformRejectionFromInactivePlatformAdmin() {
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build(),
                "admin@example.com",
                "Admin",
                null
        );
        reviewer.inactivate();
        ReflectionTestUtils.setField(reviewer, "id", 20L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> purchaseOrderService.rejectByPlatform(10L, 20L, "memo", "out of policy"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active PLATFORM_ADMIN can change platform-managed order states.");
    }
}
