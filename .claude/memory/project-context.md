# Project Context

## Application Identity

- **Name**: EFFORT-Web
- **Type**: Enterprise Field Service Management Platform
- **Artifact**: `com.ayansys:effort:1.7.0` (WAR)
- **Java Version**: 21
- **Spring Version**: 6.1.6 (Spring MVC, not Spring Boot)
- **Servlet Spec**: Jakarta EE (Jakarta Servlet 6.0, NOT javax)

## Multi-Tenant Architecture

EFFORT serves multiple customers from a single codebase. Each deployment is configured for a specific customer/environment via XML constants files:

- `siemens-*-constants.xml` — Siemens environments
- `nd-*-constants.xml` — ND environments
- `ltfs-*-constants.xml` — LTFS environments
- `atos-*-constants.xml` — Atos environments
- `aeon-*-constants.xml` — Aeon environments
- and others

Environment switching is done by commenting/uncommenting lines in `root-context.xml`.

## Codebase Scale

- **~3000+ Java source files** across `src/main/java/`
- **20+ controllers** handling different feature areas
- **15+ managers** containing business logic
- **10+ DAOs** with extensive RowCallbackHandler usage
- **Hundreds of JSP views** with Apache Tiles layout
- **40+ Spring XML configuration files**
- **Minimal test coverage** (3 test files)

## Current Development Focus

Branch `Java21_Sonar_Changes_MB_Latest_2026-02-02_cursor_sonar_fixes` indicates:
- Migration to Java 21
- SonarQube compliance fixes
- Code quality improvements

## Key Runtime Dependencies

- MySQL database (primary data store)
- MongoDB (document store)
- Redis (optional session store, currently commented out)
- ActiveMQ Artemis (JMS messaging)
- Apache Tomcat (servlet container)

## Deployment Model

Traditional WAR deployment to Apache Tomcat. Not containerized by default. Environment configuration baked into WAR via XML constants selection.
