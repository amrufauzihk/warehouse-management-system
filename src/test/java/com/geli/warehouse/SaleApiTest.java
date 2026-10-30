package com.geli.warehouse;

import com.geli.warehouse.exception.InsufficientStockException;
import com.geli.warehouse.service.SaleService;
import com.geli.warehouse.web.dto.CreateSaleRequest;
import com.geli.warehouse.web.dto.SaleLineRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaleApiTest extends ApiTestSupport {

    @Autowired
    private SaleService saleService;

    @Test
    void stopsSellingWhenStockRunsOutAndLeavesTheBalanceUntouched() throws Exception {
        long itemId = createItem(sku("mug"), "12.50", 2);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":2}]}
                                """.formatted(itemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].balanceAfter").value(0))
                .andExpect(jsonPath("$.totalAmount").value(25.00));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":1}]}
                                """.formatted(itemId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.available").value(0))
                .andExpect(jsonPath("$.requested").value(1));

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void rollsBackTheWholeSaleWhenALaterLineIsShort() throws Exception {
        long first = createItem(sku("aaa"), "5.00", 4);
        long second = createItem(sku("bbb"), "5.00", 0);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":1},{"itemId":%d,"quantity":1}]}
                                """.formatted(first, second)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/items/" + first))
                .andExpect(jsonPath("$.stockQuantity").value(4));
        mockMvc.perform(get("/api/v1/items/" + second))
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void duplicateReferenceDoesNotDeductStockTwice() throws Exception {
        long itemId = createItem(sku("ref"), "3.00", 5);
        String body = """
                {"reference":"pos-77","lines":[{"itemId":%d,"quantity":1}]}
                """.formatted(itemId);

        mockMvc.perform(post("/api/v1/sales").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("POS-77"));

        mockMvc.perform(post("/api/v1/sales").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("POS-77")));

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(jsonPath("$.stockQuantity").value(4));

        mockMvc.perform(get("/api/v1/sales/ref/pos-77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("POS-77"));
    }

    @Test
    void duplicateLinesInOneSaleAreCombinedBeforeTheStockCheck() throws Exception {
        long itemId = createItem(sku("merge"), "2.00", 5);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":3},{"itemId":%d,"quantity":3}]}
                                """.formatted(itemId, itemId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requested").value(6));

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void onlyOneConcurrentSaleCanTakeTheLastUnit() throws Exception {
        long itemId = createItem(sku("race"), "9.00", 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> sellLastUnit(itemId, ready, start));
            Future<Boolean> second = pool.submit(() -> sellLastUnit(itemId, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            boolean soldFirst = first.get(20, TimeUnit.SECONDS);
            boolean soldSecond = second.get(20, TimeUnit.SECONDS);
            assertThat(List.of(soldFirst, soldSecond)).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    private boolean sellLastUnit(long itemId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            return false;
        }
        try {
            saleService.create(new CreateSaleRequest(null, List.of(new SaleLineRequest(itemId, null, 1))));
            return true;
        } catch (InsufficientStockException ex) {
            return false;
        }
    }
}
