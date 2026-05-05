# AI MCP Lab — Final Architecture

## Overview

ai-mcp-lab is a Spring Boot 3.2 + Java 21 proof-of-concept that demonstrates the Model Context
Protocol (MCP) as an "AI-native API layer" on top of a conventional REST backend. The same MySQL
database, the same JdbcTemplate queries, the same five employees and three departments — reachable
either via HTTP from a browser, or via JSON-RPC from an AI model running inside Cursor or MCP
Inspector. The interesting design challenge is not the CRUD operations. It is the transport-switching
architecture that lets one JAR serve both audiences without duplicating any business logic.

---

## The Big Picture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  AI CLIENTS                                                                             │
│                                                                                         │
│  ┌───────────────────┐   ┌────────────────────────┐   ┌──────────────────────────────┐ │
│  │  Cursor IDE        │   │  MCP Inspector          │   │  Claude Desktop              │ │
│  │  (spawns JAR as   │   │  npx @modelcontext-     │   │  (any MCP-aware AI client)   │ │
│  │   child process)  │   │  protocol/inspector     │   │                              │ │
│  └────────┬──────────┘   └───────────┬─────────────┘   └──────────────┬───────────────┘ │
│           │ stdio (stdin/stdout)      │ stdio or HTTP/SSE              │ stdio or HTTP   │
└───────────┼──────────────────────────┼────────────────────────────────┼─────────────────┘
            │                          │                                 │
            │                          ▼                                 │
            │          ┌───────────────────────────────┐                 │
            │          │  HTTP/SSE endpoints            │                 │
            │          │  GET  /sse   (SSE stream)      │◄────────────────┘
            │          │  POST /mcp/messages (JSON-RPC) │
            │          └───────────────┬───────────────┘
            │                          │
            ▼                          ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  ai-mcp-lab.jar  (Spring Boot embedded Tomcat, port 8080)                              │
│                                                                                        │
│  ┌────────────────────────────────────────────────────────────────────────────────┐    │
│  │  MCP SERVER (McpServerConfig.java — @ConditionalOnBean)                        │    │
│  │                                                                                │    │
│  │  ┌─────────────────────────┐    ┌──────────────────────────────────────────┐  │    │
│  │  │  StdioTransport          │    │  HttpServletSseServerTransportProvider   │  │    │
│  │  │  (LoggingStdioTransport  │    │  profile=mcp-http                        │  │    │
│  │  │   Provider, profile=mcp) │    │  /sse + /mcp/messages                    │  │    │
│  │  └──────────┬──────────────┘    └────────────────────┬─────────────────────┘  │    │
│  │             └─────────────────────────┬──────────────┘                        │    │
│  │                                       ▼                                       │    │
│  │              McpSyncServer — 11 tools registered                              │    │
│  │              getDatabaseStatus · listEmployees · getEmployeeById              │    │
│  │              createEmployee · updateEmployee · deleteEmployee                 │    │
│  │              listDepartments · getDepartmentById · createDepartment           │    │
│  │              updateDepartment · deleteDepartment                              │    │
│  └──────────────────────────────┬────────────────────────────────────────────────┘    │
│                                 │ calls                                                │
│  ┌────────────────────────────────────────────────────────────────────────────────┐    │
│  │  SHARED LAYER  (same beans in all three modes)                                 │    │
│  │                                                                                │    │
│  │  ┌──────────────────┐  ┌────────────────────┐  ┌──────────────────────────┐   │    │
│  │  │  EmployeeTool     │  │  DepartmentTool     │  │  DatabaseStatusTool      │   │    │
│  │  │  (MCP adapter)   │  │  (MCP adapter)      │  │  (MCP adapter)           │   │    │
│  │  └────────┬─────────┘  └────────┬────────────┘  └──────────────────────────┘   │    │
│  │           │                     │                                               │    │
│  │  ┌────────▼─────────────────────▼──────────────────────────────────────────┐   │    │
│  │  │  EmployeeRepository          DepartmentRepository                        │   │    │
│  │  │  JdbcTemplate queries        JdbcTemplate queries + LEFT JOIN count      │   │    │
│  │  └────────────────────────────────────────┬────────────────────────────────┘   │    │
│  └──────────────────────────────────────────┼────────────────────────────────────┘    │
│                                             │                                          │
│  ┌──────────────────────────────────────────▼────────────────────────────────────┐    │
│  │  REST LAYER  (active in default and mcp-http modes)                            │    │
│  │  EmployeeController   GET/POST/PUT/DELETE /api/employees                       │    │
│  │  DepartmentController GET/POST/PUT/DELETE /api/departments                     │    │
│  └──────────────────────────────────────────┬────────────────────────────────────┘    │
│                                             │ same repositories                        │
└─────────────────────────────────────────────┼──────────────────────────────────────────┘
                                              ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  MySQL — database: ai_mcp_lab                                                          │
