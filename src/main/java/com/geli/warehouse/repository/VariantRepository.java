package com.geli.warehouse.repository;

import com.geli.warehouse.domain.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {

    Optional<Variant> findBySku(String sku);

    List<Variant> findByItem_IdOrderBySkuAsc(Long itemId);

    boolean existsByItem_IdAndActiveTrue(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Variant v where v.id = :id")
    Optional<Variant> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select v.item.id, count(v), sum(case when v.active = true then 1 else 0 end)
            from Variant v
            where v.item.id in :ids
            group by v.item.id
            """)
    List<Object[]> summarizeByItemIds(@Param("ids") Collection<Long> ids);

    @Query("""
            select v from Variant v
            join fetch v.item i
            where v.active = true
              and i.active = true
              and v.stockQuantity <= :maxStock
            """)
    List<Variant> findSellableVariantsAtOrBelow(@Param("maxStock") int maxStock);
}
