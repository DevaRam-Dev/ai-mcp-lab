package com.mcplab.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcplab.repository.DepartmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP tools for CRUD operations on the Departments table.
 *
 * After the DAO refactor this class is a thin wrapper:
 *   - args parsing (Map<String,Object> → typed values)
 *   - input validation with MCP-friendly error strings
 *   - delegation to DepartmentRepository (the single source of truth for SQL)
 *   - JSON serialisation of the result for the AI client
 *
 * No SQL lives here anymore. DepartmentRepository owns it all.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentTool {

    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info(box("DepartmentTool initialized and ready")
            + lbl("Layer",   "MCP-TOOL → DepartmentTool")
            + lbl("ANALOGY", "AI-accessible department desk opened"));
    }

    // =========================================================================
    // listDepartments
    // =========================================================================
    public String listDepartments(Map<String, Object> args) {
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : listDepartments")
            + lbl("Layer",       "MCP-TOOL → DepartmentTool")
            + lbl("Input",       "no filters")
            + lbl("Description", "Return all departments with employee counts as JSON"));
        try {
            var rows = departmentRepository.findAll();
            log.info("[MCP-TOOL → DepartmentTool] OUTPUT: listDepartments | count={} | duration={}ms",
                    rows.size(), System.currentTimeMillis() - start);
            return toJson(Map.of("departments", rows, "count", rows.size()));
        } catch (Exception e) {
            log.error("[MCP-TOOL → DepartmentTool] ERROR: listDepartments | {}", e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // getDepartmentById
    // =========================================================================
    public String getDepartmentById(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : getDepartmentById")
            + lbl("Layer",       "MCP-TOOL → DepartmentTool")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Return single department record by ID"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");
        try {
            String result = departmentRepository.findById(id)
                    .map(this::toJson)
                    .orElse(error("Department with id " + id + " not found"));
            log.info("[MCP-TOOL → DepartmentTool] OUTPUT: getDepartmentById | id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[MCP-TOOL → DepartmentTool] ERROR: getDepartmentById | id={} | {}", id, e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // createDepartment
    // =========================================================================
    public String createDepartment(Map<String, Object> args) {
        String name     = getString(args, "name");
        String location = getString(args, "location");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : createDepartment")
            + lbl("Layer",       "MCP-TOOL → DepartmentTool")
            + lbl("Input",       "name=" + name + ", location=" + location)
            + lbl("Description", "Create new department record"));
        if (isBlank(name)) return error("Required parameter 'name' is missing or empty");

        try {
            var created = departmentRepository.create(name, location);
            log.info("[MCP-TOOL → DepartmentTool] OUTPUT: createDepartment | insertedId={} | duration={}ms",
                    created.get("id"), System.currentTimeMillis() - start);
            return toJson(created);
        } catch (Exception e) {
            log.error("[MCP-TOOL → DepartmentTool] ERROR: createDepartment | {}", e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // updateDepartment — partial update
    // =========================================================================
    public String updateDepartment(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : updateDepartment")
            + lbl("Layer",       "MCP-TOOL → DepartmentTool")
            + lbl("Input",       "id=" + id + ", name=" + args.get("name") + ", location=" + args.get("location"))
            + lbl("Description", "Partial update — only non-null fields applied"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");

        String name     = getString(args, "name");
        String location = getString(args, "location");

        if (name == null && location == null) {
            return error("Provide at least one field to update: name, location");
        }

        try {
            String result = departmentRepository.update(id, name, location)
                    .map(this::toJson)
                    .orElse(error("Department with id " + id + " not found"));
            log.info("[MCP-TOOL → DepartmentTool] OUTPUT: updateDepartment | id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[MCP-TOOL → DepartmentTool] ERROR: updateDepartment | id={} | {}", id, e.getMessage(), e);
            return error(e.getMessage());
        }
    }

    // =========================================================================
    // deleteDepartment — blocks if employees still reference this department
    // =========================================================================
    public String deleteDepartment(Map<String, Object> args) {
        Integer id = getInt(args, "id");
        long start = System.currentTimeMillis();
        log.info(box("MCP-TOOL : deleteDepartment")
            + lbl("Layer",       "MCP-TOOL → DepartmentTool")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Delete department — blocked if employees still exist"));
        if (id == null) return error("Required parameter 'id' is missing or invalid");

        int empCount = departmentRepository.countEmployees(id);
        if (empCount > 0) {
            return error("Department has " + empCount + " employee(s); delete or reassign them first");
        }

        try {
            if (!departmentRepository.delete(id)) {
                return error("Department with id " + id + " not found");
            }
            log.info("[MCP-TOOL → DepartmentTool] OUTPUT: deleteDepartment | deleted id={} | duration={}ms",
                    id, System.currentTimeMillis() - start);
            return toJson(Map.of("deleted", true, "id", id));
        } catch (Exception e) {
            log.error("[MCP-TOOL → DepartmentTool] ERROR: deleteDepartment | id={} | {}", id, e.getMessage(), e);
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
