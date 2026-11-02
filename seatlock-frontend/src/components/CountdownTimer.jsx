import React, { useState, useEffect, useRef } from 'react';

/**
 * Countdown timer that counts down from expiresAt to now.
 *
 * Props:
 *  - expiresAt  ISO date string
 *  - onExpiry   callback when timer reaches zero
 */
export default function CountdownTimer({ expiresAt, onExpiry }) {
  const [remaining, setRemaining] = useState(() => calcRemaining(expiresAt));
  const expiredRef = useRef(false);

  useEffect(() => {
    expiredRef.current = false;
    setRemaining(calcRemaining(expiresAt));

    const id = setInterval(() => {
      const r = calcRemaining(expiresAt);
      setRemaining(r);
      if (r <= 0 && !expiredRef.current) {
        expiredRef.current = true;
        clearInterval(id);
        onExpiry();
      }
    }, 1000);

    return () => clearInterval(id);
  }, [expiresAt, onExpiry]);

  const mins = Math.floor(Math.max(remaining, 0) / 60);
  const secs = Math.max(remaining, 0) % 60;
  const isUrgent = remaining <= 30 && remaining > 0;

  return (
    <div className={`countdown ${isUrgent ? 'countdown--urgent' : ''}`}>
      <span className="countdown-icon">⏳</span>
      <span className="countdown-time">
        {String(mins).padStart(2, '0')}:{String(secs).padStart(2, '0')}
      </span>
      {isUrgent && <span className="countdown-warn">Hurry up!</span>}
    </div>
  );
}

function calcRemaining(expiresAt) {
  return Math.round((new Date(expiresAt).getTime() - Date.now()) / 1000);
}
