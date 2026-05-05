# AI MCP Lab — Demo Runbook

This document walks through demoing ai-mcp-lab end-to-end. Follow the steps top-to-bottom. Each
section is self-contained — pre-flight checks, the actual command, and verification of expected
output. Designed so the presenter never has to think mid-demo.

---

## Before You Start

### Required tools

| Tool | Why | Quick check |
|---|---|---|
| Java 21 | Runs the JAR | `java -version` → `openjdk 21` or similar |
| MySQL (running) | The JAR connects on startup | `ss -lntp \| grep 3306` → port is LISTEN |
| Cursor IDE | MCP client for Parts B and C | Cursor window open, project loaded |
| Maven (optional) | Only needed if you rebuilt source | `mvn -v` |

### Verify the environment before anything else

```bash
# Java version
java -version

# MySQL listening on port 3306?
ss -lntp | grep 3306

# ai_mcp_lab database exists?
mysql -uroot -pPin@801698 -e "SHOW DATABASES LIKE 'ai_mcp_lab';"

# Expected: one row returned:
# +---------------------+
# | Database (ai_mcp_lab) |
# +---------------------+
# | ai_mcp_lab          |
# +---------------------+

# Quick table check — five employees, three departments
mysql -uroot -pPin@801698 ai_mcp_lab -e "SELECT COUNT(*) AS employees FROM Employees; SELECT COUNT(*) AS departments FROM Departments;"
```

### JAR location

```
/home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab/target/ai-mcp-lab-0.0.1-SNAPSHOT.jar
```

All `java -jar` commands in this runbook use that path. Shorten it if you have already
`cd`'d into the project root: `target/ai-mcp-lab-0.0.1-SNAPSHOT.jar`.

### Terminal layout

Keep two terminals open throughout the demo:

- **Terminal 1** — JAR runner (start / stop the server, watch logs)
- **Terminal 2** — test commands (`curl`, `mysql`, `ps`, `ss`)

---

## One-Time Setup (Before the Demo Session)

Do these once. Skip if the JAR is already fresh and MySQL is already seeded.

### Step 0.1 — Build the JAR

```bash
cd /home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab
mvn clean package -DskipTests
```

