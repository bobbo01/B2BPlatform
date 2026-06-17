package com.bobbo01.supplyhub.domain.purchase.repository;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseOrderStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderStatusHistoryRepository extends JpaRepository<PurchaseOrderStatusHistory, Long> {

    @EntityGraph(attributePaths = {"changedByUser"})
    List<PurchaseOrderStatusHistory> findAllByPurchaseOrderIdOrderByChangedAtAsc(Long purchaseOrderId);

    @EntityGraph(attributePaths = {"changedByUser"})
    List<PurchaseOrderStatusHistory> findAllByPurchaseOrderIdInOrderByChangedAtAsc(List<Long> purchaseOrderIds);
}
