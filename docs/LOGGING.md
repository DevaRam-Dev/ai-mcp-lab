# AI MCP Lab — Logging & Observability Guide

## Why MCP servers need different logging rules

In a conventional Spring Boot REST service, all log output goes to stdout (via Spring Boot's
default Logback console appender) and everything works. In MCP stdio mode, stdout is the wire
protocol. Every byte written there is interpreted by the AI client as part of a JSON-RPC frame.
A stray log line — even a single newline — corrupts the framing and causes the client to
disconnect with a JSON parse error.

This project has two logging regimes, gated by the active Spring profile, and they follow
different rules by necessity.

---

## Logger Configuration Overview

Spring Boot 3.2 ships with Logback as the default logging framework. No explicit `logback-spring.xml`
is present in this project — all configuration is done via `application*.properties`, which
Spring Boot's `LogbackLoggingSystem` translates to Logback configuration at startup.

### Per-profile log behaviour

| Setting | Default (no profile) | `mcp` (stdio) | `mcp-http` (HTTP/SSE) |
|---|---|---|---|
| `spring.main.banner-mode` | on | **off** | on |
| `logging.level.root` | `INFO` | **WARN** | `INFO` |
| `logging.level.org.springframework` | _(inherits root)_ | **WARN** (explicit) | _(inherits root)_ |
| `logging.level.com.mcplab` | `INFO` | `INFO` | `INFO` |
| `logging.level.org.springframework.jdbc.core` | `INFO` | _(inherits root → WARN)_ | `DEBUG` |
| `logging.level.io.modelcontextprotocol` | _(inherits root)_ | _(inherits root → WARN)_ | `DEBUG` |
| `logging.pattern.console` | _(Spring default)_ | **`""`** (blanked) | _(Spring default)_ |
| Console output destination | stdout | stdout suppressed | stdout |
| Wire message output | N/A | **stderr** (System.err) | N/A |

### The cardinal rule: in stdio mode, STDOUT is the wire protocol

`application-mcp.properties` sets `logging.pattern.console=` (empty string), which suppresses
the Logback console appender's output for all log events routed through SLF4J. Combined with
`logging.level.root=WARN`, the only log events that could reach stdout are WARN or ERROR level
entries from any package. In practice, a healthy startup produces no output to stdout at all.

**All observable log output in stdio mode flows through `System.err.println()` directly.** This
is why `LoggingStdioTransportProvider` bypasses SLF4J entirely for its wire messages — it calls
`System.err.println()` in `emitInbound()` and `emitOutbound()`. MCP Inspector displays stderr in
its "Error output" panel; Cursor discards stderr by default (see the workaround below).

---

## The LoggingStdioTransportProvider

`LoggingStdioTransportProvider` (`com.mcplab.server`) is a decorator around the MCP SDK's
`StdioServerTransportProvider`. It intercepts every byte crossing the stdin/stdout boundary and
logs complete JSON-RPC lines to stderr.

### What it does

- Wraps `System.in` with `LoggingInputStream`: buffers bytes until `'\n'`, then emits the full
  line to stderr with the prefix `>>> MCP IN  : ` before passing bytes to the SDK
- Wraps `System.out` with `LoggingOutputStream`: forwards bytes to real `System.out` immediately
  (critical — protocol framing must not be delayed), then buffers until `'\n'` and emits to
  stderr with the prefix `<<< MCP OUT : `
- The bulk-read override (`read(byte[], int, int)`) ensures the SDK's chunk-reads are fully
  intercepted, not just single-byte reads

### Startup banner

The constructor emits a three-line banner directly to stderr before any JSON-RPC exchange:

```
=========================================
MCP wire logging active — inbound/outbound JSON-RPC lines appear on stderr
=========================================
```

This uses `System.err.println()`, not SLF4J, ensuring it appears even when `logging.pattern.console=`
has suppressed the Logback console appender. The banner confirms the logging wrapper is loaded and
the MCP wire is instrumented.

### Output format

```
>>> MCP IN  : {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
<<< MCP OUT : {"jsonrpc":"2.0","id":1,"result":{"tools":[{"name":"getDatabaseStatus", ...}]}}
>>> MCP IN  : {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"listEmployees","arguments":{}}}
<<< MCP OUT : {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"{\"employees\":[...]...}"}]}}
```

