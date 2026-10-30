package com.geli.warehouse.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemDetailResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        boolean sellableAsItem,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
}
