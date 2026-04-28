/**
 * departments.ts — API functions for every /api/departments endpoint
 *
 * WHY THIS FILE EXISTS:
 * Same rationale as api/employees.ts — one file per resource, components stay
 * free of HTTP details, the API contract is visible from the function signatures.
 *
 * BACKEND ENDPOINTS COVERED:
 *   GET    /api/departments      → listDepartments()
 *   GET    /api/departments/:id  → getDepartment(id)
 *   POST   /api/departments      → createDepartment(data)
 *   PUT    /api/departments/:id  → updateDepartment(id, data)
 *   DELETE /api/departments/:id  → deleteDepartment(id)
 *
 * IMPORTANT DELETE BEHAVIOUR:
 * DELETE /api/departments/:id returns HTTP 409 Conflict if the department still
 * has employees. The error body is { status: 409, message: "Department has N employee(s)..." }.
 * DepartmentsPage.tsx reads that message and shows it to the user so they know
 * exactly what to do (reassign or delete the employees first).
 *
 * DEPENDS ON: api/client.ts, types/Department.ts
 * DEPENDED ON BY: DepartmentsPage.tsx, DepartmentForm.tsx, EmployeesPage.tsx,
 *                 EmployeeForm.tsx (loads departments for the dropdown)
 */

import client from './client';
import type { Department, DepartmentRequest } from '../types/Department';

// ---- List / Read ----

/**
 * Fetches all departments. Each department includes an employeeCount field
 * computed by the backend's LEFT JOIN query.
 *
 * @returns Promise<Department[]> — always an array, never null.
 */
export const listDepartments = (): Promise<Department[]> =>
  client.get<Department[]>('/api/departments').then((response) => response.data);

/**
 * Fetches a single department by primary key.
 *
 * @param id - The department's auto-incremented id.
 * @returns Promise<Department> — rejects with HTTP 404 if not found.
 */
export const getDepartment = (id: number): Promise<Department> =>
  client.get<Department>(`/api/departments/${id}`).then((response) => response.data);

// ---- Create ----

/**
 * Creates a new department.
 *
 * @param data - { name: string (required), location?: string | null (optional) }
 * @returns Promise<Department> — the created row with id and employeeCount: 0.
 *          Rejects with HTTP 400 if name is missing, or HTTP 409 if the name
 *          already exists (uqDepartmentsName unique constraint).
 */
export const createDepartment = (data: DepartmentRequest): Promise<Department> =>
  client.post<Department>('/api/departments', data).then((response) => response.data);

// ---- Update ----

/**
 * Partially updates an existing department.
 *
 * @param id   - The department's primary key.
 * @param data - At least one of { name, location }. Omitted fields keep their value.
 * @returns Promise<Department> — the full updated row (including updated employeeCount).
 *          Rejects with HTTP 404 if the id doesn't exist.
 */
export const updateDepartment = (
  id: number,
  data: DepartmentRequest
): Promise<Department> =>
  client.put<Department>(`/api/departments/${id}`, data).then((response) => response.data);

// ---- Delete ----

/**
 * Deletes a department by id.
 *
 * @param id - The department's primary key.
 * @returns Promise<void> — HTTP 204 on success.
 *          Rejects with HTTP 404 if not found.
 *          Rejects with HTTP 409 if the department still has employees — the
 *          rejection includes error.response.data.message explaining the problem.
 */
export const deleteDepartment = (id: number): Promise<void> =>
  client.delete(`/api/departments/${id}`).then(() => undefined);
