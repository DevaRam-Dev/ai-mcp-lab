/**
 * Modal.tsx — Reusable modal dialog with backdrop
 *
 * WHY THIS FILE EXISTS:
 * Both EmployeeForm and DepartmentForm need to appear in a centred overlay dialog.
 * Rather than duplicating the backdrop, close-on-ESC, and stopPropagation logic
 * inside every form, we extract it into one reusable Modal component.
 * This is the "single responsibility" principle applied to UI — this component
 * knows how to show/hide a dialog; it knows nothing about departments or employees.
 *
 * USAGE PATTERN:
 *   <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Add Employee">
 *     <EmployeeForm ... />
 *   </Modal>
 *
 * BEHAVIOUR:
 *   - Backdrop click  → calls onClose (dismiss without saving)
 *   - ESC key         → calls onClose
 *   - Click inside box → does NOT close (stopPropagation prevents backdrop click)
 *   - isOpen = false  → renders null (no DOM nodes, no overhead)
 *
 * DEPENDS ON: React (useEffect), app.css (modal styles)
 * DEPENDED ON BY: EmployeesPage.tsx, DepartmentsPage.tsx
 */

import { useEffect } from 'react';

// ---- Prop Types ----

/**
 * Props accepted by the Modal component.
 *
 * `children` is the standard React "slot" pattern — it accepts any JSX content
 * passed between the opening and closing <Modal> tags (typically a form component).
 * `React.ReactNode` is the broadest child type: JSX, strings, numbers, null, arrays.
 */
interface ModalProps {
  isOpen: boolean;            // Controls visibility; false = not in DOM at all
  onClose: () => void;        // Called when the user wants to dismiss the modal
  title: string;              // Shown in the modal header bar
  children: React.ReactNode;  // The form (or any content) rendered inside the modal
}

// ---- Component ----

function Modal({ isOpen, onClose, title, children }: ModalProps) {

  // ---- ESC Key Listener ----

  /**
   * useEffect adds a keydown listener while the modal is open and removes it when
   * the modal closes. The dependency array [isOpen, onClose] means:
   *   - When isOpen becomes true  → effect runs → listener is added
   *   - When isOpen becomes false → cleanup runs → listener is removed
   *
   * WHY CLEANUP MATTERS: Without the cleanup `return`, every time isOpen flips from
   * false → true we'd add another listener, stacking up multiple calls to onClose
   * on a single ESC press. The cleanup function tears down exactly what we set up.
   */
  useEffect(() => {
    if (!isOpen) return; // No point adding a listener when the modal isn't visible

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose(); // Standard UX: ESC dismisses dialogs
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    // Cleanup: runs before the next effect execution, or when component unmounts
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  // ---- Early Return When Closed ----

  /**
   * Returning null removes the modal from the DOM entirely when not open.
   * This is better than CSS `display: none` because:
   *   - Screen readers don't encounter hidden content
   *   - React doesn't maintain virtual DOM nodes for invisible UI
   *   - No risk of keyboard focus accidentally landing inside a hidden modal
   */
  if (!isOpen) return null;

  // ---- Render ----

  return (
    /**
     * The outer div is the full-screen semi-transparent backdrop.
     * Clicking it directly (not inside the dialog box) triggers onClose.
     */
    <div className="modalBackdrop" onClick={onClose}>

      {/**
       * The inner div is the visible dialog box.
       * `e.stopPropagation()` stops the click from bubbling up to the backdrop.
       * Without this, clicking anywhere INSIDE the dialog would close it immediately
       * because the click would bubble up and trigger the backdrop's onClick.
       */}
      <div className="modalBox" onClick={(e) => e.stopPropagation()}>

        {/* Modal header: title on the left, close button on the right */}
        <div className="modalHeader">
          <h2 className="modalTitle">{title}</h2>
          <button
            className="modalCloseBtn"
            onClick={onClose}
            aria-label="Close modal"  // Accessibility: screen readers announce this label
          >
            ×  {/* HTML entity for multiplication sign — looks like a close × */}
          </button>
        </div>

        {/* Modal body: the form component passed as children */}
        <div className="modalBody">
          {children}
        </div>

      </div>
    </div>
  );
}

export default Modal;
