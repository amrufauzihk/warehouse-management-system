package com.geli.warehouse.exception;

public class DuplicateReferenceException extends RuntimeException {

    public DuplicateReferenceException(String reference) {
        super("Sale reference " + reference + " already exists");
    }
}
