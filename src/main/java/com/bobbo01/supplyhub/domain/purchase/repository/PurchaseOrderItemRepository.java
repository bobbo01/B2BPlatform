package com.bobbo01.supplyhub.domain.purchase.repository;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<PurchaseOrderItem> findAllByPurchaseOrderIdOrderByCreatedAtAsc(Long purchaseOrderId);

    @EntityGraph(attributePaths = {"product"})
    List<PurchaseOrderItem> findAllByPurchaseOrderIdInOrderByCreatedAtAsc(List<Long> purchaseOrderIds);
}
