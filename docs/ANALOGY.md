# AI MCP Lab — The Complete Analogy Guide

> **Goal:** Read this file once, close your eyes, and see the entire system.  
> Every layer, every technology, every flow — mapped to something you already know.

---

## Part 1 — The Grand Metaphor: A Government Records Office

Imagine a large **Government Records Office** in a city. It stores records about employees and
departments for an organisation. Citizens (humans) and Government Officials (AI models) both need
to query these records — but they use completely different windows.

| Who | Comes through | Uses | Talks in |
|---|---|---|---|
| Citizen (curl / browser / React app) | **Front Door — REST Window** | URL-based requests | HTTP verbs + JSON |
| Government Official (Cursor / Claude) | **Side Door — MCP Window** | Named tool requests | JSON-RPC over stdio or SSE |
| The Clerk (Repository) | Both windows point to **the same desk** | JdbcTemplate SQL | MySQL |

Both windows open into **the same room**. Same file cabinets (MySQL tables). Same clerk
(`EmployeeRepository`, `DepartmentRepository`). No duplicate cabinets, no duplicate clerks.

This is the entire architecture of `ai-mcp-lab` in one picture.

---

## Part 2 — Technology Nicknames (Sticky Labels)

Every technology gets a nickname you will not forget.

| Technology | Nickname | What it actually does |
|---|---|---|
| Spring Boot | **The Auto-Butler** | Sets up your entire mansion automatically — wires all rooms, hires all staff, opens all doors. You just say "start." |
| Spring MVC (`@RestController`) | **The Receptionist** | Stands at the front desk, receives HTTP requests, calls the right department, sends back a typed response. |
| MCP SDK (`McpSyncServer`) | **The AI Interpreter** | Stands at the side door for AI clients. Speaks JSON-RPC. Knows the exact vocabulary AI models understand. |
| JdbcTemplate | **The Careful Typist** | Fills in pre-printed SQL forms. Never freehand — always uses blanks (`?`) to prevent forgery (SQL injection). |
| HikariCP | **The Car Pool** | A fleet of 10 cars (database connections) ready to go. You borrow one, drive to MySQL, come back, return the car. No waiting in line for a new car every time. |
| MySQL | **The Filing Cabinet** | Permanent, organised storage. Two drawers: `Employees` and `Departments`. |
| Spring Profiles | **The Uniform Room** | The same building, different uniforms. Put on the `mcp` uniform and you become a stdio MCP server. Put on `mcp-http` and you open the SSE window too. Take it all off and you are just a REST server. |
| `McpServerConfig` | **The Switchboard Operator** | Looks at which uniform is being worn, plugs the right cables. If no MCP uniform → no MCP cables plugged in at all. |
| `LoggingStdioTransportProvider` | **The Glass-Walled Corridor** | The stdio pipe now has glass walls. Every message that passes through it is visible to the observer (stderr). The messages are not changed — just watched. |
| `EmployeeTool` / `DepartmentTool` | **The AI-Friendly Translators** | Take the AI's arguments (`Map<String,Object>`) and translate them into the clerk's language (typed Java values + SQL). Like a court interpreter who also validates the witness's passport before they testify. |
| `DatabaseStatusTool` | **The Night Watchman's Check** | Knocks on the filing cabinet (`SELECT 1`) to confirm it is still there and answers. Reports back in plain English. |
| `GlobalExceptionHandler` | **The Triage Nurse** | If something goes wrong, this nurse decides: "404 — we don't have that file" or "409 — the rule says you can't do that." Communicates the bad news calmly, not as a crash. |
| SSE (Server-Sent Events) | **The Public Address System** | The server holds a microphone open (`GET /sse`). AI clients tune in. When a tool call response is ready, the server speaks into the mic. One-way broadcast, always live. |
| stdio (stdin/stdout) | **The Pneumatic Tube** | The old-fashioned message tube between Cursor's desk and the JAR's desk. Private, direct, no network needed. Only the two ends can read it. |
| `@ConditionalOnBean` | **The Safety Interlock** | A gate on the MCP server assembly line: "Only build this machine if the conveyor belt (transport) exists." Prevents the factory from exploding when run in REST-only mode. |
| Java Records (`EmployeeRequest`, `EmployeeResponse`) | **Standardised Forms** | Pre-printed government forms with fixed fields. The receptionist hands the citizen a form to fill in (request). When the clerk is done, they stamp a pre-printed receipt (response). |
| `@ControllerAdvice` | **The Department of Complaints** | A separate department that receives all unresolved problems forwarded from every receptionist desk. Converts raw exceptions into polite, formatted apology letters. |
| Jackson (`ObjectMapper`) | **The Translator Between Languages** | Converts between Java objects (how the program thinks) and JSON strings (how the AI thinks). Like a bilingual secretary who types in both languages fluently. |
| Logback / SLF4J | **The Office Diary** | Every significant event is written down. In stdio mode, the diary is kept in a back room (stderr) so it does not interfere with the official wire (stdout). In HTTP mode, the diary is read aloud at the front desk (console). |
| `@SpringBootApplication` | **The Master Key** | One annotation that unlocks everything: component scan (finds all staff), auto-configuration (wires all rooms), `@Configuration` (allows custom furniture). |

