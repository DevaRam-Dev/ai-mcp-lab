# com.mcplab.server

## What this directory holds
The MCP server bootstrap and entry point classes.

## Role in MCP Architecture
This is the **core layer** — it wires together the Spring Boot application context with the
MCP Java SDK server lifecycle.  The class here owns the `McpServer` (or Spring Boot's
`@SpringBootApplication` main class) and configures the **transport** (stdio or SSE/HTTP)
that AI clients use to connect.

```
AI Client (Claude Desktop / Claude Code)
        │  MCP Protocol (JSON-RPC 2.0)
        ▼
  [ Transport Layer ]  ← configured here
        │
  [ McpServer ]        ← registered here
        │
  [ Tools / Resources / Prompts ]  ← defined in com.mcplab.tools
```

## Example files that will go here
| File | Purpose |
|------|---------|
| `AiMcpLabApplication.java` | `@SpringBootApplication` main class — lives at `com.mcplab` (root package) to enable full component scan |
| `McpServerConfig.java` | Registers `McpServer` bean, selects transport (stdio vs SSE) |
| `SseServerTransportConfig.java` | Optional — SSE/HTTP transport setup for remote AI clients |
