package com.mcplab.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcplab.tools.DatabaseStatusTool;
import com.mcplab.tools.DepartmentTool;
import com.mcplab.tools.EmployeeTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/*
 * THIS SERVER SUPPORTS THREE OPERATIONAL MODES
 * =============================================
 *
 * 1. DEFAULT (no profile)
 *    Run:    java -jar target/ai-mcp-lab.jar
 *    or:     mvn spring-boot:run
 *    Tomcat: ON, port 8080
 *    Active: REST API only (/api/employees, etc.)
 *    MCP:    NOT exposed — no transport bean in context
 *    Use:    Browser / curl / React testing of REST endpoints
 *
 * 2. STDIO MCP (--spring.profiles.active=mcp)
 *    Run:    java -jar target/ai-mcp-lab.jar --spring.profiles.active=mcp
 *    Tomcat: OFF (spring.main.web-application-type=none in application-mcp.properties)
 *    Active: MCP over stdin / stdout
 *    REST:   NOT exposed — no Tomcat
 *    Use:    Cursor, Claude Desktop, MCP Inspector spawning JAR as child process
 *
 * 3. HTTP/SSE MCP (--spring.profiles.active=mcp-http)
 *    Run:    java -jar target/ai-mcp-lab.jar --spring.profiles.active=mcp-http
 *    Tomcat: ON, port 8080
 *    Active: BOTH — REST API + MCP at /sse (SSE stream) and /mcp/messages (POST)
 *    Use:    UAT / production deployment; network-accessible AI clients connect via URL
 *
 * All three modes share the same tool beans:
 *   EmployeeRepository, DepartmentRepository, EmployeeTool, DepartmentTool, DatabaseStatusTool.
 * Only the MCP transport (and whether Tomcat starts) differs.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHAT IS AN MCP SERVER?
 * ──────────────────────────────────────────────────────────────────────────────
 * An MCP (Model Context Protocol) server is like a REST API but designed for
 * AI models instead of browsers.
 *
 *   REST API  →  Browser → HTTP GET /api/employees → @RestController → JSON
 *   MCP       →  AI model → tools/call "listEmployees" → @Tool → text result
 *
 * The protocol is JSON-RPC 2.0 over a chosen transport (stdio or HTTP/SSE).
 * The AI model first asks "what tools do you have?" (tools/list), then calls
 * them by name (tools/call) — exactly like a frontend calls REST endpoints.
 *
 * HOW THIS DIFFERS FROM @RestController
 * ──────────────────────────────────────
 *   @RestController  — mapped by URL, called by HTTP clients, returns JSON/HTML
 *   @Tool (MCP)      — mapped by tool NAME, called by AI via JSON-RPC,
 *                      returns McpSchema.CallToolResult (plain text or structured)
 *
 * You can have BOTH in the same Spring Boot app:
 *   REST controllers for human users, MCP tools for AI agents.
 */
@Slf4j
@Configuration
public class McpServerConfig {

    private static final String BOX_H = "═".repeat(76);

    private static String box(String title) {
        return "\n╔" + BOX_H + "╗\n║  " + String.format("%-74s", title) + "║\n╚" + BOX_H + "╝";
    }

    private static String lbl(String label, Object value) {
        return "\n   " + String.format("%-11s", label) + " : " + value;
    }

    @PostConstruct
    void logStartup() {
        log.info(box("McpServerConfig loaded — profile beans will follow")
            + lbl("Layer",   "CONFIG → McpServerConfig")
            + lbl("Time",    java.time.Instant.now()));
    }

    // =========================================================================
    // TRANSPORT BEAN — STDIO (profile: mcp)
    // =========================================================================
    // Active only when --spring.profiles.active=mcp is set.
    // Tomcat is disabled in that profile via application-mcp.properties
    // (spring.main.web-application-type=none), so the JVM becomes a quiet
    // stdin/stdout process that MCP clients can spawn as a child process.
    //
    // LoggingStdioTransportProvider wraps System.in/System.out with
    // interceptors that log every raw JSON-RPC line to STDERR so you can
    // study the wire protocol in MCP Inspector's "Error output" panel.
    // =========================================================================
    @Bean
    @Profile("mcp")
    public McpServerTransportProvider stdioTransportProvider() {
        log.info("[CONFIG → McpServerConfig] stdio transport bean created (profile=mcp)");
        return new LoggingStdioTransportProvider();
    }

    // =========================================================================
    // TRANSPORT BEAN — HTTP/SSE (profile: mcp-http)
    // =========================================================================
    // Active only when --spring.profiles.active=mcp-http is set.
    // Tomcat IS running in this profile (default web mode), so AI clients
    // connect over HTTP instead of stdin/stdout:
    //
    //   SSE stream (server → client):  GET  http://host:8080/sse
    //   JSON-RPC requests (client → server):  POST http://host:8080/mcp/messages
    //
    // The objectMapper param is auto-injected from Spring Boot's default Jackson
    // bean — the same mapper already used by @RestControllers for JSON serialisation.
    //
    // "/mcp/messages" is the path where clients POST tool calls.
    // The SSE endpoint path ("/sse") is set in the servlet registration below.
    // =========================================================================
    @Bean
    @Profile("mcp-http")
    public McpServerTransportProvider httpSseTransportProvider(ObjectMapper objectMapper) {
        log.info("[CONFIG → McpServerConfig] HTTP/SSE transport bean created (profile=mcp-http)");
        return new HttpServletSseServerTransportProvider(objectMapper, "/mcp/messages");
    }