---

## Part 3 — Why MCP Exists (The Deep Analogy)

### The Problem with REST for AI

Imagine you are a very intelligent but literal-minded robot. I give you a phone and say:  
*"Call the records office and ask for Employee 42."*

You would ask me:
- What is the phone number?
- What exact words do I say when they answer?
- If they say "extension required," what extension?
- What format do they give the answer in?
- If it says "404 Not Found" in the response, is that a problem?

**REST APIs are designed for programmers who already know all these answers.** The programmer
writes code that constructs the URL (`/api/employees/42`), adds auth headers, handles status
codes, and parses JSON. The AI model cannot reliably do any of that — it would hallucinate URLs,
forget headers, and misread error codes.

### The Solution: MCP is a Vending Machine

Instead of the phone, give the AI a **vending machine**. The vending machine:

1. Has buttons labelled in plain English: `Get Employee By ID`, `List All Employees`, `Create Employee`
2. Has slots labelled with what to put in: `Employee ID (number)`, `Name (text)`, `Salary (number)`
3. Returns the snack in plain text, no wrapper to parse
4. If something goes wrong, displays a plain-English message: `Employee not found`

The AI reads the button labels (tool descriptions), presses the right button, inserts the right
coins (typed arguments from the schema), and receives the snack (plain text response). No URL
construction, no header management, no status code interpretation.

**MCP = a vending machine designed for AI models.**

### The Three-Act Analogy for MCP's Existence

**Act 1 — The World Without MCP:**
AI asks: "Get me the employee list."
You tell it: "Call `GET /api/employees` with an `Authorization: Bearer <token>` header."
AI tries. Hallucinates the token. Gets a 401. Gives up. Or worse: makes up data.

**Act 2 — The Problem Stated:**
AI models are brilliant at language and reasoning. They are not reliable HTTP clients. They were
trained on natural language, not on curl manual pages. What they need is a vocabulary of named
capabilities they can discover and use without knowing the underlying protocol.

**Act 3 — The World With MCP:**
AI sees: tool `listEmployees`, description "Returns all employees, optionally filtered by department."
AI calls: `tools/call listEmployees` with `{"departmentId": 1}`.
Server returns: a JSON string of employee records.
AI reads it, reasons about it, responds to the user.
No URLs. No headers. No status codes. Just: name, arguments, result.

---

## Part 4 — The MCP Handshake Story

### The Story of the First Conversation (Stdio Mode)

Picture Cursor as a **new manager** arriving at the office. The JAR is the **specialist contractor**
who just walked in. They have never met. Before any real work happens, they go through a formal
introduction ritual.

**Scene 1 — Cursor spawns the JAR as a child process**

```
Cursor:  [opens pneumatic tube to JAR's desk]
JAR:     [wakes up, Spring Boot initialises, MCP server loads]
stderr:  =========================================
         MCP wire logging active — inbound/outbound JSON-RPC lines appear on stderr
         =========================================
```

The JAR is now listening at the end of the tube (stdin). Cursor starts speaking.

---

**Scene 2 — The Initialize Handshake**

```
Cursor → JAR (>>> MCP IN):
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{
  "protocolVersion":"2024-11-05",
  "clientInfo":{"name":"cursor","version":"1.0"},
  "capabilities":{}
}}
```

Translation: *"Hello. I am Cursor. I speak MCP protocol version 2024-11-05. What can you do?"*

```
JAR → Cursor (<<< MCP OUT):
{"jsonrpc":"2.0","id":1,"result":{
  "protocolVersion":"2024-11-05",
  "serverInfo":{"name":"ai-mcp-lab","version":"0.0.1"},
  "capabilities":{"tools":{}}
}}
```

Translation: *"Hello. I am ai-mcp-lab. I also speak 2024-11-05. I have tools. Ask me what they are."*

**The handshake is a mutual introduction.** Both sides confirm they speak the same dialect of
the protocol. If versions mismatched, they would politely decline to continue.

---

**Scene 3 — Tool Registration / Discovery (The Menu)**

