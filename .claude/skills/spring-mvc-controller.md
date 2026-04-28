---
name: spring-mvc-controller
description: Skill for creating and reviewing Spring MVC controllers following enterprise patterns
---

# Spring MVC Controller Skill

## Purpose

Guide the creation, review, and modification of Spring MVC controller classes in the EFFORT-Web layered architecture.

## When to Use

- Creating new controller endpoints
- Reviewing existing controller code for best practices
- Refactoring controllers to follow proper separation of concerns
- Adding new URL mappings or request handlers

## Architecture Role

Controllers are the entry point for HTTP requests. They must:
1. Accept and validate input
2. Delegate business logic to the Manager layer
3. Prepare model attributes for the JSP view
4. Return view names or JSON responses

```
HTTP Request → DispatcherServlet → Controller → Manager → DAO → Database
                                       ↓
                                  JSP View (via Tiles)
```

## Best Practices

### Controller Structure

```java
@Controller
@RequestMapping("/web")
public class ExampleController {

    private final ExampleManager exampleManager;

    @Autowired
    public ExampleController(ExampleManager exampleManager) {
        this.exampleManager = exampleManager;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String listItems(HttpServletRequest request, Model model) {
        // Validate session/auth
        // Delegate to manager
        // Set model attributes
        // Return view name
        return "itemList";
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveItem(@RequestParam String name,
                                         HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            exampleManager.saveItem(name);
            response.put("status", "success");
        } catch (Exception e) {
            logger.error("Error saving item: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
```

### Enterprise Standards

- **No business logic** in controllers — delegate everything to Manager layer
- **Validate inputs** at the controller boundary before passing to managers
- **Use `@ResponseBody`** for AJAX/JSON endpoints, return view names for page renders
- **Handle exceptions** gracefully — return proper HTTP status codes
- **Use `HttpServletRequest`** for session access (this project uses servlet sessions, not Spring Session primarily)
- **Follow existing URL patterns**: `/web/`, `/service/`, `/custService/`, `/onDemand/`
- **Use parameterized logging** with SLF4J — `logger.error("msg: {}", val)` not string concatenation

### Common URL Mapping Patterns in EFFORT

| Controller | Base Path | Purpose |
|---|---|---|
| WebController | `/web/` | Primary web UI endpoints |
| WebExtendedController | `/web/ext/` | Extended web features |
| OnDemandController | `/onDemand/` | Mobile/on-demand API |
| EmployeeController | `/employeeService/` | Employee management |
| MapController | `/map/` | Map/location features |
| WorkFlowController | `/workFlow/` | Workflow management |
| CustomerConfigController | `/configure/` | Configuration endpoints |
| BIController | `/bi/` | Business intelligence |

### Security Considerations

- Check user authentication via `HttpServletRequest.getSession()`
- Validate CSRF tokens where applicable
- Use OWASP ESAPI for input sanitization
- Never expose stack traces in responses
