# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## AI Development Context

You are assisting in this project as a **Senior Solution Architect and Java Full Stack Developer**.

Focus on enterprise-level Java development, clean architecture, and production-grade best practices. When generating or modifying code, follow the layered architecture, coding standards, and security guidelines defined below.

---

## Project Overview

**EFFORT-Web** — A large-scale enterprise Java web application (WAR) built with Spring Framework 6.1.6 on Java 21. The project uses XML-based Spring configuration (not Spring Boot). It serves as a multi-tenant field service management platform with customer-specific environment configurations.

- **Group/Artifact:** `com.ayansys:effort:1.7.0`
- **Packaging:** WAR (deployed to Apache Tomcat)
- **Java:** 21
- **Spring:** 6.1.6 (Spring MVC, Spring Security 6.1.9, Spring ORM, Spring JMS, Spring AOP)
- **Jakarta EE:** Uses Jakarta namespace (`jakarta.servlet`, `jakarta.mail`, etc.) — NOT javax

---

## Technology Stack

**Backend:** Java 21, Spring MVC, Spring Core, Spring Security, Spring JDBC, Spring JMS, Spring AOP

**Frontend:** JSP, JSTL, HTML, CSS, JavaScript, jQuery, Bootstrap 5, FontAwesome 6, Select2, Apache Tiles

**Database:** MySQL (primary, via HikariCP), MongoDB (Spring Data MongoDB), Redis (session store via Lettuce)

**Server:** Apache Tomcat (WAR deployment)

**Build Tool:** Maven

**Messaging:** ActiveMQ Artemis (JMS)

**Reporting:** JasperReports

**Code Quality:** SonarQube (`sonar-maven-plugin 3.10.0`), JaCoCo (`0.8.11`)

**Security Libraries:** OWASP ESAPI, Spring Security ACL, JWT (jjwt + java-jwt)

**Logging:** Log4j2

---

## Build Commands

```bash
# Compile
mvn compile

# Package as WAR
mvn package

# Clean build
mvn clean package

# Run SonarQube analysis
mvn sonar:sonar

# Generate JaCoCo coverage report (after tests)
mvn verify

# Skip tests during build
mvn package -DskipTests
```

There are very few tests (`src/test/java/` has only 3 files). No established test suite to run.

---

## Architecture

### Design Patterns

- MVC (Model-View-Controller)
- DAO (Data Access Object)
- Service/Manager Layer
- Factory Pattern

### Layered Architecture

```
Controller Layer → Manager/Service Layer → DAO Layer → Database
                                                     ↕
View Layer (JSP) ← Controller ← Tiles Layout
```

**Controller → Manager → DAO → Database → JSP View**

### Layer Structure

- **`controller/`** — Spring MVC controllers handling web requests. URL mappings: `/web/`, `/service/`, `/custService/`, `/onDemand/`, `/report/`, `/map/`, `/workFlow/`, `/configure/`, `/employeeService/`.
- **`manager/`** — Business logic layer (equivalent to "Service" in standard Spring). Contains `WebManager`, `SyncManager`, `ConfiguratorManager`, `MailService`, bulk uploaders, etc.
- **`dao/`** — Data access layer using Spring JDBC (not JPA/Hibernate). Contains `rowcallbackhandler/` sub-package for result set mapping.
- **`service/`** — Small package with JWT/Superset token services only (not the primary service layer).

### Base Packages

- **`com.ayansys.effort`** — Main application code (controllers, managers, DAOs, models, config)
- **`com.spoors.effort.filter`** — Servlet filters (GZIP, charset encoding, XSS, security headers, concurrent login)
- **`com.kb.authentication.provider`** — Custom authentication provider

### Spring Configuration (XML-based)

All Spring config is in `src/main/webapp/WEB-INF/spring/`:

- **`root-context.xml`** — Root application context. Component-scans `com.ayansys.effort`. Imports `activemq-context.xml`. Loads environment-specific constants via `context:property-placeholder`.
- **`appServlet/servlet-context.xml`** — DispatcherServlet config.
- **`constants.xml` + `all-constants.xml`** — Shared properties across all environments.
- **`*-constants.xml`** — Per-environment/customer DB connection strings, feature flags, and settings. **To switch environments**, uncomment the appropriate `context:property-placeholder` line in `root-context.xml`.
- **`schedulars*.xml`** — Scheduled task definitions (customer-specific scheduler configs).
- **`tiles-context.xml` / `tiles-definitions.xml`** — Apache Tiles view layout definitions.
- **`jasper-views.xml`** — JasperReports view configuration.
- **`acl-context.xml`** — Spring Security ACL config.
- **`activemq-context.xml`** — JMS/ActiveMQ (Artemis) messaging config.
- **`redis-bean.xml`** — Redis session/cache config (Spring Session with Lettuce driver).

### Key Supporting Packages

- **`model/`** / **`beans/`** — Domain model POJOs and DTOs
- **`config/`** — Java-based config (`SecurityConfig.java`, `MetricsConfig.java`)
- **`scheduling/`** — Scheduled task implementations
- **`handler/`** — Exception and event handlers
- **`interceptor/`** — Spring MVC interceptors
- **`util/` / `util1/`** — Utilities, listeners, i18n property files (`LocalStrings_*.properties`)
- **`expression/`** — Custom expression/formula evaluation
- **`mongo/`** — MongoDB data access
- **`plugin/`** — Plugin system for extensibility
- **`route/`** — Route/trip planning logic
- **`inapp/`** — In-app notifications
- **`remote/`** — Remote system integrations
- **`bi/`** — Business intelligence features
- **`custom/`** — Custom entity handling
- **`acl/`** — Access control list implementation
- **`validators/`** — Input validation logic
- **`setting/`** — `ProductConstants` bean and app settings
- **`Jms/`** — JMS message producers/consumers (ActiveMQ Artemis)
- **`task/`** — Async task execution
- **`context/`** — Application context listeners (`EffortContextListener`)
- **`gsp/`** / **`gotym/`** — Integration modules

