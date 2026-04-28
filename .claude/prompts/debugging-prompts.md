# Debugging Prompts

Reusable prompts for diagnosing and resolving production and development issues.

---

## Diagnose Production Error

```
I'm seeing this error in production:

[PASTE ERROR/STACK TRACE]

Help me diagnose this:
1. Identify the root cause from the stack trace
2. Trace the execution path from the entry point to the failure
3. Identify what data or conditions could cause this
4. Check if this is environment-specific (look at constants.xml configs)
5. Suggest the fix with code changes
6. Suggest how to prevent this class of error in the future
```

---

## Debug Slow Endpoint

```
The endpoint [URL_PATH] is performing slowly (taking [X] seconds).

Analyze for performance issues:
1. Find the controller method handling this URL
2. Trace all database queries in the execution path
3. Check for N+1 query patterns
4. Check for missing pagination on large result sets
5. Check for unnecessary data loading
6. Check for blocking operations (HTTP calls, file I/O) in the request path
7. Suggest specific optimizations with code changes
```

---

## Debug Data Inconsistency

```
We're seeing incorrect data for [DESCRIPTION]:
- Expected: [EXPECTED]
- Actual: [ACTUAL]
- Affected: [SCOPE - one customer, all customers, specific records]

Investigate:
1. Find the DAO query that reads this data
2. Find all code paths that write/modify this data
3. Check for race conditions (concurrent access, missing transactions)
4. Check scheduled tasks that might affect this data
5. Check if the issue is environment-specific (different constants.xml)
6. Verify the database query returns correct data when run directly
```

---

## Debug Session Issue

```
Users are experiencing [SESSION_ISSUE_DESCRIPTION]:
- Session timeout unexpectedly
- Concurrent login conflicts
- Lost session data

Investigate:
1. Check session configuration in the active *-constants.xml
   (defaultMaxInactiveSessionTimeoutInMinutes)
2. Check ConcurrentLoginRedirectFilter behavior
3. Check if Redis session store is active (root-context.xml)
4. Check SessionListener and RequestListener implementations
5. Check if the issue correlates with specific user actions or timing
```

---

## Debug Spring Configuration Issue

```
The application is [BEHAVIOR_DESCRIPTION] which seems like a configuration issue.

Investigate:
1. Which *-constants.xml is currently active in root-context.xml?
2. Is the relevant property defined in that constants file?
3. Is the property being read correctly (check PropertyPlaceholder resolution)?
4. Are there conflicting bean definitions?
5. Is the component scan picking up the relevant class?
6. Check if the issue is caused by missing XML imports
```

---

## Debug Database Connection Issue

```
Getting database connection errors: [ERROR_MESSAGE]

Investigate:
1. Check HikariCP configuration in the active constants XML
2. Check database URL, credentials, and connectivity
3. Check for connection leaks (unclosed connections):
   - Look for methods that open connections without @Transactional
   - Look for JdbcTemplate usage outside Spring-managed beans
4. Check pool size vs concurrent load
5. Check MySQL wait_timeout vs HikariCP maxLifetime
6. Check for long-running queries that hold connections
```

---

## Debug Scheduled Task Issue

```
The scheduled task [TASK_NAME] is [ISSUE: not running / running incorrectly /
causing errors].

Investigate:
1. Find the task definition in schedulars*.xml
2. Find the Java implementation class
3. Check the cron expression or fixed-rate/delay configuration
4. Check if task:annotation-driven is active in root-context.xml
5. Check if the task throws exceptions (check error logging)
6. Check if multiple scheduler XML files conflict
7. Verify the task works correctly when invoked manually
```

---

## Trace Full Request Lifecycle

```
Trace the complete lifecycle of a request to [URL_PATH]:

1. Servlet filters applied (check web.xml filter-mapping order)
2. Spring Security processing (authentication, authorization)
3. DispatcherServlet → HandlerMapping → Controller method
4. Controller → Manager method calls
5. Manager → DAO queries executed
6. Response: View resolution (Tiles) or JSON serialization
7. Response filters (GZIP compression if applicable)

Include: method names, parameter passing, and return values at each step.
```
