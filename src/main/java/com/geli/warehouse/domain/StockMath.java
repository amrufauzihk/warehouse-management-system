package com.geli.warehouse.domain;

import com.geli.warehouse.exception.BusinessRuleException;
import com.geli.warehouse.exception.InsufficientStockException;

final class StockMath {

    static final int MAX_UNITS = 1_000_000;

    private StockMath() {
    }

    static int apply(int current, int delta, String sku) {
        int next;
        try {
            next = Math.addExact(current, delta);
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("Stock change for " + sku + " is out of range");
        }
        if (next < 0) {
            int requested = delta < 0 ? -delta : delta;
            throw new InsufficientStockException(sku, current, requested);
        }
        if (next > MAX_UNITS) {
            throw new BusinessRuleException("Stock for " + sku + " cannot exceed " + MAX_UNITS);
        }
        return next;
    }
}
