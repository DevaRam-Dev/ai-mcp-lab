---
name: large-codebase-understanding
description: Skill for systematically understanding and mapping a large enterprise Java codebase
---

# Large Codebase Understanding Skill

## Purpose

Provide strategies for efficiently understanding and navigating a 3000+ file enterprise Java codebase with minimal test coverage and XML-based configuration.

## When to Use

- New to the codebase and need to understand it quickly
- Investigating how a specific feature works end-to-end
- Understanding the impact of a proposed change
- Mapping dependencies between components

## Understanding Strategy

### Level 1: Bird's Eye View (5 minutes)

```
EFFORT-Web (com.ayansys.effort)
├── controller/    → 20+ controllers handling HTTP
├── manager/       → 15+ managers with business logic
├── dao/           → 10+ DAOs accessing MySQL via JDBC
├── model/         → Domain objects
├── beans/         → DTOs and value objects
├── config/        → Java config (Security, Metrics)
├── scheduling/    → Cron/scheduled tasks
├── handler/       → Exception handlers
├── interceptor/   → Request interceptors
├── mongo/         → MongoDB access
├── Jms/           → ActiveMQ messaging
├── util/          → Shared utilities
├── validators/    → Input validation
├── setting/       → App constants (ProductConstants)
└── filter/ (com.spoors) → Servlet filters
```

### Level 2: Feature Tracing (15 minutes per feature)

Pick a feature and trace it end-to-end:

1. **Start with the URL** — match to controller via `@RequestMapping`
2. **Read the controller method** — understand input parameters and response type
3. **Follow the manager call** — understand business rules applied
4. **Read the DAO query** — understand what data is fetched/modified
5. **Check the JSP** — understand how data is presented
6. **Check Tiles definition** — understand the page layout

### Level 3: Configuration Understanding (30 minutes)

The behavior is heavily driven by XML configuration:

1. **`web.xml`** — filter chain order, servlet mappings, listeners
2. **`root-context.xml`** — which environment is active, what's imported
3. **Active `*-constants.xml`** — database URL, feature flags, app settings
4. **`servlet-context.xml`** — view resolvers, interceptors, resource mappings
5. **`tiles-definitions.xml`** — page layout structure

### Level 3: Cross-Cutting Concerns

Understand these once, apply everywhere:

| Concern | Mechanism |
|---|---|
| Authentication | Spring Security + custom AuthenticationProvider |
| Authorization | Spring Security ACL |
| Transaction management | `@Transactional` on Manager methods |
| GZIP compression | `GZIP2WayFilter` on service endpoints |
| Session management | Servlet sessions (Redis available but optional) |
| Logging | Log4j2 via SLF4J |
| Scheduling | Spring `@Scheduled` + XML scheduler configs |
| i18n | `LocalStrings_*.properties` in util1/ |

## Impact Analysis

Before changing code, assess impact:

```bash
# Who calls this method?
grep -rn "methodName" src/main/java/ --include="*.java"

# Who extends/implements this class?
grep -rn "extends ClassName\|implements InterfaceName" src/main/java/

# What Spring beans reference this?
grep -rn "beanName\|ClassName" src/main/webapp/WEB-INF/spring/

# What JSPs use this model attribute?
grep -rn "attributeName" src/main/webapp/WEB-INF/views/
```

## Key Relationships

```
ProductConstants ← read by → All Managers (feature flags)
root-context.xml → imports → activemq-context.xml, *-constants.xml
web.xml → defines → filter chain order, servlet mapping
SecurityConfig.java → configures → authentication, URL access rules
EffortContextListener → initializes → application startup tasks
SessionListener → handles → session lifecycle events
```

## Efficiency Tips

- Start reading from the controller layer — it's the thinnest and gives you the feature map
- Manager classes tell you the business rules
- DAO classes tell you the data model
- `ProductConstants` tells you what features are configurable
- The active `*-constants.xml` tells you the runtime behavior
