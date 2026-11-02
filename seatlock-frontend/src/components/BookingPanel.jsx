import React, { useState } from 'react';
import CountdownTimer from './CountdownTimer.jsx';

/**
 * Right-hand booking panel showing selection, lock controls, and confirmation form.
 *
 * Props:
 *  - seats            all seat objects (for label lookup)
 *  - selectedSeatIds
 *  - lockedSeatIds
 *  - phase            'select' | 'locked' | 'confirmed'
 *  - expiresAt
 *  - busy
 *  - onLock
 *  - onConfirm        (passengerName) => void
 *  - onCancel
 *  - onExpiry
 */
export default function BookingPanel({
  seats,
  selectedSeatIds,
  lockedSeatIds,
  phase,
  expiresAt,
  busy,
  onLock,
  onConfirm,
  onCancel,
  onExpiry,
}) {
  const [passengerName, setPassengerName] = useState('');

  const seatLabel = (id) => {
    const s = seats.find((s) => s.id === id);
    return s ? s.seatNumber : id;
  };

  return (
    <aside className="booking-panel">
      <h2 className="panel-title">🎫 Booking Panel</h2>

      {/* ——— SELECT phase ——— */}
      {phase === 'select' && (
        <>
          <div className="panel-section">
            <h3>Selected Seats</h3>
            {selectedSeatIds.length === 0 ? (
              <p className="panel-hint">Click on available seats to select them.</p>
            ) : (
              <div className="chip-list">
                {selectedSeatIds.map((id) => (
                  <span key={id} className="chip chip--blue">
                    {seatLabel(id)}
                  </span>
                ))}
              </div>
            )}
          </div>

          <button
            className="btn btn--primary"
            disabled={selectedSeatIds.length === 0 || busy}
            onClick={onLock}
          >
            {busy ? 'Locking…' : 'Proceed to Checkout'}
          </button>
        </>
      )}

      {/* ——— LOCKED phase ——— */}
      {phase === 'locked' && (
        <>
          <div className="panel-section">
            <h3>Locked Seats</h3>
            <div className="chip-list">
              {lockedSeatIds.map((id) => (
                <span key={id} className="chip chip--orange">
                  🔒 {seatLabel(id)}
                </span>
              ))}
            </div>
          </div>

          {expiresAt && (
            <CountdownTimer expiresAt={expiresAt} onExpiry={onExpiry} />
          )}

          <div className="panel-section">
            <label className="label" htmlFor="passengerName">
              Passenger Name
            </label>
            <input
              id="passengerName"
              className="input"
              type="text"
              placeholder="e.g. John Doe"
              value={passengerName}
              onChange={(e) => setPassengerName(e.target.value)}
              autoFocus
            />
          </div>

          <div className="btn-group">
            <button
              className="btn btn--success"
              disabled={busy}
              onClick={() => {
                onConfirm(passengerName);
                setPassengerName('');
              }}
            >
              {busy ? 'Confirming…' : '✓ Confirm Booking'}
            </button>
            <button
              className="btn btn--danger"
              disabled={busy}
              onClick={onCancel}
            >
              ✗ Cancel
            </button>
          </div>
        </>
      )}

      {/* ——— CONFIRMED phase ——— */}
      {phase === 'confirmed' && (
        <div className="panel-section panel-success">
          <span className="success-icon">✅</span>
          <p>Booking confirmed! Preparing for next booking…</p>
        </div>
      )}
    </aside>
  );
}
