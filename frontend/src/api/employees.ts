/**
 * employees.ts — API functions for every /api/employees endpoint
 *
 * WHY THIS FILE EXISTS:
 * Keeps all HTTP logic for employees in one place. Components never call axios
 * directly — they import these typed functions. Benefits:
 *   - The function signatures document the API contract (inputs, outputs, errors)
 *   - If the backend URL or response shape changes, we fix it here only
 *   - Each function can be tested in isolation (mock client, check args)
 *
 * BACKEND ENDPOINTS COVERED:
 *   GET    /api/employees              → listEmployees()
 *   GET    /api/employees?departmentId → listEmployees(departmentId)
 *   GET    /api/employees/:id          → getEmployee(id)
 *   POST   /api/employees              → createEmployee(data)
 *   PUT    /api/employees/:id          → updateEmployee(id, data)
 *   DELETE /api/employees/:id          → deleteEmployee(id)
 *
 * ERROR HANDLING CONVENTION:
 * These functions do NOT catch errors. They let the promise reject so the calling
 * component can decide how to show the error. The server's error JSON is at
 * error.response.data — e.g., { status: 400, message: "Email is required" }.
 *
 * DEPENDS ON: api/client.ts, types/Employee.ts
 * DEPENDED ON BY: EmployeesPage.tsx, EmployeeForm.tsx
 */

import client from './client';
import type { Employee, EmployeeRequest } from '../types/Employee';

// ---- List / Read ----

/**
 * Fetches all employees, with an optional department filter.
 *
 * @param departmentId - When provided, the backend adds WHERE departmentId = X.
 *                       When omitted (undefined), all employees are returned.
 * @returns Promise<Employee[]> — resolves to an array (empty array if none match,
 *          never null).
 *
 * WHY `params` OBJECT: Axios serialises { departmentId: 1 } → ?departmentId=1 in the URL.
 * We only include the key when departmentId has a value — sending ?departmentId=undefined
 * would produce a literal "undefined" string in the URL, which the backend ignores but
 * is sloppy. The conditional {} skips the param entirely when not filtering.
 */
export const listEmployees = (departmentId?: number): Promise<Employee[]> =>
  client
    .get<Employee[]>('/api/employees', {
      params: departmentId !== undefined ? { departmentId } : {},
    })
    .then((response) => response.data);

/**
 * Fetches a single employee by primary key.
 *
 * @param id - The employee's auto-incremented id.
 * @returns Promise<Employee> — rejects with an AxiosError (HTTP 404) if not found.
 */
export const getEmployee = (id: number): Promise<Employee> =>
  client.get<Employee>(`/api/employees/${id}`).then((response) => response.data);

// ---- Create ----

/**
 * Creates a new employee. All four fields are required.
 *
 * @param data - { name, email, departmentId, salary }
 * @returns Promise<Employee> — the created row, with id and createdAt now populated.
 *          Rejects with HTTP 400 if validation fails, or HTTP 409 if the departmentId
 *          references a department that doesn't exist.
 */
export const createEmployee = (data: EmployeeRequest): Promise<Employee> =>
  client.post<Employee>('/api/employees', data).then((response) => response.data);

// ---- Update ----

/**
 * Partially updates an existing employee.
 * Only the fields present in `data` are changed; omitted fields keep their DB value.
 *
 * @param id   - The employee's primary key.
 * @param data - Partial<EmployeeRequest>: can be just { salary: 95000 } or all four fields.
 * @returns Promise<Employee> — the full updated row.
 *          Rejects with HTTP 404 if the id doesn't exist.
 *
 * WHY Partial<EmployeeRequest>: TypeScript's Partial<T> makes every field in T optional.
 * This matches the backend's partial-update behaviour — null / missing fields are skipped.
 */
export const updateEmployee = (
  id: number,
  data: Partial<EmployeeRequest>
): Promise<Employee> =>
  client.put<Employee>(`/api/employees/${id}`, data).then((response) => response.data);

// ---- Delete ----

/**
 * Deletes an employee by id.
 *
 * @param id - The employee's primary key.
 * @returns Promise<void> — the backend returns HTTP 204 No Content on success.
 *          Rejects with HTTP 404 if the id doesn't exist.
 *
 * WHY `then(() => undefined)`: HTTP 204 has no response body. Axios still resolves
 * the promise but response.data would be an empty string. We explicitly return void
 * so callers don't accidentally try to use a non-existent body.
 */
export const deleteEmployee = (id: number): Promise<void> =>
  client.delete(`/api/employees/${id}`).then(() => undefined);
