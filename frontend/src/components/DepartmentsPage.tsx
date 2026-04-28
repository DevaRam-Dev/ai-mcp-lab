/**
 * DepartmentsPage.tsx — Full CRUD page for the Departments resource
 *
 * WHY THIS FILE EXISTS:
 * This is the "container" (or "smart") component for the Departments feature.
 * It owns all the state and orchestrates interactions:
 *   - Fetching the department list from the API
 *   - Showing a loading indicator while fetching
 *   - Opening create / edit modals
 *   - Confirming and executing deletes
 *   - Surfacing server error messages (especially the 409 FK violation)
 *
 * The DepartmentForm component (the actual form fields) lives in DepartmentForm.tsx.
 * The modal shell (backdrop, header, ESC-close) lives in Modal.tsx.
 * Separation of concerns: this file knows about departments; the others are reusable.
 *
 * ROUTE: /departments  (registered in App.tsx)
 *
 * DEPENDS ON: api/departments.ts, types/Department.ts, Modal.tsx, DepartmentForm.tsx
 * DEPENDED ON BY: App.tsx (loaded by React Router when URL is /departments)
 */

import { useState, useEffect, useCallback } from 'react';
import { listDepartments, deleteDepartment } from '../api/departments';
import type { Department } from '../types/Department';
import Modal from './Modal';
import DepartmentForm from './DepartmentForm';

// ---- Component ----

