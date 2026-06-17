package com.bobbo01.supplyhub.domain.purchase.entity;

import com.bobbo01.supplyhub.domain.user.entity.User;
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
@Table(name = "purchase_order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private PurchaseOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private PurchaseOrderStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @Column(name = "change_note", length = 1000)
    private String changeNote;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Builder
    public PurchaseOrderStatusHistory(
            PurchaseOrder purchaseOrder,
            PurchaseOrderStatus fromStatus,
            PurchaseOrderStatus toStatus,
            User changedByUser,
            String changeNote,
            LocalDateTime changedAt
    ) {
        this.purchaseOrder = purchaseOrder;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUser = changedByUser;
        this.changeNote = changeNote;
        this.changedAt = changedAt;
    }

    public static PurchaseOrderStatusHistory record(
            PurchaseOrder purchaseOrder,
            PurchaseOrderStatus fromStatus,
            PurchaseOrderStatus toStatus,
            User changedByUser,
            String changeNote
    ) {
        return PurchaseOrderStatusHistory.builder()
                .purchaseOrder(purchaseOrder)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedByUser(changedByUser)
                .changeNote(changeNote)
                .changedAt(LocalDateTime.now())
                .build();
    }
}
