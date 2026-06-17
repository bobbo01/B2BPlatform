package com.bobbo01.supplyhub.domain.cart.service;

import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.cart.entity.CartStatus;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.product.repository.ProductRepository;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemRejectsQuantityBelowMinimumOrderQuantity() {
        Cart cart = openCart();
        Product product = product(5);

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(10L, 20L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be at least the product minimum order quantity.");
    }

    @Test
    void updateItemQuantityRejectsOtherUsersCartItem() {
        User owner = companyUser("owner@example.com");
        User otherUser = companyUser("other@example.com");
        Cart cart = Cart.open(company(), owner);
        CartItem cartItem = CartItem.fromProduct(cart, product(2), 2);

        ReflectionTestUtils.setField(owner, "id", 1L);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        ReflectionTestUtils.setField(cartItem, "id", 30L);

        when(cartItemRepository.findById(30L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.updateItemQuantity(2L, 30L, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cart items can only be changed by the cart owner.");
    }

    @Test
    void clearOpenCartDeletesAllItemsInOwnersOpenCart() {
        User owner = companyUser("owner@example.com");
        Cart cart = Cart.open(company(), owner);
        CartItem item = CartItem.fromProduct(cart, product(2), 2);

        ReflectionTestUtils.setField(owner, "id", 1L);
        ReflectionTestUtils.setField(cart, "id", 10L);

        when(cartRepository.findByOwnerIdAndStatus(1L, CartStatus.OPEN)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(item));

        cartService.clearOpenCart(1L);

        verify(cartItemRepository).deleteAll(List.of(item));
    }

    @Test
    void clearOpenCartDoesNothingWhenNoOpenCartExists() {
        when(cartRepository.findByOwnerIdAndStatus(1L, CartStatus.OPEN)).thenReturn(Optional.empty());

        cartService.clearOpenCart(1L);

        verify(cartItemRepository, never()).findAllByCartIdOrderByCreatedAtAsc(10L);
    }

    private Cart openCart() {
        User owner = companyUser("owner@example.com");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Cart cart = Cart.open(company(), owner);
        ReflectionTestUtils.setField(cart, "id", 10L);
        return cart;
    }

    private Product product(int minOrderQty) {
        Product product = Product.builder()
                .sku("SKU-1")
                .productName("Paper")
                .unitPrice(BigDecimal.TEN)
                .currencyCode("KRW")
                .minOrderQty(minOrderQty)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(product, "id", 20L);
        return product;
    }

    private User companyUser(String email) {
        return User.createOAuthUser(
                company(),
                Role.builder().roleName(RoleNames.CART_USER).description(RoleNames.CART_USER).build(),
                email,
                "User",
                null
        );
    }

    private Company company() {
        Company company = Company.builder().companyName("Example").status("ACTIVE").build();
        ReflectionTestUtils.setField(company, "id", 100L);
        return company;
    }
}
