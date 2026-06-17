package com.bobbo01.supplyhub.domain.purchase.repository;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrder;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @EntityGraph(attributePaths = {"purchaseRequest"})
    List<PurchaseOrder> findAllWithPurchaseRequestByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @EntityGraph(attributePaths = {"purchaseRequest", "buyer", "platformReviewedBy"})
    @Query("""
            select purchaseOrder
            from PurchaseOrder purchaseOrder
            where purchaseOrder.id = :purchaseOrderId
            """)
    java.util.Optional<PurchaseOrder> findDetailById(@Param("purchaseOrderId") Long purchaseOrderId);

    List<PurchaseOrder> findAllByStatusOrderByCreatedAtAsc(PurchaseOrderStatus status);

    List<PurchaseOrder> findAllByStatusInOrderByCreatedAtDesc(List<PurchaseOrderStatus> statuses);

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"company", "buyer", "purchaseRequest", "platformReviewedBy"})
    @Query("""
            select purchaseOrder
            from PurchaseOrder purchaseOrder
            where purchaseOrder.status in :statuses
            order by purchaseOrder.createdAt desc
            """)
    List<PurchaseOrder> findAllForAdminListByStatusInOrderByCreatedAtDesc(
            @Param("statuses") List<PurchaseOrderStatus> statuses
    );

    @EntityGraph(attributePaths = {"company", "buyer", "purchaseRequest", "platformReviewedBy"})
    @Query("""
            select purchaseOrder
            from PurchaseOrder purchaseOrder
            order by purchaseOrder.createdAt desc
            """)
    List<PurchaseOrder> findAllForAdminListOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"company", "buyer", "purchaseRequest", "settledBy"})
    @Query("""
            select purchaseOrder
            from PurchaseOrder purchaseOrder
            where purchaseOrder.status = :status
            order by purchaseOrder.deliveredAt desc, purchaseOrder.createdAt desc
            """)
    List<PurchaseOrder> findAllForSettlementListByStatusOrderByDeliveredAtDesc(
            @Param("status") PurchaseOrderStatus status
    );

    boolean existsByPurchaseRequestId(Long purchaseRequestId);
}
