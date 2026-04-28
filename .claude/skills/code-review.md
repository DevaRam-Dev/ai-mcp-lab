---
name: code-review
description: Skill for conducting enterprise-grade code reviews focused on quality, security, and maintainability
---

# Code Review Skill

## Purpose

Guide systematic code review of Java, JSP, and configuration changes in the EFFORT-Web project.

## When to Use

- Reviewing pull requests or code changes
- Auditing existing code for quality issues
- Preparing code for SonarQube compliance
- Identifying technical debt

## Code Review Checklist

### Architecture & Design

- [ ] Changes respect Controller → Manager → DAO layering
- [ ] No business logic in controllers
- [ ] No HTTP/servlet dependencies in Manager or DAO layers
- [ ] No direct SQL in Manager or Controller layers
- [ ] Single Responsibility — each class has one clear purpose
- [ ] Dependencies injected via `@Autowired` (preferably constructor injection)

### Security

- [ ] All SQL queries use parameterized statements (`?` placeholders)
- [ ] User input validated before use
- [ ] JSP output encoded with `<c:out>` or ESAPI
- [ ] No sensitive data in log statements
- [ ] No hardcoded credentials or secrets
- [ ] Authentication/authorization checked for protected endpoints

### Error Handling

- [ ] Exceptions properly caught, logged, and handled
- [ ] No swallowed exceptions (empty catch blocks)
- [ ] User-facing errors are clear and don't expose internals
- [ ] Resources closed properly (try-with-resources)

### Performance

- [ ] No N+1 query patterns
- [ ] Bulk operations use batch processing
- [ ] Large result sets paginated
- [ ] No unnecessary object creation in loops

### Code Quality

- [ ] No unused imports, variables, or methods
- [ ] Method size is reasonable (< 50 lines preferred)
- [ ] Consistent naming conventions (camelCase for methods/variables)
- [ ] Meaningful variable and method names
- [ ] No magic numbers — use named constants
- [ ] Logging uses SLF4J parameterized format
- [ ] No `System.out.println` or `e.printStackTrace()`

### Jakarta EE Compliance

- [ ] Uses `jakarta.*` namespace, not `javax.*`
- [ ] Servlet API uses `jakarta.servlet`
- [ ] Mail API uses `jakarta.mail`

### SonarQube Compliance

Common SonarQube rules to check:
- Cognitive complexity within limits
- No empty catch blocks
- Resources properly closed
- No deprecated API usage
- String literals not duplicated (extract to constants)
- Methods have reasonable parameter count (< 7)

## Review Response Template

```
## Code Review Summary

**Overall**: [Approve / Request Changes / Needs Discussion]

### Issues Found
1. **[Critical/Major/Minor]** — Description of issue
   - File: `path/to/File.java:lineNumber`
   - Fix: Description of recommended fix

### Positive Observations
- What was done well

### Suggestions (Optional)
- Non-blocking improvement ideas
```
