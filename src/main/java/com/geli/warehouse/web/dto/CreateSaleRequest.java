package com.geli.warehouse.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSaleRequest(
        @Size(max = 64, message = "Reference cannot exceed 64 characters")
        String reference,

        @NotEmpty(message = "At least one sale line is required")
        @Size(max = 50, message = "A sale can contain at most 50 lines")
        List<@Valid SaleLineRequest> lines
) {
}
