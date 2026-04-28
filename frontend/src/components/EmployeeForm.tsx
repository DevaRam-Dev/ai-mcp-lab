/**
 * EmployeeForm.tsx — Create and Edit form for an Employee
 *
 * WHY THIS FILE EXISTS:
 * Same dual-mode pattern as DepartmentForm — `mode` prop controls whether we
 * POST (create) or PUT (update). More complex than DepartmentForm because:
 *   - Four fields instead of two
 *   - departmentId is a <select> dropdown populated from an external departments list
 *   - Email needs regex-based format validation
 *   - Salary must be a positive number (parsed from string)
 *
 * WHY `departments` IS A PROP AND NOT FETCHED HERE:
 * EmployeesPage already fetches departments for the filter dropdown.
 * Passing that list down as a prop avoids a second, redundant API call.
 * This is the "lift state up" React pattern — put shared data as high as needed,
 * then pass it down to children that need it.
 *
 * DEPENDS ON: api/employees.ts, types/Employee.ts, types/Department.ts, React (useState)
 * DEPENDED ON BY: EmployeesPage.tsx (renders this inside a <Modal>)
 */

import { useState } from 'react';
import { createEmployee, updateEmployee } from '../api/employees';
import type { Employee, EmployeeRequest } from '../types/Employee';
import type { Department } from '../types/Department';

// ---- Prop Types ----

interface EmployeeFormProps {
  mode: 'create' | 'edit';
  employee?: Employee;         // Present in 'edit' mode; prefills all four fields
  departments: Department[];   // Passed from EmployeesPage; populates the dropdown
  onClose: () => void;
  onSuccess: () => void;
}

// ---- Component ----

