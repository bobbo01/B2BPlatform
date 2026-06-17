package com.bobbo01.supplyhub.domain.purchase.entity;

import com.bobbo01.supplyhub.domain.cart.entity.CartItem;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "purchase_request_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_request_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Builder
    public PurchaseRequestItem(
            PurchaseRequest purchaseRequest,
            Product product,
            Integer quantity,
            BigDecimal unitPrice,
            String currencyCode
    ) {
        this.purchaseRequest = purchaseRequest;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currencyCode = currencyCode;
    }

    public static PurchaseRequestItem fromCartItem(PurchaseRequest purchaseRequest, CartItem cartItem) {
        return PurchaseRequestItem.builder()
                .purchaseRequest(purchaseRequest)
                .product(cartItem.getProduct())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .currencyCode(cartItem.getCurrencyCode())
                .build();
    }
}
