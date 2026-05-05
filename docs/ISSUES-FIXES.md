# AI MCP Lab — Issues & Fixes Reference

This document captures every real bug encountered during the build of ai-mcp-lab, in
chronological order. Each issue follows the same 4-field pattern: **Symptom** (what you see),
**Cause** (why it happens), **Fix** (the exact change), **Lesson** (the generalizable principle).
Use this as a debugging cheat sheet when symptoms recur.

---

## Quick Index

| # | Category | One-line symptom summary |
|---|---|---|
| [1](#issue-1--spring-boot-scans-the-wrong-package) | Package Scanning | Controllers not found; REST returns 404 despite correct `@RequestMapping` |
| [2](#issue-2--mysql-credentials-dont-work) | Database | `Access denied for user 'mcp_user'@'localhost'` — HikariCP fails at startup |
| [3](#issue-3--mcp-stdio-mode-crashes--garbles-output) | MCP Stdio | Cursor MCP panel errors immediately; garbled JSON-RPC due to Tomcat/banner on stdout |
| [4](#issue-4--stale-jar-after-source-or-property-changes) | Build / JAR | Code or property changes have no effect after JAR restart |
| [5](#issue-5--schemaSql-wipes-manually-inserted-test-data-on-restart) | SQL Init | Manually-inserted rows vanish on every JAR restart |
| [6](#issue-6--get-sse-returns-404-no-static-resource-sse) | MCP HTTP | `GET /sse` returns 404 `NoResourceFoundException` in mcp-http mode |
| [7](#issue-7--port-8080-already-in-use-on-restart) | Port | `Port 8080 was already in use` — new JAR cannot start |
| [8](#issue-8--cursor-shows-0-tools-after-adding-profile-to-transport-bean) | Spring Profiles | Cursor MCP panel shows server connected but 0 tools |
| [9](#issue-9--ai-ignores-tool-and-asks-for-clarification) | AI Routing | AI asks "which Meera Iyer do you mean?" instead of calling `listEmployees` |
| [10](#issue-10--two-mcp-servers-ai-consistently-picks-just-one) | MCP Multi-Server | Both servers active in `~/.cursor/mcp.json`; AI always routes to one |

---

## Issue 1 — Spring Boot Scans the Wrong Package

### Symptom

At startup, beans were not found. `EmployeeController`, `DepartmentController`,
`EmployeeRepository`, `DepartmentRepository`, and the tool beans were all absent from the Spring
context. REST endpoints returned 404 despite correct `@RequestMapping` annotations on the
controllers. No obvious error message pointed at the root cause — the application started
successfully, it just had no beans.

### Cause

`AiMcpLabApplication.java` was placed in `com.mcplab.server` alongside `McpServerConfig`. Spring
Boot's `@SpringBootApplication` annotation includes `@ComponentScan`, which scans only the
package containing the annotated class and its sub-packages. With the main class in
`com.mcplab.server`, Spring scanned `com.mcplab.server.*` and found `McpServerConfig` and
`LoggingStdioTransportProvider` — but skipped every sibling package:

```
com.mcplab.controller  ← invisible (sibling, not sub-package)
com.mcplab.repository  ← invisible
com.mcplab.tools       ← invisible
com.mcplab.server      ← scanned (main class lives here)
```

### Fix

Moved `AiMcpLabApplication.java` to the root package `com.mcplab`. With the main class at the
root, `@ComponentScan` covers the entire tree:

```
com.mcplab                ← scanned (main class lives here)
com.mcplab.controller     ← scanned (sub-package)
com.mcplab.repository     ← scanned (sub-package)
com.mcplab.tools          ← scanned (sub-package)
com.mcplab.server         ← scanned (sub-package)
```

No annotation changes were needed — the class itself is unchanged. The fix is purely positional.

### Lesson

The package containing `@SpringBootApplication` is the component-scan root. Every package
outside that tree is invisible, producing the same symptom as a missing `@Component` or `@Service`
annotation — but the actual problem is placement, not annotation. Plan your package hierarchy
around the main class location before creating sub-packages. If the main class genuinely cannot
be at the root, use `@SpringBootApplication(scanBasePackages = {"com.mcplab"})` explicitly.

---

## Issue 2 — MySQL Credentials Don't Work

### Symptom

Application failed to start with:

```
HikariPool-1 - Exception during pool initialization.
com.mysql.cj.jdbc.exceptions.CommunicationsException: ...
  Access denied for user 'mcp_user'@'localhost' (using password: YES)
```

HikariCP could not create a single connection. All REST endpoints and MCP tool calls failed
because the `JdbcTemplate` bean had no working `DataSource` to work with.

### Cause

`application.properties` was written with a planned-but-not-yet-created dedicated application
user:

```properties
spring.datasource.username=mcp_user
```

The comment in the file even included the SQL to create that user (`CREATE USER 'mcp_user'@'localhost'
IDENTIFIED BY 'your_password_here'`). The user had never been created in MySQL. The database was
only accessible as `root`.

### Fix

Updated `application.properties` to use the actual working credentials:

```properties
spring.datasource.username=root
spring.datasource.password=Pin@801698
```

The production-stance comment ("never use root; create a grant-restricted user") was kept in the
file as a reminder. The `mcp_user` SQL was left in comments as the intended eventual configuration
for a production deployment.

### Lesson

Match config to the actual environment, not to the intended future environment. For a personal
lab POC, using `root` with working credentials is faster and safer than configuring a user that
does not yet exist. Document the production stance clearly (dedicated user, minimal grants,
password from an env variable or secret manager) even when you do not implement it for the lab.
And verify credentials with a raw MySQL CLI call before debugging the application:

```bash
mysql -uroot -pPin@801698 ai_mcp_lab -e "SELECT 1;"
```

If that fails, the problem is credentials — not Spring Boot, not HikariCP, not application code.

---

## Issue 3 — MCP Stdio Mode Crashes / Garbles Output

### Symptom

When Cursor spawned the JAR in stdio mode (`--spring.profiles.active=mcp`), the MCP connection
failed immediately. Cursor's MCP panel showed the server as erroring out. MCP Inspector showed
an empty "Messages" tab and a flood of non-JSON output in the "Error output" panel. The Spring
startup banner and Tomcat initialization logs appeared on stdout, making the JSON-RPC stream
unreadable.

### Cause

In MCP stdio mode, **stdout is the exclusive wire protocol channel**. Every byte on stdout is
interpreted by the AI client as part of a JSON-RPC frame. Without a profile-specific override,
Spring Boot's default startup behavior writes to stdout:

1. The Spring Boot banner (`  .   ____          _            ...`)
2. Tomcat port binding log lines
3. Any INFO-level SLF4J logs from the Logback console appender

All of these preceded the first JSON-RPC frame and corrupted the stream. The AI client received
what looked like garbage, tried to parse it as JSON-RPC, failed, and disconnected.

Additionally, Tomcat started and bound port 8080 even though stdio mode has no use for a web
server — wasting memory and potentially conflicting with other processes.

### Fix

Created `src/main/resources/application-mcp.properties` with four settings:

```properties
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=
logging.level.root=WARN
logging.level.org.springframework=WARN
logging.level.com.mcplab=INFO
```

- `web-application-type=none` — prevents Tomcat from starting entirely; the JVM becomes a quiet
  stdio process
- `banner-mode=off` — suppresses the Spring Boot ASCII banner written to stdout at startup
- `logging.pattern.console=` (empty) — blanks the Logback console appender output so SLF4J INFO
  logs do not reach stdout
- `logging.level.root=WARN` — suppresses all library-level INFO logging

This profile is activated automatically when the JAR is started with
`--spring.profiles.active=mcp`. Wire messages from `LoggingStdioTransportProvider` still appear
on **stderr** (using `System.err.println()` directly, bypassing the Logback appender).

### Lesson

In MCP stdio mode, stdout is sacred — it carries the JSON-RPC protocol. Any non-protocol byte
on stdout corrupts the stream and causes the AI client to disconnect without a meaningful error
message. Always suppress Tomcat, banners, and all stdout-bound logging in the stdio profile.
Use a dedicated `application-mcp.properties` to enforce this cleanly. Route observability output
(build markers, wire messages) to **stderr** via `System.err.println()` directly — not through
SLF4J, whose console appender targets stdout by default.

---

## Issue 4 — Stale JAR After Source or Property Changes

### Symptom

Source code was edited — a new method added, a tool description updated, or a property changed
in `application.properties`. After restarting the JAR (`Ctrl+C` + `java -jar target/...`), the
changes were invisible. The JAR continued behaving exactly as before. The new log lines never
appeared; the updated property values were not in effect.

### Cause

Spring Boot's fat JAR is a **snapshot baked at build time**. After `mvn package`, all compiled
`.class` files and all resources (including `application.properties`, `application-mcp.properties`,
`schema.sql`, `data.sql`) are copied into the JAR at `target/ai-mcp-lab-0.0.1-SNAPSHOT.jar`.
Editing the source files in `src/main/resources/` or `src/main/java/` changes the source on
disk — it does not change the JAR. The JAR reads its embedded copy.

This is the Spring Boot equivalent of editing a WAR's source without redeploying — common
knowledge in traditional Spring MVC (`EFFORT-Web`), but easy to forget when the JAR looks like
a simple file you can just re-run.

### Fix

After any source or properties change, rebuild before restarting:

```bash
mvn clean package -DskipTests
```

Then restart the JAR. Use the build markers added to `McpServerConfig` to confirm at runtime
that the new version is running:

```bash
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http 2>&1 \
  | grep "McpServerConfig loaded"
```

The `@PostConstruct` marker logs `McpServerConfig loaded — profile-active beans will follow @ <Instant>`
with the JVM startup timestamp. If the timestamp does not match your most recent build, the JAR
is stale.

### Lesson

Properties files live **inside** the JAR after `mvn package`. Editing the source has no effect
until you rebuild. Build markers are cheap and effective — log a startup timestamp so you can
always confirm which JAR version is running. Adopt `mvn clean package -DskipTests` as a reflex:
after any change, rebuild first, then restart. The `clean` goal is important — without it,
incremental compilation can miss resource changes.

---

## Issue 5 — schema.sql Wipes Manually-Inserted Test Data on Restart

### Symptom

New records inserted during a test session — a Marketing department, several test employees,
salary updates — disappeared every time the JAR restarted. The database always came back with
exactly the five seed employees (Ravi Kumar, Priya Sharma, Amit Patel, Kiran Nair, Sunita Reddy)
and three original departments from `data.sql`. No error was logged; the wipe was silent.

### Cause

`application.properties` originally had:

```properties
spring.sql.init.mode=always
```

With `mode=always`, Spring Boot's `SqlInitializationAutoConfiguration` runs `schema.sql` and
`data.sql` on **every startup**, against any datasource type (including MySQL). The first line of
`schema.sql` is:

```sql
DROP TABLE IF EXISTS `Employees`;
DROP TABLE IF EXISTS `Departments`;
```

This wiped all manually-inserted data before re-creating tables and re-seeding from `data.sql`.
The application behaved correctly for a clean lab setup — it just did not preserve state.

### Fix

Changed `spring.sql.init.mode` to `never` in `application.properties`:

```properties
spring.sql.init.mode=never
```

With `never`, Spring Boot skips both `schema.sql` and `data.sql` on startup. The database
content persists across restarts. To re-seed from scratch when needed, run the scripts manually:

```bash
mysql -uroot -pPin@801698 ai_mcp_lab < src/main/resources/schema.sql
mysql -uroot -pPin@801698 ai_mcp_lab < src/main/resources/data.sql
```

### Lesson

`spring.sql.init.mode=always` is appropriate for CI environments where reproducibility matters
more than persistence. `never` is the right default for local development where you want data
to survive restarts. Choose consciously per environment. The `always`/`never` decision should be
explicit in every profile's properties — do not let it be accidentally inherited from the base
`application.properties`. If you need per-profile behaviour, override in
`application-{profile}.properties`.

---

## Issue 6 — GET /sse Returns 404 "No Static Resource sse"

### Symptom

In `mcp-http` profile, the SSE endpoint was unreachable:

```bash
$ curl -i http://localhost:8080/sse
HTTP/1.1 500 Internal Server Error
# or
HTTP/1.1 404 Not Found
{"timestamp":"...","status":404,"error":"Not Found","message":"No static resource sse."}
```

The `McpSyncServer` bean was created (visible in logs), and `HttpServletSseServerTransportProvider`
was constructed — but `/sse` returned nothing useful. MCP Inspector's connection attempt failed
immediately.

### Cause

Spring Boot registers `DispatcherServlet` at the URL pattern `/*` by default — it intercepts
**all** incoming HTTP requests. `HttpServletSseServerTransportProvider` extends `HttpServlet`
directly; it is a raw servlet, not a Spring MVC controller annotated with `@RequestMapping`.
Without an explicit `ServletRegistrationBean`, the servlet was created as a Spring bean but
never mapped to any URL. `DispatcherServlet` intercepted every request to `/sse`, found no
`@RequestMapping("/sse")` handler, and returned 404 or 500.

### Fix

Added an explicit `ServletRegistrationBean` in `McpServerConfig.java`, gated to the `mcp-http`
profile:

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

`setLoadOnStartup(1)` ensures this servlet initialises before `DispatcherServlet` (whose default
`loadOnStartup` is -1, meaning lazy). Once registered, Tomcat routes `/sse` and `/mcp/messages`
directly to the MCP transport servlet, bypassing `DispatcherServlet` entirely.

### Lesson

`DispatcherServlet` silently intercepts everything mapped to `/*` — including paths that should
belong to other servlets. Any third-party component that extends `HttpServlet` directly (MCP SDK,
OAuth providers, SAML handlers) needs an explicit `ServletRegistrationBean` with its own URL
patterns. Without this, `DispatcherServlet` wins the path race and returns 404 or 500 because
it has no matching `@RequestMapping`. When a URL returns 404 and the bean is in the context,
check whether the handler is a raw servlet that needs its own registration.

---

## Issue 7 — Port 8080 Already In Use on Restart

### Symptom

Starting the JAR after a `Ctrl+C` failed with:

```
Web server failed to start. Port 8080 was already in use.
Action: Identify and stop the process that's listening on port 8080, or configure this
application to listen on another port.
```

The application could not start at all.

### Cause

The previous JVM process was not fully terminated before the new one started. `Ctrl+C` sends
`SIGINT` to the foreground process, but the JVM may take a moment to release the socket — especially
if shutdown hooks or connection pool cleanup runs. In some configurations (IDE run, background
scripts, or a missed `Ctrl+C`), the previous process survives entirely.

### Fix

Before every restart, verify the port is free:

```bash
pkill -f "ai-mcp-lab"   # terminate the old process
sleep 1                  # allow socket TIME_WAIT to clear
ss -lntp | grep 8080    # must return nothing
```

Only start the new JAR once the port check is clean. If the port is still bound after `pkill`,
wait a few more seconds — TCP `TIME_WAIT` lasts up to 60 seconds but typically clears within 1–2.

### Lesson

Always verify that the previous process is terminated before starting a new one. Have this
three-line sequence as a development reflex — faster to run than to debug a startup failure:

```bash
pkill -f "ai-mcp-lab"; sleep 1; ss -lntp | grep 8080
```

Do not rely on visual confirmation that the terminal "looks done" — `Ctrl+C` triggers a shutdown,
but the OS socket release is asynchronous. `ss -lntp | grep 8080` is the definitive check;
empty output means the port is free.

---

## Issue 8 — Cursor Shows 0 Tools After Adding @Profile to Transport Bean

### Symptom

After refactoring `McpServerConfig` to gate the transport beans with `@Profile("mcp")` and
`@Profile("mcp-http")`, Cursor's MCP panel showed the server as connected but listed **0 tools**.
The server appeared healthy but could not execute any tool calls. MCP Inspector confirmed the
`tools/list` response was empty.

### Cause

`mcpSyncServer` depends on a `McpServerTransportProvider` bean to exist. In the **default profile**
(no `mcp` or `mcp-http` flag), neither `@Profile("mcp")` bean nor `@Profile("mcp-http")` bean
fires — so no `McpServerTransportProvider` is in the Spring context. Without a transport,
`mcpSyncServer()` was still being called by Spring but could not build a functional server.
The result was a partially-constructed MCP server with no transport and no registered tools.

The failure was **silent** — no exception, no startup error, just an empty tools list.

### Fix

Added `@ConditionalOnBean(McpServerTransportProvider.class)` to the `mcpSyncServer` bean method:

```java
@Bean
@ConditionalOnBean(McpServerTransportProvider.class)
public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
                                   DatabaseStatusTool databaseStatusTool,
                                   EmployeeTool employeeTool,
                                   DepartmentTool departmentTool) {
    log.info("[MCP] MCP server built — {} tools registered", 11);
    return McpServer.sync(transportProvider)
            .serverInfo("ai-mcp-lab", "1.0.0")
            // ... 11 tool registrations
            .build();
}
```

Now:
- **Default profile**: no transport bean → `mcpSyncServer` is skipped entirely (graceful, no error)
- **mcp profile**: `stdioTransportProvider` bean exists → `mcpSyncServer` is created with stdio transport → 11 tools registered
- **mcp-http profile**: `httpSseTransportProvider` bean exists → `mcpSyncServer` is created with HTTP/SSE transport → 11 tools registered

### Lesson

Profile-conditional beans create **implicit dependencies** on other profile-conditional beans.
Use `@ConditionalOnBean` to make those dependencies explicit and ensure downstream beans are only
created when their dependencies exist. Without the condition, you get partial context construction
with no error and no log line telling you what went wrong — exactly the hardest class of bug to
diagnose. The rule: if bean A is `@Profile`-gated, every bean that requires A should be
`@ConditionalOnBean(A.class)`.

---

## Issue 9 — AI Ignores Tool and Asks for Clarification

### Symptom

User typed: `"tell about Meera Iyer"`. The expected behavior was for Cursor's AI to call
`listEmployees` (or similar), search the result for Meera Iyer, and report back. Instead, the
AI asked: `"Which Meera Iyer do you mean? Could you provide more details?"` — and never invoked
any MCP tool.

### Cause

MCP tool descriptions described **what the tool returns**, not **when to use it**:

```
"Returns all employees. Optionally filter by departmentId..."
```

The AI's tool-routing logic uses the description as a relevance signal against the user's query.
The word "employees" was in the description, but the user's query contained neither "employee"
nor any other word from the description. The AI could not connect "Meera Iyer" (a person's
name) to "listEmployees" (a data-retrieval function), so it fell back to asking the user for
clarification rather than making a potentially wrong tool call.

### Fix

Two complementary fixes:

**(a) Prompt-side — be explicit in queries:**

```
"Tell me about employee Meera Iyer"        → triggers listEmployees correctly
"Using ai-mcp-lab-http, find Meera Iyer"  → anchors to the specific server and intent
```

**(b) Description-side — add WHEN-to-use trigger phrases:**

Rewrite tool descriptions to lead with the trigger condition rather than the return shape:

```
Before: "Returns all employees. Optionally filter by departmentId..."
After:  "Use this tool when the user asks about any employee, person, or team member by
         name, email, department, or salary — even if they don't use the word 'employee'.
         Returns an array of employee objects with id, name, email, departmentId, salary."
```

### Lesson

MCP tool descriptions are the AI's routing menu. Write them as **WHEN-to-use instructions**
(trigger phrases) rather than WHAT-it-returns data-shape descriptions. The AI matches user intent
to tool descriptions — not to tool names and not to return types. Descriptions that lead with
trigger conditions (`"Use this tool when the user asks about..."`) produce more reliable routing
than descriptions that lead with output shape (`"Returns a list of..."`). Test routing with
ambiguous queries before the demo — if the AI asks for clarification instead of calling a tool,
the description is the first thing to fix.

---

## Issue 10 — Two MCP Servers Active, AI Consistently Picks Just One

### Symptom

Both `ai-mcp-lab-stdio` and `ai-mcp-lab-http` were enabled simultaneously in
`~/.cursor/mcp.json`. The AI consistently routed all tool calls to one server (typically the
first alphabetically) regardless of which server was started most recently or which was more
explicitly referenced in the prompt. The second server appeared healthy in Cursor's MCP panel
(green dot, 11 tools) but received no actual tool calls.

```json
{
  "mcpServers": {
    "ai-mcp-lab-http": {
      "url": "http://localhost:8080/sse"
    },
    "ai-mcp-lab-stdio": {
      "command": "java",
      "args": ["-jar", "/home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab/target/ai-mcp-lab-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=mcp"]
    }
  }
}
```

### Cause

Both servers expose identical tool names (`listEmployees`, `getDatabaseStatus`, etc.) with
identical descriptions and identical underlying data. The AI applies **deduplication-by-effect**:
when two tools appear to do the same thing, it routes to whichever appears first in its tool
catalog. Cursor resolves the ordering alphabetically by server name — `ai-mcp-lab-http` sorts
before `ai-mcp-lab-stdio`, so HTTP/SSE won all routing decisions.

This is correct AI behavior, not a defect. The AI does not know or care about transport modes —
it sees `listEmployees` twice and picks one.

### Fix

This is an expected behavior pattern, not a code bug. Three practical approaches:

**(a) Disable one server during a demo or test session** — only one server active at a time
eliminates ambiguity entirely:

```
In Cursor → Settings → Tools & MCPs:
  ✅ ai-mcp-lab-http  (on)
  ☐  ai-mcp-lab-stdio (off)
```

**(b) Anchor the prompt explicitly to a server:**

```
"Using ai-mcp-lab-stdio, list all employees"
"@ai-mcp-lab-http get department by ID 2"
```

**(c) Give each server distinct tool name prefixes** — when running two servers that serve
different data or different environments, namespace the tool names to make them non-identical:

```
Server A: hr_listEmployees, hr_createEmployee
Server B: rpt_listMonthlyHeadcount, rpt_getSalaryBand
```

### Lesson

When multiple MCP servers expose semantically identical tools, AI clients deduplicate by
relevance and route to one server consistently. This is correct behavior — the AI cannot know
which transport you want, and guessing would be worse. Design multi-server setups with either
(a) only one server active at a time, (b) distinct tool descriptions that give the AI a
semantic reason to prefer one server for a given query, or (c) namespace prefixes in tool names.
Never rely on the AI to alternate between two servers with identical tools.

---

## Summary Table

| # | Issue title | Category | Fix in one line | Lesson in one line |
|---|---|---|---|---|
| 1 | Spring Boot scans wrong package | Package Scanning | Move `AiMcpLabApplication.java` to root package `com.mcplab` | `@SpringBootApplication` package = component-scan root; sibling packages are invisible |
| 2 | MySQL credentials don't work | Database | Set `spring.datasource.username=root` and correct password in `application.properties` | Match config to the actual environment; verify with raw `mysql` CLI before blaming Spring |
| 3 | Stdio mode garbles output | MCP Stdio | Add `application-mcp.properties` with `web-application-type=none`, `banner-mode=off`, `logging.pattern.console=` | stdout is the wire protocol in stdio mode; suppress everything non-JSON-RPC |
| 4 | Stale JAR after changes | Build / JAR | `mvn clean package -DskipTests` after every source or properties change | Properties live inside the JAR; editing source doesn't touch the running artifact |
| 5 | schema.sql wipes test data | SQL Init | Change `spring.sql.init.mode` from `always` to `never` in `application.properties` | `always` is for CI reproducibility; `never` is for local dev with persistent state |
| 6 | /sse returns 404 | MCP HTTP | Add `ServletRegistrationBean` for `/sse` and `/mcp/messages` in `McpServerConfig` | `DispatcherServlet` intercepts `/*`; raw servlets need explicit `ServletRegistrationBean` |
| 7 | Port 8080 already in use | Port | `pkill -f "ai-mcp-lab"; sleep 1; ss -lntp \| grep 8080` before every restart | Verify port is free before starting; don't rely on visual Ctrl+C confirmation |
| 8 | 0 tools after profile refactor | Spring Profiles | Add `@ConditionalOnBean(McpServerTransportProvider.class)` to `mcpSyncServer` | Profile-gated beans create implicit dependencies; use `@ConditionalOnBean` to make them explicit |
| 9 | AI asks for clarification | AI Routing | Rewrite tool descriptions with WHEN-to-use trigger phrases; be explicit in prompts | Tool descriptions are the AI's routing menu; write trigger conditions, not return shapes |
| 10 | AI picks only one of two servers | MCP Multi-Server | Disable one server, anchor prompts explicitly, or namespace tool names | Identical tools from two servers → AI deduplicates; design for distinct tool descriptions or single-server-per-session |

---

## Cross-Cutting Lessons

### Spring Boot mechanics (Issues 1, 2, 4, 7)

Several issues were pure Spring Boot fundamentals that have exact analogues in traditional
Spring MVC. The `@SpringBootApplication` placement rule (Issue 1) is the Boot equivalent of
`<context:component-scan base-package="...">` in `root-context.xml` — get the root wrong and
nothing is found. The stale JAR problem (Issue 4) is the same as deploying a WAR without
repackaging. The port conflict (Issue 7) happens on any server process. And credentials (Issue 2)
need to match the actual database, not the intended future database. None of these required
understanding MCP or AI — they were ordinary environment and configuration issues that surface
early in any new project. The pattern: rule out the boring stuff first (package, credentials,
process, port) before investigating the interesting stuff (protocol, AI routing).

### MCP protocol specifics (Issues 3, 6, 8)

Three issues came from MCP's specific requirements that differ from conventional REST services.
Stdio mode's stdout constraint (Issue 3) is fundamental to how stdin/stdout JSON-RPC framing
works — there is no error tolerance, and the failure mode is silent corruption rather than an
exception. The `DispatcherServlet` path conflict (Issue 6) is specific to the MCP SDK's
`HttpServletSseServerTransportProvider` being a raw `HttpServlet` rather than a Spring MVC
controller. The `@ConditionalOnBean` requirement (Issue 8) arises from the profile-switching
architecture where transport beans are optional — a pattern unusual in conventional Spring apps
where the web server is always present. All three require understanding both MCP's architecture
and Spring Boot's wiring model simultaneously.

### AI client behavior and tool design (Issues 9, 10)

Two issues were not bugs in the Java code at all — they were mismatches between how the AI
routes tool calls and how the tools were designed. Issue 9 (AI asks for clarification instead of
calling a tool) taught that tool descriptions must be written as routing rules (`"Use this tool
when..."`) rather than API documentation (`"Returns a list of..."`). Issue 10 (AI ignores one of
two duplicate servers) taught that the AI de-duplicates by effect and does not alternate between
semantically identical tools. These two issues will appear in every MCP project that registers
multiple servers or writes tool descriptions from a developer's perspective rather than from the
AI's routing perspective.

### Data and environment management (Issues 5, 2, 4)

Issues 2, 4, and 5 share a theme: the running environment does not match what you think it
does. Wrong credentials (Issue 2) — the environment has different MySQL users than you assumed.
Stale JAR (Issue 4) — the running artifact contains different code than you just edited. SQL
init wipe (Issue 5) — the database contains different data than you manually inserted. In each
case, the application was technically correct; the mismatch was between the code's assumptions
and the actual state of the environment. The fix pattern is always: add a verification step
(`mysql -e "SELECT 1"`, build markers, `spring.sql.init.mode=never`) that makes the true
environment state visible before debugging the application.

---

See also: [ARCHITECTURE.md](ARCHITECTURE.md) — the three operational modes that Issues 3, 6, and 8 relate to  
See also: [LOGGING.md](LOGGING.md) — how build markers (Issue 4) and wire logging (Issue 3) work  
See also: [DEBUGGING-STORY.md](DEBUGGING-STORY.md) — narrative walk-through of these same issues  
See also: [DEMO-STEPS.md](DEMO-STEPS.md) — "Common Gotchas" section maps Issues 3, 4, 6, 7, and 9 to live demo failures
