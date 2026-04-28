package com.mcplab.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcplab.repository.DepartmentRepository;
import com.mcplab.repository.EmployeeRepository;
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

    // =========================================================================
    // listEmployees — optional departmentId filter
    // =========================================================================
    public String listEmployees(Map<String, Object> args) {
        try {
            Integer deptId = getInt(args, "departmentId");
            var rows = employeeRepository.findAll(deptId);
            log.info("listEmployees: {} row(s), deptFilter={}", rows.size(), deptId);
            return toJson(Map.of("employees", rows, "count", rows.size()));
        } catch (Exception e) {
            log.error("listEmployees failed", e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // getEmployeeById
    // =========================================================================
    public String getEmployeeById(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        if (id == null) return error("Required parameter 'id' is missing or invalid");
        try {
            return employeeRepository.findById(id)
                    .map(this::toJson)
                    .orElse(error("Employee with id " + id + " not found"));
        } catch (Exception e) {
            log.error("getEmployeeById failed id={}", id, e);
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

        if (isBlank(name))   return error("Required parameter 'name' is missing or empty");
        if (isBlank(email))  return error("Required parameter 'email' is missing or empty");
        if (deptId == null)  return error("Required parameter 'departmentId' is missing or invalid");
        if (salary == null)  return error("Required parameter 'salary' is missing or invalid");

        if (!departmentRepository.existsById(deptId)) {
            return error("Department with id " + deptId + " does not exist");
        }

        try {
            var created = employeeRepository.create(name, email, deptId, salary);
            log.info("createEmployee: inserted id={}", created.get("id"));
            return toJson(created);
        } catch (Exception e) {
            log.error("createEmployee failed", e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // updateEmployee — partial update (only provided fields are applied)
    // =========================================================================
    public String updateEmployee(Map<String, Object> args) {
        Integer id    = getInt(args, "id");
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
            return employeeRepository.update(id, name, email, deptId, salary)
                    .map(this::toJson)
                    .orElse(error("Employee with id " + id + " not found"));
        } catch (Exception e) {
            log.error("updateEmployee failed id={}", id, e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // deleteEmployee
    // =========================================================================
    public String deleteEmployee(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        if (id == null) return error("Required parameter 'id' is missing or invalid");
        try {
            if (!employeeRepository.delete(id)) {
                return error("Employee with id " + id + " not found");
            }
            log.info("deleteEmployee: deleted id={}", id);
            return toJson(Map.of("deleted", true, "id", id));
        } catch (Exception e) {
            log.error("deleteEmployee failed id={}", id, e);
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
}
