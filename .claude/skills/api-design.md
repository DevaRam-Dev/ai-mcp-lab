---
name: api-design
description: Skill for designing REST-like API endpoints in Spring MVC controllers
---

# API Design Skill

## Purpose

Guide the design of REST-like API endpoints exposed by Spring MVC controllers for mobile clients and AJAX calls.

## When to Use

- Creating new API endpoints for mobile or web AJAX consumers
- Reviewing API response formats for consistency
- Designing new controller methods returning JSON
- Integrating with external services

## API Patterns in EFFORT

EFFORT exposes two types of endpoints:
1. **Page endpoints** — return JSP view names (rendered by Tiles)
2. **JSON/AJAX endpoints** — return `@ResponseBody` Map/Object (serialized by Jackson)

### JSON Response Pattern

```java
@RequestMapping(value = "/api/items", method = RequestMethod.GET)
@ResponseBody
public Map<String, Object> getItems(@RequestParam int customerId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
    Map<String, Object> response = new HashMap<>();
    try {
        List<Map<String, Object>> items = manager.getItems(customerId, page, size);
        int totalCount = manager.getItemCount(customerId);

        response.put("status", "success");
        response.put("data", items);
        response.put("totalCount", totalCount);
        response.put("page", page);
        response.put("size", size);
    } catch (Exception e) {
        logger.error("Error fetching items for customer={}: {}", customerId, e.getMessage(), e);
        response.put("status", "error");
        response.put("message", "Failed to retrieve items");
    }
    return response;
}
```

### Consistent Response Format

All JSON endpoints should follow this structure:

```json
{
    "status": "success" | "error",
    "data": { ... },
    "message": "Error description (only on error)",
    "totalCount": 100,
    "page": 0,
    "size": 20
}
```

### Mobile/Service Endpoints

Mobile endpoints under `/service/*` and `/custService/*` use GZIP compression via `GZIP2WayFilter`. Ensure:
- Request body may be GZIP-compressed
- Response is GZIP-compressed
- Use Jackson annotations on model objects for serialization control

### Best Practices

- Use descriptive URL paths: `/web/employees/list`, not `/web/getEmpList`
- Use appropriate HTTP methods: GET for reads, POST for writes
- Include pagination for list endpoints
- Return consistent response structures
- Validate all input parameters
- Log API access at DEBUG level, errors at ERROR level
- Use `@RequestParam(required = false)` for optional parameters with defaults
