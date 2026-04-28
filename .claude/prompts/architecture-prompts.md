# Architecture Prompts

Reusable prompts for architectural analysis and design tasks.

---

## Analyze Module Architecture

```
Analyze the architecture of the [MODULE_NAME] module in this project.

1. Identify all classes involved (controller, manager, DAO, model)
2. Map the data flow from HTTP request to database
3. Identify external dependencies and integrations
4. List any architectural violations (layer bypassing, circular dependencies)
5. Assess complexity and suggest decomposition if needed

Focus on: separation of concerns, SOLID principles, and enterprise patterns.
```

---

## Design New Feature

```
Design the implementation for: [FEATURE_DESCRIPTION]

Following the EFFORT-Web architecture:
1. Controller: Define endpoints (URL, HTTP method, parameters, response type)
2. Manager: Define business logic methods and validation rules
3. DAO: Define database queries (parameterized SQL)
4. Model/DTO: Define data classes needed
5. View (JSP): Define UI components if applicable
6. Configuration: Any XML config changes needed (Tiles, security, etc.)

Consider: multi-tenancy (customer_id), security (authentication/authorization),
performance (pagination, caching), and error handling.
```

---

## Evaluate Architecture Decision

```
Evaluate this architectural decision: [DECISION_DESCRIPTION]

Consider:
1. Does it fit the existing Controller → Manager → DAO pattern?
2. What is the impact on existing code?
3. Are there simpler alternatives?
4. What are the performance implications?
5. Does it introduce any security concerns?
6. How does it affect maintainability?
7. Is it consistent with the rest of the codebase?
```

---

## Assess Technical Debt

```
Analyze [FILE_OR_PACKAGE] for technical debt:

1. Code complexity (method length, class size, cyclomatic complexity)
2. Duplication (repeated code patterns)
3. Architecture violations (layer bypass, wrong dependencies)
4. Outdated patterns (pre-Java 21, javax instead of jakarta)
5. Missing error handling or logging
6. Security vulnerabilities (SQL injection, XSS)
7. Performance concerns (N+1 queries, missing pagination)

Prioritize findings as: Critical, Major, Minor
Suggest concrete refactoring steps for each finding.
```

---

## Map Feature Dependencies

```
Map all dependencies for the [FEATURE_NAME] feature:

1. Which controllers handle it?
2. Which managers are involved?
3. Which DAOs are called?
4. Which database tables are accessed?
5. Which configuration properties affect it?
6. Which scheduled tasks interact with it?
7. Which external services are called?
8. Which JSP views render it?

Create a dependency diagram showing the relationships.
```

---

## Review Spring Configuration

```
Review the Spring XML configuration for [CONFIG_FILE]:

1. Are all beans properly scoped?
2. Are there unused or duplicate bean definitions?
3. Are property placeholders correctly resolved?
4. Are there circular dependency risks?
5. Is the configuration consistent with Java-based config (SecurityConfig, etc.)?
6. Are there any deprecated APIs or patterns?
```

---

## Plan Migration Strategy

```
Create a migration plan for: [MIGRATION_DESCRIPTION]

Consider:
1. Current state analysis — what exists today
2. Target state — what it should look like after
3. Step-by-step migration path (incremental, reversible steps)
4. Risk assessment — what could go wrong at each step
5. Testing strategy — how to verify each step (given minimal test coverage)
6. Rollback plan — how to revert if issues arise
7. Timeline estimate — relative sizing of each step
```
