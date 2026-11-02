import React, { useEffect, useRef } from 'react';

/**
 * Toast notification component.
 *
 * Props:
 *  - toast   { message, type, id }   or null
 *  - onClose () => void
 */
export default function Toast({ toast, onClose }) {
  const timerRef = useRef(null);

  useEffect(() => {
    if (!toast) return;
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(onClose, 4000);
    return () => clearTimeout(timerRef.current);
  }, [toast, onClose]);

  if (!toast) return null;

  return (
    <div className={`toast toast--${toast.type}`} role="alert">
      <span className="toast-message">{toast.message}</span>
      <button className="toast-close" onClick={onClose} aria-label="Close">
        ×
      </button>
    </div>
  );
}
