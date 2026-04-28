---
name: codebase-navigation
description: Skill for efficiently navigating and understanding the EFFORT-Web codebase structure
---

# Codebase Navigation Skill

## Purpose

Enable efficient navigation of the 3000+ Java file codebase to quickly locate relevant code for any task.

## When to Use

- Finding where a feature is implemented
- Tracing request flow from URL to database
- Locating configuration for a specific behavior
- Understanding package organization

## Quick Navigation Guide

### By URL Pattern → Controller

| URL Pattern | Controller Class |
|---|---|
| `/web/*` | `WebController` |
| `/web/ext/*` | `WebExtendedController` |
| `/service/*` | Check multiple controllers (mobile API) |
| `/custService/*` | `CustomEntityServiceController` |
| `/onDemand/*` | `OnDemandController` |
| `/report/*` | Report controllers |
| `/map/*` | `MapController` |
| `/workFlow/*` | `WorkFlowController` |
| `/configure/*` | `CustomerConfigController`, `CustomerConfigActionController` |
| `/employeeService/*` | `EmployeeController` |
| `/bi/*` | `BIController` |

### By Feature → Package

| Feature Area | Package(s) |
|---|---|
| Business logic | `manager/` |
| Database access | `dao/`, `dao/rowcallbackhandler/` |
| Domain models | `model/`, `beans/` |
| Scheduled jobs | `scheduling/` |
| Email/SMS | `manager/MailService`, `manager/SmsManager` |
| File operations | `util/` |
| Plugin system | `plugin/` |
| Workflow | `controller/WorkFlowController` |
| MongoDB operations | `mongo/` |
| JMS messaging | `Jms/` |
| Security/ACL | `acl/`, `config/SecurityConfig.java` |
| Custom entities | `custom/` |
| Route planning | `route/` |
| Reporting | `resources/reports/*.jrxml` |
| App settings | `setting/ProductConstants` |
| Input validation | `validators/` |

### By Configuration

| Config Area | File Location |
|---|---|
| Root Spring context | `WEB-INF/spring/root-context.xml` |
| Servlet config | `WEB-INF/spring/appServlet/servlet-context.xml` |
| Environment properties | `WEB-INF/spring/*-constants.xml` |
| Web filters/servlets | `WEB-INF/web.xml` |
| Tiles layout | `WEB-INF/spring/tiles-definitions.xml` |
| Scheduler config | `WEB-INF/spring/schedulars*.xml` |
| Security | `config/SecurityConfig.java` + `acl-context.xml` |
| Logging | `src/main/resources/log4j2.properties` |
| Maven build | `pom.xml` |

### Tracing a Request

To trace a feature from URL to database:

1. **Find the controller method**: Search for `@RequestMapping` with the URL path
2. **Find the manager call**: Look at what the controller delegates to
3. **Find the DAO call**: Look at what the manager calls
4. **Find the SQL**: Look at the DAO method's query
5. **Find the view**: Look at the returned view name and match in `tiles-definitions.xml`

### Search Strategies

```bash
# Find controller handling a URL
grep -r "RequestMapping.*\/web\/employees" src/main/java/

# Find where a DAO method is called
grep -r "employeeDao.getById" src/main/java/

# Find Spring bean definitions
grep -r "bean id=\"" src/main/webapp/WEB-INF/spring/

# Find a specific property usage
grep -r "productId" src/main/webapp/WEB-INF/spring/
```