Expected last lines:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  ~11 s
```

### Step 0.2 — Verify the Cursor MCP config

Cursor's global MCP configuration lives at `~/.cursor/mcp.json`. Both server entries must be
present — one for stdio mode and one for HTTP/SSE mode.

```bash
cat ~/.cursor/mcp.json
```

Expected content (verify this matches exactly):

```json
{
  "mcpServers": {
    "ai-mcp-lab-stdio": {
      "command": "java",
      "args": [
        "-jar",
        "/home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab/target/ai-mcp-lab-0.0.1-SNAPSHOT.jar",
        "--spring.profiles.active=mcp"
      ]
    },
    "ai-mcp-lab-http": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

If either entry is missing, add it and save the file. Cursor reads `mcp.json` at startup and
when you open Settings → Tools & MCPs.

### Step 0.3 — Ensure MySQL is running

```bash
sudo systemctl status mysql | grep -E "active|running"
# Expected: "active (running)"

# If not running:
sudo systemctl start mysql
```

---

## Pre-Flight Before Any Demo Run

Run these every time before starting the JAR. They clear leftover state that causes the most
common demo failures.

```bash
# Kill any leftover JAR process
pkill -f "ai-mcp-lab" 2>/dev/null; echo "kill sent (or nothing was running)"

# Wait one second for sockets to close
sleep 1

# Verify port 8080 is free
ss -lntp | grep 8080
# Expected: (empty — no output)

# Verify no java/ai-mcp-lab process is still alive
ps aux | grep "ai-mcp-lab" | grep -v grep
# Expected: (empty — no output)
```

---

## DEMO PART A — REST Mode (Default Profile)

**Goal:** show that the REST endpoints work exactly as a conventional Spring Boot API — no MCP
involved, just `curl` calling `EmployeeController` and `DepartmentController`.

### Step A1 — Start the JAR (Terminal 1)

```bash
cd /home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar
```

Watch for these markers in Terminal 1's output:

| Marker | Expected in default mode? |
|---|---|
| `McpServerConfig loaded — profile-active beans will follow @ ...` | ✅ Yes |
| `[MCP] stdio transport bean created (profile=mcp)` | ❌ No |
| `[MCP] HTTP/SSE transport bean created (profile=mcp-http)` | ❌ No |
| `[MCP] MCP server built — 11 tools registered` | ❌ No — no transport bean means no MCP server |
| `Tomcat started on port(s): 8080` | ✅ Yes |
| `Started AiMcpLabApplication in X.X seconds` | ✅ Yes |

The MCP markers are absent because no `@Profile("mcp")` or `@Profile("mcp-http")` bean fires
in default mode. This is the `@ConditionalOnBean` guard working correctly.

### Step A2 — List employees (Terminal 2)

```bash
curl -s http://localhost:8080/api/employees | python3 -m json.tool
```

Expected: JSON array of five employees — Ravi Kumar, Priya Sharma, Amit Patel, Kiran Nair,
Sunita Reddy.

### Step A3 — List departments (Terminal 2)

```bash
curl -s http://localhost:8080/api/departments | python3 -m json.tool
```

Expected: JSON array of three departments — Engineering (Hyderabad), Sales (Mumbai), HR
(Bangalore) — each with an `employeeCount` field from the LEFT JOIN query.

### Step A4 — Stop the JAR

Press `Ctrl+C` in Terminal 1. The JAR shuts down cleanly.

---

## DEMO PART B — MCP HTTP/SSE Mode (Production-Style)

**Goal:** show the same data reachable over MCP via HTTP/SSE — the transport pattern for
production deployments where the AI client connects over the network rather than spawning
the server as a child process.

### Pre-flight

```bash
pkill -f "ai-mcp-lab" 2>/dev/null; sleep 1
ss -lntp | grep 8080   # must be empty
```

### Step B1 — Start the JAR in HTTP/SSE mode (Terminal 1)

```bash
cd /home/deva/Projects/CursorAI_Projects/MCP/ai-mcp-lab
java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
```

All four MCP build markers should appear:

| Marker | Expected? |
|---|---|
| `McpServerConfig loaded — profile-active beans will follow @ ...` | ✅ |
| `[MCP] HTTP/SSE transport bean created (profile=mcp-http)` | ✅ |
| `[MCP] HTTP servlet registered at /sse and /mcp/messages` | ✅ |
| `[MCP] MCP server built — 11 tools registered` | ✅ |
| `Tomcat started on port(s): 8080` | ✅ |
| `Started AiMcpLabApplication in X.X seconds` | ✅ |

If you see fewer than all four MCP markers, the JAR is stale or the profile flag was not passed.
Rebuild with `mvn clean package -DskipTests` and retry.

### Step B2 — Prove the /sse endpoint is alive (Terminal 2)

```bash
curl -i -s --max-time 2 http://localhost:8080/sse | head -12
```

Expected output:

```
HTTP/1.1 200
Content-Type: text/event-stream;charset=UTF-8
Transfer-Encoding: chunked
...

event: endpoint
data: /mcp/messages?sessionId=<UUID>
```

The `event: endpoint` line tells AI clients where to POST their JSON-RPC requests. This is the
MCP HTTP/SSE handshake. If you get `404` or `500`, the servlet registration bean was not
created — check that `--spring.profiles.active=mcp-http` was passed.

### Step B3 — Connect Cursor to the HTTP/SSE server

In Cursor:

1. Open **Settings → Tools & MCPs** (or `Cmd/Ctrl+Shift+P` → "MCP: Open Settings")
2. Find the `ai-mcp-lab-http` entry
3. Toggle it **ON** if not already active
4. Wait 5–10 seconds for Cursor to complete the handshake
5. Expected: 🟢 green dot next to `ai-mcp-lab-http` + **"11 tools enabled"** shown

If it stays red, check that Terminal 1's JAR is still running and `/sse` returned 200 in Step B2.

### Step B4 — Run a tool call from Cursor chat

In Cursor's Composer or Chat window, type:

```
Using ai-mcp-lab-http, list all employees
```

Cursor may show a permission prompt ("Allow ai-mcp-lab-http to read data?") — click **Allow**.

Expected: Cursor displays a table or prose listing all five employees with their departments
and salaries, sourced from `EmployeeTool.listEmployees()` via `EmployeeRepository.findAll()`.

### Step B5 — Watch the tool call traffic in Terminal 1

While Cursor is calling the tool, Terminal 1's console shows the SDK's debug logging (because
`logging.level.io.modelcontextprotocol=DEBUG` is set in `application-mcp-http.properties`):

```
DEBUG ... - session <UUID>: dispatching tools/list
DEBUG ... - session <UUID>: dispatching tools/call listEmployees
INFO  ... - listEmployees: 5 row(s), deptFilter=null
DEBUG o.s.jdbc.core.JdbcTemplate - Executing prepared SQL query
DEBUG o.s.jdbc.core.JdbcTemplate - Executing SQL statement [SELECT id, name...]
```

Point this out during the demo — every tool call from the AI maps to a real SQL query through
`EmployeeRepository`, the same repository that `EmployeeController` uses for REST callers.

### Step B6 — More demo prompts to try

| Prompt to type in Cursor | Tool(s) expected to fire |
|---|---|
| `"Tell me about Priya Sharma"` | `listEmployees` (search by name in result) |
| `"Which department has the most employees?"` | `listDepartments` + counts |
| `"Add a new employee: Rahul Joshi, rahul.joshi@mcplab.com, Engineering, salary 78000"` | `getDepartmentById` (validate dept) → `createEmployee` |
| `"What is the current database status?"` | `getDatabaseStatus` |
| `"Delete department Marketing"` | `getDepartmentById` → `deleteDepartment` (or error if employees present) |

After the `createEmployee` prompt, verify in Terminal 2:

```bash
mysql -uroot -pPin@801698 ai_mcp_lab \
  -e "SELECT id, name, email, salary FROM Employees WHERE name='Rahul Joshi';"
```

Expected: new row with auto-assigned `id` and `createdAt`. The AI used `EmployeeRepository.create()`
— the same code path as `POST /api/employees`.

### Step B7 — Stop the JAR

`Ctrl+C` in Terminal 1.

---

## DEMO PART C — MCP Stdio Mode (Cursor Child-Spawn)

**Goal:** show the same 11 tools available when the JAR runs as a child process of Cursor, with
Cursor managing the entire lifecycle via stdin/stdout. This is the lowest-friction local-dev mode
— no server to start manually, no port to manage.

> **Important:** in stdio mode you do **not** start the JAR manually. Cursor spawns it
> automatically when you enable the `ai-mcp-lab-stdio` server. The presenter's only action is
> toggling the right entry in Cursor's settings.

### Pre-flight

```bash
pkill -f "ai-mcp-lab" 2>/dev/null; sleep 1
ps aux | grep "ai-mcp-lab" | grep -v grep   # must be empty
```

No port check needed — stdio mode does not open port 8080.

### Step C1 — Switch Cursor to the stdio server

In Cursor **Settings → Tools & MCPs**:

1. Toggle **OFF** `ai-mcp-lab-http` (if it was on from Part B)
2. Wait 2 seconds
3. Toggle **ON** `ai-mcp-lab-stdio`
4. Wait 5–10 seconds for Cursor to spawn the JAR and complete the MCP handshake
5. Expected: 🟢 green dot + **"11 tools enabled"** next to `ai-mcp-lab-stdio`

### Step C2 — Run the same demo prompts

Stdio mode exposes identical tools — same MCP server, same repositories, same MySQL data. The
AI cannot tell the difference between transport modes from the response content.

```
List all employees from Engineering department
```

### Step C3 — Explain the difference (talking points)

| Dimension | HTTP/SSE (Part B) | Stdio (Part C) |
|---|---|---|
| Who starts the server | You (`java -jar ... --spring.profiles.active=mcp-http`) | Cursor (via `~/.cursor/mcp.json` config) |
| Lifecycle | Independent — survives Cursor restart | Tied to Cursor — dies with Cursor |
| Reachable from | Any HTTP client that knows the URL | Only the Cursor process that spawned it |
| Multiple AI clients | ✅ Each SSE connection is a separate session | ❌ One-client-at-a-time |
| Tomcat | ON (port 8080) | OFF (`spring.main.web-application-type=none`) |
| REST endpoints | Active (`/api/employees`, etc.) | Not exposed |
| Best for | Deployed servers, team-shared AI agents | Local personal-dev, secure isolated access |

### Step C4 (Optional) — Show the child process

```bash
ps -ef | grep "ai-mcp-lab" | grep -v grep
```

Expected: a `java` process with a parent PID that belongs to Cursor. This makes the lifecycle
relationship visible: when Cursor exits, that java process disappears with it.

---

## Quick Verification Cheat Sheet

Use this table during the demo when something looks wrong and you need to diagnose fast.

| What to check | Command (Terminal 2) | Expected result |
|---|---|---|
| Port 8080 free? | `ss -lntp \| grep 8080` | (empty) |
| JAR running? | `ps aux \| grep ai-mcp-lab \| grep -v grep` | 0 or 1 process |
| REST employees works? | `curl -s http://localhost:8080/api/employees \| head -c 200` | JSON starting with `[{"id":1` |
| REST departments works? | `curl -s http://localhost:8080/api/departments \| head -c 200` | JSON starting with `[{"id":1` |
| `/sse` alive? | `curl -i -s --max-time 2 http://localhost:8080/sse \| head -3` | `HTTP/1.1 200` + `text/event-stream` |
| MySQL connected? | `mysql -uroot -pPin@801698 ai_mcp_lab -e "SELECT 1;"` | `1` |
| Build markers fired? | (Terminal 1 logs) | All 4 `[MCP]` lines visible |
| Cursor sees 11 tools? | Cursor Settings → Tools & MCPs | 🟢 "11 tools enabled" |

---

## Common Gotchas During a Demo

**Stale JAR — code changes not appearing.**
Forgot to `mvn clean package` after editing a source or properties file. The JAR embedded its
files at build time; editing the source never touches the running artifact.
Fix: `mvn clean package -DskipTests` then restart.

**Port 8080 still in use after Ctrl+C.**
The previous JVM is still in TCP `TIME_WAIT` or was not fully stopped.
Fix:
```bash
pkill -f "ai-mcp-lab"; sleep 1; ss -lntp | grep 8080
```

**Cursor status dot stays red after toggling on.**
The server handshake failed — usually because the JAR isn't running (for `ai-mcp-lab-http`) or
the JAR path in `~/.cursor/mcp.json` points to an old or missing artifact (for
`ai-mcp-lab-stdio`).
Fix: toggle off → wait 2 seconds → toggle on. Verify JAR exists at the path in `mcp.json`.

**Both servers enabled at the same time.**
When both `ai-mcp-lab-stdio` and `ai-mcp-lab-http` are active, the AI client may call tools on
either server. The responses will be the same but the demo narrative becomes confusing.
Fix: enable only ONE server for each demo segment.

**MySQL not running — HikariPool fails at startup.**
The JAR connects to MySQL during Spring context initialisation. If MySQL is down, HikariCP
throws a `HikariPool-1 - Exception during pool initialization` and the JAR exits.
Fix: `sudo systemctl start mysql` before starting the JAR.

**`Picked up JAVA_TOOL_OPTIONS: ...` warning on startup.**
Some environments set `JAVA_TOOL_OPTIONS` globally (for remote debugging, agents, etc.). This
warning goes to stderr and is harmless. Ignore it — it does not affect behaviour.

**AI asks for clarification instead of calling a tool.**
The tool description is ambiguous or the AI does not know it should call a tool for this query.
Rephrase to be more explicit: "Using ai-mcp-lab-http, call getDatabaseStatus" rather than
"Is the database okay?". Alternatively, start the prompt with `@ai-mcp-lab-http` to anchor the
AI to the specific server.

---

## Suggested 5-Minute Demo Flow

For the actual peer-dev presentation, this sequence hits all the key points without rushing.

| Time | What to do | Why |
|---|---|---|
| **0:00–1:00** | Open `docs/ARCHITECTURE.md` in a browser or editor; point at the ASCII diagram — "two doors, one room" | Sets the mental model before any code runs |
| **1:00–2:00** | Start JAR in `mcp-http` mode (Part B, Step B1); point at Terminal 1 and read the four `[MCP]` build markers aloud | Shows the profile-gating and startup instrumentation |
| **2:00–3:00** | Open Cursor → Settings → Tools & MCPs; show `ai-mcp-lab-http` going from grey to 🟢 "11 tools enabled" | The "wow moment" — AI discovers tools automatically |
| **3:00–4:00** | Type `"list all employees"` in Cursor chat; show the response; point at Terminal 1 log showing the SQL query | Connects AI output → MCP tool → JdbcTemplate → MySQL |
| **4:00–5:00** | Type `"Add Rahul Joshi to Engineering, salary 78000"` in Cursor chat; allow the permission; run the MySQL verify command | Shows a write operation — AI as a data-entry agent |
| **Bonus** | Toggle off `mcp-http`, toggle on `mcp-stdio` in Cursor; re-run `"list all employees"` | Same JAR, same tools, different transport — makes the architecture claim tangible |

```bash
# MySQL verify command for the "Add Rahul Joshi" step
mysql -uroot -pPin@801698 ai_mcp_lab \
  -e "SELECT id, name, email, departmentId, salary FROM Employees ORDER BY id DESC LIMIT 3;"
```

---

Total demo time: **5–7 minutes** if everything is rehearsed. Pre-flight matters more than
runtime — most demo failures come from leftover state, not code. Run the pre-flight section
twice the day before, and once immediately before the presentation starts.

---

See also: [ARCHITECTURE.md](ARCHITECTURE.md) — the "two doors, one room" diagram and mode comparison table  
See also: [LOGGING.md](LOGGING.md) — how to redirect stdio stderr to a file if you need wire traffic during the demo  
See also: [DEBUGGING-STORY.md](DEBUGGING-STORY.md) — the gotchas section maps to real issues from Issues 4, 7, and 8
