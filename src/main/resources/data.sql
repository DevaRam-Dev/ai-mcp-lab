-- =============================================================================
-- data.sql — Seed / sample data for ai_mcp_lab
-- =============================================================================
--
-- WHY ARE SCHEMA AND DATA SPLIT INTO TWO FILES?
-- ==============================================
-- Spring Boot guarantees this execution order on startup:
--   1. schema.sql  — DDL first  (tables must exist before rows can be inserted)
--   2. data.sql    — DML second (this file — runs after schema.sql completes)
--
-- If both were in one file, a CREATE TABLE followed immediately by an INSERT
-- would also work — but Spring Boot's two-file convention is cleaner because:
--   a) Schema and seed data have different lifecycles. In a team environment
--      you might want to skip data seeding in production (change init.mode)
--      while always running schema migrations.
--   b) CI pipelines often run schema separately from test-data loading.
--   c) Keeping DDL and DML separate mirrors standard database migration
--      tools (Flyway uses V1__schema.sql, V2__seed_data.sql etc.).
--
-- HOW SPRING BOOT AUTO-EXECUTES THESE FILES
-- ==========================================
-- The properties in application.properties control this behaviour:
--
--   spring.sql.init.mode=always
--     → Run SQL init scripts every startup (required for external DBs like MySQL).
--     → Default is "embedded" which only triggers for H2/HSQLDB/Derby.
--
--   spring.sql.init.schema-locations=classpath:schema.sql
--     → Tells Spring Boot WHERE to find the DDL script.
--     → "classpath:" prefix means: look in src/main/resources/
--
--   spring.sql.init.data-locations=classpath:data.sql
--     → Tells Spring Boot WHERE to find the DML seed script (this file).
--
-- Spring Boot's SqlInitializationAutoConfiguration picks up these properties
-- and runs them via ScriptUtils.executeSqlScript() against the DataSource bean.
-- No code, no @Bean, no XML — just properties and these two files.
--
-- LEARNING NOTE — Spring MVC vs Spring Boot:
-- ------------------------------------------
-- Spring MVC (EFFORT-Web):
--   "Run these SQL scripts manually before deploying. DBA creates the schema.
--    Dev team populates test data by hand or via a separate SQL file they
--    run once."
--
-- Spring Boot (this project):
--   "Drop this file in src/main/resources/ and set spring.sql.init.mode=always.
--    Spring Boot runs it automatically. Every developer, every CI build,
--    every Docker container starts with the same clean seed data."
--
-- NOTE ON RE-RUNS:
-- Since schema.sql DROPs and re-CREATEs tables on every startup, this data.sql
-- uses plain INSERT INTO (not INSERT IGNORE). The tables are always empty
-- when data.sql runs, so there are no duplicate key conflicts.
-- =============================================================================


-- =============================================================================
-- SEED DATA: Departments
-- =============================================================================
-- Insert parent rows first. Employees.departmentId FK requires these to exist.
-- Explicit id values (1, 2, 3) let data.sql reference them in Employees INSERTs
-- below without needing subqueries to look them up.
-- =============================================================================
INSERT INTO `Departments` (id, name, location) VALUES
    (1, 'Engineering', 'Hyderabad'),
    (2, 'Sales',       'Mumbai'),
    (3, 'HR',          'Bangalore');


-- =============================================================================
-- SEED DATA: Employees
-- =============================================================================
-- Five employees spread across all three departments.
-- createdAt is omitted — MySQL fills it with CURRENT_TIMESTAMP automatically
-- (as defined by the DEFAULT clause in schema.sql).
--
-- salary is DECIMAL(10,2): values are exact, no floating-point surprise.
-- departmentId must match one of the ids inserted above (FK constraint).
-- =============================================================================
INSERT INTO `Employees` (id, name, email, `departmentId`, salary) VALUES
    -- Engineering (departmentId = 1) — two employees
    (1, 'Ravi Kumar',    'ravi.kumar@mcplab.com',    1, 85000.00),
    (2, 'Priya Sharma',  'priya.sharma@mcplab.com',  1, 92000.00),

    -- Sales (departmentId = 2) — two employees
    (3, 'Amit Patel',    'amit.patel@mcplab.com',    2, 65000.00),
    (4, 'Kiran Nair',    'kiran.nair@mcplab.com',    2, 68000.00),

    -- HR (departmentId = 3) — one employee
    (5, 'Sunita Reddy',  'sunita.reddy@mcplab.com',  3, 70000.00);