function EmployeeForm({ mode, employee, departments, onClose, onSuccess }: EmployeeFormProps) {

  // ---- Form Field State ----

  const [name, setName] = useState<string>(employee?.name ?? '');
  const [email, setEmail] = useState<string>(employee?.email ?? '');

  /**
   * departmentId is stored as a string because HTML <select> always gives us a string
   * in e.target.value — even if the option value is the number 1, e.target.value is "1".
   * We convert to number only when building the API payload (parseInt).
   */
  const [departmentId, setDepartmentId] = useState<string>(
    employee?.departmentId?.toString() ?? '' // '' means "no selection" in the dropdown
  );

  /**
   * salary is also stored as a string for the same reason — <input type="number">
   * still gives e.target.value as a string. We parseFloat before the API call.
   */
  const [salary, setSalary] = useState<string>(
    employee?.salary?.toString() ?? ''
  );

  // ---- Validation and Submit State ----

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // ---- Validation ----

  /**
   * validate() runs four checks before allowing the form to submit.
   * Returns true = form is valid, false = has at least one error.
   *
   * EMAIL REGEX EXPLANATION:
   *   /^[^\s@]+@[^\s@]+\.[^\s@]+$/
   *   ^           = start of string
   *   [^\s@]+     = one or more chars that are NOT whitespace or @  (the local part)
   *   @           = literal @ symbol
   *   [^\s@]+     = one or more chars (the domain name)
   *   \.          = literal dot
   *   [^\s@]+     = one or more chars (the TLD, e.g. "com")
   *   $           = end of string
   *
   * This is intentionally minimal — it catches "missing @" and "missing domain"
   * but not all invalid emails. The backend's @Email annotation is the authoritative check.
   */
  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!name.trim()) {
      errors.name = 'Name is required';
    }

    if (!email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Must be a valid email address';
    }

    if (!departmentId) {
      errors.departmentId = 'Please select a department';
    }

    const salaryNum = parseFloat(salary);
    if (!salary) {
      errors.salary = 'Salary is required';
    } else if (isNaN(salaryNum) || salaryNum <= 0) {
      // isNaN checks for non-numeric input; <= 0 catches negative numbers and zero
      errors.salary = 'Salary must be a positive number';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // ---- Submit Handler ----

  /**
   * handleSubmit validates, builds the payload, and calls the appropriate API.
   *
   * @param e - React form submit event (needed to call e.preventDefault())
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError('');

    /**
     * Build the typed EmployeeRequest payload.
     * We parse departmentId and salary from strings to numbers here because
     * HTML inputs always yield strings, but the API expects numbers.
     *
     * parseInt(value, 10): the second argument (radix 10) forces decimal parsing.
     * Without it, values starting with '0' could be interpreted as octal in older
     * JavaScript environments — a subtle bug.
     */
    const payload: EmployeeRequest = {
      name: name.trim(),
      email: email.trim(),
      departmentId: parseInt(departmentId, 10),
      salary: parseFloat(salary),
    };

    try {
      if (mode === 'create') {
        await createEmployee(payload);
      } else {
        await updateEmployee(employee!.id, payload);
      }

      onSuccess(); // Trigger parent to re-fetch the employee table
      onClose();   // Close the modal
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      setSubmitError(
        axiosError?.response?.data?.message ?? 'An unexpected error occurred. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // ---- Render ----

  return (
    <form onSubmit={handleSubmit} noValidate>

      {/* Server-side error banner (e.g., duplicate email 409, invalid dept 409) */}
      {submitError && (
        <div className="errorBanner" role="alert">
          {submitError}
        </div>
      )}

      {/* ---- Name Field ---- */}
      <div className="formGroup">
        <label htmlFor="empName">
          Full Name <span className="requiredStar">*</span>
        </label>
        <input
          id="empName"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={fieldErrors.name ? 'inputError' : ''}
          placeholder="e.g. Ravi Kumar"
          autoFocus
        />
        {fieldErrors.name && (
          <span className="fieldError" role="alert">{fieldErrors.name}</span>
        )}
      </div>

      {/* ---- Email Field ---- */}
      <div className="formGroup">
        <label htmlFor="empEmail">
          Email <span className="requiredStar">*</span>
        </label>
        <input
          id="empEmail"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={fieldErrors.email ? 'inputError' : ''}
          placeholder="e.g. ravi.kumar@mcplab.com"
        />
        {fieldErrors.email && (
          <span className="fieldError" role="alert">{fieldErrors.email}</span>
        )}
      </div>

      {/* ---- Department Dropdown ---- */}
      <div className="formGroup">
        <label htmlFor="empDept">
          Department <span className="requiredStar">*</span>
        </label>
        <select
          id="empDept"
          value={departmentId}
          onChange={(e) => setDepartmentId(e.target.value)}
          className={fieldErrors.departmentId ? 'inputError' : ''}
        >
          {/*
            The blank "placeholder" option forces the user to make an active choice.
            Without it, the first real department would be pre-selected silently, and
            the user might submit the form without realising they picked the wrong dept.
            Setting value="" ensures our validation catches "no selection" (empty string is falsy).
          */}
          <option value="">-- Select Department --</option>
          {departments.map((dept) => (
            /*
              `key` is required by React for lists — it lets React identify which item
              changed when the list updates, enabling efficient DOM reconciliation.
              Always use a stable, unique identifier (here: dept.id) not the array index.
            */
            <option key={dept.id} value={dept.id.toString()}>
              {dept.name}
            </option>
          ))}
        </select>
        {fieldErrors.departmentId && (
          <span className="fieldError" role="alert">{fieldErrors.departmentId}</span>
        )}
      </div>

      {/* ---- Salary Field ---- */}
      <div className="formGroup">
        <label htmlFor="empSalary">
          Salary (₹) <span className="requiredStar">*</span>
        </label>
        <input
          id="empSalary"
          type="number"
          min="1"
          step="1000"   // The up/down spinner arrows jump in ₹1000 increments
          value={salary}
          onChange={(e) => setSalary(e.target.value)}
          className={fieldErrors.salary ? 'inputError' : ''}
          placeholder="e.g. 85000"
        />
        {fieldErrors.salary && (
          <span className="fieldError" role="alert">{fieldErrors.salary}</span>
        )}
      </div>

      {/* ---- Form Actions ---- */}
      <div className="formActions">
        <button
          type="button"
          onClick={onClose}
          className="btnSecondary"
          disabled={isSubmitting}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="btnPrimary"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Saving…' : mode === 'create' ? 'Add Employee' : 'Save Changes'}
        </button>
      </div>

    </form>
  );
}

export default EmployeeForm;
