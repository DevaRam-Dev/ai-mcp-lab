package com.mcplab.controller;

import com.mcplab.dto.DepartmentRequest;
import com.mcplab.dto.DepartmentResponse;
import com.mcplab.exception.BusinessRuleException;
import com.mcplab.exception.ResourceNotFoundException;
import com.mcplab.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Departments resource.
 *
 * All department responses include employeeCount. The repository uses a
 * LEFT JOIN so the count is always present — 0 for empty departments.
 */
@Slf4j
@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    // =========================================================================
    // GET /api/departments
    // =========================================================================
    // 200 — array of all departments, each with employeeCount
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> listDepartments() {
        List<DepartmentResponse> departments = departmentRepository.findAll()
                .stream()
                .map(DepartmentResponse::from)
                .toList();

        log.info("GET /api/departments → {} row(s)", departments.size());
        return ResponseEntity.ok(departments);
    }

    // =========================================================================
    // GET /api/departments/{id}
    // =========================================================================
    // 200 — single department with employeeCount
    // 404 — id not found
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable int id) {
        DepartmentResponse response = departmentRepository.findById(id)
                .map(DepartmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department with id " + id + " not found"
                ));

        log.info("GET /api/departments/{} → found", id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/departments
    // =========================================================================
    // 201 — department created
    // 400 — validation failure (name is required)
    // 409 — duplicate name (MySQL uqDepartmentsName unique constraint)
    // =========================================================================
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {

        DepartmentResponse created = DepartmentResponse.from(
                departmentRepository.create(request.name(), request.location())
        );

        log.info("POST /api/departments → created id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // PUT /api/departments/{id}
    // =========================================================================
    // Partial update — null fields keep their current value.
    // 200 — updated department
    // 404 — id not found
    // =========================================================================
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable int id,
            @RequestBody DepartmentRequest request) { // no @Valid — all fields optional for PUT

        if (request.name() == null && request.location() == null) {
            throw new BusinessRuleException(
                    "Provide at least one field to update: name, location"
            );
        }

        DepartmentResponse updated = departmentRepository
                .update(id, request.name(), request.location())
                .map(DepartmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department with id " + id + " not found"
                ));

        log.info("PUT /api/departments/{} → updated", id);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // DELETE /api/departments/{id}
    // =========================================================================
    // 204 — deleted
    // 404 — id not found
    // 409 — department still has employees (pre-empts FK constraint violation)
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable int id) {
        int empCount = departmentRepository.countEmployees(id);
        if (empCount > 0) {
            throw new BusinessRuleException(
                    "Department has " + empCount + " employee(s); delete or reassign them first"
            );
        }

        if (!departmentRepository.delete(id)) {
            throw new ResourceNotFoundException("Department with id " + id + " not found");
        }

        log.info("DELETE /api/departments/{} → deleted", id);
        return ResponseEntity.noContent().build();
    }
}
