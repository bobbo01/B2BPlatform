package com.bobbo01.supplyhub.domain.admin.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.commerce.service.PurchaseOrderViewAssembler;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderSettlementStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseOrderStatusHistoryRepository;
import com.bobbo01.supplyhub.domain.purchase.service.PurchaseOrderService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock
    private PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @Spy
    private PurchaseOrderViewAssembler purchaseOrderViewAssembler = new PurchaseOrderViewAssembler();

    @InjectMocks
    private PlatformAdminService platformAdminService;

    @Test
    void loadsUserViewsWithCompanyDataAndCaseInsensitiveSorting() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User companyUser = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                "Bravo@example.com",
                "Bravo",
                null
        );
        User platformUser = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "alpha@example.com",
                "Alpha",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(companyUser, "id", 2L);
        ReflectionTestUtils.setField(platformUser, "id", 3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAllWithCompanyOrderByEmailAsc()).thenReturn(List.of(platformUser, companyUser));

        var views = platformAdminService.getUserViews(1L);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).email()).isEqualTo("alpha@example.com");
        assertThat(views.get(0).accountType()).isEqualTo(RoleNames.PLATFORM_ADMIN);
        assertThat(views.get(0).companyName()).isNull();
        assertThat(views.get(1).email()).isEqualTo("Bravo@example.com");
        assertThat(views.get(1).companyName()).isEqualTo("Example");
        verify(userRepository).findAllWithCompanyOrderByEmailAsc();
        verify(userRepository, never()).findAll();
    }

    @Test
    void loadsCompanyViewsWithCaseInsensitiveSorting() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        Company bravo = Company.builder().companyName("bravo").companyDomain("bravo.example.com").status("ACTIVE").build();
        Company alpha = Company.builder().companyName("Alpha").companyDomain("alpha.example.com").status("INACTIVE").build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(bravo, "id", 10L);
        ReflectionTestUtils.setField(alpha, "id", 11L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(companyRepository.findAllOrderByCompanyNameAsc()).thenReturn(List.of(alpha, bravo));

        var views = platformAdminService.getCompanyViews(1L);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).companyName()).isEqualTo("Alpha");
        assertThat(views.get(0).status()).isEqualTo("INACTIVE");
        assertThat(views.get(1).companyName()).isEqualTo("bravo");
        assertThat(views.get(1).status()).isEqualTo("ACTIVE");
        verify(companyRepository).findAllOrderByCompanyNameAsc();
        verify(companyRepository, never()).findAll();
    }

    @Test
    void updatesCompanyUserStatus() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        platformAdminService.updateUserStatus(1L, 2L, "INACTIVE");

        assertThat(target.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updatesCompanyUserPurchasingRole() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        Role approverRole = Role.builder().roleName(RoleNames.APPROVER).description("approver").build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.APPROVER)).thenReturn(Optional.of(approverRole));

        platformAdminService.updateUserPurchasingRole(1L, 2L, RoleNames.APPROVER);

        assertThat(target.getPurchasingRole().getRoleName()).isEqualTo(RoleNames.APPROVER);
    }

    @Test
    void rejectsUpdatingPlatformAdminPurchasingRole() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "target@example.com",
                "Target",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> platformAdminService.updateUserPurchasingRole(1L, 2L, RoleNames.APPROVER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PLATFORM_ADMIN user does not have a purchasing role.");
    }

    @Test
    void rejectsAssigningNonPurchasingRole() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        Role platformRole = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.PLATFORM_ADMIN)).thenReturn(Optional.of(platformRole));

        assertThatThrownBy(() -> platformAdminService.updateUserPurchasingRole(1L, 2L, RoleNames.PLATFORM_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only purchasing roles can be assigned in this operation.");
    }

    @Test
    void grantsCompanyAdminToCompanyUser() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        platformAdminService.updateUserCompanyAdmin(1L, 2L, true);

        assertThat(target.hasCompanyAdminRole()).isTrue();
    }

    @Test
    void rejectsGrantingCompanyAdminToPlatformAdmin() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "target@example.com",
                "Target",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> platformAdminService.updateUserCompanyAdmin(1L, 2L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PLATFORM_ADMIN user cannot hold COMPANY_ADMIN.");
    }

    @Test
    void rejectsRemovingLastActiveCompanyAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        target.grantCompanyAdmin();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(1L);

        assertThatThrownBy(() -> platformAdminService.updateUserCompanyAdmin(1L, 2L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active COMPANY_ADMIN");
    }

    @Test
    void rejectsInactivatingLastActiveCompanyAdmin() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 10L);
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User target = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        target.grantCompanyAdmin();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(1L);

        assertThatThrownBy(() -> platformAdminService.updateUserStatus(1L, 2L, "INACTIVE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active COMPANY_ADMIN");
    }

    @Test
    void updatesCompanyStatus() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(admin, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        platformAdminService.updateCompanyStatus(1L, 10L, "INACTIVE");

        assertThat(company.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void listsPendingPlatformApprovalOrders() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("20.00"), new BigDecimal("20.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(null)
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .currencyCode("KRW")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL)))
                .thenReturn(List.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L))).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdInOrderByChangedAtAsc(List.of(30L)))
                .thenReturn(List.of());

        var results = platformAdminService.getPlatformApprovalOrders(1L, "pending");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().purchaseOrderId()).isEqualTo(30L);
        assertThat(results.getFirst().totalAmount()).isEqualByComparingTo("20.00");
        assertThat(results.getFirst().canApprove()).isTrue();
        assertThat(results.getFirst().canReject()).isTrue();
        assertThat(results.getFirst().hasAvailableAction()).isTrue();
    }

    @Test
    void listsApprovedPlatformOrders() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("25.00"), new BigDecimal("25.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(admin, "approved memo");
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("25.00"))
                .currencyCode("KRW")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                PurchaseOrderStatus.PAYMENT_PENDING
        )))
                .thenReturn(List.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L))).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdInOrderByChangedAtAsc(List.of(30L)))
                .thenReturn(List.of());

        var results = platformAdminService.getPlatformApprovalOrders(1L, "approved");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo(PurchaseOrderStatus.PAYMENT_PENDING.name());
        assertThat(results.getFirst().statusLabel()).isEqualTo("결제 대기");
        assertThat(results.getFirst().platformReviewMemo()).isEqualTo("approved memo");
        assertThat(results.getFirst().platformReviewerName()).isEqualTo("Admin");
        assertThat(results.getFirst().hasAvailableAction()).isFalse();
        assertThat(results.getFirst().actionGuideMessage()).isEqualTo("구매자의 결제를 기다리는 상태입니다.");
    }

    @Test
    void normalizesUnknownOrderFilterToPending() {
        assertThat(platformAdminService.normalizeOrderFilter("weird")).isEqualTo("pending");
        assertThat(platformAdminService.normalizeOrderFilter("APPROVED")).isEqualTo("approved");
        assertThat(platformAdminService.normalizeOrderFilter("all")).isEqualTo("all");
    }

    @Test
    void forwardsPlatformApprovalToPurchaseOrderService() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        platformAdminService.approvePurchaseOrder(1L, 10L, "approved");

        verify(purchaseOrderService).approveByPlatform(10L, 1L, "approved");
    }

    @Test
    void forwardsPlatformRejectionToPurchaseOrderService() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        platformAdminService.rejectPurchaseOrder(1L, 10L, "needs revision", "out of policy");

        verify(purchaseOrderService).rejectByPlatform(10L, 1L, "needs revision", "out of policy");
    }

    @Test
    void normalizesDeliveryOrderFilter() {
        assertThat(platformAdminService.normalizeOrderFilter("delivery")).isEqualTo("delivery");
    }

    @Test
    void loadsDeliveryOrders() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("25.00"), new BigDecimal("25.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.approveByPlatform(admin, "approved memo");
        purchaseOrder.markPaid();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("25.00"))
                .currencyCode("KRW")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                PurchaseOrderStatus.PAID,
                PurchaseOrderStatus.READY_TO_SHIP,
                PurchaseOrderStatus.SHIPPED,
                PurchaseOrderStatus.DELIVERED
        ))).thenReturn(List.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L))).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdInOrderByChangedAtAsc(List.of(30L)))
                .thenReturn(List.of());

        var results = platformAdminService.getPlatformApprovalOrders(1L, "delivery");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo(PurchaseOrderStatus.PAID.name());
        assertThat(results.getFirst().paidAt()).isNotNull();
        assertThat(results.getFirst().canMarkReadyToShip()).isTrue();
        assertThat(results.getFirst().canMarkShipped()).isFalse();
        assertThat(results.getFirst().canMarkDelivered()).isFalse();
        assertThat(results.getFirst().hasAvailableAction()).isTrue();
    }

    @Test
    void allFilterKeepsOrderActionsBoundToEachOrderStatus() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );

        PurchaseRequest pendingRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder pendingOrder = PurchaseOrder.createDraft(company, pendingRequest, buyer);
        pendingOrder.applyPricingSnapshot(new BigDecimal("10.00"), new BigDecimal("10.00"), "KRW");
        pendingOrder.submitForPlatformApproval();

        PurchaseRequest paidRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder paidOrder = PurchaseOrder.createDraft(company, paidRequest, buyer);
        paidOrder.applyPricingSnapshot(new BigDecimal("20.00"), new BigDecimal("20.00"), "KRW");
        paidOrder.submitForPlatformApproval();
        paidOrder.approveByPlatform(admin, "approved");
        paidOrder.markPaid();

        PurchaseRequest rejectedRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder rejectedOrder = PurchaseOrder.createDraft(company, rejectedRequest, buyer);
        rejectedOrder.applyPricingSnapshot(new BigDecimal("30.00"), new BigDecimal("30.00"), "KRW");
        rejectedOrder.submitForPlatformApproval();
        rejectedOrder.rejectByPlatform(admin, "memo", "out of policy");

        PurchaseOrderItem pendingItem = PurchaseOrderItem.builder()
                .purchaseOrder(pendingOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("10.00"))
                .currencyCode("KRW")
                .build();
        PurchaseOrderItem paidItem = PurchaseOrderItem.builder()
                .purchaseOrder(paidOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("20.00"))
                .currencyCode("KRW")
                .build();
        PurchaseOrderItem rejectedItem = PurchaseOrderItem.builder()
                .purchaseOrder(rejectedOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("30.00"))
                .currencyCode("KRW")
                .build();

        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(pendingRequest, "id", 20L);
        ReflectionTestUtils.setField(paidRequest, "id", 21L);
        ReflectionTestUtils.setField(rejectedRequest, "id", 22L);
        ReflectionTestUtils.setField(pendingOrder, "id", 30L);
        ReflectionTestUtils.setField(paidOrder, "id", 31L);
        ReflectionTestUtils.setField(rejectedOrder, "id", 32L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingOrder, paidOrder, rejectedOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(pendingItem, paidItem, rejectedItem));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdInOrderByChangedAtAsc(List.of(30L, 31L, 32L)))
                .thenReturn(List.of());

        var results = platformAdminService.getPlatformApprovalOrders(1L, "all");

        assertThat(results).hasSize(3);
        assertThat(results.get(0).status()).isEqualTo(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL.name());
        assertThat(results.get(0).canApprove()).isTrue();
        assertThat(results.get(0).canReject()).isTrue();
        assertThat(results.get(0).canMarkReadyToShip()).isFalse();

        assertThat(results.get(1).status()).isEqualTo(PurchaseOrderStatus.PAID.name());
        assertThat(results.get(1).canApprove()).isFalse();
        assertThat(results.get(1).canReject()).isFalse();
        assertThat(results.get(1).canMarkReadyToShip()).isTrue();
        assertThat(results.get(1).hasAvailableAction()).isTrue();

        assertThat(results.get(2).status()).isEqualTo(PurchaseOrderStatus.REJECTED.name());
        assertThat(results.get(2).hasAvailableAction()).isFalse();
        assertThat(results.get(2).actionGuideMessage()).contains("반려");
    }

    @Test
    void loadsRejectedOrders() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        purchaseOrder.applyPricingSnapshot(new BigDecimal("25.00"), new BigDecimal("25.00"), "KRW");
        purchaseOrder.submitForPlatformApproval();
        purchaseOrder.rejectByPlatform(admin, "memo", "out of policy");
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("25.00"))
                .currencyCode("KRW")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(purchaseOrder, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                PurchaseOrderStatus.REJECTED
        ))).thenReturn(List.of(purchaseOrder));
        when(purchaseOrderItemRepository.findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List.of(30L))).thenReturn(List.of(item));
        when(purchaseOrderStatusHistoryRepository.findAllByPurchaseOrderIdInOrderByChangedAtAsc(List.of(30L)))
                .thenReturn(List.of(
                        PurchaseOrderStatusHistory.record(
                                purchaseOrder,
                                PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL,
                                PurchaseOrderStatus.REJECTED,
                                admin,
                                "out of policy"
                        )
                ));

        var results = platformAdminService.getPlatformApprovalOrders(1L, "rejected");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo(PurchaseOrderStatus.REJECTED.name());
        assertThat(results.getFirst().platformRejectionReason()).isEqualTo("out of policy");
        assertThat(results.getFirst().statusHistory()).hasSize(1);
        assertThat(results.getFirst().statusHistory().getFirst().toStatusLabel()).isEqualTo("반려");
        assertThat(results.getFirst().statusHistory().getFirst().transitionLabel()).isEqualTo("주문 승인 대기 -> 반려");
        assertThat(results.getFirst().statusHistory().getFirst().changedByDisplayName()).isEqualTo("Admin");
        assertThat(results.getFirst().statusHistory().getFirst().changeNoteDisplay()).isEqualTo("out of policy");
        assertThat(results.getFirst().hasAvailableAction()).isFalse();
        assertThat(results.getFirst().actionGuideMessage()).contains("반려");
    }

    @Test
    void forwardsDeliveryActionsToPurchaseOrderService() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        platformAdminService.markOrderReadyToShip(1L, 10L);
        platformAdminService.markOrderShipped(1L, 11L);
        platformAdminService.markOrderDelivered(1L, 12L);

        verify(purchaseOrderService).markReadyToShip(10L, 1L);
        verify(purchaseOrderService).markShipped(11L, 1L);
        verify(purchaseOrderService).markDelivered(12L, 1L);
    }

    @Test
    void loadsSettlementSummaryAndDeliveredOrders() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        User buyer = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.APPROVER).description("approver").build(),
                "buyer@example.com",
                "Buyer",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, buyer, null);
        PurchaseOrder settledOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        settledOrder.applyPricingSnapshot(new BigDecimal("30.00"), new BigDecimal("30.00"), "KRW");
        settledOrder.submitForPlatformApproval();
        settledOrder.approveByPlatform(admin, "approved");
        settledOrder.markPaid();
        settledOrder.markReadyToShip();
        settledOrder.markShipped();
        settledOrder.markDelivered();
        settledOrder.markSettled(admin);

        PurchaseOrder unsettledOrder = PurchaseOrder.createDraft(company, purchaseRequest, buyer);
        unsettledOrder.applyPricingSnapshot(new BigDecimal("20.00"), new BigDecimal("20.00"), "KRW");
        unsettledOrder.submitForPlatformApproval();
        unsettledOrder.approveByPlatform(admin, "approved");
        unsettledOrder.markPaid();
        unsettledOrder.markReadyToShip();
        unsettledOrder.markShipped();
        unsettledOrder.markDelivered();

        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(company, "id", 10L);
        ReflectionTestUtils.setField(purchaseRequest, "id", 20L);
        ReflectionTestUtils.setField(settledOrder, "id", 30L);
        ReflectionTestUtils.setField(unsettledOrder, "id", 31L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(purchaseOrderRepository.findAllForAdminListByStatusInOrderByCreatedAtDesc(List.of(
                PurchaseOrderStatus.PAYMENT_PENDING,
                PurchaseOrderStatus.PAID,
                PurchaseOrderStatus.READY_TO_SHIP,
                PurchaseOrderStatus.SHIPPED,
                PurchaseOrderStatus.DELIVERED
        ))).thenReturn(List.of(settledOrder, unsettledOrder));
        when(purchaseOrderRepository.findAllForSettlementListByStatusOrderByDeliveredAtDesc(PurchaseOrderStatus.DELIVERED))
                .thenReturn(List.of(settledOrder, unsettledOrder));

        var summary = platformAdminService.getSettlementSummary(1L);
        var orders = platformAdminService.getSettlementOrders(1L);

        assertThat(summary.totalOrderSales()).isEqualByComparingTo("50.00");
        assertThat(summary.unsettledSales()).isEqualByComparingTo("20.00");
        assertThat(summary.settledSales()).isEqualByComparingTo("30.00");
        assertThat(summary.unsettledOrderCount()).isEqualTo(1);
        assertThat(summary.settledOrderCount()).isEqualTo(1);
        assertThat(orders).hasSize(2);
        assertThat(orders.getFirst().settlementStatus()).isEqualTo(PurchaseOrderSettlementStatus.SETTLED.name());
    }

    @Test
    void forwardsSettlementActionsToPurchaseOrderService() {
        User admin = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        platformAdminService.markOrderSettled(1L, 10L);
        platformAdminService.markOrdersSettled(1L, List.of(11L, 12L));

        verify(purchaseOrderService).markSettled(10L, 1L);
        verify(purchaseOrderService).markSettled(11L, 1L);
        verify(purchaseOrderService).markSettled(12L, 1L);
    }
}
