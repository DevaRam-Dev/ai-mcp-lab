/**
 * DepartmentForm.tsx — Create and Edit form for a Department
 *
 * WHY THIS FILE EXISTS:
 * A single component handles both "add new department" and "edit existing department".
 * The `mode` prop switches behaviour:
 *   - 'create' → blank form, calls POST /api/departments on submit
 *   - 'edit'   → form prefilled from the `department` prop, calls PUT on submit
 *
 * Reusing one component for both modes avoids duplicating form markup and validation
 * logic. This is the "DRY" principle — Don't Repeat Yourself.
 *
 * VALIDATION LAYERS:
 *   1. Client-side (this file): instant feedback, no network round-trip
 *   2. Server-side (Spring Boot): authoritative — catches anything the client misses
 * Both layers are necessary. Never trust only the client.
 *
 * DEPENDS ON: api/departments.ts, types/Department.ts, React (useState)
 * DEPENDED ON BY: DepartmentsPage.tsx (renders this inside a <Modal>)
 */

import { useState } from 'react';
import { createDepartment, updateDepartment } from '../api/departments';
import type { Department, DepartmentRequest } from '../types/Department';

// ---- Prop Types ----

interface DepartmentFormProps {
  mode: 'create' | 'edit';    // Determines which API call to make on submit
  department?: Department;     // Present in 'edit' mode; prefills the fields
  onClose: () => void;         // Called when user cancels or after a successful save
  onSuccess: () => void;       // Called after save so the parent can refresh its list
}

// ---- Component ----

