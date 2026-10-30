package com.geli.warehouse.repository;

import com.geli.warehouse.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop100ByItem_IdAndVariantIsNullOrderByIdDesc(Long itemId);

    List<StockMovement> findTop100ByVariant_IdOrderByIdDesc(Long variantId);
}
