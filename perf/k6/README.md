# k6 load scenarios

These scripts are **not** part of `./mvnw test`. They need a running API (`dev,seed`) plus PostgreSQL and Redis.

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed
k6 run perf/k6/login-and-list.js
k6 run perf/k6/fulfil-sales-order.js
k6 run perf/k6/dashboard-cache.js
```

Record p95 latency and `429` counts. Tune `RATE_LIMIT_*` if you are hammering a single demo user.

Indexes added for list/report paths (see Flyway `V18`):

- `catalog.products (organization_id, stock_quantity) WHERE active`
- `sales.sales_orders (organization_id, status, created_at DESC)`
- `purchasing.purchase_orders (organization_id, status, created_at DESC)`

Use `EXPLAIN ANALYZE` on those list queries against a copy of production-like data before adding more indexes.
