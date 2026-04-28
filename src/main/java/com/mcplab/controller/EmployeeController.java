package com.mcplab.controller;

import com.mcplab.dto.EmployeeRequest;
import com.mcplab.dto.EmployeeResponse;
import com.mcplab.exception.BusinessRuleException;
import com.mcplab.exception.ResourceNotFoundException;
import com.mcplab.repository.DepartmentRepository;
import com.mcplab.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Employees resource.
 *
 * All business-rule and not-found errors are thrown as typed exceptions;
 * GlobalExceptionHandler translates them to the correct HTTP status codes.
 *
 * @CrossOrigin allows the React dev server (Vite default: port 5173) to call
 * these endpoints directly in the browser during development.
 */
@Slf4j
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    // =========================================================================
    // GET /api/employees
    // GET /api/employees?departmentId=1
    // =========================================================================
    // 200 — array of employees (empty array if none match)
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listEmployees(
            @RequestParam(required = false) Integer departmentId) {

        List<EmployeeResponse> employees = employeeRepository.findAll(departmentId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();

        log.info("GET /api/employees deptFilter={} → {} row(s)", departmentId, employees.size());
        return ResponseEntity.ok(employees);
    }

    // =========================================================================
    // GET /api/employees/{id}
    // =========================================================================
    // 200 — single employee
    // 404 — id not found
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable int id) {
        EmployeeResponse response = employeeRepository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee with id " + id + " not found"
                ));

        log.info("GET /api/employees/{} → found", id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/employees
    // =========================================================================
    // 201 — employee created, body contains the persisted row
    // 400 — validation failure (missing/invalid fields)
    // 409 — departmentId does not reference an existing department
    // =========================================================================
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        if (!departmentRepository.existsById(request.departmentId())) {
            throw new BusinessRuleException(
                    "Department with id " + request.departmentId() + " does not exist"
            );
        }

        EmployeeResponse created = EmployeeResponse.from(
                employeeRepository.create(
                        request.name(),
                        request.email(),
                        request.departmentId(),
                        request.salary()
                )
        );

        log.info("POST /api/employees → created id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // PUT /api/employees/{id}
    // =========================================================================
    // Partial update — only the non-null fields in the request body are applied.
    // 200 — updated employee
    // 404 — id not found
    // 409 — new departmentId does not reference an existing department
    // =========================================================================
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable int id,
            @RequestBody EmployeeRequest request) {  // no @Valid — all fields optional for PUT

        if (request.name() == null
                && request.email() == null
                && request.departmentId() == null
                && request.salary() == null) {
            throw new BusinessRuleException(
                    "Provide at least one field to update: name, email, departmentId, salary"
            );
        }

        if (request.departmentId() != null
                && !departmentRepository.existsById(request.departmentId())) {
            throw new BusinessRuleException(
                    "Department with id " + request.departmentId() + " does not exist"
            );
        }

        EmployeeResponse updated = employeeRepository
                .update(id, request.name(), request.email(), request.departmentId(), request.salary())
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee with id " + id + " not found"
                ));

        log.info("PUT /api/employees/{} → updated", id);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // DELETE /api/employees/{id}
    // =========================================================================
    // 204 — deleted (no body)
    // 404 — id not found
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable int id) {
        if (!employeeRepository.delete(id)) {
            throw new ResourceNotFoundException("Employee with id " + id + " not found");
        }
        log.info("DELETE /api/employees/{} → deleted", id);
        return ResponseEntity.noContent().build();
    }
}
