package com.geli.warehouse.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull(message = "quantityDelta is required")
        Integer quantityDelta,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {
    @AssertTrue(message = "quantityDelta cannot be zero")
    public boolean isNonZero() {
        return quantityDelta == null || quantityDelta != 0;
    }
}
