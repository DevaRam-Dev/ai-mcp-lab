---
name: service-layer
description: Skill for designing and implementing the Manager/Service business logic layer
---

# Service (Manager) Layer Skill

## Purpose

Guide the creation and review of Manager classes that contain business logic in the EFFORT-Web architecture. In this project, the Manager layer serves the role typically called "Service" in standard Spring applications.

## When to Use

- Implementing new business logic
- Refactoring business logic out of controllers or DAOs
- Reviewing Manager classes for SOLID compliance
- Adding transactional boundaries

## Architecture Role

```
Controller → Manager (Business Logic) → DAO (Data Access) → Database
```

Managers are Spring `@Component` or `@Service` beans that:
1. Encapsulate business rules and orchestration
2. Coordinate between multiple DAOs
3. Handle transaction boundaries
4. Perform data transformation between controller DTOs and DAO entities

## Best Practices

### Manager Class Structure

```java
@Component
public class ExampleManager {

    private static final Logger logger = LoggerFactory.getLogger(ExampleManager.class);

    private final ExampleDao exampleDao;
    private final NotificationManager notificationManager;

    @Autowired
    public ExampleManager(ExampleDao exampleDao,
                           NotificationManager notificationManager) {
        this.exampleDao = exampleDao;
        this.notificationManager = notificationManager;
    }

    @Transactional
    public void processOrder(Order order, int customerId) {
        // 1. Validate business rules
        validateOrderRules(order);

        // 2. Persist via DAO
        int orderId = exampleDao.insertOrder(order, customerId);

        // 3. Side effects (notifications, integrations)
        notificationManager.sendOrderConfirmation(orderId, customerId);

        logger.info("Order processed: orderId={}, customerId={}", orderId, customerId);
    }

    private void validateOrderRules(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
    }
}
```

### Enterprise Standards

- **Single Responsibility** — each Manager handles one domain concern
- **Transaction management** — use `@Transactional` for operations that modify data
- **No direct HTTP/Servlet dependencies** — Managers should not reference `HttpServletRequest`, `HttpSession`, etc.
- **No direct SQL** — all data access goes through DAO layer
- **Use constructor injection** for mandatory dependencies
- **Log business-significant events** at INFO level, errors at ERROR level

### Key Managers in EFFORT

| Manager | Responsibility |
|---|---|
| WebManager | Core web business logic |
| SyncManager | Data synchronization |
| ConfiguratorManager | System configuration |
| MailService | Email sending |
| SmsManager | SMS notifications |
| DayPlanManager | Day planning logic |
| EffortPluginManager | Plugin system orchestration |
| WebSupportManager | Support feature logic |
| EntityBulkUploader | Bulk data import |

### Error Handling Pattern

```java
public Result performOperation(Input input) {
    try {
        // business logic
        return Result.success(data);
    } catch (DataAccessException e) {
        logger.error("Database error during operation: {}", e.getMessage(), e);
        throw new BusinessException("Operation failed due to data error", e);
    } catch (Exception e) {
        logger.error("Unexpected error during operation: {}", e.getMessage(), e);
        throw new BusinessException("Operation failed unexpectedly", e);
    }
}
```
