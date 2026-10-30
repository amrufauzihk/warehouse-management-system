package com.geli.warehouse.web;

import com.geli.warehouse.service.ItemService;
import com.geli.warehouse.web.dto.CreateItemRequest;
import com.geli.warehouse.web.dto.ItemDetailResponse;
import com.geli.warehouse.web.dto.ItemSummaryResponse;
import com.geli.warehouse.web.dto.PageResponse;
import com.geli.warehouse.web.dto.UpdateItemRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "Items")
public class ItemController {

    private static final Set<String> SORTS = Set.of("sku", "name", "price", "createdAt");

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    @Operation(summary = "List items")
    public PageResponse<ItemSummaryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return itemService.list(query, active, PageRequests.of(page, size, sort, SORTS));
    }

    @PostMapping
    @Operation(summary = "Create an item")
    public ResponseEntity<ItemDetailResponse> create(@Valid @RequestBody CreateItemRequest request) {
        ItemDetailResponse body = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/items/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an item and its variants")
    public ItemDetailResponse get(@PathVariable Long id) {
        return itemService.get(id);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get an item by SKU")
    public ItemDetailResponse getBySku(@PathVariable String sku) {
        return itemService.getBySku(sku);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the editable fields of an item")
    public ItemDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an item so it can no longer be sold")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        itemService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
