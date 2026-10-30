package com.geli.warehouse.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleSummaryResponse(
        Long id,
        String reference,
        BigDecimal totalAmount,
        int lineCount,
        Instant createdAt
) {
}
