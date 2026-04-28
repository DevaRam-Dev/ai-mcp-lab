# Architecture Overview

## Layered Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    CLIENT (Browser / Mobile App)                  │
└──────────────────────────────┬───────────────────────────────────┘
                               │ HTTP
┌──────────────────────────────▼───────────────────────────────────┐
│                      FILTER CHAIN                                │
│  BinarySafeCharacterEncodingFilter → CharsetFilter →             │
│  GZIP2WayFilter → SpringSecurityFilterChain →                    │
│  AdditionalResponseHeadersFilter → ConcurrentLoginRedirectFilter │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│              SPRING MVC DISPATCHER SERVLET                        │
│  Interceptors → Controller Resolution → View Resolution          │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                     CONTROLLER LAYER                              │
│  WebController, WebExtendedController, OnDemandController,       │
│  EmployeeController, MapController, WorkFlowController,          │
│  BIController, CustomerConfigController, EntitySpecController,   │
│  SmsController, InAppController, ToolController                  │
│                                                                   │
│  Responsibilities: Input validation, request handling,            │
│  response formatting, view name resolution                        │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                     MANAGER LAYER (Business Logic)                │
│  WebManager, SyncManager, ConfiguratorManager, MailService,      │
│  SmsManager, DayPlanManager, EffortPluginManager,                │
│  WebSupportManager, WebExtensionManager, WebAdditionalSupport,   │
│  EntityBulkUploader, EmployeeBulkUploader, GoogleAccessToken     │
│                                                                   │
│  Responsibilities: Business rules, orchestration, transactions,   │
│  data transformation, cross-DAO coordination                      │
└──────────────┬─────────────┬─────────────────┬──────────────────┘
               │             │                 │
┌──────────────▼──┐  ┌──────▼──────┐  ┌───────▼─────────┐
│   DAO LAYER     │  │  MONGO      │  │  JMS/MESSAGING  │
│   (Spring JDBC) │  │  (MongoDB)  │  │  (ActiveMQ)     │
│                 │  │             │  │                  │
│  JdbcTemplate   │  │  MongoOps   │  │  JMS Template   │
│  RowCallback    │  │             │  │                  │
│  Handlers       │  │             │  │                  │
└───────┬─────────┘  └──────┬──────┘  └────────┬────────┘
        │                   │                   │
   ┌────▼────┐      ┌──────▼──────┐     ┌──────▼──────┐
   │  MySQL  │      │  MongoDB    │     │  ActiveMQ   │
   │         │      │             │     │  Artemis    │
   └─────────┘      └─────────────┘     └─────────────┘
```

## View Layer

```
Controller returns view name → TilesViewResolver → tiles-definitions.xml
    → Layout template (header, sidebar, content, footer)
    → Content JSP from WEB-INF/views/
    → Rendered HTML with Bootstrap 5, jQuery, FontAwesome 6
```

## Cross-Cutting Concerns

| Concern | Implementation |
|---|---|
| Security | Spring Security Filter Chain + ACL |
| Transactions | `@Transactional` on Manager methods |
| Logging | Log4j2 via SLF4J |
| Compression | GZIP2WayFilter (service endpoints) |
| Session | Servlet sessions (Redis optional) |
| Scheduling | Spring `@Scheduled` + XML scheduler configs |
| Metrics | Micrometer + Prometheus registry |
| Encoding | UTF-8 forced via CharsetFilter |
| i18n | LocalStrings_*.properties |

## Configuration Hierarchy

```
web.xml
  └── root-context.xml
        ├── Component scan: com.ayansys.effort
        ├── Import: activemq-context.xml
        ├── Property source: constants.xml + [env]-constants.xml + all-constants.xml
        ├── Bean definitions: ProductConstants, multipartResolver, dataSource, etc.
        └── appServlet/servlet-context.xml
              ├── View resolvers (Tiles, Jasper, JSP)
              ├── Interceptors
              └── Resource handlers
```