│                                                                                        │
│  ┌─────────────────────────────────────────┐  ┌────────────────────────────────────┐  │
│  │  Departments                             │  │  Employees                          │  │
│  │  id · name · location                   │  │  id · name · email · departmentId   │  │
│  │  1  Engineering  Hyderabad              │  │  salary · createdAt                 │  │
│  │  2  Sales        Mumbai                 │◄─┤  FK: departmentId → Departments.id  │  │
│  │  3  HR           Bangalore              │  │  5 seed rows across 3 departments   │  │
│  └─────────────────────────────────────────┘  └────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  REST CLIENTS  (active in default and mcp-http modes)                                   │
│  curl · browser · React frontend (Vite dev server, port 5173)                           │
│  GET /api/employees    GET /api/departments    POST/PUT/DELETE on both                  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## The "Two Doors, One Room" Insight

REST controllers and MCP tools share `EmployeeRepository` and `DepartmentRepository` — the same
Spring beans, the same SQL, the same HikariCP connection pool. There is no duplication at the data
layer.

```
curl GET /api/employees/2         →  EmployeeController  →  EmployeeRepository.findById(2)  →  MySQL
AI: "get employee 2 via MCP"      →  EmployeeTool        →  EmployeeRepository.findById(2)  →  MySQL
```

Both paths execute the same parameterised `SELECT ... FROM Employees WHERE id = ?`. The only
difference is the entry point: HTTP dispatch through Spring MVC's `DispatcherServlet`, versus
JSON-RPC dispatch through `McpSyncServer`. The room (data access) is identical; only the door
(protocol) changes.

This is why MCP does not require a separate database, a separate service, or even a separate
codebase. It is an additional transport layer added on top of what already exists.

---

## Three Operational Modes

| Mode | Profile flag | Tomcat | REST endpoints | MCP transport | Best used by |
|---|---|---|---|---|---|
| **Default** | _(none)_ | ON, port 8080 | Active (`/api/employees`, `/api/departments`) | None — no transport bean | `curl`, browser, React dev server |
| **Stdio MCP** | `--spring.profiles.active=mcp` | OFF | Not exposed | stdin/stdout JSON-RPC | Cursor IDE, MCP Inspector, Claude Desktop (process spawn) |
| **HTTP/SSE MCP** | `--spring.profiles.active=mcp-http` | ON, port 8080 | Active | HTTP GET `/sse` + POST `/mcp/messages` | Network AI clients, UAT, production |

```bash
# Mode 1 — Default (REST only)
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar

# Mode 2 — Stdio MCP (AI clients spawn the JAR as a child process)
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp

# Mode 3 — HTTP/SSE MCP (REST + MCP over HTTP, both doors open)
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
```

---

## Component Breakdown

| Component | File | Lives in | Role | Tech |
|---|---|---|---|---|
| `AiMcpLabApplication` | `AiMcpLabApplication.java` | `com.mcplab` | Spring Boot entry point; `@SpringBootApplication` drives component scan and auto-configuration | Spring Boot 3.2 |
| `McpServerConfig` | `McpServerConfig.java` | `com.mcplab.server` | Wires transport beans (profile-gated) and builds `McpSyncServer` with 11 tools; uses `@ConditionalOnBean` to skip MCP in default mode | MCP SDK 0.9.0 |
| `LoggingStdioTransportProvider` | `LoggingStdioTransportProvider.java` | `com.mcplab.server` | Wraps `StdioServerTransportProvider`; intercepts every raw JSON-RPC line and mirrors it to stderr with `>>> MCP IN` / `<<< MCP OUT` prefixes | MCP SDK, `System.err` |
| `EmployeeController` | `EmployeeController.java` | `com.mcplab.controller` | REST controller; `GET/POST/PUT/DELETE /api/employees`; delegates to `EmployeeRepository` | Spring MVC `@RestController` |
| `DepartmentController` | `DepartmentController.java` | `com.mcplab.controller` | REST controller; `GET/POST/PUT/DELETE /api/departments`; validates employee count before delete | Spring MVC `@RestController` |
| `EmployeeRepository` | `EmployeeRepository.java` | `com.mcplab.repository` | All Employee SQL; parameterised queries; dynamic `UPDATE` builder; returns `Map<String,Object>` rows; shared by REST and MCP | Spring JdbcTemplate |
| `DepartmentRepository` | `DepartmentRepository.java` | `com.mcplab.repository` | All Department SQL; LEFT JOIN for `employeeCount`; `existsById` and `countEmployees` helpers; shared by REST and MCP | Spring JdbcTemplate |
| `EmployeeTool` | `EmployeeTool.java` | `com.mcplab.tools` | MCP adapter for employees; parses `Map<String,Object>` args → typed values; validates inputs; serialises results to JSON string | `@Component`, Jackson |
| `DepartmentTool` | `DepartmentTool.java` | `com.mcplab.tools` | MCP adapter for departments; same arg-parse/validate/serialise pattern as `EmployeeTool` | `@Component`, Jackson |
| `DatabaseStatusTool` | `DatabaseStatusTool.java` | `com.mcplab.tools` | MCP health-check tool; runs `SELECT 1` against HikariCP; returns human-readable status string; catches and formats exceptions for AI consumption | `@Component`, JdbcTemplate |
| `GlobalExceptionHandler` | `GlobalExceptionHandler.java` | `com.mcplab.exception` | Maps `ResourceNotFoundException` → 404 and `BusinessRuleException` → 409 for REST clients | `@ControllerAdvice` |
| `EmployeeRequest` / `EmployeeResponse` | `dto/` | `com.mcplab.dto` | REST request body and response DTO; `EmployeeResponse.from(Map)` converts repository rows | Java records |
| `DepartmentRequest` / `DepartmentResponse` | `dto/` | `com.mcplab.dto` | Same pattern for departments; `DepartmentResponse.from(Map)` preserves `employeeCount` | Java records |