`>>>` = inbound (AI client → server). `<<<` = outbound (server → AI client). Each is a complete
JSON-RPC object on a single line — the SDK uses newline-delimited JSON-RPC framing.

### Where output appears

In stdio mode, stderr is separate from stdout. The destinations are:

| Client | Where stderr goes |
|---|---|
| MCP Inspector | "Error output" panel, visible in the UI |
| Cursor IDE | Discarded by default (see workaround below) |
| Terminal (direct `java -jar`) | Terminal where the JAR was started |

---

## Build Markers for Runtime Verification

`McpServerConfig` logs startup markers so a developer can confirm at runtime which version of the
code is loaded and which profile-gated beans were created. Look for these lines in the log (or on
stderr in mcp mode):

| When | Logger | Message |
|---|---|---|
| After `McpServerConfig` bean is created (`@PostConstruct`) | `McpServerConfig` | `McpServerConfig loaded — profile-active beans will follow @ <instant>` |
| When stdio transport bean is created (profile=mcp) | `McpServerConfig` | `[MCP] stdio transport bean created (profile=mcp)` |
| When HTTP/SSE transport bean is created (profile=mcp-http) | `McpServerConfig` | `[MCP] HTTP/SSE transport bean created (profile=mcp-http)` |
| When HTTP servlet is registered (profile=mcp-http) | `McpServerConfig` | `[MCP] HTTP servlet registered at /sse and /mcp/messages` |
| When `McpSyncServer` is built | `McpServerConfig` | `[MCP] MCP server built — 11 tools registered` |
| When `LoggingStdioTransportProvider` is constructed | `LoggingStdioTransportProvider` | Banner to stderr + `LoggingStdioTransportProvider constructed — wire logging banner emitted to stderr` |

**How to use them to verify a fresh build:** After `mvn clean package`, start the JAR in HTTP/SSE
mode (stdout is safe) and tail the console:

```bash
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http 2>&1 | grep -E "\[MCP\]|McpServer"
```

You should see all five markers appear in order. If you see the old timestamp from a previous
`@PostConstruct` call, your JAR is stale — rebuild with `mvn clean package -DskipTests`.

---

## How to See MCP Wire Traffic

### In stdio mode via MCP Inspector (recommended)

MCP Inspector (`npx @modelcontextprotocol/inspector`) is the easiest way to see wire traffic
because it displays stderr inline in its own UI. Start it pointing at the JAR:

```bash
npx @modelcontextprotocol/inspector java -jar /full/path/to/ai-mcp-lab-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=mcp
```

The Inspector spawns the JAR as a child process, connects over stdio, and shows:
- **Messages tab**: every `tools/list` and `tools/call` request/response with pretty-printed JSON
- **Error output panel**: stderr, including the startup banner and any `>>> MCP IN` / `<<< MCP OUT` lines

### In stdio mode via Cursor (wire traffic on disk)

Cursor spawns the JAR as described in your Cursor MCP config and discards the JAR's stderr.
To capture wire traffic to a file, redirect stderr in the Cursor MCP configuration:

```json
{
  "mcpServers": {
    "ai-mcp-lab-stdio": {
      "command": "/bin/sh",
      "args": [
        "-c",
        "java -jar /full/path/to/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp 2>>/tmp/ai-mcp-lab-stderr.log"
      ]
    }
  }
}
```

Then watch the log in a separate terminal:

```bash
tail -f /tmp/ai-mcp-lab-stderr.log
```

You will see the startup banner and every `>>> MCP IN` / `<<< MCP OUT` line as Cursor calls tools.

### In HTTP/SSE mode (default Spring Boot console)

HTTP/SSE mode uses a normal HTTP transport — stdout is never the wire protocol. All logs go to the
terminal where you ran `java -jar`, via the standard Spring Boot console appender:

```bash
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
```

To verify SSE connectivity independently of any AI client:

