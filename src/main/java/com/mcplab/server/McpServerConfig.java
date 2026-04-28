package com.mcplab.server;

import com.mcplab.tools.DatabaseStatusTool;
import com.mcplab.tools.DepartmentTool;
import com.mcplab.tools.EmployeeTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * WHAT IS AN MCP SERVER?
 * ======================
 * An MCP (Model Context Protocol) server is like a REST API — but designed for AI models
 * instead of browsers or mobile apps.
 *
 * REST API (what you build in EFFORT-Web):
 *   Browser          → HTTP GET /api/customer/42    → @RestController → JSON response
 *
 * MCP Server (what this class configures):
 *   Claude / AI model → MCP call "getCustomerById"  → @Tool method   → text response
 *
 * The protocol is JSON-RPC 2.0 over a chosen transport (stdio or HTTP/SSE).
 * The AI model first asks "what tools do you have?" (tools/list), then calls them
 * by name (tools/call) — exactly like how a frontend calls REST endpoints.
 *
 * LEARNING NOTE:
 * An MCP server does NOT serve web pages or REST responses to browsers.
 * It serves TOOLS that AI models can discover and call.
 * Your existing @RestControllers in EFFORT-Web serve humans.
 * This MCP server serves the AI agent sitting on top of those humans.
 *
 *
 * HOW THIS DIFFERS FROM @RestController
 * ======================================
 * @RestController in Spring MVC:
 *   - Mapped by URL  (@GetMapping("/customers/{id}"))
 *   - Called by HTTP clients (browsers, Postman, mobile apps)
 *   - Returns JSON, HTML, or other HTTP responses
 *   - Registered automatically by DispatcherServlet via @ComponentScan
 *
 * @Tool method in MCP (registered below):
 *   - Mapped by tool NAME ("getDatabaseStatus")
 *   - Called by AI models via JSON-RPC, not HTTP
 *   - Returns McpSchema.CallToolResult (plain text or structured content)
 *   - Registered explicitly in McpServer.sync(...).tool(...).build()
 *
 * You can have BOTH in the same Spring Boot app:
 *   - REST controllers for your human users
 *   - MCP tools for AI agents assisting those users
 */
@Configuration
public class McpServerConfig {

    /*
     * WHAT IS STDIO TRANSPORT?
     * ========================
     * "stdio" stands for Standard Input / Standard Output — the same streams you use
     * when you pipe commands in a terminal:
     *
     *   echo "hello" | grep "he"
     *             ↑ stdout        ↑ stdin
     *
     * When an AI client (e.g. Claude Desktop, Claude Code) launches this JAR as a
     * subprocess, it communicates by writing JSON-RPC messages to the process's stdin
     * and reading responses from stdout.  No network port is opened; no HTTP server is
     * needed.  This is the simplest MCP transport and is the default for local
     * AI client integrations.
     *
     * StdioServerTransportProvider wires System.in → reads MCP requests
     *                                    System.out → writes MCP responses
     *
     * Analogy for Spring MVC developers:
     *   StdioServerTransportProvider ≈ the Connector element in Tomcat's server.xml.
     *   It defines HOW the server receives and sends messages (stdin/stdout here,
     *   vs TCP port 8080 in Tomcat's case).
     *
     * Alternative: HttpServletSseServerTransportProvider
     *   Use this when you want a remote AI client to connect over HTTP/SSE instead.
     *   Swap this bean out — no other code changes required.
     */
    @Bean
    public McpServerTransportProvider stdioTransportProvider() {
        // No-arg constructor: uses System.in and System.out with a default ObjectMapper.
        // The MCP SDK handles all JSON-RPC framing internally.
        return new StdioServerTransportProvider();
    }

    /*
     * THE MCP SERVER BEAN
     * ===================
     * McpServer.sync(transport) creates a synchronous MCP server — each tool call
     * blocks until your handler method returns.  This is the right choice for
     * database queries, file reads, and any standard Java I/O.
     *
     * McpServer.async(transport) exists for reactive/non-blocking workloads (Project
     * Reactor Mono/Flux).  Ignore it until you need it.
     *
     * The builder mirrors what you used to do with XML beans in EFFORT-Web:
     *
     *   XML (EFFORT-Web):                   Java builder (here):
     *   <bean id="mcpServer" ...>           McpServer.sync(transport)
     *     <property name="name" ../>            .serverInfo("ai-mcp-lab", "1.0.0")
     *     <property name="tools" ../>           .tool(tool, handler)
     *   </bean>                                 .build()
     *
     * Spring Boot calls this method once at startup, stores the returned
     * McpSyncServer as a singleton bean, and injects it wherever needed.
     *
     * REGISTRATION PATTERN (same for every tool):
     *   .tool(
     *       new McpSchema.Tool(name, description, jsonSchema),  // what AI sees
     *       (exchange, args) -> new McpSchema.CallToolResult(   // what runs
     *           bean.method(args), false
     *       )
     *   )
     *
     * 'args' is Map<String,Object> — the AI's JSON arguments deserialised by the SDK.
     * 'false' means isError=false; each tool method handles its own errors internally
     * and returns {"error":"..."} JSON rather than throwing.
     */
    @Bean
    public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
                                       DatabaseStatusTool databaseStatusTool,
                                       EmployeeTool employeeTool,
                                       DepartmentTool departmentTool) {

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
