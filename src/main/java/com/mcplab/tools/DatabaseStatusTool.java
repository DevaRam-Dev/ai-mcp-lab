package com.mcplab.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * WHAT IS AN MCP TOOL?
 * ====================
 * An MCP Tool is a named, described Java function that an AI model can discover and call.
 * It has three parts — think of them as the AI's "API contract":
 *
 *   1. NAME         — The identifier the AI uses to invoke it.   e.g. "getDatabaseStatus"
 *   2. DESCRIPTION  — Natural language the AI reads to decide WHEN to use it.
 *   3. INPUT SCHEMA — JSON Schema declaring what parameters the AI may pass.
 *
 * These three parts are registered in McpServerConfig.java (the "wiring" layer).
 * THIS class is the "implementation" layer — the actual Java logic that runs.
 *
 * Separation of concerns (mirrors your EFFORT-Web layered architecture):
 *
 *   McpServerConfig   ← wiring layer  (tool name, description, schema)
 *   DatabaseStatusTool ← logic layer  (this file — the actual work)
 *
 * That is the same split as:
 *   servlet-context.xml + @RequestMapping  ←→  McpServerConfig tool registration
 *   @Service / @Manager method             ←→  this class
 *
 *
 * HOW AI DISCOVERS THIS TOOL
 * ==========================
 * When an AI client (e.g. Claude Desktop) connects to this MCP server, it sends:
 *
 *   Request:   { "method": "tools/list" }
 *   Response:  { "tools": [ { "name": "getDatabaseStatus",
 *                              "description": "Returns the current connection status...",
 *                              "inputSchema": { "type": "object", "properties": {} } } ] }
 *
 * The AI model reads the name and description to decide whether this tool is relevant
 * to what the user asked.  It never calls a tool blindly — it reasons about when to
 * use it based on the description you write.
 *
 *
 * HOW AI CALLS THIS TOOL
 * ======================
 * Once the AI decides to use the tool, it sends:
 *
 *   Request:   { "method": "tools/call",
 *                "params":  { "name": "getDatabaseStatus", "arguments": {} } }
 *   Response:  { "content": [ { "type": "text",
 *                                "text": "Database status: CONNECTED | ..." } ] }
 *
 * The handler lambda in McpServerConfig.java receives ("arguments": {}) as a Map,
 * calls getDatabaseStatus() on this bean, and wraps the returned String in a
 * McpSchema.CallToolResult.  The AI reads the plain-text response.
 *
 *
 * LEARNING NOTE — THIS IS NOT A REST ENDPOINT
 * ============================================
 * Spring MVC:
 *   @GetMapping("/status")     → browser / Postman sends HTTP GET /status
 *                               → Spring routes to method via DispatcherServlet
 *
 * MCP Tool:
 *   "getDatabaseStatus" tool   → AI asks: "what tools do you have?"  (tools/list)
 *                               → AI decides to call it               (tools/call)
 *                               → MCP server routes to this bean
 *
 * There is NO HTTP endpoint for this tool.  You cannot curl it.
 * Only an MCP-aware AI client can call it.  The AI finds it automatically through
 * tool discovery — you never hard-code "call getDatabaseStatus" in a prompt.
 *
 *
 * @Component vs @RestController
 * =============================
 * @RestController  — registers with DispatcherServlet; handles HTTP requests from humans
 * @Component       — registers with Spring context; called by McpServerConfig handler lambda
 *
 * Both are Spring-managed singletons.  The difference is WHO calls them and HOW they
 * are registered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseStatusTool {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
     * JdbcTemplate is auto-configured by Spring Boot's JdbcTemplateAutoConfiguration.
     * It reads spring.datasource.* from application.properties and wires HikariCP
     * for us — no XML bean definitions needed.
     *
     * Equivalent to what you had in EFFORT-Web's root-context.xml:
     *   <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
     *       <constructor-arg ref="dataSource"/>
     *   </bean>
     *
     * @RequiredArgsConstructor (Lombok) generates the constructor that injects this field.
     * It replaces:
     *   public DatabaseStatusTool(JdbcTemplate jdbcTemplate) {
     *       this.jdbcTemplate = jdbcTemplate;
     *   }
     */
    private final JdbcTemplate jdbcTemplate;

    /*
     * getDatabaseStatus()
     * ===================
     * The tool implementation method.  This is a plain Java method — no MCP annotation
     * exists in the core SDK (unlike Spring AI's higher-level @Tool annotation).
     * The binding between this method and the MCP protocol is done explicitly in
     * McpServerConfig.java via the .tool(definition, handler) builder call.
     *
     * The pattern is intentional: business logic stays here, protocol wiring stays
     * in config.  Keeping them separate makes this method testable with a plain
     * JUnit test — no MCP infrastructure needed.
     *
     * Spring MVC equivalent you already know:
     *   @GetMapping("/db/status")          ← protocol wiring (in controller)
     *   public String getStatus() { ... }  ← business logic (in service/manager)
     *
     * Here:
     *   McpServerConfig.java .tool(...)   ← protocol wiring
     *   getDatabaseStatus()               ← business logic (this method)
     */
    public String getDatabaseStatus() {

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        try {
            /*
             * "SELECT 1" is the standard lightweight connectivity probe.
             * It does not touch any application table — it just forces the JDBC driver
             * to open a real connection and round-trip to MySQL, proving the pool is alive.
             *
             * queryForObject returns the scalar result (the integer 1) or throws
             * DataAccessException if the database is unreachable, credentials are wrong,
             * or the connection pool is exhausted.
             */
            Integer probeResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            log.info("Database health check passed. SELECT 1 = {}", probeResult);

            return String.format(
                    "Database status: CONNECTED | "
                            + "Database: ai_mcp_lab | "
                            + "Probe: SELECT 1 = %d | "
                            + "Checked at: %s",
                    probeResult,
                    timestamp
            );

        } catch (Exception e) {
            /*
             * WHY RETURN A PLAIN STRING INSTEAD OF THROWING?
             * ===============================================
             * AI models cannot read Java stack traces.  If this method throws, the MCP
             * handler in McpServerConfig catches it and returns a generic error to the AI.
             * The AI then has no context to give the user a useful response.
             *
             * By catching here and returning a structured "ERROR: ..." string, the AI
             * receives a human-readable message it can relay directly:
             *
             *   User asks: "Is the database up?"
             *   AI calls:  getDatabaseStatus()
             *   Tool returns: "ERROR: Database unreachable | Reason: Connection refused | ..."
             *   AI replies: "The database appears to be down. The connection was refused."
             *
             * Compare to throwing: the AI would receive "Tool execution failed" with no
             * context — it cannot help the user diagnose the problem.
             *
             * Rule of thumb: MCP tool error messages are user-facing, not developer-facing.
             * Write them the way you would write a UI error message.
             */
            log.error("Database health check failed at {}", timestamp, e);

            return String.format(
                    "ERROR: Database unreachable | "
                            + "Reason: %s | "
                            + "Checked at: %s",
                    e.getMessage(),
                    timestamp
            );
        }
    }
}
