# Code Review Prompts

Reusable prompts for conducting thorough code reviews on enterprise Java code.

---

## Full Code Review

```
Perform a comprehensive code review of [FILE_PATH]:

Review for:
1. **Architecture**: Does it follow Controller → Manager → DAO layering?
2. **Security**: SQL injection, XSS, input validation, sensitive data exposure
3. **Performance**: N+1 queries, missing pagination, unnecessary object creation
4. **Error Handling**: Proper exception handling, no swallowed exceptions
5. **Code Quality**: SOLID principles, naming, method size, complexity
6. **Logging**: SLF4J parameterized format, appropriate levels, no sensitive data
7. **Jakarta EE**: Uses jakarta namespace, not javax
8. **SonarQube**: Cognitive complexity, resource management, code smells

Format findings as:
- [CRITICAL] Issue description (line X)
- [MAJOR] Issue description (line X)
- [MINOR] Issue description (line X)
- [POSITIVE] What was done well
```

---

## Security-Focused Review

```
Perform a security-focused review of [FILE_PATH]:

Check for:
1. SQL injection — are all queries parameterized?
2. XSS — is all user-generated output encoded?
3. Authentication — are protected endpoints properly secured?
4. Authorization — is ACL/role checking applied?
5. Input validation — are all parameters validated?
6. Sensitive data — any credentials, tokens, PII in logs or responses?
7. CSRF — are POST endpoints protected?
8. File upload — are file types and sizes validated?
9. Session management — proper session handling?
10. Error disclosure — do error responses reveal internals?

For each finding, provide:
- Vulnerability type
- Exact location (file:line)
- Risk level (High/Medium/Low)
- Recommended fix with code example
```

---

## Performance Review

```
Review [FILE_PATH] for performance issues:

Analyze:
1. Database queries — N+1 patterns, missing indexes, full table scans
2. Data loading — loading more data than needed, missing pagination
3. Object creation — unnecessary allocations in loops
4. String operations — concatenation in loops (use StringBuilder)
5. Connection management — proper use of connection pooling
6. Caching opportunities — repeated expensive operations
7. Batch operations — individual operations vs batch
8. Transaction scope — transactions too long or too short

For each issue, estimate impact (High/Medium/Low) and provide optimized code.
```

---

## Review Controller Endpoint

```
Review this controller endpoint for enterprise standards:

[PASTE CONTROLLER METHOD OR FILE:LINE_RANGE]

Check:
1. Input validation — are all parameters validated before use?
2. Delegation — does it delegate to Manager, not call DAO directly?
3. Error handling — are exceptions caught and handled appropriately?
4. Response format — consistent JSON structure or proper view name?
5. Security — authentication/authorization checked?
6. Logging — request logged at appropriate level?
7. OWASP — input encoded/sanitized where needed?
```

---

## Review DAO Method

```
Review this DAO method:

[PASTE DAO METHOD OR FILE:LINE_RANGE]

Check:
1. SQL parameterization — all user inputs via ? placeholders?
2. Query efficiency — proper JOINs, column selection, indexes?
3. Result mapping — correct use of RowCallbackHandler/RowMapper?
4. Null handling — what happens when query returns no results?
5. Error handling — DataAccessException properly handled?
6. SQL readability — complex queries well-formatted?
7. Transaction awareness — method aware of transaction context?
```

---

## Review Manager Business Logic

```
Review this Manager class/method:

[PASTE CODE OR FILE:LINE_RANGE]

Check:
1. Single Responsibility — does it do one thing well?
2. No HTTP dependencies — free of HttpServletRequest/HttpSession?
3. Transaction boundaries — @Transactional where needed?
4. Error handling — business exceptions properly thrown?
5. Logging — business events logged at INFO, errors at ERROR?
6. Validation — business rules validated before DAO calls?
7. Dependency injection — dependencies properly injected?
8. Testability — could this be unit tested (if tests existed)?
```

---

## Review Pull Request / Diff

```
Review this code change:

[PASTE DIFF OR DESCRIBE CHANGES]

Evaluate:
1. Does the change achieve its stated goal?
2. Are there any unintended side effects?
3. Does it follow the existing architecture patterns?
4. Are there any security implications?
5. Are there any performance implications?
6. Is error handling adequate?
7. Is the change backward compatible?
8. Is the change the simplest solution?
```

---

## Pre-Commit Checklist Review

```
Before committing changes to [FILE_PATH], verify:

- [ ] Code compiles without errors (mvn compile)
- [ ] No hardcoded credentials or secrets
- [ ] All SQL queries parameterized
- [ ] JSP output encoded with <c:out>
- [ ] Uses jakarta namespace (not javax)
- [ ] Logging uses SLF4J parameterized format
- [ ] No System.out.println or e.printStackTrace()
- [ ] Exception handling is proper (no swallowed exceptions)
- [ ] No unused imports or variables
- [ ] Method names follow camelCase convention
- [ ] Changes respect Controller → Manager → DAO layering
```
