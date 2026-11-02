import React, { useState, useCallback } from 'react';
import Header from './components/Header.jsx';
import SeatMap from './components/SeatMap.jsx';
import StatusLegend from './components/StatusLegend.jsx';
import BookingPanel from './components/BookingPanel.jsx';
import BookingHistory from './components/BookingHistory.jsx';
import Toast from './components/Toast.jsx';
import { useSeats } from './hooks/useSeats.js';
import { useBooking } from './hooks/useBooking.js';

export default function App() {
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type, id: Date.now() });
  }, []);

  const clearToast = useCallback(() => setToast(null), []);

  const { seats, loading, error, refresh } = useSeats();
  const [bookingRefreshKey, setBookingRefreshKey] = useState(0);

  const triggerBookingRefresh = useCallback(() => {
    setBookingRefreshKey((prev) => prev + 1);
  }, []);

  const booking = useBooking({
    onToast: showToast,
    refreshSeats: refresh,
    onBookingConfirmed: triggerBookingRefresh,
  });

  return (
    <div className="app">
      <Header />

      <Toast toast={toast} onClose={clearToast} />

      <main className="main">
        {/* Left: Seat Map */}
        <section className="main-left">
          {loading && seats.length === 0 ? (
            <div className="loading-overlay">
              <div className="spinner" />
              <p>Loading seat map…</p>
            </div>
          ) : error && seats.length === 0 ? (
            <div className="error-box">
              <p>⚠️ {error}</p>
              <button className="btn btn--primary" onClick={refresh}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <SeatMap
                seats={seats}
                selectedSeatIds={booking.selectedSeatIds}
                lockedSeatIds={booking.lockedSeatIds}
                phase={booking.phase}
                onToggle={booking.toggleSeat}
              />
              <StatusLegend />
            </>
          )}
        </section>

        {/* Right: Booking Panel */}
        <BookingPanel
          seats={seats}
          selectedSeatIds={booking.selectedSeatIds}
          lockedSeatIds={booking.lockedSeatIds}
          phase={booking.phase}
          expiresAt={booking.expiresAt}
          busy={booking.busy}
          onLock={booking.handleLock}
          onConfirm={booking.handleConfirm}
          onCancel={booking.handleCancel}
          onExpiry={booking.handleExpiry}
        />
      </main>

      <BookingHistory refreshTrigger={bookingRefreshKey} />
    </div>
  );
}
