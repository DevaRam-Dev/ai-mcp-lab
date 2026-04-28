---
name: database-optimization
description: Skill for optimizing MySQL database queries and schema in Spring JDBC applications
---

# Database Optimization Skill

## Purpose

Guide database performance optimization for MySQL queries executed via Spring JDBC in the EFFORT-Web application.

## When to Use

- Investigating slow queries
- Reviewing query patterns for optimization opportunities
- Adding indexes to improve query performance
- Optimizing batch operations
- Analyzing database connection pool configuration

## Query Optimization

### EXPLAIN Analysis

Before optimizing, analyze the query execution plan:

```sql
EXPLAIN SELECT e.id, e.name, e.status
FROM employees e
WHERE e.customer_id = 123
AND e.is_active = 1
ORDER BY e.created_date DESC;
```

Look for:
- `type: ALL` (full table scan) — needs index
- `rows` — high number means inefficient scan
- `Extra: Using filesort` — may need index for ORDER BY
- `Extra: Using temporary` — query may need restructuring

### Index Strategies

```sql
-- Composite index for common filter + sort pattern
CREATE INDEX idx_emp_customer_active_date
ON employees(customer_id, is_active, created_date DESC);

-- Covering index (all queried columns in index)
CREATE INDEX idx_emp_covering
ON employees(customer_id, is_active, id, name, status);
```

### Query Patterns

**Avoid SELECT ***
```java
// WRONG
"SELECT * FROM employees WHERE customer_id = ?"

// CORRECT — only select needed columns
"SELECT id, name, email, status FROM employees WHERE customer_id = ?"
```

**Use LIMIT for pagination**
```java
"SELECT id, name FROM employees WHERE customer_id = ? ORDER BY name LIMIT ? OFFSET ?"
```

**Avoid subqueries when JOINs work**
```java
// SLOW — correlated subquery
"SELECT * FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE region = ?)"

// FASTER — JOIN
"SELECT o.* FROM orders o JOIN customers c ON o.customer_id = c.id WHERE c.region = ?"
```

## Batch Operations

```java
// Batch INSERT for bulk data
public void insertBatch(List<Item> items) {
    String sql = "INSERT INTO items (name, customer_id, created_date) VALUES (?, ?, NOW())";
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setString(1, items.get(i).getName());
            ps.setInt(2, items.get(i).getCustomerId());
        }
        public int getBatchSize() { return items.size(); }
    });
}
```

## Connection Pool Tuning (HikariCP)

Key parameters to tune in environment constants XML:

| Parameter | Guideline |
|---|---|
| `maximumPoolSize` | Start with `(2 * CPU cores) + number of disks` |
| `minimumIdle` | Set equal to maximumPoolSize for consistent performance |
| `connectionTimeout` | 30000ms (30s) — fail fast |
| `idleTimeout` | 600000ms (10min) |
| `maxLifetime` | 1800000ms (30min) — less than MySQL wait_timeout |

## Performance Monitoring

- Check slow query log in MySQL
- Monitor HikariCP metrics via `MetricsConfig.java` (Micrometer/Prometheus)
- Watch for connection pool exhaustion (`HikariPool-1 - Connection is not available`)

## Common MySQL Optimization Tips

- Use `INT` over `VARCHAR` for IDs and foreign keys
- Normalize appropriately but denormalize for read-heavy patterns
- Use `DATETIME` over `VARCHAR` for dates
- Set appropriate `innodb_buffer_pool_size` (70-80% of available RAM on DB server)
- Avoid `LIKE '%term%'` — use full-text search for text matching
