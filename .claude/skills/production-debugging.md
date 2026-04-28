---
name: production-debugging
description: Skill for debugging production issues in enterprise Spring MVC applications
---

# Production Debugging Skill

## Purpose

Guide systematic debugging of production issues using logs, code analysis, and domain knowledge.

## When to Use

- Investigating production errors or exceptions
- Diagnosing performance degradation
- Tracing data inconsistencies
- Analyzing error patterns from logs

## Debugging Workflow

### Step 1: Gather Information

- **Error message/stack trace** — exact exception and line numbers
- **Affected URL/endpoint** — which controller method
- **Timing** — when did it start, is it intermittent or constant
- **Scope** — one customer or all customers
- **Recent changes** — any recent deployments or config changes

### Step 2: Trace the Request Flow

```
URL → web.xml filter chain → Spring Security → Controller → Manager → DAO → MySQL
```

1. Identify the controller from the URL pattern
2. Find the controller method that handles the request
3. Trace through the manager call
4. Examine the DAO query and parameters

### Step 3: Common Production Issues

#### Database Connection Exhaustion

**Symptoms**: `HikariPool-1 - Connection is not available, request timed out`

**Investigation**:
- Check for unclosed connections (missing `@Transactional` or resource leaks)
- Check HikariCP pool size vs concurrent user load
- Look for long-running queries holding connections

#### NullPointerException

**Investigation**:
```java
// Check the line number from stack trace
// Common causes:
// 1. DAO returned null (query found no rows)
// 2. Session attribute missing (user session expired)
// 3. Configuration property not set for this environment
```

#### Session-Related Issues

**Investigation**:
- Check if Redis session store is active (currently commented out in `root-context.xml`)
- Check session timeout setting in environment constants (`defaultMaxInactiveSessionTimeoutInMinutes`)
- Check `ConcurrentLoginRedirectFilter` for multi-login conflicts

#### SQL Errors

**Investigation**:
```java
// Find the exact SQL in the DAO method
// Common causes:
// 1. Column name mismatch (schema changed but query didn't)
// 2. Data type mismatch (String passed for INT column)
// 3. Constraint violation (duplicate key, foreign key)
```

#### OutOfMemoryError

**Investigation**:
- Check for unbounded result sets (missing LIMIT in queries)
- Check for large file uploads consuming heap
- Check JasperReports generation with large datasets
- Review scheduled tasks that may accumulate data in memory

### Step 4: Environment-Specific Debugging

Since EFFORT uses per-environment constants:

1. Identify which `*-constants.xml` is active in `root-context.xml`
2. Check if the property/feature flag exists in that constants file
3. Verify database connection settings match the target environment

### Log Analysis Patterns

```bash
# Find errors in a time range
grep "ERROR" application.log | grep "2024-01-15 14:"

# Find all exceptions for a specific class
grep "ExampleManager" application.log | grep -i "exception"

# Find slow operations (if timing is logged)
grep "completed in" application.log | awk -F'in ' '{print $2}' | sort -rn | head -20
```

### Quick Diagnostic Checklist

- [ ] Can you reproduce the issue?
- [ ] Is it environment-specific? (check which constants.xml is active)
- [ ] Is it customer-specific? (check customer-level configuration)
- [ ] Is it time-dependent? (scheduled task conflict, timezone issue)
- [ ] Is it data-dependent? (specific data causing the error)
- [ ] Is there a recent deployment correlation?
