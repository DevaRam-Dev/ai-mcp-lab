# src/test/java/com/mcplab/server

## What this directory holds
Integration and unit tests for the MCP server bootstrap layer.

## Role in MCP Architecture
Tests here verify that the **server starts correctly**, tools are registered, and the MCP
protocol handshake (initialize → list-tools → call-tool) works end-to-end against a real
(or in-memory H2) database.

```
@SpringBootTest
    └── loads full application context
            ├── DataSource → H2 in-memory (test profile)
            ├── McpServer  → all @Tool methods registered
            └── McpClient (test) → sends JSON-RPC, asserts responses
```

**Analogy for Spring MVC developers:**
- `@SpringBootTest` ≈ `@ContextConfiguration(locations = "classpath:root-context.xml")`
  in your EFFORT-Web tests — but with zero XML setup.
- `MockMvc` tests REST endpoints; `McpClient` (SDK test helper) tests MCP tool calls.

## Example files that will go here
| File | Purpose |
|------|---------|
| `AiMcpLabApplicationTests.java` | Smoke test — verifies Spring context loads without errors |
| `McpServerStartupTest.java` | Verifies MCP server initialises, lists expected tools |
| `ToolRegistrationTest.java` | Asserts each `@Tool` method is discoverable via the SDK |
