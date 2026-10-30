package com.geli.warehouse.domain;

import com.geli.warehouse.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Price is required");
        }
        if (amount.signum() < 0) {
            throw new BadRequestException("Price cannot be negative");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
