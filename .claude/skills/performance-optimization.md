---
name: performance-optimization
description: Skill for identifying and resolving performance issues in enterprise Java applications
---

# Performance Optimization Skill

## Purpose

Guide performance analysis and optimization across database queries, application logic, and infrastructure configuration.

## When to Use

- Investigating slow endpoints or pages
- Reviewing queries for N+1 problems
- Optimizing bulk operations
- Configuring connection pools and caches
- Profiling memory or CPU issues

## Database Performance

### Query Optimization

```java
// WRONG — N+1 query pattern
List<Order> orders = orderDao.getAllOrders();
for (Order order : orders) {
    List<Item> items = itemDao.getItemsByOrderId(order.getId()); // N queries!
}

// CORRECT — single JOIN query
public List<OrderWithItems> getOrdersWithItems(int customerId) {
    String sql = """
        SELECT o.id, o.order_date, i.item_name, i.quantity
        FROM orders o
        JOIN order_items i ON o.id = i.order_id
        WHERE o.customer_id = ?
        ORDER BY o.order_date DESC
        """;
    return jdbcTemplate.query(sql, mapper, customerId);
}
```

### Pagination

```java
// Always paginate large result sets
public List<Map<String, Object>> getItems(int customerId, int offset, int limit) {
    String sql = "SELECT id, name FROM items WHERE customer_id = ? LIMIT ? OFFSET ?";
    return jdbcTemplate.queryForList(sql, customerId, limit, offset);
}
```

### Indexing Awareness

```sql
-- Ensure indexes exist for frequently filtered/joined columns
CREATE INDEX idx_items_customer_id ON items(customer_id);
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date);
```

## Application Performance

### Connection Pooling (HikariCP)

The project uses HikariCP. Key tuning parameters in the environment constants XML:
- `maximumPoolSize` — match to expected concurrent DB operations
- `connectionTimeout` — fail fast rather than block forever
- `idleTimeout` — release connections not in active use

### Caching with Redis

```java
// Check Redis cache before hitting database
public CustomerData getCustomerData(int customerId) {
    String cacheKey = "customer:" + customerId;
    CustomerData cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return cached;
    }

    CustomerData data = customerDao.getById(customerId);
    redisTemplate.opsForValue().set(cacheKey, data, 30, TimeUnit.MINUTES);
    return data;
}
```

### Bulk Operations

```java
// WRONG — individual inserts in a loop
for (Item item : items) {
    jdbcTemplate.update("INSERT INTO items (name) VALUES (?)", item.getName());
}

// CORRECT — batch insert
jdbcTemplate.batchUpdate("INSERT INTO items (name) VALUES (?)",
    new BatchPreparedStatementSetter() {
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setString(1, items.get(i).getName());
        }
        public int getBatchSize() { return items.size(); }
    });
```

### GZIP Compression

Already configured for `/service/*`, `/custService/*`, `/onDemand/*` via `GZIP2WayFilter`. Ensure API responses benefit from compression.

## Performance Checklist

- [ ] Queries use indexes and avoid full table scans
- [ ] No N+1 query patterns
- [ ] Large result sets are paginated
- [ ] Bulk operations use batch processing
- [ ] Expensive computations are cached where appropriate
- [ ] Connection pool is properly sized
- [ ] Transactions are kept short
- [ ] No unnecessary object creation in hot paths
