package com.geli.warehouse.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record VariantResponse(
        Long id,
        Long itemId,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
