package com.geli.warehouse.service;

import com.geli.warehouse.domain.SkuCode;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.DuplicateSkuException;
import com.geli.warehouse.repository.SkuCodeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SkuService {

    private static final String SKU_PATTERN = "[A-Z0-9][A-Z0-9._-]{0,63}";

    private final SkuCodeRepository skuCodeRepository;

    public SkuService(SkuCodeRepository skuCodeRepository) {
        this.skuCodeRepository = skuCodeRepository;
    }

    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("SKU is required");
        }
        String sku = raw.trim().toUpperCase(Locale.ROOT);
        if (!sku.matches(SKU_PATTERN)) {
            throw new BadRequestException("SKU may contain letters, digits, dots, underscores, and hyphens");
        }
        return sku;
    }

    public String normalizeReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String reference = raw.trim().toUpperCase(Locale.ROOT);
        if (!reference.matches(SKU_PATTERN)) {
            throw new BadRequestException(
                    "Sale reference may contain letters, digits, dots, underscores, and hyphens");
        }
        return reference;
    }

    @Transactional
    public void reserve(String normalizedSku) {
        if (skuCodeRepository.existsById(normalizedSku)) {
            throw new DuplicateSkuException(normalizedSku);
        }
        try {
            skuCodeRepository.saveAndFlush(new SkuCode(normalizedSku));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSkuException(normalizedSku);
        }
    }
}
