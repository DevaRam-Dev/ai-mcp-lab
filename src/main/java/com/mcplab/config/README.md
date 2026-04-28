# com.mcplab.config

## What this directory holds
Spring `@Configuration` classes that wire application infrastructure beans.

## Role in MCP Architecture
Spring Boot auto-configures most infrastructure (DataSource, JdbcTemplate, HikariCP) from
`application.properties`.  This package holds **manual overrides and cross-cutting concerns**
that auto-configuration cannot infer — e.g. custom connection-pool tuning, MCP transport
selection, CORS, and security rules.

```
application.properties
        │  (Spring Boot reads at startup)
        ▼
[ Auto-Configuration ]  ←  DataSource, JdbcTemplate, HikariCP wired automatically
        │
[ @Configuration classes here ]  ←  override / extend auto-config where needed
        │
[ McpServer + Tools ]  ←  consume the configured beans
```

**Analogy for Spring MVC developers:**
- Your `root-context.xml` + `servlet-context.xml` XML bean definitions
  → replaced by `@Configuration` classes here + `application.properties`.
- One `@Bean` method = one `<bean id="..." class="...">` XML element.

## Example files that will go here
| File | Purpose |
|------|---------|
| `DataSourceConfig.java` | Custom HikariCP pool settings (max pool size, timeouts) beyond defaults |
| `McpTransportConfig.java` | Selects stdio vs SSE transport; registers the `McpServer` bean |
| `SecurityConfig.java` | Spring Security rules — restrict MCP endpoint access if using SSE |
| `WebConfig.java` | CORS config, message converters, interceptors |
