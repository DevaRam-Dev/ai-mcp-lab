---
name: refactoring-legacy
description: Skill for safely refactoring legacy Java code while maintaining functionality
---

# Legacy Code Refactoring Skill

## Purpose

Guide safe, incremental refactoring of legacy code in a large enterprise codebase (3000+ Java files) where test coverage is minimal.

## When to Use

- Addressing SonarQube findings
- Reducing cognitive complexity of large methods
- Extracting reusable logic from duplicated code
- Modernizing Java patterns (Java 21 features)
- Fixing code smells without changing behavior

## Refactoring Safety Rules

Given the **minimal test coverage** in this project:

1. **Make small, focused changes** — one refactoring at a time
2. **Preserve behavior exactly** — refactoring changes structure, not behavior
3. **Test manually** after each change before proceeding
4. **Keep the original code commented temporarily** if uncertain
5. **Compile after every change** — `mvn compile`

## Common Refactoring Patterns

### Extract Method (Reduce Complexity)

```java
// BEFORE — long method with high cognitive complexity
public void processOrder(Order order, HttpServletRequest request) {
    // 50+ lines of mixed validation, processing, notification
}

// AFTER — decomposed into clear steps
public void processOrder(Order order, HttpServletRequest request) {
    validateOrder(order);
    Order savedOrder = persistOrder(order);
    notifyStakeholders(savedOrder);
}

private void validateOrder(Order order) { ... }
private Order persistOrder(Order order) { ... }
private void notifyStakeholders(Order order) { ... }
```

### Replace Magic Numbers with Constants

```java
// BEFORE
if (status == 1) { ... }
else if (status == 5) { ... }

// AFTER
private static final int STATUS_ACTIVE = 1;
private static final int STATUS_COMPLETED = 5;

if (status == STATUS_ACTIVE) { ... }
else if (status == STATUS_COMPLETED) { ... }
```

### Use Java 21 Features

```java
// Text blocks for SQL
String sql = """
    SELECT id, name, status
    FROM items
    WHERE customer_id = ?
    AND is_active = 1
    ORDER BY created_date DESC
    """;

// Pattern matching for instanceof
if (obj instanceof String s) {
    process(s);
}

// Switch expressions
String label = switch (status) {
    case 1 -> "Active";
    case 2 -> "Inactive";
    case 5 -> "Completed";
    default -> "Unknown";
};
```

### Replace try-catch-finally with try-with-resources

```java
// BEFORE
InputStream is = null;
try {
    is = new FileInputStream(file);
    // process
} finally {
    if (is != null) { is.close(); }
}

// AFTER
try (InputStream is = new FileInputStream(file)) {
    // process
}
```

### Eliminate Duplicate String Literals

```java
// BEFORE — SonarQube flags this
response.put("status", "success");
// ... elsewhere
response.put("status", "success");

// AFTER
private static final String STATUS_KEY = "status";
private static final String STATUS_SUCCESS = "success";

response.put(STATUS_KEY, STATUS_SUCCESS);
```

## SonarQube-Driven Refactoring Priority

1. **Critical** — Security vulnerabilities, resource leaks
2. **Major** — High cognitive complexity, code duplication, empty catch blocks
3. **Minor** — Naming conventions, unused variables, deprecated API usage

## Enterprise Standards

- Never refactor and add features in the same change
- Document the refactoring intent in commit messages
- When unsure about behavior, read the calling code to understand usage
- Refactor in the Manager/DAO layers first (less UI risk)
