# AI MCP Lab — Debugging Story

This document captures the real bugs encountered during the build of ai-mcp-lab and how they were
diagnosed and fixed. Each issue maps to a real production-shaped lesson. The bugs are presented in
the order they were encountered — not severity order — because the sequence matters: fixing one
issue often reveals the next, and understanding that chain is the point.

---

## Issue 1 — Spring Boot scans the wrong package

**Symptom:** Application starts but REST controllers return 404 for all requests. No beans are
registered for `EmployeeController` or `DepartmentController`. No errors at startup.

**Diagnosis:** `@SpringBootApplication` triggers `@ComponentScan` on the package of the annotated
class. The main class had been placed in `com.mcplab.server` (alongside `McpServerConfig`) rather
than the root package `com.mcplab`. Spring scanned `com.mcplab.server.*` and found the config and
transport classes — but skipped `com.mcplab.controller`, `com.mcplab.repository`, and
`com.mcplab.tools` entirely.

**Fix:** Moved `AiMcpLabApplication.java` from `com.mcplab.server` to `com.mcplab`. The root
package now contains only the entry point; all sub-packages are below it and therefore included
in the component scan automatically.

**Lesson:** `@SpringBootApplication` is a placement contract as much as an annotation. It must
live at the root of your package hierarchy. Placing it in a sub-package silently excludes all
sibling packages. The symptom (controllers not found) looks like a routing issue but is a
wiring issue. Check the package first.

---

## Issue 2 — MySQL credentials don't work — wrong user

**Symptom:** Application starts but immediately throws `Access denied for user 'mcp_user'@'localhost'`.
HikariCP fails to create even one connection. All REST calls and MCP tool calls return errors.

**Diagnosis:** `application.properties` had been set up with a planned-but-not-yet-created
dedicated database user (`mcp_user`). The comment in the file even included the SQL to create it
(`CREATE USER 'mcp_user'@'localhost' IDENTIFIED BY 'your_password_here'`). The user had never
been created in MySQL.

**Fix:** Changed `spring.datasource.username` to `root` and `spring.datasource.password` to
the actual root password (`Pin@801698`). The production note ("never use root in production")
was preserved as a comment. The `mcp_user` was left in the comments as the intended eventual
configuration.

**Lesson:** A comment explaining what the credential *should* be is not the same as the
credential actually existing. Verify connectivity with the raw JDBC credentials before writing
any application code: `mysql -u mcp_user -p ai_mcp_lab < /dev/null` is a two-second check that
saves twenty minutes of debugging Spring startup logs.

---

## Issue 3 — Stdio MCP mode crashes because Tomcat tries to start

**Symptom:** Running `java -jar target/ai-mcp-lab.jar --spring.profiles.active=mcp` causes the
application to start Tomcat on port 8080, write Spring's startup banner and log lines to stdout,
and then fail when the AI client (MCP Inspector) receives non-JSON output and disconnects.

**Diagnosis:** Without a profile-specific override, Spring Boot always starts an embedded web
server (Tomcat). The Spring banner written to stdout at startup is enough to corrupt the JSON-RPC
framing before the first real message is exchanged.

**Fix:** Created `application-mcp.properties` with three settings:
1. `spring.main.web-application-type=none` — disables Tomcat entirely; the JVM becomes a quiet
   stdio process
2. `spring.main.banner-mode=off` — silences the Spring Boot banner (which goes to stdout)
3. `logging.pattern.console=` (empty) — suppresses the Logback console appender's output so
   SLF4J log lines do not appear on stdout

**Lesson:** In stdio MCP mode, stdout is sacred. Any non-JSON-RPC byte written there is a bug.
The three properties above are the minimum required to keep stdout clean. They should be the
first thing in `application-mcp.properties`, not an afterthought. If you add a new library that
logs to stdout at startup (some do), you will need to suppress it explicitly.

---

## Issue 4 — Stale JAR with old credentials keeps running

**Symptom:** After fixing the MySQL credentials in `application.properties`, the application
still uses the old credentials. The fix appears to have no effect.

