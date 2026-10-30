package com.geli.warehouse.web.dto;

import java.math.BigDecimal;

public record InventoryPositionResponse(
        String type,
        Long itemId,
        Long variantId,
        String sku,
        String name,
        String itemName,
        BigDecimal price,
        int stockQuantity
) {
}
