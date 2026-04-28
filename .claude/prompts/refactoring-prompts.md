# Refactoring Prompts

Reusable prompts for safe, incremental refactoring of legacy enterprise Java code.

---

## Reduce Method Complexity

```
Refactor this method to reduce its cognitive complexity:

[PASTE METHOD OR FILE:LINE_RANGE]

Requirements:
1. Extract logical blocks into well-named private methods
2. Preserve exact existing behavior — no functional changes
3. Keep the public method as a high-level orchestrator
4. Ensure all extracted methods have clear single responsibilities
5. Maintain existing logging and error handling
6. Keep it compilable — don't break references
```

---

## Eliminate Code Duplication

```
I've identified duplicated code in these locations:

- [FILE1:LINE_RANGE]
- [FILE2:LINE_RANGE]
- [FILE3:LINE_RANGE]

Refactor to eliminate duplication:
1. Identify the common pattern
2. Create a shared method in the appropriate utility or base class
3. Replace all duplicate instances with calls to the shared method
4. Handle any differences via parameters
5. Ensure no behavior changes
6. Verify all callers are updated
```

---

## Modernize to Java 21

```
Modernize this Java code to use Java 21 features:

[PASTE CODE]

Apply where beneficial:
1. Text blocks for multi-line strings (especially SQL queries)
2. Pattern matching for instanceof
3. Switch expressions (replacing switch statements)
4. Records for simple data carriers (if applicable)
5. Sealed classes (if applicable for type hierarchies)
6. Enhanced for-loops or Stream API where it improves readability

Do NOT modernize if the new syntax makes the code less readable or
if the change has any risk of behavior difference.
```

---

## Fix SonarQube Issue

```
SonarQube reported this issue:

Rule: [RULE_ID]
Severity: [Critical/Major/Minor]
File: [FILE_PATH]
Line: [LINE_NUMBER]
Message: [SONAR_MESSAGE]

Fix this issue:
1. Explain why SonarQube flagged this
2. Show the current problematic code
3. Provide the fixed code
4. Explain why the fix is correct
5. Verify no behavior change
6. Check if the same issue exists elsewhere in the file
```

---

## Extract Class from God Class

```
The class [CLASS_NAME] is too large ([LINE_COUNT] lines) and handles multiple responsibilities.

Decompose it:
1. Identify the distinct responsibilities in the class
2. Group methods by responsibility
3. Propose new classes for each responsibility group
4. Define the dependencies between the new classes
5. Show the refactored code for the original class (now delegating)
6. Show one of the extracted classes as a complete example
7. Ensure all Spring wiring (@Autowired, @Component) is correct
```

---

## Refactor DAO Query

```
Refactor this DAO method for better performance and readability:

[PASTE DAO METHOD]

Check and improve:
1. SQL query optimization (JOINs vs subqueries, index usage)
2. Proper parameterization (? placeholders, no concatenation)
3. Column selection (remove SELECT *, list specific columns)
4. Pagination support if returning lists
5. Use Java 21 text blocks for SQL strings
6. Proper result mapping (RowCallbackHandler or RowMapper)
7. Error handling for empty results
```

---

## Migrate javax to jakarta

```
Migrate this file from javax to jakarta namespace:

[FILE_PATH]

1. Replace all javax.servlet imports with jakarta.servlet
2. Replace javax.mail with jakarta.mail
3. Replace javax.jms with jakarta.jms
4. Replace javax.validation with jakarta.validation
5. Verify all method signatures are compatible
6. Check for any javax references in annotations or string literals
7. Ensure the file compiles after migration
```

---

## Consolidate Configuration

```
Review and consolidate the Spring XML configuration:

Files: [LIST OF XML FILES]

1. Identify duplicate bean definitions across files
2. Find unused beans that can be removed
3. Identify properties that could be moved to shared constants.xml
4. Suggest which XML beans could migrate to Java @Configuration
5. Check for inconsistencies between environment-specific configs
```

---

## Improve Error Handling

```
Review error handling in [FILE_PATH]:

1. Find empty catch blocks and add proper handling
2. Find catch blocks that swallow exceptions (catch with only log, no rethrow)
3. Ensure all resources are properly closed (use try-with-resources)
4. Ensure error responses don't expose internal details
5. Add missing error handling for null returns from DAO
6. Ensure exceptions include contextual information
7. Verify logging uses parameterized format with exception as last arg
```
