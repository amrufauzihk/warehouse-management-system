package com.geli.warehouse.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String reference,
        BigDecimal totalAmount,
        List<SaleLineResponse> lines,
        Instant createdAt
) {
}
