package com.geli.warehouse.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateVariantRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name cannot exceed 200 characters")
        String name,

        @NotNull(message = "Attributes are required")
        Map<String, String> attributes,

        @Schema(example = "27.00")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "1000000.00", message = "Price cannot exceed 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
