package com.mcplab.controller;

import com.mcplab.dto.DepartmentRequest;
import com.mcplab.dto.DepartmentResponse;
import com.mcplab.exception.BusinessRuleException;
import com.mcplab.exception.ResourceNotFoundException;
import com.mcplab.repository.DepartmentRepository;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        log.info(box("DepartmentController initialized and ready")
            + lbl("Layer",   "API → DepartmentController")
            + lbl("ANALOGY", "Front desk receptionist on duty"));
    }

    // =========================================================================
    // GET /api/departments
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> listDepartments() {
        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : GET /api/departments")
            + lbl("Layer",       "API → DepartmentController")
            + lbl("Input",       "none")
            + lbl("Description", "Return all departments with employee counts"));

        List<DepartmentResponse> departments = departmentRepository.findAll()
                .stream()
                .map(DepartmentResponse::from)
                .toList();

        log.info(heavyBox("REQUEST END : GET /api/departments — 200 OK")
            + lbl("Output",   departments.size() + " department(s) returned")
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(departments);
    }

    // =========================================================================
    // GET /api/departments/{id}
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable int id) {
        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : GET /api/departments/{id}")
            + lbl("Layer",       "API → DepartmentController")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Return single department with employee count"));

        DepartmentResponse response = departmentRepository.findById(id)
                .map(DepartmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department with id " + id + " not found"
                ));

        log.info(heavyBox("REQUEST END : GET /api/departments/{id} — 200 OK")
            + lbl("Output",   "department id=" + response.id() + " name=" + response.name())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/departments
    // =========================================================================
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {

        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : POST /api/departments")
            + lbl("Layer",       "API → DepartmentController")
            + lbl("Input",       "name=" + request.name() + ", location=" + request.location())
            + lbl("Description", "Create new department record"));

        DepartmentResponse created = DepartmentResponse.from(
                departmentRepository.create(request.name(), request.location())
        );

        log.info(heavyBox("REQUEST END : POST /api/departments — 201 Created")
            + lbl("Output",   "created id=" + created.id() + " name=" + created.name())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // PUT /api/departments/{id}
    // =========================================================================
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable int id,
            @RequestBody DepartmentRequest request) { // no @Valid — all fields optional for PUT

        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : PUT /api/departments/{id}")
            + lbl("Layer",       "API → DepartmentController")
            + lbl("Input",       "id=" + id + ", name=" + request.name() + ", location=" + request.location())
            + lbl("Description", "Partial update — only non-null fields applied"));

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

        log.info(heavyBox("REQUEST END : PUT /api/departments/{id} — 200 OK")
            + lbl("Output",   "updated id=" + updated.id())
            + lbl("Duration", (System.currentTimeMillis() - start) + "ms"));
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // DELETE /api/departments/{id}
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable int id) {
        long start = System.currentTimeMillis();
        log.info(heavyBox("REQUEST START : DELETE /api/departments/{id}")
            + lbl("Layer",       "API → DepartmentController")
            + lbl("Input",       "id=" + id)
            + lbl("Description", "Delete department; blocked if employees still exist"));

        int empCount = departmentRepository.countEmployees(id);
        if (empCount > 0) {
            throw new BusinessRuleException(
                    "Department has " + empCount + " employee(s); delete or reassign them first"
            );
        }

        if (!departmentRepository.delete(id)) {
            throw new ResourceNotFoundException("Department with id " + id + " not found");
        }

        log.info(heavyBox("REQUEST END : DELETE /api/departments/{id} — 204 No Content")
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
