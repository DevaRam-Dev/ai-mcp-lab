---
name: dao-hibernate
description: Skill for implementing DAO layer using Spring JDBC (and understanding legacy JDBC patterns)
---

# DAO / Data Access Layer Skill

## Purpose

Guide the creation and review of DAO classes in the EFFORT-Web project. Note: despite the skill name, this project uses **Spring JDBC** (JdbcTemplate / NamedParameterJdbcTemplate), NOT Hibernate/JPA.

## When to Use

- Writing new database queries
- Creating new DAO classes
- Reviewing data access code for SQL injection risks
- Optimizing database queries
- Implementing RowCallbackHandlers for result set mapping

## Architecture Role

```
Manager → DAO → JdbcTemplate → MySQL Database
                    ↓
            RowCallbackHandler (result mapping)
```

DAOs are the sole point of database interaction. They must:
1. Execute parameterized SQL queries
2. Map results to domain objects
3. Handle data-level exceptions
4. Never contain business logic

## Best Practices

### DAO Class Structure

```java
@Repository
public class ExampleDao {

    private static final Logger logger = LoggerFactory.getLogger(ExampleDao.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getItemsByCustomer(int customerId) {
        String sql = "SELECT id, name, status FROM items WHERE customer_id = ? AND is_active = 1";
        return jdbcTemplate.queryForList(sql, customerId);
    }

    public int insertItem(String name, int customerId) {
        String sql = "INSERT INTO items (name, customer_id, created_date) VALUES (?, ?, NOW())";
        jdbcTemplate.update(sql, name, customerId);

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public void updateItemStatus(int itemId, String status) {
        String sql = "UPDATE items SET status = ?, updated_date = NOW() WHERE id = ?";
        int rows = jdbcTemplate.update(sql, status, itemId);
        if (rows == 0) {
            logger.warn("No item found with id={} to update", itemId);
        }
    }
}
```

### RowCallbackHandler Pattern

This project uses `RowCallbackHandler` extensively for complex result set processing:

```java
public class ItemCallBackHandler implements RowCallbackHandler {

    private final List<ItemDto> items = new ArrayList<>();

    @Override
    public void processRow(ResultSet rs) throws SQLException {
        ItemDto item = new ItemDto();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setStatus(rs.getString("status"));
        items.add(item);
    }

    public List<ItemDto> getItems() {
        return items;
    }
}
```

Usage in DAO:
```java
public List<ItemDto> getItemsWithDetails(int customerId) {
    String sql = "SELECT i.id, i.name, i.status FROM items i WHERE i.customer_id = ?";
    ItemCallBackHandler handler = new ItemCallBackHandler();
    jdbcTemplate.query(sql, handler, customerId);
    return handler.getItems();
}
```

### Enterprise Standards

- **Always use parameterized queries** — NEVER concatenate user input into SQL strings
- **Use `?` placeholders** with `JdbcTemplate` for positional parameters
- **Return `List<Map<String, Object>>`** for simple queries, or use RowCallbackHandlers for complex mapping
- **Handle `EmptyResultDataAccessException`** when using `queryForObject`
- **Keep SQL readable** — use multi-line strings for complex queries
- **Index awareness** — ensure WHERE clause columns are indexed
- **Avoid SELECT *** — explicitly list required columns

### Security — SQL Injection Prevention

```java
// CORRECT — parameterized
String sql = "SELECT * FROM users WHERE username = ? AND customer_id = ?";
jdbcTemplate.queryForList(sql, username, customerId);

// WRONG — vulnerable to SQL injection
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

### Key DAOs in EFFORT

Existing DAOs are in `com.ayansys.effort.dao/` with callback handlers in `dao/rowcallbackhandler/`.
