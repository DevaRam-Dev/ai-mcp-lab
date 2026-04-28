-- =============================================================================
-- schema.sql — DDL for ai_mcp_lab database
-- =============================================================================
--
-- WHAT IS THIS FILE AND HOW DOES SPRING BOOT RUN IT?
-- ===================================================
-- Spring Boot's SqlInitializationAutoConfiguration detects this file on the
-- classpath and executes it against the configured DataSource at startup,
-- BEFORE the application accepts any requests.
--
-- Execution order is guaranteed:
--   1. schema.sql  — creates tables (DDL: CREATE, DROP, ALTER)
--   2. data.sql    — inserts sample rows (DML: INSERT)
--
-- This order matters: data.sql references tables that must already exist.
-- Spring Boot enforces it automatically — you do not need to manage it.
--
-- LEARNING NOTE — Spring MVC vs Spring Boot database initialisation:
-- ------------------------------------------------------------------
-- Spring MVC (EFFORT-Web):
--   - You ran schema DDL manually via MySQL Workbench, cli, or Flyway scripts.
--   - There was no automatic SQL execution at startup.
--   - The app assumed the schema already existed in the target database.
--
-- Spring Boot (this project):
--   - Add schema.sql + data.sql to src/main/resources/
--   - Set spring.sql.init.mode=always in application.properties
--   - Spring Boot executes them on every startup — zero manual steps.
--
-- This makes the project self-contained: a new developer clones the repo,
-- updates application.properties with their local credentials, and runs
-- the app. The database is created and seeded automatically.
--
-- IMPORTANT — DROP TABLE re-creates from scratch on every restart:
-- ----------------------------------------------------------------
-- This script DROPs and re-CREATEs tables on every startup.
-- That is intentional for a learning lab — you always start with
-- clean, predictable sample data.
--
-- For production systems, use Flyway or Liquibase instead:
--   - They track which scripts have already run (via a version table).
--   - They apply only NEW migration scripts, preserving existing data.
--   - spring.sql.init.* is for development/testing only.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Tear down: drop child tables before parent tables to satisfy FK constraints.
-- Employees references Departments, so Employees must be dropped first.
-- Backtick-quoted because table names are now PascalCase; MySQL on Linux is
-- case-sensitive for table names (lower_case_table_names=0 default).
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `Employees`;
DROP TABLE IF EXISTS `Departments`;


-- =============================================================================
-- TABLE: Departments
-- =============================================================================
-- Lookup table for organisational departments.
-- Acts as the "parent" side of the Employees foreign key relationship.
-- Must be created before Employees because Employees.departmentId references it.
-- =============================================================================
CREATE TABLE `Departments` (

    -- Surrogate primary key.
    -- AUTO_INCREMENT: MySQL assigns the next integer on each INSERT.
    -- In EFFORT-Web you used the same pattern with Spring JDBC + getGeneratedKeys().
    id          INT             NOT NULL AUTO_INCREMENT,

    -- Human-readable department name. 100 chars is generous; departments rarely
    -- have long names. NOT NULL enforces data integrity at the DB level — a
    -- department without a name is meaningless.
    name        VARCHAR(100)    NOT NULL,

    -- Physical or virtual office location for this department.
    -- NULL allowed: some departments (e.g. fully remote) have no fixed location.
    location    VARCHAR(200)    NULL,

    CONSTRAINT pkDepartments PRIMARY KEY (id),

    -- Prevent duplicate department names — the application should also validate
    -- this, but a DB-level UNIQUE constraint is the last line of defence.
    CONSTRAINT uqDepartmentsName UNIQUE (name)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Organisational departments; parent side of the Employees FK';
-- ENGINE=InnoDB: required for foreign key support. MyISAM does not support FKs.
-- utf8mb4: full Unicode including emoji. utf8 in MySQL is only 3-byte; utf8mb4
--          is the correct charset for any modern application.


-- =============================================================================
-- TABLE: Employees
-- =============================================================================
-- Stores individual employee records.
-- The "child" side of the department relationship — every employee belongs to
-- exactly one department.
-- =============================================================================
CREATE TABLE `Employees` (

    -- Surrogate primary key. Same rationale as Departments.id.
    id              INT             NOT NULL AUTO_INCREMENT,

    -- Employee's full name. 100 chars covers even long compound names.
    -- NOT NULL: an employee without a name cannot be meaningfully displayed.
    name            VARCHAR(100)    NOT NULL,

    -- Corporate email address used for login and communication.
    -- UNIQUE: two employees cannot share the same email — enforced at DB level.
    -- 150 chars: RFC 5321 specifies a maximum email length of 254; 150 is a
    -- practical limit that avoids excessively wide index entries.
    email           VARCHAR(150)    NOT NULL,

    -- Foreign key to Departments.id.
    -- NOT NULL: every employee must belong to a department (no orphan records).
    -- Column is named departmentId (not just department) to make the FK
    -- relationship obvious to any developer reading the schema.
    `departmentId`  INT             NOT NULL,

    -- Annual gross salary in the local currency (assumed INR for this lab).
    -- DECIMAL(10,2): stores up to 99,999,999.99 exactly, without floating-point
    -- rounding errors. Never use FLOAT or DOUBLE for monetary values.
    salary          DECIMAL(10, 2)  NOT NULL,

    -- Audit timestamp: when was this row created?
    -- DEFAULT CURRENT_TIMESTAMP: MySQL fills this automatically on INSERT.
    -- You do not need to pass it in application code — the DB handles it.
    -- Analogy: equivalent to @CreatedDate in Spring Data JPA.
    `createdAt`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pkEmployees PRIMARY KEY (id),
    CONSTRAINT uqEmployeesEmail UNIQUE (email),

    -- Named foreign key constraint.
    -- Naming it (fkEmployeesDepartmentId) makes error messages readable:
    --   "Cannot add or update a child row: a foreign key constraint fails
    --    (fkEmployeesDepartmentId)" — you know exactly which constraint fired.
    -- Un-named constraints produce cryptic auto-generated names.
    CONSTRAINT fkEmployeesDepartmentId
        FOREIGN KEY (`departmentId`)
        REFERENCES `Departments` (id)
        -- ON DELETE RESTRICT: prevent deleting a department that still has employees.
        -- The application must reassign or remove employees first.
        -- Alternative: ON DELETE CASCADE would auto-delete employees — dangerous in
        -- most business scenarios.
        ON DELETE RESTRICT
        ON UPDATE CASCADE   -- if Departments.id changes, cascade to Employees.departmentId

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Employee records; child side of the Departments FK';