---

## Why This Architecture — The Design Choices

### Why MCP at all (vs. just REST)?

REST APIs are designed for HTTP clients that construct URLs and parse JSON. AI models cannot reliably construct parameterised curl commands, handle auth headers, or parse error status codes. MCP defines a contract the AI already understands natively:

- ✅ Tool discovery via `tools/list` — the AI reads your description and decides if the tool is relevant
- ✅ Typed input schemas — the AI generates valid arguments; no prompt engineering needed for "pass the right JSON"
- ✅ Plain-text responses — the AI reads the result directly; no JSON parsing in the prompt
- ✅ Protocol-level error handling — the AI receives `{"error": "Employee not found"}` and can relay it to the user naturally
- ✅ Session management — the SDK handles JSON-RPC framing, ID correlation, and transport lifecycle

The result: the AI treats your tools as first-class capabilities, not as HTTP endpoints it has to guess at.

### Why support BOTH stdio and HTTP/SSE transports?

- ✅ Stdio is the lowest-friction path during development — Cursor spawns the JAR as a child process; no server to keep running, no port to manage
- ✅ Stdio is the most isolated path — the JAR is only reachable from the process that spawned it; no network exposure
- ✅ HTTP/SSE is required for production — deployed JARs cannot be spawned by AI clients; they must be network-reachable
- ✅ HTTP/SSE supports multiple simultaneous AI clients — each SSE connection is a separate session; stdio is one-client-at-a-time
- ✅ Supporting both in one JAR avoids code divergence — the tool implementations are identical; only the transport bean differs

### Why Spring profiles for mode switching?

In your EFFORT-Web project, you switched environments by commenting/uncommenting `context:property-placeholder` lines in `root-context.xml`. Spring profiles are the Boot equivalent — they let you overlay `application-mcp.properties` on top of `application.properties` at runtime, without touching source code. A CI pipeline can activate `mcp-http` by setting an environment variable:

```bash
SPRING_PROFILES_ACTIVE=mcp-http java -jar ai-mcp-lab.jar
```

- ✅ One artifact, three behaviours — no separate builds for each mode
- ✅ Properties override, not replace — `application-mcp.properties` inherits the datasource from `application.properties`
- ✅ Profile-gated `@Bean` methods in `McpServerConfig` — `@Profile("mcp")` and `@Profile("mcp-http")` gate which transport bean is created

### Why @ConditionalOnBean for mcpSyncServer?

Without `@ConditionalOnBean(McpServerTransportProvider.class)`, Spring would attempt to create `McpSyncServer` in default mode (no MCP profile), fail to find the required `McpServerTransportProvider` bean, and throw a `NoSuchBeanDefinitionException` at startup. The condition makes default mode (REST-only) a first-class citizen — the MCP server is simply absent, not broken.

- ✅ Default mode starts cleanly with no MCP infrastructure
- ✅ No manual null-checks or Optional wiring in the bean method
- ✅ The condition reads as documentation: "this bean requires a transport to exist"

### Why share repositories between REST and MCP?

The alternative — separate DAOs for MCP tools — would mean every SQL fix must be applied in two places, every schema migration must be reflected in two query sets, and every new column must be added to both code paths. Shared repositories are the single source of truth:

- ✅ SQL written once, tested once, maintained once
- ✅ A bug fix in `EmployeeRepository.update()` benefits both REST and MCP callers simultaneously
- ✅ Integration tests that cover the repository automatically cover both entry points
- ✅ Consistent data format — `EmployeeTool` and `EmployeeController` always see the same `Map<String,Object>` row shape

---

## Trust and Reachability Boundaries

