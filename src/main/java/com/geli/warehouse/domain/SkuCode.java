package com.geli.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sku_codes")
public class SkuCode {

    @Id
    @Column(length = 64)
    private String code;

    protected SkuCode() {
    }

    public SkuCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
