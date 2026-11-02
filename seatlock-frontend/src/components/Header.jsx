import React from 'react';

export default function Header() {
  return (
    <header className="header">
      <div className="header-inner">
        <h1 className="header-title">
          <span className="header-icon">🚂</span> SeatLock
        </h1>
        <p className="header-subtitle">Concurrent Train Seat Booking System</p>
      </div>
    </header>
  );
}