### Frontend

JSP views in `src/main/webapp/WEB-INF/views/` with Apache Tiles layout. Static resources in `src/main/webapp/resources/`.

### External Integrations

MySQL (HikariCP), MongoDB, Redis, ActiveMQ Artemis, Google Cloud Storage, Google Maps, Google Secret Manager, Xero (accounting), APNS/Pushy (push notifications), JasperReports, SFTP (sshj), Axis2 (SOAP web services).

---

## Coding Standards

- Follow SOLID principles
- Use dependency injection (constructor injection preferred for mandatory dependencies)
- Maintain separation of concerns — no business logic in controllers
- Use proper exception handling with meaningful error messages
- Use SLF4J/Log4j2 for logging (not `System.out.println`)
- Write readable and maintainable code
- Use parameterized queries to prevent SQL injection
- Use OWASP ESAPI for input encoding and output sanitization

---

## Performance Considerations

- Optimize database queries — avoid N+1 patterns
- Use HikariCP connection pooling (already configured)
- Implement caching when required (Redis available)
- Use pagination for large result sets
- GZIP compression is active on `/service/*`, `/custService/*`, `/onDemand/*` via `GZIP2WayFilter`

---

## Security Guidelines

- Validate all user inputs at controller level
- Use parameterized JDBC queries — never concatenate SQL strings
- Use OWASP ESAPI for encoding/validation
- Leverage Spring Security filter chain (configured in `web.xml`)
- Use proper authentication and authorization via Spring Security ACL
- Never expose sensitive information in error responses or logs

---

## Logging Standards

- Use Log4j2 (`src/main/resources/log4j2.properties`)
- Log at appropriate levels: ERROR for failures, WARN for recoverable issues, INFO for business events, DEBUG for development
- Include contextual information (user ID, request ID) in log messages
- Never log sensitive data (passwords, tokens, PII)

---

## Important Notes

- **Environment switching** is done by commenting/uncommenting `context:property-placeholder` lines in `root-context.xml` (line ~63). The currently active config is `new-test-constants.xml`.
- **No Spring Boot** — this is a traditional Spring MVC WAR application with XML config.
- **Spring Security** filter chain is configured in `web.xml` with `DelegatingFilterProxy`.
- **File uploads** use `StandardServletMultipartResolver` with ~1GB max file size.
- **SonarQube** and **JaCoCo** are configured for code quality analysis.
- **i18n** is supported via `LocalStrings_*.properties` files in `util1/` (en, fr, de, es, it, etc.).

---

## AI Expectations

When generating code:

- Follow the enterprise coding standards and layered architecture defined above
- Maintain clean architecture — respect Controller → Manager → DAO boundaries
- Suggest performance improvements where applicable
- Suggest refactoring opportunities for legacy code
- Identify potential security issues proactively
- Use Jakarta EE namespace (not javax) for all new code

---

## Claude Skills & Configuration

See `.claude/skills/` for specialized skill definitions, `.claude/memory/` for persistent project context, and `.claude/prompts/` for reusable prompt templates.

## Java Coding Standards & SonarQube Compliance

### 1. Readability & Maintainability (Junior-Friendly)
- **Prioritize Clarity:** Avoid "aggressive shortening" or complex one-liners. Ensure logic is easy for junior developers to follow.
- **Method Structure:** Keep methods focused. Avoid deep nesting and large method bodies.
- **Return Logic:** Return expressions directly without creating unnecessary temporary variables.
- **Generics:** Always use the diamond operator `<>` for generics.

### 2. Error Handling & Logging
- **No PrintStackTrace:** Never use `printStackTrace()`. Always use `Log.error("context message", e);`.
- **Nesting:** Avoid nested `try` blocks. Extract complex error-prone logic into helper methods.
- **Resources:** Always use **try-with-resources** for IO, JDBC, and any `AutoCloseable` resources.
- **Logging Levels:** Add logs after key statement levels (e.g., DB updates, API calls), but avoid "log spam" for every line.

### 3. Logic & Clean Code Rules (Sonar Best Practices)
- **Constants:** Reuse constants from `AppConstants.java`. **Do not** create duplicate string or numeric literals.
- **Parameters:** Do not reassign method parameters; treat them as effectively final.
- **Null Safety:** Always perform a null check before dereferencing an object.
- **Complexity:** Keep **Cognitive Complexity below 15**. If it exceeds this, refactor into smaller methods.
- **Cleanup:** Do not generate commented-out code, "TODOs" without tickets, or unused variables.

### 4. Project Context
- **Tech Stack:** Spring MVC with JdbcTemplate (not Spring Boot).
- **Database:** Ensure all JdbcTemplate queries follow project-standard SQL formatting.

## Workflow Rules
- **Plan First:** For any task affecting more than 2 files, always start in `/plan` mode.
- **Architectural Review:** Present a summary of planned changes and wait for user confirmation before executing file writes.
- **Context Awareness:** Before large refactors, use `ls -R` or `grep` to confirm project structure rather than reading all files.
