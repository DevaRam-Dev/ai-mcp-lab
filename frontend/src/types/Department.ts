/**
 * Department.ts — TypeScript type definitions for the Department domain
 *
 * WHY THIS FILE EXISTS:
 * Same reason as Employee.ts — centralised type definitions ensure every part of
 * the app agrees on what a "department" looks like, preventing silent shape mismatches.
 *
 * DEPENDS ON: Nothing
 * DEPENDED ON BY:
 *   - api/departments.ts   (types API return values)
 *   - DepartmentsPage.tsx  (types the `departments` state array)
 *   - DepartmentForm.tsx   (types the `department` prop)
 *   - EmployeeForm.tsx     (needs Department[] for the dropdown)
 *   - EmployeesPage.tsx    (uses Department[] for the filter dropdown and name lookup)
 *
 * MATCHES BACKEND:
 *   - Department        mirrors DepartmentResponse.java
 *   - DepartmentRequest mirrors DepartmentRequest.java
 */

// ---- Response Shape ----

/**
 * A department row as returned by:
 *   GET /api/departments
 *   GET /api/departments/:id
 *   POST /api/departments
 *   PUT  /api/departments/:id
 *
 * employeeCount is always present (never undefined) because the backend uses a
 * LEFT JOIN — departments with zero employees return 0, not null.
 */
export interface Department {
  id: number;
  name: string;
  location: string | null; // Optional in the DB (NULL allowed); use `?? '—'` when rendering
  employeeCount: number;   // Computed via COUNT(e.id) in the repository; always >= 0
}

// ---- Request Shape ----

/**
 * Payload for creating or partially updating a department.
 *
 * Both fields are optional here (using `?`) because:
 *   - POST requires `name` (validated server-side with @NotBlank)
 *   - PUT is a partial update — you can send just { name: "New Name" } or just { location: "..." }
 *
 * Sending `location: null` explicitly clears an existing location in the database.
 * Omitting `location` entirely leaves the current value unchanged (partial update).
 */
export interface DepartmentRequest {
  name?: string;
  location?: string | null;
}