function DepartmentsPage() {

  // ---- Data State ----

  /**
   * `departments` holds the array of departments fetched from the API.
   * Typed as Department[] — TypeScript will catch any field name typos when we
   * try to read dept.name, dept.employeeCount, etc. in the JSX below.
   */
  const [departments, setDepartments] = useState<Department[]>([]);

  /**
   * `isLoading` is true while the API call is in flight.
   * We show a "Loading…" message instead of an empty table, which prevents the
   * confusing "flash of empty content" that makes users think there's no data.
   */
  const [isLoading, setIsLoading] = useState<boolean>(true);

  /**
   * `fetchError` holds an error message if the initial data load fails.
   * Shown as a page-level banner with a friendly hint about the server.
   */
  const [fetchError, setFetchError] = useState<string>('');

  // ---- Modal State ----

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  /**
   * `modalMode` tells the DepartmentForm whether to render a blank create form
   * or a prefilled edit form. Stored here (not in the form) because the modal
   * title also changes based on mode.
   */
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');

  /**
   * `selectedDepartment` is set when the user clicks "Edit" on a row.
   * It's passed to DepartmentForm as the `department` prop to prefill the fields.
   * null means we're in create mode — no department has been selected yet.
   */
  const [selectedDepartment, setSelectedDepartment] = useState<Department | null>(null);

  // ---- Delete Error State ----

  /**
   * `deleteError` is separate from `fetchError` because it has a different UX:
   * - fetchError: the whole page failed to load → show message where the table would be
   * - deleteError: a specific row's delete failed → show below the page header
   *
   * The most important case: deleting a department that still has employees.
   * The backend returns HTTP 409 with message "Department has N employee(s); delete
   * or reassign them first". We show THAT exact message so the user knows what to do.
   */
  const [deleteError, setDeleteError] = useState<string>('');

  // ---- Data Fetching ----

  /**
   * fetchDepartments loads the full department list from GET /api/departments.
   *
   * WHY useCallback:
   * We need to call this function in multiple places:
   *   1. In a useEffect on mount (initial load)
   *   2. After a successful create/edit (to refresh the table)
   *   3. After a successful delete (to refresh the table)
   *
   * If we defined fetchDepartments as a plain `const` inside the component body,
   * React would create a NEW function reference on every render. The useEffect that
   * depends on it would then re-run on every render → infinite loop.
   *
   * useCallback memoises the function — it returns the SAME function reference
   * across renders as long as the dependency array contents don't change.
   * The [] dep array means "never recreate this function" (it has no external deps).
   */
  const fetchDepartments = useCallback(async () => {
    setIsLoading(true);
    setFetchError(''); // Clear any previous error so old messages don't linger

    try {
      const data = await listDepartments();
      setDepartments(data); // Replace the list entirely — always the freshest data
    } catch {
      // Provide a user-friendly message. "Connection refused" means the server is down.
      setFetchError(
        'Failed to load departments. Is the Spring Boot server running on port 8080?'
      );
    } finally {
      // `finally` always runs — this clears the loading flag whether we succeeded or failed
      setIsLoading(false);
    }
  }, []); // No external dependencies → memoised forever

  /**
   * Load departments once when the component mounts.
   *
   * useEffect runs after the first render (and after re-renders if deps change).
   * The dep array [fetchDepartments] means "re-run if fetchDepartments changes" —
   * because fetchDepartments is memoised with [], it effectively runs ONCE on mount.
   *
   * React analogy: this is equivalent to @PostConstruct in a Spring bean,
   * or document.ready() / $(function(){}) in jQuery.
   */
  useEffect(() => {
    fetchDepartments();
  }, [fetchDepartments]);

  // ---- Modal Event Handlers ----

  /**
   * openCreateModal resets selectedDepartment so the form starts blank.
   * Also clears the delete error so stale red banners disappear when we open a modal.
   */
  const openCreateModal = () => {
    setSelectedDepartment(null);
    setModalMode('create');
    setIsModalOpen(true);
    setDeleteError('');
  };

  /**
   * openEditModal stores the clicked row in state so DepartmentForm can prefill it.
   *
   * @param dept - The department the user clicked "Edit" on
   */
  const openEditModal = (dept: Department) => {
    setSelectedDepartment(dept);
    setModalMode('edit');
    setIsModalOpen(true);
    setDeleteError('');
  };

  /**
   * closeModal hides the modal and clears the selection.
   * Called by backdrop click, ESC key (in Modal.tsx), or the Cancel button.
   */
  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedDepartment(null);
  };

  // ---- Delete Handler ----

  /**
   * handleDeleteDepartment confirms deletion and calls the API.
   *
   * FLOW:
   *  1. Show window.confirm dialog — prevents accidental deletes
   *  2. Call deleteDepartment(id) — HTTP DELETE
   *  3. On success: re-fetch the table (row disappears)
   *  4. On 409 Conflict: show the server's message ("Department has N employees…")
   *  5. On other error: show a generic fallback message
   *
   * WHY window.confirm: It's the simplest possible confirmation UX. In a production
   * app you'd use a custom confirmation modal, but window.confirm blocks execution
   * synchronously, which keeps this function simple for a learning project.
   *
   * @param dept - The department the user clicked "Delete" on
   */
  const handleDeleteDepartment = async (dept: Department) => {
    const confirmed = window.confirm(
      `Delete department "${dept.name}"?\n\nThis cannot be undone.`
    );
    if (!confirmed) return; // User clicked Cancel — do nothing

    setDeleteError(''); // Clear any previous delete error before retrying

    try {
      await deleteDepartment(dept.id);
      fetchDepartments(); // Refresh the list — the deleted row will be gone
    } catch (error: unknown) {
      /**
       * The backend returns HTTP 409 when the department has employees.
       * error.response.data.message contains the human-readable explanation.
       * We surface THAT exact message rather than a vague "delete failed" so
       * users understand they need to reassign or delete the employees first.
       */
      const axiosError = error as { response?: { data?: { message?: string } } };
      setDeleteError(
        axiosError?.response?.data?.message ??
          `Failed to delete "${dept.name}". Please try again.`
      );
    }
  };

  // ---- Render ----

  return (
    <div className="pageWrapper">

      {/* ---- Page Header ---- */}
      <div className="pageHeader">
        <h1 className="pageTitle">Departments</h1>
        <button className="btnPrimary" onClick={openCreateModal}>
          + Add Department
        </button>
      </div>

      {/* ---- Error Banners ---- */}
      {/* Shown when the initial GET /api/departments call fails */}
      {fetchError && (
        <div className="errorBanner" role="alert">{fetchError}</div>
      )}
      {/* Shown when a DELETE fails — most commonly the 409 FK constraint message */}
      {deleteError && (
        <div className="errorBanner" role="alert">{deleteError}</div>
      )}

      {/* ---- Table or Loading ---- */}
      {isLoading ? (
        // Show a loading message while the first API call is in flight.
        // Without this, users would see an empty table before data arrives.
        <div className="loadingText">Loading departments…</div>
      ) : (
        <div className="tableWrapper">
          <table className="dataTable">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Location</th>
                <th>Employees</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {departments.length === 0 ? (
                // Empty state: show a helpful message instead of a blank tbody
                <tr>
                  <td colSpan={5} className="emptyRow">
                    No departments found. Click "+ Add Department" to create one.
                  </td>
                </tr>
              ) : (
                departments.map((dept) => (
                  /*
                    `key` on the <tr> is React's mechanism for efficient list updates.
                    It must be unique among siblings — dept.id is perfect because it's
                    the database primary key, guaranteed unique.
                  */
                  <tr key={dept.id}>
                    <td>{dept.id}</td>
                    <td>{dept.name}</td>
                    <td>
                      {/*
                        `dept.location ?? '—'` renders an em-dash when location is null.
                        This looks better than rendering "null" or leaving the cell blank.
                        The `??` (nullish coalescing) operator returns the right side only
                        when the left side is null or undefined (not for '' or 0).
                      */}
                      {dept.location ?? '—'}
                    </td>
                    <td>{dept.employeeCount}</td>
                    <td className="actionCell">
                      <button
                        className="btnEdit"
                        onClick={() => openEditModal(dept)}
                      >
                        Edit
                      </button>
                      <button
                        className="btnDelete"
                        onClick={() => handleDeleteDepartment(dept)}
                      >
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
      {/*
        The Modal component handles the overlay and ESC key.
        DepartmentForm renders the actual inputs.
        They're kept separate so Modal is reusable for any content, not just forms.
      */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={modalMode === 'create' ? 'Add Department' : 'Edit Department'}
      >
        <DepartmentForm
          mode={modalMode}
          department={selectedDepartment ?? undefined}
          onClose={closeModal}
          onSuccess={fetchDepartments} // On save, re-fetch the whole list
        />
      </Modal>

    </div>
  );
}

export default DepartmentsPage;