function DepartmentForm({ mode, department, onClose, onSuccess }: DepartmentFormProps) {

  // ---- Form Field State ----

  /**
   * Each input field is a controlled component — its value is stored in state and
   * updated via onChange. This gives us full control over the value for validation.
   *
   * Initial values use the `??` (nullish coalescing) operator:
   *   department?.name ?? ''  means:
   *     - In edit mode:   department exists, so use department.name
   *     - In create mode: department is undefined, so use '' (empty string)
   * The `?.` is optional chaining — it safely accesses .name even if department is undefined,
   * returning undefined instead of throwing a TypeError.
   */
  const [name, setName] = useState<string>(department?.name ?? '');
  const [location, setLocation] = useState<string>(department?.location ?? '');

  // ---- Validation and Submit State ----

  /**
   * fieldErrors maps each field name to its error message.
   * e.g., { name: "Department name is required" }
   * Using a Record<string, string> lets us add/clear individual field errors.
   */
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  /**
   * submitError holds server-side error messages from a failed API call.
   * Kept separate from fieldErrors because server errors (e.g., "duplicate name")
   * relate to the whole form, not a specific field.
   */
  const [submitError, setSubmitError] = useState<string>('');

  /**
   * isSubmitting is true during the API call. It disables the submit button to
   * prevent double-submission if the user clicks quickly while the request is in flight.
   */
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // ---- Validation ----

  /**
   * validate() checks all fields before the API call.
   * Returns true if valid (no errors), false if any field fails.
   * As a side effect it updates fieldErrors so the UI shows the right messages.
   *
   * This runs synchronously on every submit — no async needed.
   */
  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!name.trim()) {
      // `.trim()` removes leading/trailing whitespace — "   " is treated as empty
      errors.name = 'Department name is required';
    }

    setFieldErrors(errors);
    // Object.keys().length === 0 means no error keys were added → form is valid
    return Object.keys(errors).length === 0;
  };

  // ---- Submit Handler ----

  /**
   * handleSubmit is attached to the <form onSubmit> event.
   *
   * Flow:
   *  1. Prevent the browser's default form-submit behaviour (would cause page reload)
   *  2. Run client-side validation; bail out immediately if any field is invalid
   *  3. Build the request payload
   *  4. Call create or update depending on the `mode` prop
   *  5. On success: tell the parent list to refresh, then close the modal
   *  6. On error: show the server's error message in the form
   *
   * `async/await` makes asynchronous code readable as if it were synchronous —
   * equivalent to chaining .then()/.catch() but much easier to follow.
   *
   * @param e - The React form submission event
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // Stop the browser from reloading the page on form submit

    if (!validate()) return; // Bail out early if client-side validation fails

    setIsSubmitting(true);
    setSubmitError(''); // Clear any previous server error before trying again

    /**
     * Build the API payload. We send `null` for location (not an empty string) when
     * the field is blank — this tells the backend to store NULL in the database.
     * The backend accepts null as "no location"; an empty string might fail validation.
     */
    const payload: DepartmentRequest = {
      name: name.trim(),
      location: location.trim() || null, // '' → null; any non-empty string is kept
    };

    try {
      if (mode === 'create') {
        await createDepartment(payload);
      } else {
        // The `!` non-null assertion is safe: the parent always provides `department`
        // when mode === 'edit'. Without it, TypeScript would warn that department might
        // be undefined.
        await updateDepartment(department!.id, payload);
      }

      onSuccess(); // Ask the parent (DepartmentsPage) to re-fetch the table
      onClose();   // Dismiss the modal — we're done
    } catch (error: unknown) {
      /**
       * Axios wraps all HTTP error responses in an AxiosError object.
       * The Spring Boot error body is at error.response.data, shaped like:
       *   { status: 409, message: "Department name already exists", errors: {} }
       *
       * We use optional chaining (?.) throughout because we don't know for certain
       * that `error` is an AxiosError — it could be a network error with no .response.
       * The `?? 'fallback'` at the end handles that case gracefully.
       */
      const axiosError = error as { response?: { data?: { message?: string } } };
      setSubmitError(
        axiosError?.response?.data?.message ?? 'An unexpected error occurred. Please try again.'
      );
    } finally {
      // `finally` runs whether the try succeeded or catch handled an error.
      // Always re-enable the button so the user can try again after a failure.
      setIsSubmitting(false);
    }
  };

  // ---- Render ----

  return (
    /**
     * `noValidate` on the <form> disables the browser's built-in HTML5 validation UI
     * (the popup tooltip bubbles that appear below inputs). We want our own styled
     * error messages, so we disable the browser's and handle it ourselves.
     */
    <form onSubmit={handleSubmit} noValidate>

      {/* ---- Server Error Banner ---- */}
      {/*
        Conditionally rendered only when there is a server error.
        The `&&` short-circuit: if submitError is '' (falsy), nothing is rendered.
        If submitError has content, the div is shown.
      */}
      {submitError && (
        <div className="errorBanner" role="alert">
          {submitError}
        </div>
      )}

      {/* ---- Name Field ---- */}
      <div className="formGroup">
        <label htmlFor="deptName">
          Department Name <span className="requiredStar">*</span>
          {/* The red * visually signals this field is required */}
        </label>
        <input
          id="deptName"
          type="text"
          value={name}        // Controlled input — value is always from state
          onChange={(e) => setName(e.target.value)} // Update state on every keystroke
          className={fieldErrors.name ? 'inputError' : ''} // Red border when invalid
          placeholder="e.g. Engineering"
          autoFocus // Focus this field automatically when the modal opens — saves a click
        />
        {/* Conditionally show the error message below the input */}
        {fieldErrors.name && (
          <span className="fieldError" role="alert">{fieldErrors.name}</span>
        )}
      </div>

      {/* ---- Location Field ---- */}
      <div className="formGroup">
        <label htmlFor="deptLocation">
          Location
          {/* No asterisk — this field is optional */}
        </label>
        <input
          id="deptLocation"
          type="text"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          placeholder="e.g. Hyderabad (optional)"
        />
        {/* No error message needed — location has no validation rules */}
      </div>

      {/* ---- Form Action Buttons ---- */}
      <div className="formActions">
        <button
          type="button"   // IMPORTANT: type="button" prevents this from submitting the form.
          onClick={onClose}
          className="btnSecondary"
          disabled={isSubmitting} // Disable during submit so users can't cancel mid-request
        >
          Cancel
        </button>
        <button
          type="submit"   // This is the submit trigger — clicking it fires handleSubmit
          className="btnPrimary"
          disabled={isSubmitting} // Disable during submit to prevent double-submission
        >
          {/*
            Show different label during submission so users know something is happening.
            Without this, users might think the button is broken and click again.
          */}
          {isSubmitting ? 'Saving…' : mode === 'create' ? 'Create' : 'Save Changes'}
        </button>
      </div>

    </form>
  );
}

export default DepartmentForm;
