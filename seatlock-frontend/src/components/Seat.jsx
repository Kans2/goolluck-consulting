import React from 'react';

/**
 * Single seat button.
 *
 * Props:
 *  - seat        { id, seatNumber, status }
 *  - isSelected  boolean
 *  - isMyLock    boolean  (locked by current session)
 *  - onClick     () => void
 *  - disabled    boolean
 */
export default function Seat({ seat, isSelected, isMyLock, onClick, disabled }) {
  const status = seat.status; // AVAILABLE | LOCKED | BOOKED

  let className = 'seat';
  if (status === 'BOOKED') className += ' seat--booked';
  else if (isMyLock) className += ' seat--my-lock';
  else if (status === 'LOCKED') className += ' seat--other-lock';
  else if (isSelected) className += ' seat--selected';
  else className += ' seat--available';

  const isClickable = status === 'AVAILABLE' && !disabled;

  return (
    <button
      className={className}
      onClick={isClickable ? onClick : undefined}
      disabled={!isClickable}
      title={
        status === 'BOOKED'
          ? 'Booked'
          : isMyLock
            ? 'Locked by you'
            : status === 'LOCKED'
              ? 'Locked by another user'
              : isSelected
                ? 'Click to deselect'
                : 'Click to select'
      }
      aria-label={`Seat ${seat.seatNumber} — ${status}`}
    >
      <span className="seat-label">{seat.seatNumber}</span>
      {isMyLock && <span className="seat-badge">🔒</span>}
    </button>
  );
}
