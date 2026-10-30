package com.geli.warehouse.web;

import com.geli.warehouse.service.VariantService;
import com.geli.warehouse.web.dto.CreateVariantRequest;
import com.geli.warehouse.web.dto.UpdateVariantRequest;
import com.geli.warehouse.web.dto.VariantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/api/v1/items/{itemId}/variants")
    @Operation(summary = "List variants of an item")
    public List<VariantResponse> list(@PathVariable Long itemId) {
        return variantService.listForItem(itemId);
    }

    @PostMapping("/api/v1/items/{itemId}/variants")
    @Operation(summary = "Add a variant. The item must not be holding its own stock.")
    public ResponseEntity<VariantResponse> create(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateVariantRequest request
    ) {
        VariantResponse body = variantService.create(itemId, request);
        return ResponseEntity.created(URI.create("/api/v1/variants/" + body.id())).body(body);
    }

    @GetMapping("/api/v1/variants/{id}")
    @Operation(summary = "Get a variant")
    public VariantResponse get(@PathVariable Long id) {
        return variantService.get(id);
    }

    @GetMapping("/api/v1/variants/sku/{sku}")
    @Operation(summary = "Get a variant by SKU")
    public VariantResponse getBySku(@PathVariable String sku) {
        return variantService.getBySku(sku);
    }

    @PutMapping("/api/v1/variants/{id}")
    @Operation(summary = "Replace the editable fields of a variant")
    public VariantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateVariantRequest request) {
        return variantService.update(id, request);
    }

    @DeleteMapping("/api/v1/variants/{id}")
    @Operation(summary = "Deactivate a variant so it can no longer be sold")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        variantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
