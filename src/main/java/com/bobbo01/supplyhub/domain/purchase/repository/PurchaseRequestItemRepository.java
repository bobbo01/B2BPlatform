package com.bobbo01.supplyhub.domain.purchase.repository;

import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {

    @EntityGraph(attributePaths = {"purchaseRequest", "product"})
    List<PurchaseRequestItem> findAllByPurchaseRequestIdOrderByCreatedAtAsc(Long purchaseRequestId);

    @EntityGraph(attributePaths = {"purchaseRequest", "product"})
    List<PurchaseRequestItem> findAllByPurchaseRequestIdInOrderByCreatedAtAsc(List<Long> purchaseRequestIds);
}
