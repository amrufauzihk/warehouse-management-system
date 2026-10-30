# Warehouse Management API

REST API for a single shop warehouse. It tracks items, variants, prices, and stock, and it refuses a sale when any line asks for more units than are on hand.

## How to run

Requirements:

- Java 17 or newer
- Maven 3.9 or newer

```bash
mvn test
mvn spring-boot:run
```

The API listens on <http://localhost:8080>.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI document: <http://localhost:8080/v3/api-docs>
- H2 console (local only): <http://localhost:8080/h2-console>
  - JDBC URL: `jdbc:h2:mem:warehouse`
  - User: `sa`
  - Password: empty

The database is in memory. Stop the process and the data is gone. To keep it across restarts, change the JDBC URL in `src/main/resources/application.yml` to `jdbc:h2:file:./data/warehouse`.

Sample products are loaded on an empty database:

| SKU | What it is | Stock |
| --- | --- | --- |
| `MUG-001` | Ceramic mug, sold as the item itself | 40 |
| `NOTE-001` | Notebook, sold as the item itself | 2 |
| `TEE-001` | T-shirt. Not sellable directly, because it has variants | 0 on the item |
| `TEE-BLK-S` | Black / S | 10 |
| `TEE-BLK-M` | Black / M | 4 |
| `TEE-WHT-M` | White / M, kept in the catalog but out of stock | 0 |

## Assumptions

- One warehouse and one currency. Amounts are decimal money such as `12.50`, with no tax or discount engine.
- A sale is final. There is no cart and no reservation. Stock is deducted when the sale is accepted.
- Returns are not a separate flow. Put units back with a stock adjustment.
- An item is either simple or variable. Simple items hold their own price and stock. Once a variant exists, each variant has its own price and stock, and the parent item is no longer sold directly.
- SKUs are case-insensitive and unique across items and variants. `mug-001` and `MUG-001` are the same SKU.
- Deleting an item or variant only deactivates it. Past sales keep the SKU, name, and price from the moment of sale.
- Authentication is out of scope. The API is stateless JSON with CSRF disabled on purpose, because there are no browser sessions or cookies. Do not expose this service on a public network without adding auth.

## Design decisions

**Sellable stock lives in one place.** A simple item such as the mug is sold by `itemId`. A T-shirt is sold by `variantId`. Adding the first variant is rejected while the item still holds units, so stock cannot be counted twice. After variants exist, item-level adjustments are rejected too.

**The sale is all or nothing.** Lines are merged when they point at the same item or variant, then applied in one database transaction. If any line is short, inactive, or aimed at an item that has variants, the whole sale is rolled back and no balance changes.

**Stock cannot go negative, including under two cashiers.** Each sale locks the parent item rows first, ordered by id, then the variant rows, ordered by id. The same order is used for every sale, which avoids deadlocks. The balance is checked again after the lock. A second sale that wants the last unit gets `409 Insufficient Stock`.

**The balance and the ledger are written together.** `stockQuantity` is the current balance. Every receipt, adjustment, and sale also inserts a `stock_movements` row with the signed change and the balance after that change. The sale line stores the unit price and the balance after the sale, so a later price edit does not rewrite history.

**The HTTP API uses DTOs.** Request and response records are separate from the JPA entities. Unknown JSON fields are rejected. Prices use `BigDecimal`.