```
Cursor → JAR (>>> MCP IN):
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

Translation: *"Show me your menu of capabilities."*

```
JAR → Cursor (<<< MCP OUT):
{"jsonrpc":"2.0","id":2,"result":{"tools":[
  {
    "name":"getDatabaseStatus",
    "description":"Checks whether the MySQL database is reachable...",
    "inputSchema":{"type":"object","properties":{},"required":[]}
  },
  {
    "name":"listEmployees",
    "description":"Returns all employees. Optionally filter by departmentId.",
    "inputSchema":{
      "type":"object",
      "properties":{
        "departmentId":{"type":"integer","description":"Filter by department ID"}
      }
    }
  },
  ...9 more tools...
]}}
```

Translation: *"Here is our menu. 11 dishes. Each one has a name, a description, and a list of
ingredients you need to provide."*

This is **tool registration from the client's perspective.** The server pre-registered all 11 tools
in `McpServerConfig` at startup. The `tools/list` call just exposes that pre-built menu to whoever
asks. No new tool is created — the catalogue is read off the shelf.

The AI client (Cursor / Claude) **reads these descriptions and decides for itself** which tool
matches what the user asked. The description text is the AI's instruction manual.

---

**Scene 4 — Tool Invocation (Ordering from the Menu)**

User says to Cursor: *"Get me the list of all employees in department 2."*

Cursor has read the menu. It selects `listEmployees`. It forms the order slip.

```
Cursor → JAR (>>> MCP IN):
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{
  "name":"listEmployees",
  "arguments":{"departmentId":2}
}}
```

Translation: *"I would like dish #2 — listEmployees — with the ingredient departmentId=2."*

Inside the JAR, the call travels:

```
McpSyncServer (receives JSON-RPC)
  → EmployeeTool.handleListEmployees(Map args)
      → args.get("departmentId") → cast to Integer → 2
      → EmployeeRepository.findAll(2)
          → JdbcTemplate: SELECT e.*, d.name dept_name
                           FROM Employees e JOIN Departments d ON e.departmentId = d.id
                           WHERE e.departmentId = ?    [params: 2]
          → MySQL returns rows
      → Jackson ObjectMapper serialises List<Map> → JSON string
  → McpSyncServer wraps in MCP result envelope
