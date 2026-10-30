package com.geli.warehouse.web;

import com.geli.warehouse.service.SaleService;
import com.geli.warehouse.web.dto.CreateSaleRequest;
import com.geli.warehouse.web.dto.PageResponse;
import com.geli.warehouse.web.dto.SaleResponse;
import com.geli.warehouse.web.dto.SaleSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales")
public class SaleController {

    private static final Set<String> SORTS = Set.of("createdAt", "reference", "totalAmount");

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @Operation(summary = "Sell items or variants. The whole sale is rejected if any line is short on stock.")
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody CreateSaleRequest request) {
        SaleResponse body = saleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sales/" + body.id())).body(body);
    }

    @GetMapping
    @Operation(summary = "List sales")
    public PageResponse<SaleSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return saleService.list(PageRequests.of(page, size, sort, SORTS));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sale, including the price and stock balance captured at sale time")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.get(id);
    }

    @GetMapping("/ref/{reference}")
    @Operation(summary = "Get a sale by its reference")
    public SaleResponse getByReference(@PathVariable String reference) {
        return saleService.getByReference(reference);
    }
}
