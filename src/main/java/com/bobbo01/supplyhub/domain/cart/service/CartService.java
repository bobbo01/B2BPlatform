package com.bobbo01.supplyhub.domain.cart.service;

import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.cart.entity.CartStatus;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.product.repository.ProductRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart openCart(Long ownerUserId) {
        return cartRepository.findByOwnerIdAndStatus(ownerUserId, CartStatus.OPEN)
                .orElseGet(() -> {
                    User owner = userRepository.findById(ownerUserId)
                            .orElseThrow(() -> new IllegalStateException("User was not found."));
                    if (owner.isPlatformAdmin() || owner.getCompany() == null) {
                        throw new IllegalStateException("Only company users can own carts.");
                    }
                    return cartRepository.save(Cart.open(owner.getCompany(), owner));
                });
    }

    @Transactional
    public CartItem addItem(Long cartId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalStateException("Cart was not found."));
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new IllegalStateException("Items can only be added to an open cart.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product was not found."));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new IllegalStateException("Only active products can be added to a cart.");
        }

        validateMinimumOrderQuantity(product, quantity);
        return cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .map(existingItem -> {
                    existingItem.updateQuantity(validateRequestedQuantity(product, existingItem.getQuantity() + quantity));
                    return existingItem;
                })
                .orElseGet(() -> cartItemRepository.save(CartItem.fromProduct(cart, product, quantity)));
    }

    @Transactional
    public CartItem updateItemQuantity(Long ownerUserId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalStateException("Cart item was not found."));
        Cart cart = cartItem.getCart();
        assertOpenOwnedCart(ownerUserId, cart);
        cartItem.updateQuantity(validateRequestedQuantity(cartItem.getProduct(), quantity));
        return cartItem;
    }

    @Transactional
    public void removeItem(Long ownerUserId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalStateException("Cart item was not found."));
        assertOpenOwnedCart(ownerUserId, cartItem.getCart());
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearOpenCart(Long ownerUserId) {
        cartRepository.findByOwnerIdAndStatus(ownerUserId, CartStatus.OPEN)
                .ifPresent(cart -> {
                    assertOpenOwnedCart(ownerUserId, cart);
                    cartItemRepository.deleteAll(cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(cart.getId()));
                });
    }

    @Transactional
    public void checkout(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalStateException("Cart was not found."));
        cart.checkout();
    }

    private void assertOpenOwnedCart(Long ownerUserId, Cart cart) {
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new IllegalStateException("Cart items can only be changed in an open cart.");
        }
        if (!cart.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("Cart items can only be changed by the cart owner.");
        }
    }

    private void validateMinimumOrderQuantity(Product product, Integer quantity) {
        validateRequestedQuantity(product, quantity);
    }

    private Integer validateRequestedQuantity(Product product, Integer quantity) {
        if (quantity == null || quantity < product.getMinOrderQty()) {
            throw new IllegalArgumentException("Quantity must be at least the product minimum order quantity.");
        }
        return quantity;
    }
}
