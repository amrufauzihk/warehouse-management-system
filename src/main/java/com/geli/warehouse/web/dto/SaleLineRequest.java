package com.geli.warehouse.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleLineRequest(
        Long itemId,
        Long variantId,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10_000, message = "Quantity cannot exceed 10000")
        Integer quantity
) {
    @AssertTrue(message = "Each line must set exactly one of itemId or variantId")
    public boolean isSingleTarget() {
        return (itemId == null) ^ (variantId == null);
    }
}