**Diagnosis:** The JAR in `target/` was built before the credentials were corrected. When running
`java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar`, the JVM executes the embedded
`application.properties` from inside the JAR — not the source file in
`src/main/resources/application.properties`. Editing the source file changes the source, not
the deployed artifact.

**Fix:** `mvn clean package -DskipTests` after every source or properties change. `clean`
deletes the stale JAR; `package` recompiles and repackages with the new source files embedded.

**Lesson:** Spring Boot's fat JAR is a snapshot. The embedded `application.properties` is frozen
at build time. This is the same discipline as WAR deployment in your EFFORT-Web project: a
property change in `*-constants.xml` requires a rebuild and redeploy. With Spring Boot the
cycle is shorter (`mvn clean package` vs. a full WAR build), but the principle is identical.
Never assume a source change is live until you have rebuilt and restarted the JAR.

---

## Issue 5 — schema.sql wipes manually-inserted test data on every restart

**Symptom:** Employee and department records inserted via curl or MCP tool calls disappear
every time the application restarts. The database always returns to the five seed employees from
`data.sql`.

**Diagnosis:** `application.properties` had `spring.sql.init.mode=always`. On every startup,
Spring Boot runs `schema.sql` (which contains `DROP TABLE IF EXISTS` followed by `CREATE TABLE`)
and then `data.sql` (which inserts the five seed rows). All manually-inserted records are wiped.

**Fix:** Changed `spring.sql.init.mode` from `always` to `never`. With `never`, Spring Boot
skips both `schema.sql` and `data.sql` on startup. The database content persists across
restarts.

To re-seed from scratch when needed:
```bash
mysql -u root -p ai_mcp_lab < src/main/resources/schema.sql
mysql -u root -p ai_mcp_lab < src/main/resources/data.sql
```

**Lesson:** `spring.sql.init.mode=always` is appropriate for CI environments where a clean
database is required on every test run. For local development where you want data to persist,
use `never`. Document this distinction clearly — the two modes have opposite goals and
accidentally running `always` in a shared environment can wipe real data.

---

## Issue 6 — /sse returns 404 NoResourceFoundException in mcp-http mode

**Symptom:** Running with `--spring.profiles.active=mcp-http` and hitting `GET /sse` returns
HTTP 404 with error `No static resource sse`. The MCP SDK's `HttpServletSseServerTransportProvider`
is a registered bean, but the endpoint does not respond.

**Diagnosis:** Spring Boot registers `DispatcherServlet` at the URL pattern `/*` by default.
`HttpServletSseServerTransportProvider` extends `HttpServlet` directly — it is a raw servlet,
not a Spring MVC controller with `@RequestMapping`. Without an explicit
`ServletRegistrationBean`, any request to `/sse` lands in `DispatcherServlet`, which finds no
`@RequestMapping("/sse")` handler and returns 404.

**Fix:** Added `mcpServletRegistration()` in `McpServerConfig`:

```java
@Bean
@Profile("mcp-http")
public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServletRegistration(
        McpServerTransportProvider transportProvider) {
    log.info("[MCP] HTTP servlet registered at /sse and /mcp/messages");
    HttpServletSseServerTransportProvider httpProvider =
            (HttpServletSseServerTransportProvider) transportProvider;
    ServletRegistrationBean<HttpServletSseServerTransportProvider> registration =
            new ServletRegistrationBean<>(httpProvider, "/sse", "/mcp/messages");
    registration.setName("mcpServlet");
    registration.setLoadOnStartup(1);
    return registration;
}
```

`setLoadOnStartup(1)` ensures the servlet is initialised at startup rather than lazily on the
first request, so the SSE endpoint is immediately ready when the app becomes live.

**Lesson:** Spring MVC's `DispatcherServlet` intercepts everything unless you explicitly register
raw servlets at their own URL patterns. Third-party SDK components that extend `HttpServlet`
directly (rather than using Spring MVC abstractions) always need a `ServletRegistrationBean`.
This pattern is common in MCP, OAuth, and SAML libraries. When a URL returns 404 and the bean
is definitely in the context, check whether the handler is a raw servlet being routed to
`DispatcherServlet` instead of its own registration.

---

