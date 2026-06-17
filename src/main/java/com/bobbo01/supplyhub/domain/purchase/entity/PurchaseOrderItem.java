package com.bobbo01.supplyhub.domain.purchase.entity;

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
@Table(name = "purchase_order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

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
    public PurchaseOrderItem(
            PurchaseOrder purchaseOrder,
            Product product,
            Integer quantity,
            BigDecimal unitPrice,
            String currencyCode
    ) {
        this.purchaseOrder = purchaseOrder;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currencyCode = currencyCode;
    }

    public static PurchaseOrderItem fromPurchaseRequestItem(PurchaseOrder purchaseOrder, PurchaseRequestItem item) {
        return PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .product(item.getProduct())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .currencyCode(item.getCurrencyCode())
                .build();
    }
}
