package com.geli.warehouse.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemSummaryResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        int variantCount,
        boolean sellableAsItem,
        Instant createdAt,
        Instant updatedAt
) {
}
