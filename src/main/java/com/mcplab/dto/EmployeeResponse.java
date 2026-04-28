package com.mcplab.dto;

import java.util.Map;

/**
 * Outbound response body for all Employee endpoints.
 *
 * Field names are camelCase to match the DB column aliases (departmentId, createdAt)
 * and the JSON convention used by the React frontend.
 *
 * The static from() factory converts a raw JdbcTemplate result row (Map<String,Object>)
 * to a typed record so controllers never access raw maps.
 *
 * Type mapping from JDBC to Java:
 *   MySQL INT      → Integer  → int
 *   MySQL DECIMAL  → BigDecimal → double (precision is acceptable for display)
 *   MySQL DATETIME → java.sql.Timestamp → String via toString()
 */
public record EmployeeResponse(
        int id,
        String name,
        String email,
        int departmentId,
        double salary,
        String createdAt
) {
    public static EmployeeResponse from(Map<String, Object> row) {
        return new EmployeeResponse(
                ((Number) row.get("id")).intValue(),
                (String) row.get("name"),
                (String) row.get("email"),
                ((Number) row.get("departmentId")).intValue(),
                ((Number) row.get("salary")).doubleValue(),
                row.get("createdAt") != null ? row.get("createdAt").toString() : null
        );
    }
}
