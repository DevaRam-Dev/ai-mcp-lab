package com.mcplab.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcplab.repository.DepartmentRepository;
import com.mcplab.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP tools for CRUD operations on the Employees table.
 *
 * After the DAO refactor this class is a thin wrapper:
 *   - args parsing (Map<String,Object> → typed values)
 *   - input validation with MCP-friendly error strings
 *   - delegation to EmployeeRepository (the single source of truth for SQL)
 *   - JSON serialisation of the result for the AI client
 *
 * No SQL lives here anymore. EmployeeRepository owns it all.
 * McpServerConfig.java registers every method with the MCP server — unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeTool {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info(box("EmployeeTool initialized and ready")
            + lbl("Layer",   "MCP-TOOL → EmployeeTool")
            + lbl("ANALOGY", "AI-accessible HR desk opened"));
    }

    // =========================================================================
    // listEmployees — optional departmentId filter
    // =========================================================================
    public String listEmployees(Map<String, Object> args) {
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : listEmployees")
            + lbl("Layer",       "MCP-TOOL → EmployeeTool")
            + lbl("Input",       "departmentId=" + args.get("departmentId") + " (null = all departments)")
            + lbl("Description", "Return employees, optionally filtered by department"));
        try {
            Integer deptId = getInt(args, "departmentId");
            var rows = employeeRepository.findAll(deptId);
            log.info("[MCP-TOOL → EmployeeTool] OUTPUT: listEmployees | count={} | deptFilter={} | duration={}ms",
                    rows.size(), deptId, System.currentTimeMillis() - start);
            return toJson(Map.of("employees", rows, "count", rows.size()));
        } catch (Exception e) {
            log.error("[MCP-TOOL → EmployeeTool] ERROR: listEmployees | {}", e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // getEmployeeById
    // =========================================================================
    public String getEmployeeById(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : getEmployeeById")
            + lbl("Layer",       "MCP-TOOL → EmployeeTool")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Return single employee record by ID"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");
        try {
            String result = employeeRepository.findById(id)
                    .map(this::toJson)
                    .orElse(error("Employee with id " + id + " not found"));
            log.info("[MCP-TOOL → EmployeeTool] OUTPUT: getEmployeeById | id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[MCP-TOOL → EmployeeTool] ERROR: getEmployeeById | id={} | {}", id, e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // createEmployee
    // =========================================================================
    public String createEmployee(Map<String, Object> args) {
        String name   = getString(args, "name");
        String email  = getString(args, "email");
        Integer deptId = getInt(args, "departmentId");
        Double salary  = getDouble(args, "salary");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : createEmployee")
            + lbl("Layer",       "MCP-TOOL → EmployeeTool")
            + lbl("Input",       "name=" + name + ", email=" + email + ", departmentId=" + deptId + ", salary=" + salary)
            + lbl("Description", "Create new employee record"));
        if (isBlank(name))   return error("Required parameter 'name' is missing or empty");
        if (isBlank(email))  return error("Required parameter 'email' is missing or empty");
        if (deptId == null)  return error("Required parameter 'departmentId' is missing or invalid");
        if (salary == null)  return error("Required parameter 'salary' is missing or invalid");

        if (!departmentRepository.existsById(deptId)) {
            return error("Department with id " + deptId + " does not exist");
        }

        try {
            var created = employeeRepository.create(name, email, deptId, salary);
            log.info("[MCP-TOOL → EmployeeTool] OUTPUT: createEmployee | insertedId={} | duration={}ms",
                    created.get("id"), System.currentTimeMillis() - start);
            return toJson(created);
        } catch (Exception e) {
            log.error("[MCP-TOOL → EmployeeTool] ERROR: createEmployee | {}", e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // updateEmployee — partial update (only provided fields are applied)
    // =========================================================================
    public String updateEmployee(Map<String, Object> args) {
        Integer id    = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : updateEmployee")
            + lbl("Layer",       "MCP-TOOL → EmployeeTool")
            + lbl("Input",       "id=" + id + ", name=" + args.get("name") + ", deptId=" + args.get("departmentId"))
            + lbl("Description", "Partial update — only non-null fields applied"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");

        String name   = getString(args, "name");
        String email  = getString(args, "email");
        Integer deptId = getInt(args, "departmentId");
        Double salary  = getDouble(args, "salary");

        if (name == null && email == null && deptId == null && salary == null) {
            return error("Provide at least one field to update: name, email, departmentId, salary");
        }
        if (deptId != null && !departmentRepository.existsById(deptId)) {
            return error("Department with id " + deptId + " does not exist");
        }

        try {
            String result = employeeRepository.update(id, name, email, deptId, salary)
                    .map(this::toJson)
                    .orElse(error("Employee with id " + id + " not found"));
            log.info("[MCP-TOOL → EmployeeTool] OUTPUT: updateEmployee | id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[MCP-TOOL → EmployeeTool] ERROR: updateEmployee | id={} | {}", id, e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // deleteEmployee
    // =========================================================================
    public String deleteEmployee(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : deleteEmployee")
            + lbl("Layer",       "MCP-TOOL → EmployeeTool")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Delete employee record by ID"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");
        try {
            if (!employeeRepository.delete(id)) {
                return error("Employee with id " + id + " not found");
            }
            log.info("[MCP-TOOL → EmployeeTool] OUTPUT: deleteEmployee | deleted id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return toJson(Map.of("deleted", true, "id", id));
        } catch (Exception e) {
            log.error("[MCP-TOOL → EmployeeTool] ERROR: deleteEmployee | id={} | {}", id, e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("JSON serialisation failed", e);
            return "{\"error\":\"Failed to serialise response\"}";
        }
    }

    private String error(String message) {
        return toJson(Map.of("error", message));
    }

    private static Integer getInt(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static String getString(Map<String, Object> args, String key) {
        Object val = args.get(key);
        return val == null ? null : val.toString().trim();
    }

    private static Double getDouble(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Log formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static final String BOX_H = "═".repeat(76);

    private static String box(String title) {
        return "\n╔" + BOX_H + "╗\n║  " + String.format("%-74s", title) + "║\n╚" + BOX_H + "╝";
    }

    private static String lbl(String label, Object value) {
        return "\n   " + String.format("%-11s", label) + " : " + value;
    }
}
