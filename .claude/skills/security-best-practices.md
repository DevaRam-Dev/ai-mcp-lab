---
name: security-best-practices
description: Skill for implementing security best practices using Spring Security, OWASP ESAPI, and secure coding patterns
---

# Security Best Practices Skill

## Purpose

Guide secure coding practices across all application layers, leveraging Spring Security and OWASP ESAPI.

## When to Use

- Implementing authentication/authorization logic
- Reviewing code for security vulnerabilities
- Adding input validation and output encoding
- Configuring Spring Security rules
- Addressing SonarQube security findings

## Security Architecture

```
Request → GZIP Filter → Charset Filter → Spring Security Filter Chain → Controller
                                              ↓
                         Authentication Provider (com.kb.authentication.provider)
                                              ↓
                              ACL Authorization (Spring Security ACL)
```

## OWASP Top 10 Prevention

### 1. SQL Injection

```java
// ALWAYS use parameterized queries
String sql = "SELECT * FROM users WHERE id = ? AND customer_id = ?";
jdbcTemplate.queryForList(sql, userId, customerId);

// NEVER concatenate
// String sql = "SELECT * FROM users WHERE id = " + userId; // VULNERABLE
```

### 2. Cross-Site Scripting (XSS)

```java
// In Java — use ESAPI
String safe = ESAPI.encoder().encodeForHTML(userInput);
```

```jsp
<!-- In JSP — use c:out -->
<c:out value="${userInput}" />

<!-- NEVER output raw user data -->
<!-- ${userInput}  ← VULNERABLE if contains script tags -->
```

### 3. Broken Authentication

- Session timeout is configured in environment constants XML (`defaultMaxInactiveSessionTimeoutInMinutes`)
- `ConcurrentLoginRedirectFilter` prevents concurrent sessions
- HTTP-only cookies enabled in `web.xml`
- JWT tokens used for API authentication (`JwtService`)

### 4. Sensitive Data Exposure

```java
// NEVER log sensitive data
logger.info("User authenticated: userId={}", userId);
// logger.info("User authenticated: password={}", password); // NEVER

// Use Google Secret Manager for secrets (already integrated)
```

### 5. CSRF Protection

Spring Security CSRF is configured. Ensure forms include the CSRF token:

```jsp
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
```

## Spring Security Configuration

Security config is in `SecurityConfig.java` and XML-based security config. Key points:
- Filter chain configured in `web.xml` via `DelegatingFilterProxy`
- Custom authentication provider in `com.kb.authentication.provider`
- ACL-based authorization in `acl-context.xml`
- `AdditionalResponseHeadersFilter` adds security headers

## Security Headers

The `AdditionalResponseHeadersFilter` should set:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY` or `SAMEORIGIN`
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy` as appropriate

## Secure Coding Checklist

- [ ] All user inputs validated and sanitized
- [ ] All SQL queries parameterized
- [ ] JSP output encoded with `<c:out>` or ESAPI
- [ ] No sensitive data in logs
- [ ] Authentication checked before protected operations
- [ ] Authorization (ACL) enforced for resource access
- [ ] File uploads validated (type, size, content)
- [ ] Error responses don't expose internal details
