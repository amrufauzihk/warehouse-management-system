package com.geli.warehouse.web.dto;

import java.math.BigDecimal;

public record SaleLineResponse(
        Long id,
        Long itemId,
        Long variantId,
        String sku,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int balanceAfter
) {
}
