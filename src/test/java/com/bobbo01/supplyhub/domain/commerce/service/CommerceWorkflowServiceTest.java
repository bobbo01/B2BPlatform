package com.bobbo01.supplyhub.domain.commerce.service;

import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequest;
import com.bobbo01.supplyhub.domain.approval.entity.ApprovalRequestStatus;
import com.bobbo01.supplyhub.domain.approval.repository.ApprovalRequestRepository;
import com.bobbo01.supplyhub.domain.approval.service.ApprovalRequestService;
import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.cart.service.CartService;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderDetailView;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseRequestSummaryView;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseOrderService;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseRequestService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommerceWorkflowServiceTest {

    @Mock
    private CartService cartService;
    @Mock
    private PurchaseRequestService purchaseRequestService;
    @Mock
    private ApprovalRequestService approvalRequestService;
    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Spy
    private PurchaseOrderViewAssembler purchaseOrderViewAssembler;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;
    @Mock
    private PurchaseRequestItemRepository purchaseRequestItemRepository;
    @Mock
    private ApprovalRequestRepository approvalRequestRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock
    private PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommerceWorkflowService commerceWorkflowService;

    @Test
    void submitsPurchaseRequestAndAssignsFirstAvailableApprover() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = companyUser(company, RoleNames.PURCHASER, "requester@example.com");
        User approver = companyUser(company, RoleNames.APPROVER, "approver@example.com");
        Cart cart = Cart.open(company, requester);
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, cart);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(approver, "id", 2L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(purchaseRequestRepository.findById(30L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findAllByCompanyIdAndStatusIgnoreCaseAndRoleRoleNameOrderByIdAsc(10L, "ACTIVE", RoleNames.APPROVER))
                .thenReturn(List.of(approver));

        commerceWorkflowService.submitPurchaseRequest(1L, 30L);

        verify(purchaseRequestService).submit(30L);
        verify(approvalRequestService).createPendingApproval(30L, 2L);
    }

    @Test
    void approvesPendingApprovalAndCreatesOrderDraftOnce() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = companyUser(company, RoleNames.PURCHASER, "requester@example.com");
        User approver = companyUser(company, RoleNames.APPROVER, "approver@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        ApprovalRequest approvalRequest = ApprovalRequest.createPending(company, purchaseRequest, requester, approver);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(approver, "id", 2L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);
        ReflectionTestUtils.setField(approvalRequest, "id", 40L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(approvalRequestRepository.findById(40L)).thenReturn(Optional.of(approvalRequest));
        when(purchaseOrderRepository.existsByPurchaseRequestId(30L)).thenReturn(false);

        commerceWorkflowService.approveApproval(2L, 40L);

        verify(approvalRequestService).approve(40L, 2L, null);
        verify(purchaseOrderService).createDraftFromApprovedRequest(30L, 2L);
    }

    @Test
    void doesNotCreateDuplicateOrderDraftOnApproval() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = companyUser(company, RoleNames.PURCHASER, "requester@example.com");
        User approver = companyUser(company, RoleNames.APPROVER, "approver@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();
        ApprovalRequest approvalRequest = ApprovalRequest.createPending(company, purchaseRequest, requester, approver);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(approver, "id", 2L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);
        ReflectionTestUtils.setField(approvalRequest, "id", 40L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(approvalRequestRepository.findById(40L)).thenReturn(Optional.of(approvalRequest));
        when(purchaseOrderRepository.existsByPurchaseRequestId(30L)).thenReturn(true);

        commerceWorkflowService.approveApproval(2L, 40L);

        verify(approvalRequestService).approve(40L, 2L, null);
        verify(purchaseOrderService, never()).createDraftFromApprovedRequest(30L, 1L);
    }

    @Test
    void updatesCartItemQuantityThroughCartServiceForActiveCompanyUser() {
        User requester = companyUser(company(), RoleNames.PURCHASER, "requester@example.com");
        ReflectionTestUtils.setField(requester, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        commerceWorkflowService.updateCartItemQuantity(1L, 50L, 6);

        verify(cartService).updateItemQuantity(1L, 50L, 6);
    }

    @Test
    void rejectsCartChangesForCompanyUnlinkedUser() {
        User detachedUser = User.createOAuthUser(
                company(),
                Role.builder().roleName(RoleNames.PURCHASER).description(RoleNames.PURCHASER).build(),
                "detached@example.com",
                "Detached",
                null
        );
        detachedUser.leaveCompany();
        ReflectionTestUtils.setField(detachedUser, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(detachedUser));

        assertThatThrownBy(() -> commerceWorkflowService.clearCart(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only company users can use commerce workflows.");
    }

    @Test
    void rejectsPurchaseRequestDraftForCartUser() {
        User requester = companyUser(company(), RoleNames.CART_USER, "requester@example.com");
        ReflectionTestUtils.setField(requester, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> commerceWorkflowService.createPurchaseRequestDraft(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PURCHASER or APPROVER can create and submit purchase requests.");

        verify(purchaseRequestService, never()).createDraftFromCart(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void cancelsDraftPurchaseRequestForRequester() {
        Company company = company();
        User requester = companyUser(company, RoleNames.PURCHASER, "requester@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(purchaseRequestRepository.findById(30L)).thenReturn(Optional.of(purchaseRequest));

        commerceWorkflowService.cancelPurchaseRequest(1L, 30L);

        verify(purchaseRequestService).cancel(30L);
    }

    @Test
    void rejectsPurchaseRequestSubmissionForCartUser() {
        Company company = company();
        User requester = companyUser(company, RoleNames.CART_USER, "requester@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> commerceWorkflowService.submitPurchaseRequest(1L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PURCHASER or APPROVER can create and submit purchase requests.");

        verify(purchaseRequestService, never()).submit(30L);
        verify(approvalRequestService, never()).createPendingApproval(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsPurchaseRequestCancellationForCartUser() {
        Company company = company();
        User requester = companyUser(company, RoleNames.CART_USER, "requester@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> commerceWorkflowService.cancelPurchaseRequest(1L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PURCHASER or APPROVER can create and submit purchase requests.");

        verify(purchaseRequestService, never()).cancel(30L);
    }

    @Test
    void rejectsApprovalWithDecisionNote() {
        Company company = company();
        User approver = companyUser(company, RoleNames.APPROVER, "approver@example.com");
        ReflectionTestUtils.setField(approver, "id", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));

        commerceWorkflowService.rejectApproval(2L, 40L, "budget exceeded");

        verify(approvalRequestService).reject(40L, 2L, "budget exceeded");
    }

    @Test
    void rejectsApprovalForPurchaserRole() {
        Company company = company();
        User approver = companyUser(company, RoleNames.PURCHASER, "approver@example.com");
        ReflectionTestUtils.setField(approver, "id", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));

        assertThatThrownBy(() -> commerceWorkflowService.approveApproval(2L, 40L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only APPROVER can review purchase approvals.");

        verify(approvalRequestService, never()).approve(40L, 2L, null);
        verify(purchaseOrderService, never()).createDraftFromApprovedRequest(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsApprovalRejectionForPurchaserRole() {
        Company company = company();
        User approver = companyUser(company, RoleNames.PURCHASER, "approver@example.com");
        ReflectionTestUtils.setField(approver, "id", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));

        assertThatThrownBy(() -> commerceWorkflowService.rejectApproval(2L, 40L, "budget exceeded"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only APPROVER can review purchase approvals.");

        verify(approvalRequestService, never()).reject(40L, 2L, "budget exceeded");
    }

    @Test
    void loadsWorkspaceViewWithBatchedPurchaseOrderItems() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("25.00"), new BigDecimal("25.00"), "KRW");
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(null)
                .quantity(2)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByOwnerIdAndStatus(1L, com.bobbo01.supplyhub.domain.cart.entity.CartStatus.OPEN))
                .thenReturn(Optional.empty());
        when(purchaseRequestRepository.findAllByRequesterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(approvalRequestRepository.findAllByApproverIdAndStatusOrderByCreatedAtAsc(1L, ApprovalRequestStatus.PENDING))
                .thenReturn(List.of());
        when(purchaseOrderRepository.findAllWithPurchaseRequestByBuyerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L)))
                .thenReturn(List.of(item));

        CommerceWorkspaceView workspaceView = commerceWorkflowService.getWorkspaceView(1L);

        assertThat(workspaceView.purchaseOrders()).hasSize(1);
        assertThat(workspaceView.purchaseOrders().getFirst().purchaseOrderId()).isEqualTo(30L);
        assertThat(workspaceView.purchaseOrders().getFirst().itemCount()).isEqualTo(1);
        assertThat(workspaceView.purchaseOrders().getFirst().totalAmount()).isEqualByComparingTo("25.00");
        verify(purchaseOrderRepository).findAllWithPurchaseRequestByBuyerIdOrderByCreatedAtDesc(1L);
        verify(purchaseOrderItemRepository).findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L));
    }

    @Test
    void loadsWorkspaceViewWithBatchedPurchaseRequestItems() {
        Company company = company();
        User requester = companyUser(company, RoleNames.APPROVER, "requester@example.com");
        Product product = Product.builder()
                .sku("P1")
                .productName("Paper")
                .brand("Brand")
                .description("desc")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseRequest draftRequest = PurchaseRequest.createDraft(company, requester, null);
        PurchaseRequest submittedRequest = PurchaseRequest.createDraft(company, requester, null);
        submittedRequest.submit();
        ApprovalRequest approvalRequest = ApprovalRequest.createPending(company, submittedRequest, requester, requester);
        var draftItem = purchaseRequestItem(draftRequest, product, 2, "12.50");
        var submittedItem = purchaseRequestItem(submittedRequest, product, 1, "5.00");

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(product, "id", 10L);
        ReflectionTestUtils.setField(draftRequest, "id", 20L);
        ReflectionTestUtils.setField(submittedRequest, "id", 21L);
        ReflectionTestUtils.setField(draftItem, "id", 30L);
        ReflectionTestUtils.setField(submittedItem, "id", 31L);
        ReflectionTestUtils.setField(approvalRequest, "id", 40L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(cartRepository.findByOwnerIdAndStatus(1L, com.bobbo01.supplyhub.domain.cart.entity.CartStatus.OPEN))
                .thenReturn(Optional.empty());
        when(purchaseRequestRepository.findAllByRequesterIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(submittedRequest, draftRequest));
        when(approvalRequestRepository.findAllByApproverIdAndStatusOrderByCreatedAtAsc(1L, ApprovalRequestStatus.PENDING))
                .thenReturn(List.of(approvalRequest));
        when(purchaseRequestItemRepository.findAllByPurchaseRequestIdInOrderByCreatedAtAsc(List.of(21L, 20L)))
                .thenReturn(List.of(submittedItem, draftItem));
        when(purchaseOrderRepository.findAllWithPurchaseRequestByBuyerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        CommerceWorkspaceView workspaceView = commerceWorkflowService.getWorkspaceView(1L);

        assertThat(workspaceView.purchaseRequests()).extracting(PurchaseRequestSummaryView::purchaseRequestId)
                .containsExactly(21L, 20L);
        assertThat(workspaceView.purchaseRequests())
                .extracting(PurchaseRequestSummaryView::itemCount)
                .containsExactly(1, 1);
        assertThat(workspaceView.pendingApprovals()).hasSize(1);
        assertThat(workspaceView.pendingApprovals().getFirst().purchaseRequestId()).isEqualTo(21L);
        assertThat(workspaceView.pendingApprovals().getFirst().items()).hasSize(1);
        verify(purchaseRequestItemRepository).findAllByPurchaseRequestIdInOrderByCreatedAtAsc(List.of(21L, 20L));
    }

    @Test
    void loadsPurchaseOrderDetailForBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("37.50"), new BigDecimal("37.50"), "KRW");
        Product product = Product.builder()
                .sku("P1")
                .productName("Paper")
                .brand("Brand")
                .description("desc")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(3)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);
        ReflectionTestUtils.setField(item, "id", 40L);
        ReflectionTestUtils.setField(product, "id", 50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findDetailById(30L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L)).thenReturn(List.of(item));

        PurchaseOrderDetailView detailView = commerceWorkflowService.getPurchaseOrderDetail(1L, 30L);

        assertThat(detailView.purchaseOrderId()).isEqualTo(30L);
        assertThat(detailView.purchaseRequestId()).isEqualTo(20L);
        assertThat(detailView.statusCode()).isEqualTo(PurchaseOrderStatus.DRAFT.name());
        assertThat(detailView.statusLabel()).isEqualTo("주문 초안");
        assertThat(detailView.statusGuideTitle()).isEqualTo("주문 초안을 검토해 주세요");
        assertThat(detailView.statusGuideMessage()).contains("플랫폼 승인 대기");
        assertThat(detailView.terminalStatus()).isFalse();
        assertThat(detailView.itemCount()).isEqualTo(1);
        assertThat(detailView.totalAmount()).isEqualByComparingTo("37.50");
        assertThat(detailView.canSubmitForPlatformApproval()).isTrue();
        assertThat(detailView.canCancel()).isTrue();
        assertThat(detailView.canPay()).isFalse();
        assertThat(detailView.progressSteps()).hasSize(7);
        assertThat(detailView.progressSteps().getFirst().current()).isTrue();
        verify(purchaseOrderRepository).findDetailById(30L);
    }

    @Test
    void submitsPurchaseOrderForPlatformApprovalForBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findById(30L)).thenReturn(Optional.of(purchaseOrder));

        commerceWorkflowService.submitPurchaseOrderForPlatformApproval(1L, 30L);

        verify(purchaseOrderService).submitForPlatformApproval(30L, 1L);
    }

    @Test
    void cancelsPurchaseOrderForBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findById(30L)).thenReturn(Optional.of(purchaseOrder));

        commerceWorkflowService.cancelPurchaseOrder(1L, 30L);

        verify(purchaseOrderService).cancel(30L, 1L);
    }

    @Test
    void rejectsPurchaseOrderSubmissionForNonApproverBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.PURCHASER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> commerceWorkflowService.submitPurchaseOrderForPlatformApproval(1L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only APPROVER can submit or cancel purchase orders.");

        verify(purchaseOrderService, never()).submitForPlatformApproval(30L, 1L);
    }

    @Test
    void exposesPlatformReviewFieldsForRejectedOrder() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("12.50"), new BigDecimal("12.50"), "KRW");
        Product product = Product.builder()
                .sku("P1")
                .productName("Paper")
                .brand("Brand")
                .description("desc")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.rejectByPlatform(reviewer, "memo", "out of policy");

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);
        ReflectionTestUtils.setField(item, "id", 40L);
        ReflectionTestUtils.setField(product, "id", 50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findDetailById(30L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L)).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdOrderByChangedAtAsc(30L))
                .thenReturn(List.of(
                        PurchaseOrderStatusHistory.record(
                                purchaseOrder,
                                PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL,
                                PurchaseOrderStatus.REJECTED,
                                reviewer,
                                "out of policy"
                        )
                ));

        PurchaseOrderDetailView detailView = commerceWorkflowService.getPurchaseOrderDetail(1L, 30L);

        assertThat(detailView.statusCode()).isEqualTo(PurchaseOrderStatus.REJECTED.name());
        assertThat(detailView.statusLabel()).isEqualTo("반려");
        assertThat(detailView.statusGuideTitle()).isEqualTo("주문이 반려되었습니다");
        assertThat(detailView.statusGuideMessage()).contains("반려 사유");
        assertThat(detailView.terminalStatus()).isTrue();
        assertThat(detailView.platformReviewedByName()).isEqualTo("Admin");
        assertThat(detailView.platformReviewMemo()).isEqualTo("memo");
        assertThat(detailView.platformRejectionReason()).isEqualTo("out of policy");
        assertThat(detailView.canSubmitForPlatformApproval()).isFalse();
        assertThat(detailView.canCancel()).isFalse();
        assertThat(detailView.canPay()).isFalse();
        assertThat(detailView.progressSteps().get(1).current()).isTrue();
        assertThat(detailView.statusHistory()).hasSize(1);
        assertThat(detailView.statusHistory().getFirst().toStatusCode()).isEqualTo(PurchaseOrderStatus.REJECTED.name());
        assertThat(detailView.statusHistory().getFirst().transitionLabel()).isEqualTo("주문 승인 대기 -> 반려");
        assertThat(detailView.statusHistory().getFirst().changedByDisplayName()).isEqualTo("Admin");
        assertThat(detailView.statusHistory().getFirst().changeNoteDisplay()).isEqualTo("out of policy");
    }

    @Test
    void exposesDisplayFallbacksForSystemOwnedStatusHistory() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("12.50"), new BigDecimal("12.50"), "KRW");
        Product product = Product.builder()
                .sku("P1")
                .productName("Paper")
                .brand("Brand")
                .description("desc")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);
        ReflectionTestUtils.setField(item, "id", 40L);
        ReflectionTestUtils.setField(product, "id", 50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findDetailById(30L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L)).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdOrderByChangedAtAsc(30L))
                .thenReturn(List.of(
                        PurchaseOrderStatusHistory.record(
                                purchaseOrder,
                                PurchaseOrderStatus.DRAFT,
                                PurchaseOrderStatus.PAYMENT_PENDING,
                                null,
                                null
                        )
                ));

        PurchaseOrderDetailView detailView = commerceWorkflowService.getPurchaseOrderDetail(1L, 30L);

        assertThat(detailView.statusHistory()).hasSize(1);
        assertThat(detailView.statusHistory().getFirst().transitionLabel()).isEqualTo("주문 초안 -> 결제 대기");
        assertThat(detailView.statusHistory().getFirst().changedByName()).isNull();
        assertThat(detailView.statusHistory().getFirst().changedByDisplayName()).isEqualTo("시스템");
        assertThat(detailView.statusHistory().getFirst().changeNote()).isNull();
        assertThat(detailView.statusHistory().getFirst().changeNoteDisplay()).isEqualTo("-");
    }

    @Test
    void rejectsPurchaseOrderDetailForPurchaserRole() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.PURCHASER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> commerceWorkflowService.getPurchaseOrderDetail(1L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only APPROVER can submit or cancel purchase orders.");
        verify(purchaseOrderItemRepository, never()).findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L);
    }

    @Test
    void rejectsPurchaseOrderDetailForNonBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        User otherUser = companyUser(company, RoleNames.APPROVER, "other@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(otherUser, "id", 2L);
        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(purchaseOrderRepository.findDetailById(30L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> commerceWorkflowService.getPurchaseOrderDetail(2L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the buyer can manage this purchase order.");
        verify(purchaseOrderItemRepository, never()).findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L);
    }

    @Test
    void hidesBuyerActionsForConfirmedOrder() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        User reviewer = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("12.50"), new BigDecimal("12.50"), "KRW");
        Product product = Product.builder()
                .sku("P1")
                .productName("Paper")
                .brand("Brand")
                .description("desc")
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("12.50"))
                .currencyCode("KRW")
                .build();
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(reviewer, "approved");

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);
        ReflectionTestUtils.setField(item, "id", 40L);
        ReflectionTestUtils.setField(product, "id", 50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findDetailById(30L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdOrderByCreatedAtAsc(30L)).thenReturn(List.of(item));

        PurchaseOrderDetailView detailView = commerceWorkflowService.getPurchaseOrderDetail(1L, 30L);

        assertThat(detailView.statusCode()).isEqualTo(PurchaseOrderStatus.PAYMENT_PENDING.name());
        assertThat(detailView.statusLabel()).isEqualTo("결제 대기");
        assertThat(detailView.statusGuideTitle()).isEqualTo("주문이 확정되어 결제를 기다리고 있습니다");
        assertThat(detailView.statusGuideMessage()).contains("구매자가 결제를 완료");
        assertThat(detailView.terminalStatus()).isFalse();
        assertThat(detailView.canSubmitForPlatformApproval()).isFalse();
        assertThat(detailView.canCancel()).isFalse();
        assertThat(detailView.canPay()).isTrue();
        assertThat(detailView.progressSteps().get(2).current()).isTrue();
    }

    @Test
    void paysPurchaseOrderForBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.PURCHASER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(purchaseOrderRepository.findById(30L)).thenReturn(Optional.of(purchaseOrder));

        commerceWorkflowService.payPurchaseOrder(1L, 30L);

        verify(purchaseOrderService).markPaid(30L, 1L);
    }

    @Test
    void rejectsPurchaseOrderPaymentForCartUser() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.CART_USER, "buyer@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> commerceWorkflowService.payPurchaseOrder(1L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PURCHASER or APPROVER can create and submit purchase requests.");

        verify(purchaseOrderService, never()).markPaid(30L, 1L);
    }

    @Test
    void rejectsPurchaseOrderManagementForNonBuyer() {
        Company company = company();
        User buyer = companyUser(company, RoleNames.APPROVER, "buyer@example.com");
        User otherUser = companyUser(company, RoleNames.APPROVER, "other@example.com");
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(purchaseOrderRepository.findById(30L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> commerceWorkflowService.cancelPurchaseOrder(2L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the buyer can manage this purchase order.");

        verify(purchaseOrderService, never()).cancel(30L, 2L);
    }

    private User companyUser(Company company, String roleName, String email) {
        return User.createOAuthUser(
                company,
                Role.builder().roleName(roleName).description(roleName).build(),
                email,
                "User",
                null
        );
    }

    private Company company() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);
        return company;
    }

    private com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem purchaseRequestItem(
            PurchaseRequest purchaseRequest,
            Product product,
            int quantity,
            String unitPrice
    ) {
        return com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem.builder()
                .purchaseRequest(purchaseRequest)
                .product(product)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .currencyCode("KRW")
                .build();
    }
}
