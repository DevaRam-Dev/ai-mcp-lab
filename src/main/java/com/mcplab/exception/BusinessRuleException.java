package com.mcplab.exception;

/**
 * Thrown when an operation violates a business rule — e.g. deleting a department
 * that still owns employees, or referencing a non-existent department.
 * Maps to HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
