---
name: validation-patterns
description: Skill for implementing input validation at controller and service boundaries
---

# Validation Patterns Skill

## Purpose

Guide input validation patterns using OWASP ESAPI, Spring validation, and custom validators.

## When to Use

- Validating user input from web forms or API requests
- Sanitizing data before database operations
- Reviewing code for missing validation
- Implementing custom validators

## Validation Layers

```
Client (JavaScript) → Controller (Input Validation) → Manager (Business Validation) → DAO (Parameterized Queries)
```

Each layer validates what it is responsible for. Defense in depth — never rely on a single layer.

## Best Practices

### Controller-Level Validation

```java
@RequestMapping(value = "/save", method = RequestMethod.POST)
@ResponseBody
public Map<String, Object> saveItem(@RequestParam String name,
                                     @RequestParam int quantity,
                                     HttpServletRequest request) {
    Map<String, Object> response = new HashMap<>();

    // Input validation
    if (name == null || name.trim().isEmpty()) {
        response.put("status", "error");
        response.put("message", "Name is required");
        return response;
    }

    if (quantity <= 0 || quantity > 10000) {
        response.put("status", "error");
        response.put("message", "Quantity must be between 1 and 10000");
        return response;
    }

    // Sanitize using OWASP ESAPI
    String sanitizedName = ESAPI.encoder().encodeForHTML(name.trim());

    manager.saveItem(sanitizedName, quantity);
    response.put("status", "success");
    return response;
}
```

### OWASP ESAPI Encoding

```java
import org.owasp.esapi.ESAPI;

// HTML context
String safe = ESAPI.encoder().encodeForHTML(userInput);

// JavaScript context
String safeJs = ESAPI.encoder().encodeForJavaScript(userInput);

// URL parameter context
String safeUrl = ESAPI.encoder().encodeForURL(userInput);

// SQL (prefer parameterized queries instead)
// Use JdbcTemplate with ? placeholders — don't rely on encoding for SQL
```

### Business-Level Validation (Manager)

```java
public void createEmployee(EmployeeDto dto) {
    // Business rule validation
    if (employeeDao.existsByEmail(dto.getEmail())) {
        throw new ValidationException("Employee with this email already exists");
    }

    if (dto.getStartDate().isAfter(dto.getEndDate())) {
        throw new ValidationException("Start date must be before end date");
    }

    employeeDao.insert(dto);
}
```

### Custom Validators

Validators are in `com.ayansys.effort.validators/`. Follow the existing pattern:

```java
@Component
public class OrderValidator {

    public void validate(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getCustomerId() <= 0) {
            errors.add("Invalid customer ID");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Order must contain items");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }
}
```

### Enterprise Standards

- Validate at the boundary (controller for web, message listener for JMS)
- Use OWASP ESAPI for output encoding — it's already in the project
- Never trust client-side validation alone
- Use parameterized JDBC queries — encoding is not a substitute for parameterization
- Validate numeric ranges, string lengths, and allowed characters
- Return clear, user-friendly validation error messages
