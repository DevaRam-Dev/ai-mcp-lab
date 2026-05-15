package com.mcplab.controller;

import com.mcplab.dto.EmployeeRequest;
import com.mcplab.dto.EmployeeResponse;
import com.mcplab.exception.BusinessRuleException;
import com.mcplab.exception.ResourceNotFoundException;
import com.mcplab.repository.DepartmentRepository;
import com.mcplab.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        log.info(box("EmployeeController initialized and ready")
            + lbl("Layer",   "API → EmployeeController")
            + lbl("ANALOGY", "HR desk receptionist on duty"));
    }

    // =========================================================================
    // GET /api/employees
    // GET /api/employees?departmentId=1
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listEmployees(
            @RequestParam(required = false) Integer departmentId) {

        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : GET /api/employees")
            + lbl("Layer",       "API → EmployeeController")
            + lbl("Input",       "departmentId=" + departmentId + " (null = all departments)")
            + lbl("Description", "Return employees, optionally filtered by department"));

        List<EmployeeResponse> employees = employeeRepository.findAll(departmentId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();

        log.info(heavyBox("REQUEST END : GET /api/employees — 200 OK")
            + lbl("Output",   employees.size() + " employee(s) returned, deptFilter=" + departmentId)
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(employees);
    }

    // =========================================================================
    // GET /api/employees/{id}
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable int id) {
        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : GET /api/employees/{id}")
            + lbl("Layer",       "API → EmployeeController")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Return single employee by ID"));

        EmployeeResponse response = employeeRepository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee with id " + id + " not found"
                ));

        log.info(heavyBox("REQUEST END : GET /api/employees/{id} — 200 OK")
            + lbl("Output",   "employee id=" + response.id() + " name=" + response.name())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/employees
    // =========================================================================
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : POST /api/employees")
            + lbl("Layer",       "API → EmployeeController")
            + lbl("Input",       "name=" + request.name() + ", departmentId=" + request.departmentId())
            + lbl("Description", "Create new employee record"));

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

        log.info(heavyBox("REQUEST END : POST /api/employees — 201 Created")
            + lbl("Output",   "created id=" + created.id() + " name=" + created.name())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // PUT /api/employees/{id}
    // =========================================================================
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable int id,
            @RequestBody EmployeeRequest request) {  // no @Valid — all fields optional for PUT

        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : PUT /api/employees/{id}")
            + lbl("Layer",       "API → EmployeeController")
            + lbl("Input",       "id=" + id + ", name=" + request.name() + ", deptId=" + request.departmentId())
            + lbl("Description", "Partial update — only non-null fields applied"));

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

        log.info(heavyBox("REQUEST END : PUT /api/employees/{id} — 200 OK")
            + lbl("Output",   "updated id=" + updated.id())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // DELETE /api/employees/{id}
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable int id) {
        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : DELETE /api/employees/{id}")
            + lbl("Layer",       "API → EmployeeController")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Delete employee record by ID"));

        if (!employeeRepository.delete(id)) {
            throw new ResourceNotFoundException("Employee with id " + id + " not found");
        }

        log.info(heavyBox("REQUEST END : DELETE /api/employees/{id} — 204 No Content")
            + lbl("Output",   "deleted id=" + id)
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Log formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static final String HEAVY_BAR = "█".repeat(78);

    private static String heavyBox(String title) {
        return "\n" + HEAVY_BAR + "\n█  " + String.format("%-74s", title) + "█\n" + HEAVY_BAR;
    }

    private static final String BOX_H = "═".repeat(76);

    private static String box(String title) {
        return "\n╔" + BOX_H + "╗\n║  " + String.format("%-74s", title) + "║\n╚" + BOX_H + "╝";
    }

    private static String lbl(String label, Object value) {
        return "\n   " + String.format("%-11s", label) + " : " + value;
    }
}