**H2 is the default database** so a reviewer can run the app with no extra services. Schema comes from JPA mappings. The same locking approach works on PostgreSQL; only the datasource would change.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/items` | List items. Optional `query`, `active`, `page`, `size`, `sort` |
| `POST` | `/api/v1/items` | Create an item and its opening stock |
| `GET` | `/api/v1/items/{id}` | Item plus variants |
| `GET` | `/api/v1/items/sku/{sku}` | Item by SKU |
| `PUT` | `/api/v1/items/{id}` | Change name, description, price, or active flag. Not SKU or stock |
| `DELETE` | `/api/v1/items/{id}` | Deactivate |
| `GET` | `/api/v1/items/{itemId}/variants` | Variants of an item |
| `POST` | `/api/v1/items/{itemId}/variants` | Add a variant |
| `GET` | `/api/v1/variants/{id}` | One variant |
| `GET` | `/api/v1/variants/sku/{sku}` | Variant by SKU |
| `PUT` | `/api/v1/variants/{id}` | Change name, attributes, price, or active flag |
| `DELETE` | `/api/v1/variants/{id}` | Deactivate |
| `POST` | `/api/v1/items/{itemId}/stock-adjustments` | Signed stock change on a simple item |
| `POST` | `/api/v1/variants/{variantId}/stock-adjustments` | Signed stock change on a variant |
| `GET` | `/api/v1/items/{itemId}/stock-movements` | Latest item-level ledger, newest first |
| `GET` | `/api/v1/variants/{variantId}/stock-movements` | Latest variant ledger, newest first |
| `POST` | `/api/v1/sales` | Sell. Rejected when stock is short |
| `GET` | `/api/v1/sales` | List sales |
| `GET` | `/api/v1/sales/{id}` | Sale with snapshotted prices |
| `GET` | `/api/v1/sales/ref/{reference}` | Sale by reference |
| `GET` | `/api/v1/inventory?maxStock=5` | Sellable positions at or below a balance |

Errors use `application/problem+json`. Validation problems include `fieldErrors`. An out-of-stock sale returns `409` with `sku`, `available`, and `requested`. Selling the parent of a variant, or selling something inactive, returns `422`.

`sort` looks like `name,asc` or `createdAt,desc`. Allowed item fields are `sku`, `name`, `price`, and `createdAt`. Allowed sale fields are `createdAt`, `reference`, and `totalAmount`.

## Examples

These calls assume a fresh process, so the seeded ids are stable.

Sell one seeded mug (`MUG-001` is item `1`). The balance goes from 40 to 39:

```bash
curl -s -X POST http://localhost:8080/api/v1/sales \
  -H 'Content-Type: application/json' \
  -d '{"reference":"POS-1001","lines":[{"itemId":1,"quantity":1}]}'
```

The seeded white shirt is out of stock (`TEE-WHT-M` is variant `3`). This sale is rejected and the balance stays 0:

```bash
curl -s -X POST http://localhost:8080/api/v1/sales \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"variantId":3,"quantity":1}]}'
```

```json
{
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Not enough stock for TEE-WHT-M: requested 1, available 0",
  "sku": "TEE-WHT-M",
  "available": 0,
  "requested": 1
}
```

Selling the t-shirt parent (`TEE-001` is item `3`) is also rejected, because that product is sold by variant:

```bash
curl -s -X POST http://localhost:8080/api/v1/sales \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"itemId":3,"quantity":1}]}'
```

Receive five units of Black / S (`TEE-BLK-S` is variant `1`):

```bash
curl -s -X POST http://localhost:8080/api/v1/variants/1/stock-adjustments \
  -H 'Content-Type: application/json' \
  -d '{"quantityDelta":5,"note":"supplier delivery"}'
```

Low and empty sellable stock:

```bash
curl -s 'http://localhost:8080/api/v1/inventory?maxStock=2'
```

Create another simple item. Use the `id` from this response for later calls. SKUs are stored in uppercase.

```bash
curl -s -X POST http://localhost:8080/api/v1/items \
  -H 'Content-Type: application/json' \
  -d '{"sku":"MUG-100","name":"Ceramic Mug","description":"12oz","price":12.50,"initialStock":2}'
```

To turn that new item into variants, bring its own stock to zero first, then add the variant:

```bash
curl -s -X POST http://localhost:8080/api/v1/items/ITEM_ID/stock-adjustments \
  -H 'Content-Type: application/json' \
  -d '{"quantityDelta":-2,"note":"count moved onto colors"}'

curl -s -X POST http://localhost:8080/api/v1/items/ITEM_ID/variants \
  -H 'Content-Type: application/json' \
  -d '{"sku":"MUG-100-RED","name":"Red","attributes":{"color":"Red"},"price":13.00,"initialStock":4}'
```

## Project layout

```text
src/main/java/com/geli/warehouse
  domain        JPA entities, money and stock rules
  repository    Spring Data repositories and lock queries
  service       Items, variants, stock ledger, sales
  web           Controllers, request and response records, error mapping
  config        Auditing clock, OpenAPI, sample data
```

## Tests

`mvn test` covers creation and validation, the variant stock rule, the ledger, all-or-nothing sales, duplicate sale references, price snapshots, the seeded catalog, and two concurrent sales competing for the last unit.
