/**
 * EmployeesPage.tsx — Full CRUD page for the Employees resource
 *
 * WHY THIS FILE EXISTS:
 * Container component for the Employees feature. More complex than DepartmentsPage
 * because it has an additional department filter and needs departments data for
 * two purposes: the filter dropdown AND the EmployeeForm's department dropdown.
 *
 * STATE INTERACTIONS:
 *   On mount      → load employees (all) AND departments (for filter + form)
 *   Filter change → reload employees filtered by the selected departmentId
 *   Create/Edit   → reload employees only (departments rarely change)
 *   Delete        → reload employees only
 *
 * DEPARTMENT NAME IN THE TABLE:
 * The Employee API returns `departmentId: 1` (an integer FK), not the name.
 * To show "Engineering" instead of "1" in the table, we look up the name from
 * the `departments` list we already loaded. No extra API call needed.
 *
 * ROUTE: /employees  (registered in App.tsx)
 *
 * DEPENDS ON: api/employees.ts, api/departments.ts, types/, Modal.tsx, EmployeeForm.tsx
 * DEPENDED ON BY: App.tsx
 */

import { useState, useEffect, useCallback } from 'react';
import { listEmployees, deleteEmployee } from '../api/employees';
import { listDepartments } from '../api/departments';
import type { Employee } from '../types/Employee';
import type { Department } from '../types/Department';
import Modal from './Modal';
import EmployeeForm from './EmployeeForm';

// ---- Component ----

