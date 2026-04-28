---
name: architecture-review
description: Skill for reviewing and assessing architecture decisions in enterprise Spring MVC applications
---

# Architecture Review Skill

## Purpose

Guide architectural assessment of the EFFORT-Web application, identifying structural issues, dependency problems, and improvement opportunities.

## When to Use

- Evaluating proposed changes for architectural impact
- Identifying architectural violations (layer bypassing, circular dependencies)
- Planning major feature additions
- Assessing technical debt at the structural level

## EFFORT Architecture Assessment

### Current Architecture

```
                    ┌─────────────────────────────────────┐
                    │          Web Browser / Mobile        │
                    └────────────────┬────────────────────┘
                                     │
                    ┌────────────────▼────────────────────┐
                    │     Servlet Filters (GZIP, Auth,    │
                    │     Charset, Security Headers)      │
                    └────────────────┬────────────────────┘
                                     │
                    ┌────────────────▼────────────────────┐
                    │   Spring Security Filter Chain       │
                    └────────────────┬────────────────────┘
                                     │
                    ┌────────────────▼────────────────────┐
                    │     DispatcherServlet (Spring MVC)   │
                    └────────────────┬────────────────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                      ▼
        Controllers            Controllers            Controllers
        (Web/AJAX)             (Service/API)          (Config/Admin)
              │                      │                      │
              ▼                      ▼                      ▼
        ┌────────────────────────────────────────────────────┐
        │              Manager Layer (Business Logic)         │
        └────────────────────────┬───────────────────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                   ▼
          DAO (JDBC)      MongoDB Access      Redis Cache
              │                  │                   │
              ▼                  ▼                   ▼
           MySQL            MongoDB              Redis
```

### Architecture Review Checklist

#### Layer Integrity
- [ ] Controllers only call Managers, never DAOs directly
- [ ] Managers don't reference HttpServletRequest/HttpSession
- [ ] DAOs contain no business logic
- [ ] No circular dependencies between packages

#### Configuration
- [ ] Environment-specific config properly externalized in constants XML files
- [ ] No hardcoded environment values in Java code
- [ ] Feature flags managed via ProductConstants bean

#### Scalability Concerns
- [ ] Session state manageable (Redis session store available but commented)
- [ ] Database connection pooling configured (HikariCP)
- [ ] Scheduled tasks don't conflict in multi-instance deployment
- [ ] JMS messaging properly handles concurrent consumers

#### Separation of Concerns
- [ ] Presentation logic in JSP/Tiles only
- [ ] Business rules in Manager layer only
- [ ] Data access in DAO layer only
- [ ] Cross-cutting concerns (logging, security, transactions) via AOP/filters

### Common Architectural Anti-Patterns to Watch

1. **God Class** — Manager classes exceeding 2000+ lines indicate need for decomposition
2. **Feature Envy** — A class that uses another class's data more than its own
3. **Layer Bypass** — Controller directly accessing DAO skipping Manager
4. **Circular Dependency** — Package A depends on B and B depends on A
5. **Configuration Sprawl** — Too many environment-specific XML files with duplicated properties
