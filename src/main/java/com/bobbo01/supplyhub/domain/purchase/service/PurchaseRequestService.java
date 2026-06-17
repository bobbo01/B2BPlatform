package com.bobbo01.supplyhub.domain.purchase.service;

import com.bobbo01.supplyhub.domain.cart.entity.Cart;
import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.cart.entity.CartStatus;
import com.bobbo01.supplyhub.domain.cart.repository.CartItemRepository;
import com.bobbo01.supplyhub.domain.cart.repository.CartRepository;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestItemRepository;
import com.bobbo01.supplyhub.domain.purchase.repository.PurchaseRequestRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository
    ) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PurchaseRequest createDraftFromCart(Long cartId, Long requesterUserId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalStateException("Cart was not found."));
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new IllegalStateException("Only open carts can create purchase request drafts.");
        }
        if (!cart.getOwner().getId().equals(requester.getId())) {
            throw new IllegalStateException("Only the cart owner can create a purchase request draft.");
        }

        PurchaseRequest purchaseRequest = purchaseRequestRepository.save(
                PurchaseRequest.createDraft(cart.getCompany(), requester, cart)
        );
        List<CartItem> cartItems = cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(cartId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("A purchase request draft requires at least one cart item.");
        }
        purchaseRequestItemRepository.saveAll(
                cartItems.stream()
                        .map(cartItem -> PurchaseRequestItem.fromCartItem(purchaseRequest, cartItem))
                        .toList()
        );
        return purchaseRequest;
    }

    @Transactional
    public void submit(Long purchaseRequestId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        purchaseRequest.submit();
    }

    @Transactional
    public void cancel(Long purchaseRequestId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new IllegalStateException("Purchase request was not found."));
        purchaseRequest.cancel();
    }
}