```bash
# Open the SSE stream — keep this running; it will stream events as clients connect
curl -i http://localhost:8080/sse

# In a second terminal, send a tools/list JSON-RPC call
curl -s -X POST http://localhost:8080/mcp/messages \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

To enable the SDK's own DEBUG logging for SSE session events, set in
`application-mcp-http.properties` (already present):

```properties
logging.level.io.modelcontextprotocol=DEBUG
```

---

## Logger Settings Reference

| Logger | Default level | mcp (stdio) level | mcp-http level | Bump to DEBUG when… |
|---|---|---|---|---|
| `root` | `INFO` | `WARN` | `INFO` | General Spring/library noise needs investigation |
| `com.mcplab` | `INFO` | `INFO` | `INFO` | Rarely needed — INFO already covers tool calls and SQL operations |
| `org.springframework.jdbc.core` | `INFO` | `WARN` | `DEBUG` | Debugging a specific SQL query or parameter binding issue |
| `io.modelcontextprotocol` | `INFO` | `WARN` | `DEBUG` | SSE session lifecycle, JSON-RPC dispatch issues |
| `org.springframework.web` | `INFO` | N/A | `INFO` | Tracing REST request routing or 4xx/5xx causes |

---

## Common Log Lines Cheat Sheet

Lines to look for when verifying the stack is healthy. In stdio mode these appear on stderr; in
other modes they appear in the console.

| What | What to look for |
|---|---|
| App context started | `Started AiMcpLabApplication in X.XXX seconds` |
| HikariCP connected | `HikariPool-1 - Start completed` |
| Tomcat ready (default / mcp-http) | `Tomcat started on port 8080` |
| MCP server config loaded | `McpServerConfig loaded — profile-active beans will follow @` |
| Stdio transport active | `[MCP] stdio transport bean created (profile=mcp)` |
| Wire logging banner | `MCP wire logging active — inbound/outbound JSON-RPC lines appear on stderr` |
| HTTP servlet registered | `[MCP] HTTP servlet registered at /sse and /mcp/messages` |
| Tools registered | `[MCP] MCP server built — 11 tools registered` |
| First MCP handshake (inbound) | `>>> MCP IN  : {"jsonrpc":"2.0","id":1,"method":"initialize",` |
| First MCP handshake (outbound) | `<<< MCP OUT : {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":` |

---

## Log Levels and When to Use Them

| Level | When to use | Example in this project |
|---|---|---|
| `ERROR` | Something failed that requires immediate attention; the operation did not complete | `log.error("Database health check failed at {}", timestamp, e)` in `DatabaseStatusTool` |
| `WARN` | Something unexpected happened but the system recovered; worth investigating later | `log.warn(...)` — not currently used, but appropriate for connection pool warnings |
| `INFO` | A significant, expected business event completed successfully | `log.info("listEmployees: {} row(s), deptFilter={}", rows.size(), deptId)` in `EmployeeTool` |
| `DEBUG` | Detailed diagnostic information; too verbose for normal operation | SQL queries via `org.springframework.jdbc.core` at DEBUG, MCP SDK events via `io.modelcontextprotocol` |
| `TRACE` | Extremely verbose; individual byte-level operations | Not used; MCP wire trace is handled by `LoggingStdioTransportProvider` via `System.err` |

The practical rule for this project: run with the defaults shown in the table above. When
something breaks, first check if the startup banner and build markers appeared (proves fresh code
is running), then check wire traffic via MCP Inspector or the stderr redirect, then temporarily
enable `DEBUG` on the specific package that owns the layer you're investigating.

---

See also: [ARCHITECTURE.md](ARCHITECTURE.md) — where each logger lives in the layered architecture  
See also: [DEBUGGING-STORY.md](DEBUGGING-STORY.md) — how wire logging and build markers were used to diagnose real bugs

## Logs at start up
✅ McpServerConfig loaded — profile-active beans will follow @ ...  
✅ [MCP] HTTP/SSE transport bean created (profile=mcp-http)  
✅ [MCP] HTTP servlet registered at /sse and /mcp/messages  
✅ [MCP] MCP server built — 11 tools registered  
✅ Tomcat started on port 8080  
✅ Started AiMcpLabApplication in 1.469 seconds  

# Notice the sequence:  

1.McpServerConfig loaded first (configuration class instantiated)  
2.HTTP/SSE transport bean created (the @Profile("mcp-http") bean fired)  
3.HTTP servlet registered (the ServletRegistrationBean wired)  
4.MCP server built — 11 tools registered (mcpSyncServer assembled all tools)  
5.Tomcat started (web server up, ready to receive HTTP)  
6.Started AiMcpLabApplication (Spring Boot fully initialized)  