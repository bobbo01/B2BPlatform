package com.bobbo01.supplyhub.domain.purchase.service;

import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestStatus;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository purchaseRequestItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PurchaseRequestService purchaseRequestService;

    @Test
    void createsDraftFromOpenCartAndCopiesItems() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
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
        Cart cart = Cart.open(company, requester);
        CartItem cartItem = CartItem.fromProduct(cart, product, 2);

        ReflectionTestUtils.setField(requester, "id", 1L);
        ReflectionTestUtils.setField(cart, "id", 10L);

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(purchaseRequestRepository.save(any(PurchaseRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(cartItem));

        PurchaseRequest purchaseRequest = purchaseRequestService.createDraftFromCart(10L, 1L);

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.DRAFT);

        ArgumentCaptor<List<PurchaseRequestItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseRequestItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(1);
        assertThat(itemCaptor.getValue().getFirst().getQuantity()).isEqualTo(2);
        assertThat(itemCaptor.getValue().getFirst().getUnitPrice()).isEqualByComparingTo("12.50");
    }

    @Test
    void submitsDraftPurchaseRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));

        purchaseRequestService.submit(10L);

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.SUBMITTED);
    }

    @Test
    void cancelsDraftPurchaseRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));

        purchaseRequestService.cancel(10L);

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.CANCELLED);
    }

    @Test
    void rejectsCancellingSubmittedPurchaseRequest() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        User requester = User.createOAuthUser(
                company,
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "alice@example.com",
                "Alice",
                null
        );
        PurchaseRequest purchaseRequest = PurchaseRequest.createDraft(company, requester, null);
        purchaseRequest.submit();

        when(purchaseRequestRepository.findById(10L)).thenReturn(Optional.of(purchaseRequest));

        assertThatThrownBy(() -> purchaseRequestService.cancel(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Purchase request is not in the required state");
    }
}
