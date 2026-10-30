package com.geli.warehouse.repository;

import com.geli.warehouse.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    Optional<Item> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select i from Item i
            where i.active = true
              and i.stockQuantity <= :maxStock
              and not exists (
                  select v.id from Variant v
                  where v.item = i and v.active = true
              )
            """)
    List<Item> findSellableItemsAtOrBelow(@Param("maxStock") int maxStock);
}
