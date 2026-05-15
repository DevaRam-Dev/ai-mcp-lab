package com.mcplab.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single source of truth for all Department SQL.
 * Used by both DepartmentTool (MCP) and DepartmentController (REST).
 *
 * Every query that returns department rows includes employeeCount via a
 * LEFT JOIN on Employees so callers always get consistent, complete data.
 *
 * SQL naming follows schema.sql exactly:
 *   Table  : `Departments`  (backtick-quoted, PascalCase)
 *   Columns: id, name, location
 *   FK ref : `Employees`.`departmentId`  (in the LEFT JOIN and countEmployees)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DepartmentRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        log.info(box("DepartmentRepository initialized and ready")
            + lbl("Layer",   "DB → DepartmentRepository")
            + lbl("ANALOGY", "Department filing cabinet unlocked and ready"));
    }

    // Base SELECT used by all queries that return department rows.
    // The LEFT JOIN ensures departments with zero employees still appear
    // with employeeCount = 0.
    // GROUP BY all non-aggregate columns satisfies MySQL ONLY_FULL_GROUP_BY.
    private static final String BASE_SELECT =
            "SELECT d.id, d.name, d.location, COUNT(e.id) AS employeeCount "
                    + "FROM `Departments` d "
                    + "LEFT JOIN `Employees` e ON e.`departmentId` = d.id ";

    // =========================================================================
    // findAll
    // =========================================================================
    public List<Map<String, Object>> findAll() {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                BASE_SELECT + "GROUP BY d.id, d.name, d.location ORDER BY d.id"
        );
        log.debug("[DB] SELECT | table=Departments | resultCount={} | timeTaken={}ms",
                rows.size(), System.currentTimeMillis() - start);
        return rows;
    }

    // =========================================================================
    // findById
    // =========================================================================
    public Optional<Map<String, Object>> findById(int id) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                BASE_SELECT + "WHERE d.id = ? GROUP BY d.id, d.name, d.location",
                id
        );
        log.debug("[DB] SELECT | table=Departments | id={} | found={} | timeTaken={}ms",
                id, !rows.isEmpty(), System.currentTimeMillis() - start);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // =========================================================================
    // create
    // =========================================================================
    // location may be null — the column is defined as NULL in schema.sql.
    public Map<String, Object> create(String name, String location) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `Departments` (name, location) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, location); // setString(null) → SQL NULL
            return ps;
        }, keyHolder);

        int newId = keyHolder.getKey().intValue();
        log.info("[DB → DepartmentRepository] INSERT Departments | insertedId={}", newId);
        return findById(newId).orElseThrow();
    }

    // =========================================================================
    // update
    // =========================================================================
    public Optional<Map<String, Object>> update(int id, String name, String location) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE `Departments` SET ");

        if (name != null)     { sql.append("name = ?, ");     params.add(name); }
        if (location != null) { sql.append("location = ?, "); params.add(location); }

        if (params.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE id = ?");
        params.add(id);

        int updated = jdbcTemplate.update(sql.toString(), params.toArray());
        if (updated == 0) return Optional.empty();

        log.info("[DB → DepartmentRepository] UPDATE Departments | updatedId={}", id);
        return findById(id);
    }

    // =========================================================================
    // delete
    // =========================================================================
    public boolean delete(int id) {
        int deleted = jdbcTemplate.update("DELETE FROM `Departments` WHERE id = ?", id);
        if (deleted > 0) log.info("[DB → DepartmentRepository] DELETE Departments | deletedId={}", id);
        return deleted > 0;
    }

    // =========================================================================
    // existsById
    // =========================================================================
    // Used by EmployeeRepository callers to validate foreign key before INSERT/UPDATE.
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `Departments` WHERE id = ?", Integer.class, id
        );
        return count != null && count > 0;
    }

    // =========================================================================
    // countEmployees
    // =========================================================================
    // Used before deleteDepartment to enforce the business rule: a department
    // with employees cannot be deleted (mirrors ON DELETE RESTRICT in schema.sql).
    public int countEmployees(int departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `Employees` WHERE `departmentId` = ?",
                Integer.class, departmentId
        );
        return count != null ? count : 0;
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
