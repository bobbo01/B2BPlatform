package com.bobbo01.supplyhub.domain.product.repository;

import com.bobbo01.supplyhub.domain.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    List<Product> findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc();

    boolean existsByIsActiveTrueAndCategory_CategoryCode(String categoryCode);

    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    @Query("""
            select p
            from Product p
            join p.category c
            where p.isActive = true
              and (:categoryCode = 'all' or c.categoryCode = :categoryCode)
              and (
                    :normalizedQuery = ''
                    or lower(coalesce(p.productName, '')) like concat('%', :normalizedQuery, '%')
                    or lower(coalesce(p.brand, '')) like concat('%', :normalizedQuery, '%')
                    or lower(coalesce(p.sku, '')) like concat('%', :normalizedQuery, '%')
              )
            order by c.sortOrder asc, p.productName asc
            """)
    Page<Product> findCatalogPage(
            @Param("categoryCode") String categoryCode,
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );

    @Query("""
            select count(p)
            from Product p
            join p.category c
            where p.isActive = true
              and (:categoryCode = 'all' or c.categoryCode = :categoryCode)
              and (
                    :normalizedQuery = ''
                    or lower(coalesce(p.productName, '')) like concat('%', :normalizedQuery, '%')
                    or lower(coalesce(p.brand, '')) like concat('%', :normalizedQuery, '%')
                    or lower(coalesce(p.sku, '')) like concat('%', :normalizedQuery, '%')
              )
            """)
    long countCatalogProducts(
            @Param("categoryCode") String categoryCode,
            @Param("normalizedQuery") String normalizedQuery
    );

    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    Optional<Product> findByIdAndIsActiveTrue(Long id);
}
