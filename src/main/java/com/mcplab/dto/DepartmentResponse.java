package com.mcplab.dto;

import java.util.Map;

/**
 * Outbound response body for all Department endpoints.
 *
 * employeeCount is always present; the repository uses a LEFT JOIN so
 * departments with zero employees return 0, never null.
 */
public record DepartmentResponse(
        int id,
        String name,
        String location,
        long employeeCount
) {
    public static DepartmentResponse from(Map<String, Object> row) {
        return new DepartmentResponse(
                ((Number) row.get("id")).intValue(),
                (String) row.get("name"),
                (String) row.get("location"),
                row.get("employeeCount") != null
                        ? ((Number) row.get("employeeCount")).longValue()
                        : 0L
        );
    }
}
