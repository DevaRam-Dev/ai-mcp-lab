package com.mcplab.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound request body for Department create (POST) and update (PUT).
 *
 * POST — use with @Valid: name is required, location is optional.
 * PUT  — use without @Valid: null fields mean "do not change this field".
 */
public record DepartmentRequest(

        @NotBlank(message = "Name is required")
        String name,

        String location  // nullable — schema column is NULL-able
) {}