```

```
JAR → Cursor (<<< MCP OUT):
{"jsonrpc":"2.0","id":3,"result":{"content":[
  {"type":"text","text":"{\"employees\":[{\"id\":2,\"name\":\"Ravi Kumar\",...},...]}"}
]}}
```

Translation: *"Here is your dish. The ingredients you provided produced these results."*

Cursor reads the text. Claude reasons about it. The user sees: *"In department 2, there are 2 employees: Ravi Kumar and..."*

---

## Part 5 — The Three Uniforms (Spring Profiles as Costume Changes)

Same actor (the JAR). Three different costumes. Completely different behaviour.

### Costume 1 — The REST Waiter (Default, No Profile)

```bash
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar
```

- Tomcat opens the restaurant on **port 8080**
- REST controllers (`EmployeeController`, `DepartmentController`) stand at the counter
- MCP server: **not present** — `@ConditionalOnBean` says "no transport bean → no MCP server"
- Customers: curl, browser, React app at port 5173
- The side door for AI clients is **locked shut**

**Memory hook:** *No profile = pure REST waiter, no AI magic.*

---

### Costume 2 — The Pneumatic Tube Specialist (Profile: `mcp`)

```bash
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp
```

- Tomcat: **shut down** — `spring.main.web-application-type=NONE`
- No HTTP port open. No REST endpoints reachable.
- `LoggingStdioTransportProvider` wraps stdin/stdout in glass-walled corridors
- `McpSyncServer` listens on stdin, speaks to stdout
- Customers: **only the process that spawned this JAR** (Cursor, MCP Inspector, Claude Desktop)
- Logging: all SLF4J output **silenced** (blank console pattern) — stdout is the wire
- Wire traffic visible only on **stderr**

**Memory hook:** *Profile mcp = sealed pneumatic tube, AI-only, invisible to the network.*

---

### Costume 3 — The Full-Service Embassy (Profile: `mcp-http`)

```bash
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
```

- Tomcat: **running on port 8080**
- REST endpoints: **active** (`/api/employees`, `/api/departments`)
- MCP via HTTP/SSE: **active** (`GET /sse`, `POST /mcp/messages`)
- Both doors open simultaneously — the front door (REST) and the side door (SSE)
- Multiple AI clients can connect at once (each SSE connection = a separate session)
- Customers: everyone — curl, browser, Cursor, Claude Desktop over the network
- Logging: normal Spring Boot console, DEBUG on `io.modelcontextprotocol` and `jdbc.core`

**Memory hook:** *Profile mcp-http = both doors open, network-reachable AI embassy.*

---

## Part 6 — Layer-by-Layer Analogy Table

| Layer | Java Class(es) | Real World Analogy | Simple Words |
|---|---|---|---|
| **Entry Point** | `AiMcpLabApplication` | The building's main switch | One flip starts everything |
| **Configuration** | `McpServerConfig` | The building's wiring diagram | Decides which rooms get electricity based on the active profile |
| **REST Controller** | `EmployeeController`, `DepartmentController` | Front desk receptionist | Receives HTTP requests, validates, delegates, returns typed response |
| **MCP Tool** | `EmployeeTool`, `DepartmentTool`, `DatabaseStatusTool` | AI-side interpreter + validator | Receives AI's arguments, validates, calls repository, serialises result |
| **Repository** | `EmployeeRepository`, `DepartmentRepository` | The clerk at the shared filing desk | Runs parameterised SQL, returns raw rows as `Map<String,Object>` |
| **Database** | MySQL `ai_mcp_lab` | The filing cabinets | Two drawers: `Employees` and `Departments`, FK relationship between them |
| **Transport — Stdio** | `LoggingStdioTransportProvider` | Glass-walled pneumatic tube | Private pipe between Cursor and JAR; all traffic visible on stderr |
| **Transport — HTTP/SSE** | `HttpServletSseServerTransportProvider` | Public address system + suggestion box | `/sse` = PA system (streaming), `/mcp/messages` = suggestion box (POST) |
| **DTO** | `EmployeeRequest/Response`, `DepartmentRequest/Response` | Standardised government forms | Fixed-field input forms (request) and stamped receipts (response) |
| **Exception Handler** | `GlobalExceptionHandler` | Department of Complaints | Converts raw Java exceptions into polite HTTP error responses |
| **Connection Pool** | HikariCP (auto-configured) | The company car pool | 10 pre-warmed database connections, borrowed and returned per request |
| **Logging** | Logback + SLF4J + `System.err` | The office diary + the glass corridor | Diary (SLF4J) for normal logs; corridor (stderr) for wire traffic in stdio mode |
| **Profile System** | `application-mcp.properties`, `application-mcp-http.properties` | The uniform room | Same JAR, different behaviour overlay per profile |
| **Conditional Wiring** | `@ConditionalOnBean(McpServerTransportProvider.class)` | The safety interlock | MCP server only assembles if a transport exists |
| **JSON Serialisation** | Jackson `ObjectMapper` | Bilingual secretary | Converts between Java objects and JSON strings |
| **Input Validation** | `@Valid`, `@NotBlank`, `@Email`, `@Min` in DTOs | The form-checker at the window | Rejects incomplete or malformed input before it reaches the clerk |

---

## Part 7 — Complete Flow Narratives (Story Mode)

### Story A — "Give me all employees" via REST

**The Citizen visits the Front Door**

1. **The citizen** (React app, curl, browser) walks up to the front door and says:
   `GET /api/employees`

2. **Tomcat's security guard** (DispatcherServlet) checks: "Do I have a receptionist for this
   URL?" — yes, `EmployeeController.listEmployees()` is mapped to `GET /api/employees`.

3. **The receptionist** (`EmployeeController`) calls back to the filing room:
   `repository.findAll(null)` (no department filter).

4. **The clerk** (`EmployeeRepository`) fills in the pre-printed SQL form:
   ```sql
   SELECT e.id, e.name, e.email, e.salary, e.departmentId,
          d.name AS departmentName, e.createdAt
   FROM Employees e
   JOIN Departments d ON e.departmentId = d.id
   ```
   Borrows a car from HikariCP, drives to MySQL, gets the rows back, returns the car.

5. **The receptionist** receives the raw rows (`List<Map<String,Object>>`), converts each one to a
   typed `EmployeeResponse` record (via `EmployeeResponse.from(Map)`), wraps in a list.

6. **Jackson** (bilingual secretary) converts the `List<EmployeeResponse>` to a JSON string.

7. **HTTP 200 OK** goes back to the citizen with the JSON body.

**Total time:** under 5 ms. The only real work was the MySQL round-trip.

---

### Story B — "Give me all employees" via MCP (Stdio)

**The AI Official visits the Side Door (Pneumatic Tube)**

1. **Cursor** (the AI client) has the JAR running as a child process. It has already done the
   handshake (initialize + tools/list). The AI has read the menu.

2. User tells Cursor: *"List all employees."* Claude selects tool `listEmployees`, no filter needed.

3. Cursor drops a message into the pneumatic tube (stdin):
   ```json
   {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"listEmployees","arguments":{}}}
   ```

4. **The glass-walled corridor** (`LoggingStdioTransportProvider`) intercepts the incoming bytes,
   buffers until newline, emits to stderr:
   ```
   >>> MCP IN  : {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"listEmployees","arguments":{}}}
   ```
   Then passes the bytes unchanged to the MCP SDK.

5. **McpSyncServer** parses the JSON-RPC, finds `tools/call`, looks up the registered handler for
   `listEmployees`, calls `EmployeeTool.handleListEmployees(Map args)`.

6. **`EmployeeTool`** checks args (no `departmentId` provided → null → no filter). Calls
   `repository.findAll(null)`. Same clerk, same SQL, same HikariCP car pool as Story A.

7. **`EmployeeTool`** receives `List<Map<String,Object>>`, wraps in `{"employees": [...]}`,
   serialises with Jackson to a JSON **string**.

8. **McpSyncServer** wraps the string in the MCP content envelope:
   ```json
   {"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"{\"employees\":[...]}"}]}}
   ```

9. The output stream (`LoggingOutputStream`) emits bytes to real stdout immediately (critical for
   framing), then buffers and emits to stderr:
   ```
   <<< MCP OUT : {"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"..."}]}}
   ```

10. **Cursor receives the response.** Claude reads the `text` field. The user sees a natural
    language answer.

**Total time:** under 10 ms. The only additional cost over REST is JSON-RPC parsing overhead (< 1 ms).

---

### Story C — "Create an employee" via REST (with validation)

1. Citizen sends: `POST /api/employees` with body:
   ```json
   {"name":"Priya Sharma","email":"priya@example.com","salary":75000,"departmentId":1}
   ```

2. `EmployeeController.createEmployee(@Valid @RequestBody EmployeeRequest req)` is called. The
   `@Valid` annotation triggers **Bean Validation** — the form-checker at the window:
   - `@NotBlank` on `name` → "Priya Sharma" passes
   - `@Email` on `email` → "priya@example.com" passes
   - `@Min(0)` on `salary` → 75000 passes
   - `@NotNull` on `departmentId` → 1 passes
   All good. The form-checker stamps it.

3. Controller calls `departmentRepository.existsById(1)`. Does department 1 exist?
   Clerk checks the Departments drawer. Yes → continue. No → throw `ResourceNotFoundException`.

4. `employeeRepository.create(req)` runs:
   ```sql
   INSERT INTO Employees (name, email, salary, departmentId) VALUES (?,?,?,?)
   ```
   Returns the new auto-generated `id`.

5. Controller calls `employeeRepository.findById(newId)` — re-fetch to get the complete row
   including `createdAt` and the joined `departmentName`.

6. Returns `HTTP 201 Created` with the `EmployeeResponse` body.

---

### Story D — "Delete a department that has employees" via REST (Business Rule)

1. Citizen sends: `DELETE /api/departments/1`

2. `DepartmentController.deleteDepartment(1)` calls `departmentRepository.countEmployees(1)`.
   Query: `SELECT COUNT(*) FROM Employees WHERE departmentId = 1`. Result: 3.

3. 3 > 0 → throw `BusinessRuleException("Cannot delete department with active employees")`.

4. **The triage nurse** (`GlobalExceptionHandler.handleBusinessRuleException`) catches this,
   returns `HTTP 409 Conflict` with body `{"error":"Cannot delete department with active employees"}`.

5. Citizen receives 409. No data was changed. The filing cabinet is intact.

---

### Story E — "Is the database alive?" via MCP

1. AI calls `tools/call getDatabaseStatus` with empty arguments.

2. `DatabaseStatusTool.handle()` runs: `jdbcTemplate.queryForObject("SELECT 1", Integer.class)`.
   This is the night watchman's knock — the simplest possible database round-trip.

3. If result is 1 → returns string:
   `"Database is UP. Connected to MySQL on localhost:3306, database: ai_mcp_lab. Status checked at: 2026-05-09T10:30:00Z"`

4. If exception → catches it, returns:
   `"Database is DOWN. Error: <exception message>"`

5. AI reads the plain English string. No JSON parsing needed. Reports back to user directly.

---

### Story F — Spring Boot Startup Sequence (The Building Opening for Business)

```
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http
```

**The chronological story of a building coming to life:**

```
T+0.0s  JVM loads. SpringApplication.run() fires.
        @SpringBootApplication tells the butler: "scan all rooms under com.mcplab."

T+0.2s  Auto-configuration runs. Butler inspects the pantry (classpath):
        - Sees HikariCP → wires DataSource bean → "Car pool ready"
        - Sees JdbcTemplate → wires it to DataSource → "Typist has keys to the cars"
        - Sees Jackson → wires ObjectMapper → "Bilingual secretary hired"
        - Sees Spring MVC → wires DispatcherServlet → "Front desk receptionist hired"

T+0.4s  Component scan finds all @Component, @Repository, @RestController beans:
        EmployeeRepository, DepartmentRepository wired.
        EmployeeTool, DepartmentTool, DatabaseStatusTool wired.
        EmployeeController, DepartmentController wired.
        GlobalExceptionHandler wired.

T+0.6s  @Configuration classes processed:
        McpServerConfig @PostConstruct fires → logs:
        "McpServerConfig loaded — profile-active beans will follow @ 2026-05-09T..."

T+0.7s  Profile mcp-http is active:
        @Profile("mcp-http") HttpServletSseServerTransportProvider bean created → logs:
        "[MCP] HTTP/SSE transport bean created (profile=mcp-http)"
        "[MCP] HTTP servlet registered at /sse and /mcp/messages"

T+0.8s  @ConditionalOnBean(McpServerTransportProvider.class) satisfied → McpSyncServer built:
        11 tools registered. Logs:
        "[MCP] MCP server built — 11 tools registered"

T+1.2s  HikariCP establishes its connection pool:
        "HikariPool-1 - Start completed."

T+1.4s  Tomcat starts:
        "Tomcat started on port 8080"

T+1.5s  Everything ready:
        "Started AiMcpLabApplication in 1.469 seconds"
```

The building is open. Both doors are unlocked. All staff are at their desks.

---

## Part 8 — The Stdout Safety Rule (The Most Critical Rule)

### The Analogy: The Telephone Switchboard

In stdio mode, the stdout pipe is like a **telephone switchboard**. Every message on the wire is
an official government communiqué in a specific format. If even one clerk shouts their personal
opinion through the switchboard by accident, the entire call is corrupted — the listener on the
other end hears noise, not a message, and hangs up.

### What happens if a log line reaches stdout in stdio mode?

```
MCP Client (Cursor) receives:
INFO 2026-05-09 10:30:00 [main] com.mcplab.server.McpServerConfig: McpServerConfig loaded
{"jsonrpc":"2.0","id":1,"result":{...}}
```

The client tries to parse the first line as JSON-RPC. It is not JSON. The client throws a JSON
parse error and disconnects. **All your tools are now unreachable.**

### The Three-Layer Defense

| Layer | Mechanism | What it does |
|---|---|---|
| **1st — Silence the diary** | `logging.pattern.console=` (empty) | Tells Logback: write nothing to stdout |
| **2nd — Raise the threshold** | `logging.level.root=WARN` | Reduces what is even worth writing |
| **3rd — Redirect the glass corridor** | `System.err.println()` in `LoggingStdioTransportProvider` | Wire traffic goes to stderr, not stdout |

Only `McpSyncServer` is allowed to write to stdout in stdio mode. Nothing else. Not even a
"Hello World" debug print.

---

## Part 9 — The "Two Doors, One Room" Insight (Visualised)

```
                    ┌─────────────────────────────────────┐
                    │           THE RECORDS ROOM           │
                    │                                      │
 FRONT DOOR         │  EmployeeRepository                  │         SIDE DOOR
 (REST)             │  DepartmentRepository                │         (MCP)
─────────────────►  │                                      │  ◄──────────────────
 HTTP request       │  SELECT * FROM Employees WHERE id=?  │  JSON-RPC tools/call
 EmployeeController │  SELECT * FROM Departments           │  EmployeeTool
 @RestController    │  INSERT / UPDATE / DELETE            │  DepartmentTool
                    │                                      │
                    │            MySQL                     │
                    │   Employees ←──FK──► Departments     │
                    └─────────────────────────────────────┘
```

The SQL in `EmployeeRepository.findAll()` is written exactly once. It is called by:
- `EmployeeController` when a browser visits `GET /api/employees`
- `EmployeeTool` when Claude calls `listEmployees`

A bug fix in that SQL, applied once, fixes both paths simultaneously.

---

## Part 10 — Key Numbers to Remember

| What | Number | Memory Hook |
|---|---|---|
| **Application port** | 8080 | "80 doubled" — the dev-friendly HTTP port |
| **React dev server port** | 5173 | Vite's default; remember "5173 = Vite" |
| **MCP tools registered** | 11 | 5 employee tools + 5 department tools + 1 database status |
| **Employee seed rows** | 5 | 5 employees across 3 departments |
| **Department seed rows** | 3 | Engineering (Hyderabad), Sales (Mumbai), HR (Bangalore) |
| **Spring Boot version** | 3.2 | Boot 3 = Spring 6 = Jakarta namespace |
| **Java version** | 21 | LTS; virtual threads available |
| **MCP SDK version** | 0.9.0 | Still pre-1.0; API may change |
| **Typical REST latency** | < 5 ms | Local MySQL, no network hop |
| **Typical MCP latency** | < 10 ms | REST + JSON-RPC parsing overhead |
| **Typical startup (REST mode)** | ~2–3 s | Tomcat init is the slow part |
| **Typical startup (stdio MCP)** | ~1.5–2 s | No Tomcat = faster start |
| **HikariCP default pool size** | 10 connections | 10 cars in the pool |
| **MCP protocol version** | 2024-11-05 | The "dialect" both sides must agree on in the handshake |
| **Max file upload** | ~1 GB | Not relevant to MCP, but in the EFFORT-Web context |

---

## Part 11 — The Profile Inheritance Story (How Properties Stack)

Think of the profile properties as **layered transparent sheets** on an overhead projector.

```
Sheet 1 (bottom): application.properties
   datasource.url = jdbc:mysql://localhost:3306/ai_mcp_lab
   datasource.username = root
   datasource.password = ...
   server.port = 8080

Sheet 2 (overlay for mcp mode): application-mcp.properties
   spring.main.web-application-type = NONE      ← turns Tomcat off
   spring.main.banner-mode = off                ← silences the Spring Boot banner
   logging.level.root = WARN                    ← quiets everything
   logging.pattern.console =                    ← blanks stdout

Sheet 2 (overlay for mcp-http mode): application-mcp-http.properties
   logging.level.org.springframework.jdbc.core = DEBUG
   logging.level.io.modelcontextprotocol = DEBUG
```

`application.properties` is always loaded. The overlay **adds to or overrides** specific keys.
The datasource is never re-declared — it is inherited from the bottom sheet every time.

This is the Boot equivalent of your EFFORT-Web's `context:property-placeholder` per-environment
files in `root-context.xml`. Same concept, different mechanism.

---

## Part 12 — The @ConditionalOnBean Story (The Safety Interlock in Detail)

### Without the interlock (the accident scenario)

Imagine the MCP server assembly line has no safety check. The factory supervisor says:
"Assemble the McpSyncServer!" But there is no conveyor belt (no transport bean). The
assembly line tries to grab the belt and finds nothing. **The factory explodes**
(`NoSuchBeanDefinitionException` at Spring startup).

### With the interlock

```java
@Bean
@ConditionalOnBean(McpServerTransportProvider.class)
public McpSyncServer mcpSyncServer(McpServerTransportProvider transport, ...) {
    ...
}
```

The gate reads: "Only attempt to assemble `McpSyncServer` if `McpServerTransportProvider`
already exists on the assembly line." In default mode (no profile), neither `@Profile("mcp")`
nor `@Profile("mcp-http")` transport beans are created. The gate stays closed.
`McpSyncServer` is never attempted. The factory starts cleanly.

**The condition is also documentation:** it says out loud, in code, "this component needs a
transport to function." A future developer reading `McpServerConfig` immediately understands
the dependency without reading the Javadoc.

---

## Part 13 — JSON-RPC vs HTTP (Side-by-Side Analogy)

| Aspect | HTTP (REST) | JSON-RPC (MCP) |
|---|---|---|
| **How you name the operation** | HTTP method + URL path (`DELETE /api/employees/3`) | `method` field in JSON body (`"method":"tools/call"`) |
| **How you identify a request** | HTTP request/response pairing (implicit) | `"id"` field — you write it, the response echoes it back |
| **How errors are reported** | HTTP status code (404, 409, 500) + body | `"error"` key in the JSON response body |
| **How you discover operations** | Read API documentation / OpenAPI spec | Call `tools/list` at runtime |
| **Who typically calls it** | Any HTTP client (browser, curl, Postman) | AI model following MCP protocol |
| **Transport** | TCP (HTTP/1.1 or HTTP/2) | Can run over TCP (HTTP/SSE) or a pipe (stdio) |
| **Session management** | Stateless (each request independent) | Stateful session (initialize → use → close) |

---

## Part 14 — The Decorator Pattern (LoggingStdioTransportProvider)

### The Analogy: The Glass-Walled Elevator

The MCP SDK comes with a regular elevator (`StdioServerTransportProvider`) — opaque, you cannot
see what is being carried. `LoggingStdioTransportProvider` is the same elevator with **glass walls**.
The people inside (the JSON-RPC bytes) move identically. You just get to watch them from the outside
(stderr) without touching them.

This is the **Decorator pattern**:

```
Original object:   StdioServerTransportProvider
                   [reads stdin → gives to SDK]
                   [gets from SDK → writes stdout]

Decorated object:  LoggingStdioTransportProvider
                   [reads stdin → logs >>> to stderr → gives to SDK]
                   [gets from SDK → writes stdout → logs <<< to stderr]
```

Key insight: the decorator **never delays the output**. Bytes go to real stdout immediately.
The logging happens after, using a separate buffer. If logging slowed down stdout, the client
would see delayed responses and the session would time out.

---

## Part 15 — Mental Model Summary (Close Your Eyes and See This)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            THE ai-mcp-lab BUILDING                           │
│                                                                              │
│  UNIFORM ROOM                    WHO IS AT THE DOOR?                         │
│  ─────────────                   ────────────────────                        │
│  No uniform  → REST only         Front door: curl, browser, React             │
│  mcp uniform → Tube only         Side tube: Cursor, Claude, Inspector         │
│  mcp-http    → Both open         Both: anyone on the network                  │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  FRONT DOOR (REST)                    SIDE DOOR (MCP)                        │
│                                                                              │
│  DispatcherServlet                    McpSyncServer                          │
│       ↓                                    ↓                                 │
│  EmployeeController                   EmployeeTool                           │
│  DepartmentController                 DepartmentTool                         │
│       ↓                               DatabaseStatusTool                     │
│  EmployeeResponse.from(Map)                ↓                                 │
│  DepartmentResponse.from(Map)         Jackson → JSON string                  │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                         SHARED ROOM (always the same)                        │
│                                                                              │
│                    EmployeeRepository   DepartmentRepository                 │
│                              ↓                                               │
│                    JdbcTemplate (parameterised SQL only)                     │
│                              ↓                                               │
│                    HikariCP (10 warm cars)                                   │
│                              ↓                                               │
│                    MySQL: Employees ←──FK──► Departments                     │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  SAFETY SYSTEMS                                                              │
│                                                                              │
│  @ConditionalOnBean  → MCP server only assembles if transport exists         │
│  logging.pattern.console=  → stdout silence in stdio mode                    │
│  System.err  → wire traffic stays off the protocol line                      │
│  GlobalExceptionHandler  → all exceptions become polite error messages       │
│  @Valid + Bean Validation  → bad input rejected at the door                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Appendix — Quick Reference Cheat Sheet

### Ports

| Port | What | When active |
|---|---|---|
| 8080 | Tomcat (REST + optional MCP/SSE) | Default mode and mcp-http mode |
| 5173 | Vite React dev server | When `npm run dev` is running |
| (none) | Stdio pipe | mcp mode only — no TCP port |

### Profile Commands

```bash
# REST only (no AI)
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar

# Stdio MCP (Cursor spawns this)
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp

# HTTP/SSE MCP + REST (both doors)
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http

# Watch wire traffic in mcp mode
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp 2>&1 | cat

# Verify fresh build markers
java -jar ai-mcp-lab-0.0.1-SNAPSHOT.jar --spring.profiles.active=mcp-http 2>&1 | grep -E "\[MCP\]|McpServer"
```

### The 11 Tools

| # | Tool Name | What it does | Arguments |
|---|---|---|---|
| 1 | `getDatabaseStatus` | Health probe (`SELECT 1`) | none |
| 2 | `listEmployees` | All employees, optional dept filter | `departmentId?` |
| 3 | `getEmployeeById` | Single employee by ID | `id` (required) |
| 4 | `createEmployee` | Insert new employee | `name`, `email`, `salary`, `departmentId` |
| 5 | `updateEmployee` | Partial update by ID | `id` + any fields to change |
| 6 | `deleteEmployee` | Remove employee by ID | `id` (required) |
| 7 | `listDepartments` | All departments with employee count | none |
| 8 | `getDepartmentById` | Single department by ID | `id` (required) |
| 9 | `createDepartment` | Insert new department | `name`, `location` |
| 10 | `updateDepartment` | Partial update by ID | `id` + any fields |
| 11 | `deleteDepartment` | Remove dept (fails if has employees) | `id` (required) |

### MCP Handshake in 4 Lines

```
1. initialize          → "Hello, I speak protocol X. What are you?"
2. initialize response → "Hello, I also speak X. I have tools."
3. tools/list          → "Show me your menu."
4. tools/list response → "Here are 11 dishes with their ingredient schemas."
```

After step 4, the AI is ready to call tools. Steps 1–4 happen once per session.

---

*This document maps every component of `ai-mcp-lab` to a real-world analogy. If any part of the
system feels abstract, come back here and find its nickname — then re-read the code with the
metaphor in mind.*
