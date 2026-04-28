---
name: transaction-management
description: Skill for implementing proper transaction management with Spring declarative transactions
---

# Transaction Management Skill

## Purpose

Guide the correct use of Spring's `@Transactional` annotation and transaction boundaries in the Manager/DAO layers.

## When to Use

- Adding transactional behavior to manager methods
- Debugging transaction rollback issues
- Reviewing code for missing or incorrect transaction boundaries
- Understanding propagation and isolation levels

## Best Practices

### Basic Transaction Usage

```java
@Component
public class OrderManager {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private InventoryDao inventoryDao;

    @Transactional
    public void placeOrder(Order order) {
        // Both operations are in the same transaction
        orderDao.insertOrder(order);
        inventoryDao.decrementStock(order.getItemId(), order.getQuantity());
        // If decrementStock fails, insertOrder is also rolled back
    }
}
```

### Transaction Placement Rules

- **Place `@Transactional` on Manager/Service methods**, not on DAOs or Controllers
- **Manager layer defines transaction boundaries** — it knows when a business operation starts and ends
- **DAO methods should NOT have `@Transactional`** unless they need independent transactions

### Propagation Levels

```java
// Default — join existing transaction or create new one
@Transactional(propagation = Propagation.REQUIRED)

// Always create a new transaction (suspends existing)
@Transactional(propagation = Propagation.REQUIRES_NEW)

// Run without transaction (suspends existing if any)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
```

### Read-Only Transactions

```java
// For read-only operations — allows DB optimizations
@Transactional(readOnly = true)
public List<Order> getOrdersByCustomer(int customerId) {
    return orderDao.findByCustomerId(customerId);
}
```

### Rollback Rules

```java
// Rollback on specific exceptions
@Transactional(rollbackFor = BusinessException.class)
public void processPayment(Payment payment) { ... }

// Don't rollback on specific exceptions
@Transactional(noRollbackFor = NotificationException.class)
public void processAndNotify(Order order) { ... }
```

### Common Pitfalls

1. **Self-invocation bypass**: Calling a `@Transactional` method from within the same class bypasses the proxy — the transaction won't apply
2. **Checked exceptions**: By default, Spring only rolls back on unchecked exceptions. Use `rollbackFor` for checked exceptions
3. **Long transactions**: Keep transactions short. Don't include external HTTP calls or file I/O inside a transaction
4. **Missing `@Transactional` on write operations**: Every method that modifies data should be transactional
