package com.mcplab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Inbound request body for Employee create (POST) and update (PUT).
 *
 * POST — use with @Valid: all four fields are required.
 * PUT  — use without @Valid: null fields mean "do not change this field".
 *
 * All fields are wrapper types (String, Integer, Double) so that a JSON body
 * that omits a field deserialises to null instead of a default primitive value.
 * This lets PUT behave as a partial update without a separate DTO class.
 */
public record EmployeeRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotNull(message = "Department ID is required")
        @Positive(message = "Department ID must be a positive integer")
        Integer departmentId,

        @NotNull(message = "Salary is required")
        @Positive(message = "Salary must be a positive number")
        Double salary
) {}
