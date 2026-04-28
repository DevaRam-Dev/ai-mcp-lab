/**
 * Employee.ts — TypeScript type definitions for the Employee domain
 *
 * WHY THIS FILE EXISTS:
 * TypeScript interfaces enforce that every component and API function in the app
 * uses the same data shape for employees. Without this, a typo in a field name
 * would silently produce `undefined` at runtime — TypeScript catches it at
 * compile time instead, saving debugging time.
 *
 * DEPENDS ON: Nothing (pure type definitions — no imports needed)
 * DEPENDED ON BY:
 *   - api/employees.ts   (types the return values of every API function)
 *   - EmployeesPage.tsx  (types the `employees` state array)
 *   - EmployeeForm.tsx   (types the `employee` prop and request payload)
 *
 * MATCHES BACKEND:
 *   - Employee      mirrors EmployeeResponse.java  (what the API sends back)
 *   - EmployeeRequest mirrors EmployeeRequest.java (what we POST / PUT)
 */

// ---- Response Shape (what the API sends back to us) ----

/**
 * Represents a single employee row as returned by:
 *   GET /api/employees
 *   GET /api/employees/:id
 *   POST /api/employees   (returns the created employee)
 *   PUT  /api/employees/:id (returns the updated employee)
 *
 * Field names are camelCase and must match the JSON keys in the Spring Boot response
 * exactly — JSON is case-sensitive, so `departmentId` ≠ `DepartmentId`.
 */
export interface Employee {
  id: number;              // Auto-incremented primary key; assigned by MySQL
  name: string;            // Full name, e.g. "Ravi Kumar"
  email: string;           // Unique per the uqEmployeesEmail constraint in the DB
  departmentId: number;    // FK to the Departments table — used to look up department name
  salary: number;          // MySQL DECIMAL(10,2) arrives as a JS number via JSON
  createdAt: string | null; // ISO datetime string like "2024-01-15 10:30:00", or null
}

// ---- Request Shape (what we send TO the API) ----

/**
 * Payload for creating a new employee via POST /api/employees.
 * The backend @NotBlank / @NotNull / @Positive annotations mean all four
 * fields are required for a create — the server returns HTTP 400 if any are missing.
 *
 * For UPDATE (PUT /api/employees/:id), we use Partial<EmployeeRequest> so callers
 * can send only the fields they want to change — the backend applies partial updates.
 */
export interface EmployeeRequest {
  name: string;
  email: string;
  departmentId: number; // Must reference an existing department — server returns 409 if not
  salary: number;       // Must be > 0 — server validates with @Positive
}