function EmployeesPage() {

  // ---- Data State ----

  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [fetchError, setFetchError] = useState<string>('');

  // ---- Filter State ----

  /**
   * `filterDeptId` is the currently selected value in the "Filter by Department" dropdown.
   * '' (empty string) means "All Departments" — no filter applied.
   * Any other value is the string representation of a department id (e.g., "1").
   *
   * We store it as a string because <select> always gives us e.target.value as a string.
   * We only parse it to a number when building the API call.
   */
  const [filterDeptId, setFilterDeptId] = useState<string>('');

  // ---- Modal State ----

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);

  // ---- Delete Error State ----

  const [deleteError, setDeleteError] = useState<string>('');

  // ---- Data Fetching ----

  /**
   * fetchEmployees reloads the employee list, respecting the current filter value.
   *
   * WHY useCallback WITH [filterDeptId] IN DEPS:
   * This function needs to read `filterDeptId` to decide whether to filter.
   * If we didn't include filterDeptId in the deps array, the function would close
   * over the INITIAL value (always ''), and the filter would never work.
   *
   * Including filterDeptId means React recreates the function when the filter changes.
   * The useEffect below depends on `fetchEmployees` — so when fetchEmployees
   * is recreated, the effect re-runs automatically, triggering a filtered API call.
   *
   * This is the React "reactive data flow" pattern:
   *   filterDeptId changes → fetchEmployees is recreated → useEffect fires → API called
   */
  const fetchEmployees = useCallback(async () => {
    setIsLoading(true);
    setFetchError('');

    try {
      // Parse '' → undefined (no filter param), or '1' → 1 (filter by dept 1)
      const deptId = filterDeptId ? parseInt(filterDeptId, 10) : undefined;
      const data = await listEmployees(deptId);
      setEmployees(data);
    } catch {
      setFetchError(
        'Failed to load employees. Is the Spring Boot server running on port 8080?'
      );
    } finally {
      setIsLoading(false);
    }
  }, [filterDeptId]); // Re-create this function whenever the filter changes

  /**
   * fetchDepartments loads departments once on mount.
   * Departments are used for:
   *   1. The filter dropdown above the table
   *   2. The EmployeeForm's department <select>
   *   3. The getDepartmentName() lookup that shows "Engineering" instead of "1"
   *
   * Departments are NOT re-fetched after employee CRUD operations — departments
   * change rarely. If a new department is added while the user is on this page,
   * they would need to navigate away and back to see it in the filter. That's an
   * acceptable trade-off for a learning project.
   *
   * The [] dep array means this function is memoised forever (never recreated).
   */
  const fetchDepartments = useCallback(async () => {
    try {
      const data = await listDepartments();
      setDepartments(data);
    } catch {
      // Non-critical: the employee table still works even if the filter dropdown fails.
      // We log to the console but don't block the user with an error banner.
      console.error('Failed to load departments for filter dropdown');
    }
  }, []);

  /**
   * Load departments once when the component first mounts.
   */
  useEffect(() => {
    fetchDepartments();
  }, [fetchDepartments]);

  /**
   * Reload employees whenever the filter changes (or on initial mount).
   *
   * This effect automatically fires when fetchEmployees is recreated (i.e., when
   * filterDeptId changes). The chain is:
   *   1. User picks a department in the filter dropdown
   *   2. setFilterDeptId(newValue) updates state
   *   3. React re-renders and recreates fetchEmployees (because filterDeptId is in deps)
   *   4. This useEffect detects the new fetchEmployees reference and re-runs
   *   5. fetchEmployees calls GET /api/employees?departmentId=X with the new filter
   */
  useEffect(() => {
    fetchEmployees();
  }, [fetchEmployees]);

  // ---- Helper: Department Name Lookup ----

  /**
   * getDepartmentName converts a numeric departmentId into the department's display name.
   * Used to show "Engineering" in the table instead of the raw foreign key value "1".
   *
   * We use Array.find() on the `departments` list we already have in state — this is
   * a simple in-memory lookup, not an API call. O(n) where n = number of departments,
   * which is small (typically < 20) so performance is not a concern.
   *
   * @param deptId - The departmentId number from an Employee object
   * @returns The department's name, or '—' if not found in the local list
   */
  const getDepartmentName = (deptId: number): string => {
    const dept = departments.find((d) => d.id === deptId);
    return dept ? dept.name : '—';
  };

  // ---- Modal Event Handlers ----

  const openCreateModal = () => {
    setSelectedEmployee(null);
    setModalMode('create');
    setIsModalOpen(true);
    setDeleteError('');
  };

  const openEditModal = (emp: Employee) => {
    setSelectedEmployee(emp);
    setModalMode('edit');
    setIsModalOpen(true);
    setDeleteError('');
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedEmployee(null);
  };

  // ---- Delete Handler ----

  /**
   * handleDeleteEmployee confirms and executes deletion.
   *
   * Employee deletes are simpler than department deletes — there's no FK constraint
   * blocking them (employees don't have children). The only failure case is HTTP 404
   * (id not found), which is unlikely in normal use.
   *
   * @param emp - The employee the user clicked "Delete" on
   */
  const handleDeleteEmployee = async (emp: Employee) => {
    const confirmed = window.confirm(
      `Delete employee "${emp.name}"?\n\nThis cannot be undone.`
    );
    if (!confirmed) return;

    setDeleteError('');

    try {
      await deleteEmployee(emp.id);
      fetchEmployees(); // Refresh the list — the deleted row disappears
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      setDeleteError(
        axiosError?.response?.data?.message ??
          `Failed to delete "${emp.name}". Please try again.`
      );
    }
  };

  // ---- Filter Change Handler ----

  /**
   * handleFilterChange updates the filter state when the user picks a department.
   *
   * Because filterDeptId is in fetchEmployees' useCallback deps, React will
   * automatically recreate fetchEmployees after this setState — and the useEffect
   * that depends on fetchEmployees will fire, triggering a fresh API call.
   * No manual "if filter changed, reload" logic needed — React's reactive model
   * handles it automatically.
   *
   * @param e - The change event from the <select> element
   */
  const handleFilterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setFilterDeptId(e.target.value);
  };

  // ---- Render ----

  return (
    <div className="pageWrapper">

      {/* ---- Page Header ---- */}
      <div className="pageHeader">
        <h1 className="pageTitle">Employees</h1>
        <button className="btnPrimary" onClick={openCreateModal}>
          + Add Employee
        </button>
      </div>

      {/* ---- Department Filter Bar ---- */}
      <div className="filterBar">
        <label htmlFor="deptFilter" className="filterLabel">
          Filter by Department:
        </label>
        <select
          id="deptFilter"
          value={filterDeptId}
          onChange={handleFilterChange}
          className="filterSelect"
        >
          {/* "All Departments" resets the filter — API call will have no departmentId param */}
          <option value="">All Departments</option>
          {departments.map((dept) => (
            <option key={dept.id} value={dept.id.toString()}>
              {dept.name}
            </option>
          ))}
        </select>
      </div>

      {/* ---- Error Banners ---- */}
      {fetchError && (
        <div className="errorBanner" role="alert">{fetchError}</div>
      )}
      {deleteError && (
        <div className="errorBanner" role="alert">{deleteError}</div>
      )}

      {/* ---- Table or Loading ---- */}
      {isLoading ? (
        <div className="loadingText">Loading employees…</div>
      ) : (
        <div className="tableWrapper">
          <table className="dataTable">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Department</th>
                <th>Salary (₹)</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {employees.length === 0 ? (
                <tr>
                  <td colSpan={7} className="emptyRow">
                    {/*
                      Show a contextual empty message depending on whether a filter is active.
                      "No employees in this department" is more helpful than a generic
                      "No employees found" when the user is filtering.
                    */}
                    {filterDeptId
                      ? 'No employees in this department.'
                      : 'No employees found. Click "+ Add Employee" to create one.'}
                  </td>
                </tr>
              ) : (
                employees.map((emp) => (
                  <tr key={emp.id}>
                    <td>{emp.id}</td>
                    <td>{emp.name}</td>
                    <td>{emp.email}</td>
                    <td>
                      {/* getDepartmentName does a local array lookup — no extra API call */}
                      {getDepartmentName(emp.departmentId)}
                    </td>
                    <td className="salaryCell">
                      {/*
                        toLocaleString('en-IN') formats numbers with Indian grouping:
                        85000 → "85,000"  (en-IN uses commas at thousands)
                        This is purely cosmetic — the underlying number is unchanged.
                      */}
                      {emp.salary.toLocaleString('en-IN')}
                    </td>
                    <td>
                      {/* emp.createdAt can be null — show em-dash as placeholder */}
                      {emp.createdAt ?? '—'}
                    </td>
                    <td className="actionCell">
                      <button className="btnEdit" onClick={() => openEditModal(emp)}>
                        Edit
                      </button>
                      <button className="btnDelete" onClick={() => handleDeleteEmployee(emp)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* ---- Create / Edit Modal ---- */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={modalMode === 'create' ? 'Add Employee' : 'Edit Employee'}
      >
        <EmployeeForm
          mode={modalMode}
          employee={selectedEmployee ?? undefined}
          departments={departments}   // Pass down the already-loaded departments list
          onClose={closeModal}
          onSuccess={fetchEmployees}  // On save, re-fetch the employee list only
        />
      </Modal>

    </div>
  );
}

export default EmployeesPage;
