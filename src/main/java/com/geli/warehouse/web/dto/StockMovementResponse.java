package com.geli.warehouse.web.dto;

import com.geli.warehouse.domain.MovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        MovementType type,
        int quantityDelta,
        int balanceAfter,
        String note,
        Long saleId,
        Instant createdAt
) {
}
