package com.bobbo01.supplyhub.domain.product.entity;

import com.bobbo01.supplyhub.domain.category.entity.Category;
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
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(length = 100)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "min_order_qty", nullable = false)
    private Integer minOrderQty;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    public Product(Category category, String sku, String productName, String brand,
                   String description, String imageUrl, BigDecimal unitPrice, String currencyCode, Integer minOrderQty,
                   Boolean isActive) {
        this.category = category;
        this.sku = sku;
        this.productName = productName;
        this.brand = brand;
        this.description = description;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.currencyCode = currencyCode;
        this.minOrderQty = minOrderQty;
        this.isActive = isActive;
    }
}