| Mode | Reachable from | Trust level | Best for |
|---|---|---|---|
| Default (no profile) | Any HTTP client on the network (loopback only in dev) | Public — no auth in this POC | Browser testing, curl, React dev server |
| Stdio MCP | Only the process that spawned the JAR (Cursor, Inspector) | High — OS process isolation | Local AI-assisted development |
| HTTP/SSE MCP | Any HTTP client that can reach port 8080 | Network-level — add auth before exposing publicly | UAT environments, deployed AI integrations |

In stdio mode the JAR communicates via the spawning process's stdin/stdout. No TCP port is opened
and no network client can reach the MCP server — it is invisible to the rest of the machine. This
makes stdio the safest mode for local development: the worst an AI model can do is make bad tool
calls, which affect only the local MySQL database.

---

## File Layout

```
src/main/java/com/mcplab/
│
├── AiMcpLabApplication.java        Spring Boot entry point; @SpringBootApplication
│                                   drives component scan for all sub-packages
│
├── server/
│   ├── McpServerConfig.java        @Configuration; profile-gated transport beans;
│   │                               builds McpSyncServer with 11 tools
│   └── LoggingStdioTransportProvider.java
│                                   Stdio transport decorator; logs >>> MCP IN /
│                                   <<< MCP OUT to stderr via System.err
│
├── controller/
│   ├── EmployeeController.java     REST: GET/POST/PUT/DELETE /api/employees
│   └── DepartmentController.java   REST: GET/POST/PUT/DELETE /api/departments
│
├── repository/
│   ├── EmployeeRepository.java     All Employee SQL (shared by REST + MCP)
│   └── DepartmentRepository.java   All Department SQL + LEFT JOIN employee count
│
├── tools/
│   ├── DatabaseStatusTool.java     MCP: getDatabaseStatus (SELECT 1 health probe)
│   ├── EmployeeTool.java           MCP: listEmployees, getEmployeeById,
│   │                               createEmployee, updateEmployee, deleteEmployee
│   └── DepartmentTool.java         MCP: listDepartments, getDepartmentById,
│                                   createDepartment, updateDepartment, deleteDepartment
│
├── dto/
│   ├── EmployeeRequest.java        REST request body (Java record with @Valid fields)
│   ├── EmployeeResponse.java       REST response DTO with EmployeeResponse.from(Map)
│   ├── DepartmentRequest.java      REST request body
│   └── DepartmentResponse.java     REST response DTO with employeeCount
│
└── exception/
    ├── ResourceNotFoundException.java   Maps → HTTP 404
    ├── BusinessRuleException.java       Maps → HTTP 409
    └── GlobalExceptionHandler.java      @ControllerAdvice; translates typed exceptions
```

---

## End-to-End Timing

Typical durations observed on a local development machine (MySQL on localhost, no network latency).

| Operation | Entry point | Key steps | Typical duration |
|---|---|---|---|
| `listEmployees` via REST | `GET /api/employees` | DispatcherServlet → Controller → Repository → `SELECT … FROM Employees` → HTTP 200 | < 5 ms |
| `listEmployees` via MCP | `tools/call listEmployees` | JSON-RPC parse → McpSyncServer → EmployeeTool → Repository → same SELECT → JSON string → MCP response | < 10 ms |
| `createEmployee` via REST | `POST /api/employees` | Validation → existsById check → INSERT → SELECT (re-fetch) → HTTP 201 | < 15 ms |
| `getDatabaseStatus` via MCP | `tools/call getDatabaseStatus` | Repository → `SELECT 1` → format string → MCP response | < 5 ms |
| App startup (default mode) | `java -jar` | Spring context, HikariCP pool, Tomcat | ~2–3 s |
| App startup (mcp mode) | `java -jar … --spring.profiles.active=mcp` | Spring context, HikariCP pool, no Tomcat | ~1.5–2 s |

The dominant cost in every tool call is the MySQL round-trip. HikariCP keeps connections warm in
the pool, so there is no per-request connection overhead after the first call.

---

## What This POC Proves

**You do not need a new data layer to add AI capabilities to an existing Spring application.**
`EmployeeRepository` and `DepartmentRepository` were written once, with JdbcTemplate, for the REST
controllers. Adding 11 MCP tools required zero changes to those repositories. The MCP tools are
thin adapters (argument parsing + JSON serialisation) sitting on top of the existing DAO layer.
The architectural pattern generalises: any Spring application with a service or DAO layer can gain
MCP capabilities by adding a `McpServerConfig` and thin tool adapter classes — without touching
the existing business logic.

---

See also: [LOGGING.md](LOGGING.md) — logging configuration, wire tracing, and the stdout-safety rule  
See also: [DEBUGGING-STORY.md](DEBUGGING-STORY.md) — the real bugs encountered building this POC and what each one taught
