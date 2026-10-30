package com.geli.warehouse;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemApiTest extends ApiTestSupport {

    @Test
    void createsReadsUpdatesAndSoftDeletesAnItem() throws Exception {
        String sku = sku("MUG");
        long id = createItem(sku, "12.50", 5);

        mockMvc.perform(get("/api/v1/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku.toUpperCase(Locale.ROOT)))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.sellableAsItem").value(true));

        mockMvc.perform(get("/api/v1/items/sku/" + sku.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/v1/items/" + id)
                        .contentType("application/json")
                        .content("""
                                {"name":"Large Mug","description":null,"price":14.00,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Large Mug"))
                .andExpect(jsonPath("$.sku").value(sku.toUpperCase(Locale.ROOT)))
                .andExpect(jsonPath("$.stockQuantity").value(5));

        mockMvc.perform(delete("/api/v1/items/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":1}]}
                                """.formatted(id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("not available for sale")));
    }

    @Test
    void rejectsInvalidItemsDuplicateSkusAndUnknownFields() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .contentType("application/json")
                        .content("""
                                {"sku":"BAD SKU","name":"Mug","price":-1,"initialStock":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sku").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());

        String sku = sku("dup");
        createItem(sku, "3.00", 1);
        mockMvc.perform(post("/api/v1/items")
                        .contentType("application/json")
                        .content("""
                                {"sku":"%s","name":"Again","price":3.00,"initialStock":0}
                                """.formatted(sku.toLowerCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(sku.toUpperCase(Locale.ROOT))));

        mockMvc.perform(post("/api/v1/items")
                        .contentType("application/json")
                        .content("""
                                {"sku":"%s","name":"Nope","price":3.00,"initialStock":0,"unknown":true}
                                """.formatted(sku("new"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/items/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsItemsByNameOrSku() throws Exception {
        String sku = sku("note");
        createItem(sku, "4.50", 2);

        mockMvc.perform(get("/api/v1/items").param("query", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value(sku.toUpperCase(Locale.ROOT)));
    }
}