## Issue 7 — Port 8080 already in use when restarting

**Symptom:** Starting the JAR fails with `Web server failed to start. Port 8080 was already in
use`. The previous JAR process was stopped (Ctrl+C or kill), but the port is still bound.

**Diagnosis:** Ctrl+C sends SIGINT to the terminal foreground process, but in some configurations
the JVM catches it and enters a shutdown hook that takes time to release the port. In other
cases, the process was orphaned — started in background and not properly stopped.

**Fix:**
```bash
# Find and kill any running ai-mcp-lab process
pkill -f "java.*ai-mcp-lab"

# Verify the port is free
ss -lntp | grep 8080

# If something else is on 8080, identify it
ss -lntp | grep 8080 | awk '{print $NF}'
```

If the port is still held after `pkill`, wait 5 seconds for `TIME_WAIT` socket state to clear,
or change `server.port` in `application.properties` temporarily.

**Lesson:** Maintain awareness of background Java processes. In development, it is easy to
accumulate orphaned JVM instances — especially when starting from scripts, IDEs, or when
Ctrl+C is used in a terminal that has been closed. `pkill -f java.*ai-mcp-lab` is safe
(pattern-matched) and faster than hunting for PIDs. Add it to your restart workflow before
reaching for `lsof` and `kill`.

---

## Issue 8 — Cursor shows 0 tools after refactor

**Symptom:** After a refactoring session that reorganised the config classes, Cursor's MCP
integration reports "0 tools available" from the ai-mcp-lab server. The JAR builds and starts
successfully. MCP Inspector also shows 0 tools.

**Diagnosis:** The `mcpSyncServer()` bean in `McpServerConfig` had lost its
`@ConditionalOnBean(McpServerTransportProvider.class)` annotation during the refactor. In the
default profile (no MCP profile active), Spring attempted to create `McpSyncServer` but could
not find a `McpServerTransportProvider` bean — because neither the stdio nor the HTTP/SSE
transport beans are created in default mode. Spring failed to create `mcpSyncServer` silently
(it was conditional before the refactor and became unconditional after — a different failure
mode). In mcp profile, the transport bean existed but the server itself failed to initialise
cleanly because the registration order had changed.

**Fix:** Restored `@ConditionalOnBean(McpServerTransportProvider.class)` on `mcpSyncServer()`.
The condition serves two purposes: it prevents startup failures in default mode, and it
documents the dependency explicitly ("this bean requires a transport").

**Lesson:** `@ConditionalOnBean` is not just a workaround — it is a statement of intent. When
you refactor a `@Configuration` class, treat `@Conditional*` annotations as part of the API
contract of each bean method, not as decoration. Losing a conditional annotation is a silent
behaviour change: the bean may still be created (with wrong dependencies) or fail in a
mode-specific way that does not reproduce in your default test setup.

---

## Issue 9 — AI ignores tool, asks user "which Meera Iyer do you mean?"

**Symptom:** When asked "find Meera Iyer's employee record", Cursor's AI (Composer) asks the
user to clarify which Meera Iyer they mean, rather than calling `listEmployees` or
`getEmployeeById`. The tool is registered and MCP Inspector confirms it is visible.

**Diagnosis:** The tool description for `listEmployees` read:
> "Returns all employees. Optionally filter by departmentId..."

This description answers "WHAT the tool returns" but not "WHEN to use it". The AI does not
know it should call this tool when a user asks about a specific employee by name. The AI
assumed it needed more disambiguation from the user before making any tool calls.

**Fix:** Updated the `listEmployees` description to include explicit trigger language:
> "Returns all employees. **Use this tool whenever the user asks about an employee by name,
> email, or any attribute. Filter by departmentId to list employees in a specific department.**
> Returns an array of employee objects with id, name, email, departmentId, salary, createdAt."

After the change, the AI called `listEmployees` immediately when given a name, received the
full list, found Meera Iyer, and reported back — without asking for clarification.

**Lesson:** MCP tool descriptions are the AI's decision-making guide, not just API documentation.
The AI reads the description to decide IF and WHEN to call the tool. "What it returns" answers
the wrong question. "When to use it" is what the AI needs. Write descriptions in the second
person, imperative: "Use this tool when the user asks about X." Think of it as writing the rule
the AI should follow, not documenting the function signature.

