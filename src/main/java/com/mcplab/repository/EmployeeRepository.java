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
 * Single source of truth for all Employee SQL.
 * Used by both EmployeeTool (MCP) and EmployeeController (REST) so the
 * query logic is written and maintained in exactly one place.
 *
 * Returns Map<String,Object> rows from JdbcTemplate; callers convert to
 * their own output format (JSON string for MCP, DTO for REST).
 *
 * SQL naming follows schema.sql exactly:
 *   Table  : `Employees`  (PascalCase, backtick-quoted)
 *   Columns: id, name, email, `departmentId`, salary, `createdAt`
 *            AS aliases lock in camelCase keys regardless of MySQL driver version.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EmployeeRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        log.info(box("EmployeeRepository initialized and ready")
            + lbl("Layer",   "DB → EmployeeRepository")
            + lbl("ANALOGY", "Employee filing cabinet unlocked and ready"));
    }

    // Column list shared by every SELECT so aliases are applied consistently.
    static final String COLS =
            "id, name, email, `departmentId` AS departmentId, salary, `createdAt` AS createdAt";

    // =========================================================================
    // findAll
    // =========================================================================
    public List<Map<String, Object>> findAll(Integer departmentId) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows;
        if (departmentId != null) {
            rows = jdbcTemplate.queryForList(
                    "SELECT " + COLS + " FROM `Employees` WHERE `departmentId` = ? ORDER BY id",
                    departmentId
            );
        } else {
            rows = jdbcTemplate.queryForList(
                    "SELECT " + COLS + " FROM `Employees` ORDER BY id"
            );
        }
        log.debug("[DB] SELECT | table=Employees | resultCount={} | deptFilter={} | timeTaken={}ms",
                rows.size(), departmentId, System.currentTimeMillis() - start);
        return rows;
    }

    // =========================================================================
    // findById
    // =========================================================================
    // Returns Optional.empty() when no row exists — avoids throwing
    // EmptyResultDataAccessException from queryForObject().
    public Optional<Map<String, Object>> findById(int id) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + COLS + " FROM `Employees` WHERE id = ?",
                id
        );
        log.debug("[DB] SELECT | table=Employees | id={} | found={} | timeTaken={}ms",
                id, !rows.isEmpty(), System.currentTimeMillis() - start);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // =========================================================================
    // create
    // =========================================================================
    // Inserts a new employee and returns the complete persisted row
    // (including the id assigned by AUTO_INCREMENT and the createdAt
    // timestamp set by MySQL DEFAULT CURRENT_TIMESTAMP).
    public Map<String, Object> create(String name, String email, int departmentId, double salary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `Employees` (name, email, `departmentId`, salary) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, departmentId);
            ps.setDouble(4, salary);
            return ps;
        }, keyHolder);

        int newId = keyHolder.getKey().intValue();
        log.info("[DB → EmployeeRepository] INSERT Employees | insertedId={}", newId);
        return findById(newId).orElseThrow();
    }

    // =========================================================================
    // update
    // =========================================================================
    // Builds a dynamic SET clause from only the non-null arguments so callers
    // can do partial updates without overwriting unprovided fields.
    // Returns Optional.empty() if no row matched the given id.
    public Optional<Map<String, Object>> update(int id,
                                                 String name,
                                                 String email,
                                                 Integer departmentId,
                                                 Double salary) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE `Employees` SET ");

        // Only the COLUMN NAMES are concatenated (from our own constants).
        // User-supplied values always go through the params list as ? placeholders.
        if (name != null)         { sql.append("name = ?, ");           params.add(name); }
        if (email != null)        { sql.append("email = ?, ");          params.add(email); }
        if (departmentId != null) { sql.append("`departmentId` = ?, "); params.add(departmentId); }
        if (salary != null)       { sql.append("salary = ?, ");         params.add(salary); }

        if (params.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        sql.delete(sql.length() - 2, sql.length()); // strip trailing ", "
        sql.append(" WHERE id = ?");
        params.add(id);

        int updated = jdbcTemplate.update(sql.toString(), params.toArray());
        if (updated == 0) return Optional.empty();

        log.info("[DB → EmployeeRepository] UPDATE Employees | updatedId={}", id);
        return findById(id);
    }

    // =========================================================================
    // delete
    // =========================================================================
    // Returns true if a row was deleted, false if the id did not exist.
    public boolean delete(int id) {
        int deleted = jdbcTemplate.update("DELETE FROM `Employees` WHERE id = ?", id);
        if (deleted > 0) log.info("[DB → EmployeeRepository] DELETE Employees | deletedId={}", id);
        return deleted > 0;
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
