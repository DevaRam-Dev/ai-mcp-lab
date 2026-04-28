# com.mcplab.tools

## What this directory holds
All **MCP Tool definitions** — Java methods that AI models can discover and invoke.

## Role in MCP Architecture
Tools are the primary way an AI agent takes *action* through your server.  Each method
annotated with `@Tool` (from the MCP SDK) is exposed as a callable function in the AI's
tool-use API.  Think of this package as your **AI-native API layer**.

```
AI Model sends:  { "tool": "getCustomerById", "arguments": { "id": 42 } }
                          │
             McpServer routes to matching @Tool method
                          │
          Java method executes (queries DB, calls service, etc.)
                          │
AI Model receives:  { "content": [ { "type": "text", "text": "..." } ] }
```

**Analogy for Spring MVC developers:**
- `@RestController` + `@GetMapping` = called by *browsers/frontends*
- `@Tool` method = called by *AI models*

Same underlying logic — different caller, different registration mechanism.

## Example files that will go here
| File | Purpose |
|------|---------|
| `CustomerTools.java` | Tools: `findCustomer`, `listCustomers`, `createCustomer` |
| `TaskTools.java` | Tools: `getOpenTasks`, `assignTask`, `updateTaskStatus` |
| `ReportTools.java` | Tools: `generateSummaryReport`, `getMetrics` |
| `DatabaseQueryTool.java` | Generic tool: safe parameterised SQL query executor |
