package com.geli.warehouse.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateItemRequest(
        @Schema(example = "MUG-001")
        @NotBlank(message = "SKU is required")
        @Size(max = 64, message = "SKU cannot exceed 64 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$",
                message = "SKU may contain letters, digits, dots, underscores, and hyphens"
        )
        String sku,

        @Schema(example = "Ceramic Mug")
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name cannot exceed 200 characters")
        String name,

        @Schema(example = "12oz stoneware mug")
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @Schema(example = "12.50")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "1000000.00", message = "Price cannot exceed 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,

        @Schema(example = "40")
        @NotNull(message = "Initial stock is required")
        @Min(value = 0, message = "Initial stock cannot be negative")
        @Max(value = 1_000_000, message = "Initial stock cannot exceed 1000000")
        Integer initialStock
) {
}
