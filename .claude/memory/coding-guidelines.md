# Coding Guidelines

## Java Standards

### Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Class | PascalCase | `WebController`, `OrderManager` |
| Method | camelCase | `getOrderById`, `processPayment` |
| Variable | camelCase | `customerId`, `orderList` |
| Constant | UPPER_SNAKE | `STATUS_ACTIVE`, `MAX_RETRY_COUNT` |
| Package | lowercase | `com.ayansys.effort.controller` |
| DAO class | Suffix with `Dao` | `EmployeeDao`, `OrderDao` |
| Manager class | Suffix with `Manager` | `WebManager`, `SyncManager` |
| Controller class | Suffix with `Controller` | `WebController` |

### Class Structure Order

```java
public class ExampleManager {
    // 1. Static constants
    private static final Logger logger = LoggerFactory.getLogger(ExampleManager.class);
    private static final String STATUS_ACTIVE = "active";

    // 2. Instance fields (injected dependencies)
    private final ExampleDao exampleDao;

    // 3. Constructor (with @Autowired)
    @Autowired
    public ExampleManager(ExampleDao exampleDao) {
        this.exampleDao = exampleDao;
    }

    // 4. Public methods
    public List<Item> getItems(int customerId) { ... }

    // 5. Private helper methods
    private void validateItem(Item item) { ... }
}
```

### Method Guidelines

- Keep methods under 50 lines (extract helpers for complex logic)
- One method = one responsibility
- Use meaningful parameter names
- Return early for error cases (guard clauses)
- Use Java 21 features: text blocks, pattern matching, switch expressions

## Spring-Specific Guidelines

### Dependency Injection

```java
// PREFERRED — constructor injection
@Autowired
public ExampleManager(ExampleDao dao, NotificationManager notifier) {
    this.dao = dao;
    this.notifier = notifier;
}

// ACCEPTABLE — field injection (existing codebase pattern)
@Autowired
private ExampleDao dao;
```

### Transaction Boundaries

- Place `@Transactional` on Manager methods that modify data
- Use `@Transactional(readOnly = true)` for read-only operations
- Never put `@Transactional` on controllers or DAOs

### Controller Return Patterns

```java
// Page render — return Tiles view name
return "viewName";

// JSON response — use @ResponseBody
@ResponseBody
public Map<String, Object> apiMethod() { ... }

// Redirect
return "redirect:/web/list";
```

## SQL Guidelines

- Always use parameterized queries with `?` placeholders
- Select only needed columns (no `SELECT *`)
- Include `LIMIT` for list queries
- Add comments for complex queries
- Use text blocks for multi-line SQL (Java 21)

## JSP Guidelines

- Use JSTL tags, never scriptlets
- Always encode output with `<c:out>`
- Use `<c:url>` for URL construction
- Include CSRF tokens in forms
- Follow Bootstrap 5 component patterns

## Logging Guidelines

- Use SLF4J with parameterized messages
- Never use `System.out.println` or `e.printStackTrace()`
- Log at appropriate levels (ERROR, WARN, INFO, DEBUG)
- Include contextual IDs in log messages
- Never log passwords, tokens, or PII

## Jakarta EE Migration

All new code must use Jakarta namespace:
- `jakarta.servlet.*` (not `javax.servlet.*`)
- `jakarta.mail.*` (not `javax.mail.*`)
- `jakarta.jms.*` (not `javax.jms.*`)
- `jakarta.validation.*` (not `javax.validation.*`)
