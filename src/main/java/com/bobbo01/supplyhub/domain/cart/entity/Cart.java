package com.bobbo01.supplyhub.domain.cart.entity;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CartStatus status;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Builder
    public Cart(Company company, User owner, CartStatus status, LocalDateTime checkedOutAt) {
        this.company = company;
        this.owner = owner;
        this.status = status;
        this.checkedOutAt = checkedOutAt;
    }

    public static Cart open(Company company, User owner) {
        return Cart.builder()
                .company(company)
                .owner(owner)
                .status(CartStatus.OPEN)
                .build();
    }

    public void checkout() {
        assertStatus(CartStatus.OPEN);
        this.status = CartStatus.CHECKED_OUT;
        this.checkedOutAt = LocalDateTime.now();
    }

    public void abandon() {
        assertStatus(CartStatus.OPEN);
        this.status = CartStatus.ABANDONED;
    }

    private void assertStatus(CartStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("Cart is not in the required state: " + expectedStatus);
        }
    }
}
