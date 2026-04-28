---
name: exception-handling
description: Skill for implementing enterprise exception handling across all layers
---

# Exception Handling Skill

## Purpose

Guide consistent exception handling patterns across the Controller, Manager, and DAO layers.

## When to Use

- Adding error handling to new features
- Reviewing existing code for proper exception management
- Implementing global exception handlers
- Debugging production error scenarios

## Exception Flow

```
DAO Layer          → DataAccessException, SQLException
Manager Layer      → BusinessException, ValidationException (catch and wrap DAO exceptions)
Controller Layer   → Catch Manager exceptions, return error views/JSON
Global Handler     → @ExceptionHandler catches unhandled exceptions
```

## Best Practices

### Layer-Specific Handling

**DAO Layer** — Let Spring JDBC exceptions propagate (they're unchecked). Only catch if you need to add context:

```java
public Item getItemById(int id) {
    try {
        return jdbcTemplate.queryForObject(sql, mapper, id);
    } catch (EmptyResultDataAccessException e) {
        logger.debug("No item found with id={}", id);
        return null; // or throw custom NotFoundException
    }
}
```

**Manager Layer** — Catch and wrap with business context:

```java
public void processOrder(Order order) {
    try {
        orderDao.save(order);
        notificationManager.notifyCustomer(order);
    } catch (DataAccessException e) {
        logger.error("Failed to save order for customer={}: {}", order.getCustomerId(), e.getMessage(), e);
        throw new BusinessException("Order processing failed", e);
    }
}
```

**Controller Layer** — Return appropriate responses:

```java
@RequestMapping("/save")
@ResponseBody
public Map<String, Object> save(@RequestParam String data, HttpServletRequest request) {
    Map<String, Object> response = new HashMap<>();
    try {
        manager.process(data);
        response.put("status", "success");
    } catch (ValidationException e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
    } catch (Exception e) {
        logger.error("Unexpected error in save: {}", e.getMessage(), e);
        response.put("status", "error");
        response.put("message", "An unexpected error occurred");
    }
    return response;
}
```

### Enterprise Standards

- **Never swallow exceptions** — always log or rethrow
- **Never expose stack traces** to end users
- **Log the full exception** at ERROR level: `logger.error("msg: {}", val, exception)`
- **Use specific exception types** over generic `Exception`
- **Include contextual information** in error messages (IDs, operation names)
- **Use try-with-resources** for closeable resources (streams, connections)
- **Don't use exceptions for flow control** — check conditions before they occur
