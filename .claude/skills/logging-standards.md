---
name: logging-standards
description: Skill for implementing consistent logging using Log4j2/SLF4J across the application
---

# Logging Standards Skill

## Purpose

Ensure consistent, meaningful logging across all layers using Log4j2 with SLF4J facade.

## When to Use

- Adding logging to new or existing code
- Reviewing logging practices in code reviews
- Debugging production issues via log analysis
- Configuring log levels and appenders

## Configuration

Log4j2 config: `src/main/resources/log4j2.properties`

## Best Practices

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleManager {
    private static final Logger logger = LoggerFactory.getLogger(ExampleManager.class);
}
```

### Log Levels

| Level | Use For | Example |
|---|---|---|
| ERROR | Failures requiring attention | Database connection failure, critical business operation failure |
| WARN | Recoverable issues, degraded state | Fallback used, retry attempted, deprecated API called |
| INFO | Business-significant events | Order created, user logged in, job status changed |
| DEBUG | Development/troubleshooting details | Method entry/exit, intermediate values, query parameters |
| TRACE | Very fine-grained diagnostic | Loop iterations, detailed object state |

### Parameterized Logging

```java
// CORRECT — uses SLF4J parameterized messages (no string concatenation overhead)
logger.info("Order processed: orderId={}, customerId={}", orderId, customerId);
logger.error("Failed to process order: {}", orderId, exception);

// WRONG — string concatenation happens even if log level is disabled
logger.info("Order processed: orderId=" + orderId + ", customerId=" + customerId);
```

### What to Log

- **Controller**: Request received (DEBUG), validation failures (WARN), response status (DEBUG)
- **Manager**: Business operation start/completion (INFO), business rule violations (WARN), unexpected errors (ERROR)
- **DAO**: Query execution details (DEBUG), empty results (DEBUG), data integrity issues (WARN)
- **Scheduler**: Job start/completion (INFO), job failures (ERROR)

### What NOT to Log

- Passwords, tokens, API keys, or secrets
- Full credit card numbers or sensitive PII
- Entire request/response bodies in production (use DEBUG level)
- Redundant information already captured by the framework

### Exception Logging

```java
// CORRECT — passes exception as last argument for full stack trace
logger.error("Operation failed for userId={}: {}", userId, e.getMessage(), e);

// WRONG — loses stack trace
logger.error("Operation failed: " + e.getMessage());

// WRONG — redundant, logger already handles the exception
logger.error("Operation failed: " + e.toString(), e);
```