    // =========================================================================
    // SERVLET REGISTRATION — HTTP/SSE (profile: mcp-http)
    // =========================================================================
    // WHY THIS BEAN IS REQUIRED — the DispatcherServlet gotcha
    // ---------------------------------------------------------
    // Spring Boot registers DispatcherServlet at "/*" by default.  Without an
    // explicit ServletRegistrationBean, any request to /sse or /mcp/messages
    // lands in DispatcherServlet, which finds no matching @RequestMapping and
    // returns:
    //   500  "No static resource sse"   (or 404 NoResourceFoundException)
    //
    // HttpServletSseServerTransportProvider extends HttpServlet directly — it IS
    // a servlet, not a Spring MVC controller.  The fix is to register it at
    // exactly the paths it owns (/sse and /mcp/messages) so the servlet container
    // routes those requests to it BEFORE DispatcherServlet can intercept them.
    //
    // setLoadOnStartup(1) tells Tomcat to initialise this servlet during
    // application startup rather than lazily on the first request.  This
    // ensures the SSE endpoint is immediately ready when the app is live.
    // =========================================================================
    @Bean
    @Profile("mcp-http")
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServletRegistration(
            McpServerTransportProvider transportProvider) {
        log.info("[CONFIG → McpServerConfig] HTTP servlet registered at /sse and /mcp/messages");
        HttpServletSseServerTransportProvider httpProvider =
                (HttpServletSseServerTransportProvider) transportProvider;
        ServletRegistrationBean<HttpServletSseServerTransportProvider> registration =
                new ServletRegistrationBean<>(httpProvider, "/sse", "/mcp/messages");
        registration.setName("mcpServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    // =========================================================================
    // MCP SERVER BEAN (active only when a transport is in the context)
    // =========================================================================
    // @ConditionalOnBean ensures this bean is skipped in the DEFAULT profile
    // where no transport bean exists.  Without it, Spring would fail at startup
    // with "No qualifying bean of type McpServerTransportProvider" when running
    // in REST-only mode — where we intentionally do NOT want MCP at all.
    //
    // McpServer.sync(transport) creates a synchronous server: each tool call
    // blocks until the handler returns.  Right for DB queries and standard I/O.
    // McpServer.async(transport) exists for Project Reactor workloads; ignore
    // it until you need non-blocking tool handlers.
    //
    // Builder analogy (Spring MVC XML → Spring Boot Java):
    //   <bean id="mcpServer"><property name="tools" .../></bean>
    //     ≡  McpServer.sync(transport).serverInfo(...).tool(...).build()
    //
    // REGISTRATION PATTERN (same for every tool):
    //   .tool(
    //       new McpSchema.Tool(name, description, jsonSchema),   // what AI sees
    //       (exchange, args) -> new McpSchema.CallToolResult(    // what runs
    //           bean.method(args), false
    //       )
    //   )
    // 'args' is Map<String,Object> — the AI's JSON arguments deserialised by the SDK.
    // 'false' = isError:false; each tool method returns {"error":"..."} internally
    // rather than throwing, so the MCP envelope never carries an error flag.
    // =========================================================================
    @Bean
    @ConditionalOnBean(McpServerTransportProvider.class)
    public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
                                       DatabaseStatusTool databaseStatusTool,
                                       EmployeeTool employeeTool,
                                       DepartmentTool departmentTool) {

        log.info("[CONFIG → McpServerConfig] MCP server built — {} tools registered", 11);
        return McpServer.sync(transportProvider)

                .serverInfo("ai-mcp-lab", "1.0.0")

                .capabilities(
                        McpSchema.ServerCapabilities.builder()
                                .tools(false)
                                .build()
                )

                // ================================================================
                // TOOL 1 — getDatabaseStatus
                // ================================================================
                // See DatabaseStatusTool.java for the full educational comments
                // on the registration pattern.
                // ================================================================
                .tool(
                        new McpSchema.Tool(
                                "getDatabaseStatus",
                                "Returns the current connection status of the ai_mcp_lab MySQL database, "
                                        + "including pool size, active connections, and idle connections.",
                                "{\"type\":\"object\",\"properties\":{},\"required\":[]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                databaseStatusTool.getDatabaseStatus(),
                                false
                        )
                )

                // ================================================================
                // TOOLS 2-6 — Employees CRUD
                // ================================================================

                // TOOL 2: listEmployees
                .tool(
                        new McpSchema.Tool(
                                "listEmployees",
                                "Returns all employees. Optionally filter by departmentId to list "
                                        + "employees in a specific department. Returns an array of employee "
                                        + "objects with id, name, email, departmentId, salary, createdAt.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"departmentId\":{\"type\":\"integer\","
                                        + "\"description\":\"Filter by department ID; omit to return all employees\"}"
                                        + "},\"required\":[]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                employeeTool.listEmployees(args), false
                        )
                )

                // TOOL 3: getEmployeeById
                .tool(
                        new McpSchema.Tool(
                                "getEmployeeById",
                                "Returns a single employee by their numeric ID. "
                                        + "Returns an error if the employee does not exist.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Employee ID to look up\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                employeeTool.getEmployeeById(args), false
                        )
                )

                // TOOL 4: createEmployee
                .tool(
                        new McpSchema.Tool(
                                "createEmployee",
                                "Creates a new employee record. The departmentId must reference an "
                                        + "existing department. Email must be unique. Returns the full "
                                        + "newly created employee row including the generated id and createdAt.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"name\":{\"type\":\"string\",\"description\":\"Employee full name\"},"
                                        + "\"email\":{\"type\":\"string\",\"description\":\"Unique corporate email address\"},"
                                        + "\"departmentId\":{\"type\":\"integer\",\"description\":\"ID of an existing department\"},"
                                        + "\"salary\":{\"type\":\"number\",\"description\":\"Annual gross salary\"}"
                                        + "},\"required\":[\"name\",\"email\",\"departmentId\",\"salary\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                employeeTool.createEmployee(args), false
                        )
                )

                // TOOL 5: updateEmployee
                .tool(
                        new McpSchema.Tool(
                                "updateEmployee",
                                "Updates one or more fields of an existing employee. Only the fields "
                                        + "you provide are changed; unprovided fields keep their current values. "
                                        + "Returns the full updated employee row.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Employee ID to update\"},"
                                        + "\"name\":{\"type\":\"string\",\"description\":\"New full name\"},"
                                        + "\"email\":{\"type\":\"string\",\"description\":\"New email address\"},"
                                        + "\"departmentId\":{\"type\":\"integer\",\"description\":\"New department ID\"},"
                                        + "\"salary\":{\"type\":\"number\",\"description\":\"New annual salary\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                employeeTool.updateEmployee(args), false
                        )
                )

                // TOOL 6: deleteEmployee
                .tool(
                        new McpSchema.Tool(
                                "deleteEmployee",
                                "Deletes an employee by ID. Returns {\"deleted\":true,\"id\":N} on success "
                                        + "or an error object if the employee does not exist.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Employee ID to delete\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                employeeTool.deleteEmployee(args), false
                        )
                )

                // ================================================================
                // TOOLS 7-11 — Departments CRUD
                // ================================================================

                // TOOL 7: listDepartments
                .tool(
                        new McpSchema.Tool(
                                "listDepartments",
                                "Returns all departments with their id, name, and location. "
                                        + "Use getDepartmentById to also include the employee count.",
                                "{\"type\":\"object\",\"properties\":{},\"required\":[]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                departmentTool.listDepartments(args), false
                        )
                )

                // TOOL 8: getDepartmentById
                .tool(
                        new McpSchema.Tool(
                                "getDepartmentById",
                                "Returns a single department by ID including its current employee count. "
                                        + "Returns an error if the department does not exist.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Department ID to look up\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                departmentTool.getDepartmentById(args), false
                        )
                )

                // TOOL 9: createDepartment
                .tool(
                        new McpSchema.Tool(
                                "createDepartment",
                                "Creates a new department. Name must be unique. Location is optional. "
                                        + "Returns the newly created department row with generated id.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"name\":{\"type\":\"string\",\"description\":\"Unique department name\"},"
                                        + "\"location\":{\"type\":\"string\",\"description\":\"Physical or virtual office location (optional)\"}"
                                        + "},\"required\":[\"name\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                departmentTool.createDepartment(args), false
                        )
                )

                // TOOL 10: updateDepartment
                .tool(
                        new McpSchema.Tool(
                                "updateDepartment",
                                "Updates one or more fields of an existing department. "
                                        + "Only provided fields are changed. Returns the updated department row "
                                        + "with current employee count.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Department ID to update\"},"
                                        + "\"name\":{\"type\":\"string\",\"description\":\"New department name\"},"
                                        + "\"location\":{\"type\":\"string\",\"description\":\"New office location\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                departmentTool.updateDepartment(args), false
                        )
                )

                // TOOL 11: deleteDepartment
                .tool(
                        new McpSchema.Tool(
                                "deleteDepartment",
                                "Deletes a department by ID. Fails with a clear error if any employees "
                                        + "still belong to this department — delete or reassign them first. "
                                        + "Returns {\"deleted\":true,\"id\":N} on success.",
                                "{\"type\":\"object\","
                                        + "\"properties\":{"
                                        + "\"id\":{\"type\":\"integer\",\"description\":\"Department ID to delete\"}"
                                        + "},\"required\":[\"id\"]}"
                        ),
                        (exchange, args) -> new McpSchema.CallToolResult(
                                departmentTool.deleteDepartment(args), false
                        )
                )

                .build();
    }
}
