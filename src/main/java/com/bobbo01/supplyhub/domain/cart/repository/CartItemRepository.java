package com.bobbo01.supplyhub.domain.cart.repository;

import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByCartIdOrderByCreatedAtAsc(Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
