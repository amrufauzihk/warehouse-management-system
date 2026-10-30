package com.geli.warehouse.exception;

public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final int available;
    private final int requested;

    public InsufficientStockException(String sku, int available, int requested) {
        super("Not enough stock for " + sku + ": requested " + requested + ", available " + available);
        this.sku = sku;
        this.available = available;
        this.requested = requested;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