---

## Issue 10 — Two MCP servers, AI keeps picking just one

**Symptom:** The project was extended to include a second MCP server (e.g., a reporting server
alongside the employee/department server). When both are registered in Cursor's MCP config, the
AI consistently picks only one server and ignores the other, even for queries that should
clearly use the second server.

**Diagnosis:** Both servers exposed tools with generic names like `listEmployees`, `getStatus`,
and `createRecord`. To the AI, the two servers looked functionally identical — a `listEmployees`
tool from server A and a `listEmployees` tool from server B are indistinguishable by name alone.
The AI resolved the ambiguity by consistently picking the first server it encountered. This is
"deduplication by effect": when two tools appear to do the same thing, one gets discarded.

**Fix:** Namespaced the tool names to make them server-specific:
- Server 1: `hr_listEmployees`, `hr_getEmployeeById`, `hr_getDepartmentStatus`
- Server 2: `report_listMonthlyStats`, `report_getHeadcountTrend`

The prefix is part of the tool name, not a separate field. The descriptions were also updated to
make the scope explicit: "HR employee database — use for individual employee queries."

**Lesson:** When multiple MCP servers expose semantically similar tools, namespace them by
function domain. The AI uses tool names and descriptions together to select a tool. Generic
names like `listEmployees` collide across servers; namespaced names like `hr_listEmployees` and
`report_listEmployees` are unambiguous. Plan your tool naming convention before wiring up the
second server — retrofitting namespacing after the AI has learned the old names can cause
temporary regressions in tool selection.

---

## General Debugging Principles Learned

**Isolate layers — test DB, JAR, transport, and AI client separately.** A failure that looks
like an AI tool-selection problem might be a database connectivity issue. A failure that looks
like a database issue might be a stale JAR. Verify each layer independently before assuming the
problem is in the one you most recently changed.

```bash
# Layer 1 — DB: is MySQL reachable with the right credentials?
mysql -u root -p ai_mcp_lab -e "SELECT COUNT(*) FROM Employees;"

# Layer 2 — JAR: does the fat JAR start cleanly?
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
# Watch for: HikariPool started, Tomcat started on port 8080, [MCP] markers

# Layer 3 — Transport: does the SSE endpoint respond?
curl -i http://localhost:8080/sse

# Layer 4 — Protocol: does tools/list return 11 tools?
curl -s -X POST http://localhost:8080/mcp/messages \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | python3 -m json.tool

# Layer 5 — AI: connect Cursor or MCP Inspector and confirm tool count
```

**Read errors literally but verify with direct probes.** "Access denied for user 'mcp_user'"
literally means the user does not exist or the password is wrong — not a Spring configuration
issue. `curl` a REST endpoint before blaming the MCP layer. `ss -lntp | grep 8080` before
blaming the application for "not starting".

**Start simplest mode, layer complexity gradually.** Default mode (REST only, no MCP) is the
fastest to debug — it eliminates the transport and protocol from the failure space. Add
`--spring.profiles.active=mcp-http` once the REST layer is verified. Add the AI client last.
Each layer added narrows where the next failure can live.

**Production engineering is iterative — every fix surfaces the next issue. Normal, not
failure.** Issue 3 (Tomcat starts in stdio mode) was revealed by fixing Issue 2 (wrong
credentials — the app finally connected to MySQL and then failed at the next step). Issue 6
(404 on /sse) was revealed by fixing Issue 3 (the app finally ran in mcp-http mode and then
the first SSE request failed). This cascade is not a sign that the architecture is wrong. It
is the normal reveal-as-you-go nature of building a new integration. Document each issue as
you fix it — the next developer on a similar MCP project will hit the same issues in the same
order and will thank you.

---

See also: [ARCHITECTURE.md](ARCHITECTURE.md) — how the three operational modes relate to the issues above  
See also: [LOGGING.md](LOGGING.md) — the logging setup that made issues 2, 3, 4, and 8 diagnosable
