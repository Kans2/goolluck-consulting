import React, { useState, useEffect } from 'react';
import { fetchBookings } from '../api/seatApi';

/**
 * Table of all confirmed bookings. Auto-refreshes every 10 seconds.
 */
export default function BookingHistory({ refreshTrigger }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const data = await fetchBookings();
      setBookings(data);
    } catch {
      // silently ignore — bookings section is supplementary
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [refreshTrigger]);

  useEffect(() => {
    load();
    const id = setInterval(load, 3000);
    return () => clearInterval(id);
  }, []);

  return (
    <section className="booking-history">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
        <h2 className="section-title" style={{ margin: 0 }}>📋 Booking History</h2>
        <button
          onClick={load}
          style={{
            background: 'none',
            border: '1px solid var(--clr-border)',
            borderRadius: 'var(--radius)',
            padding: '0.35rem 0.75rem',
            cursor: 'pointer',
            fontSize: '0.8rem',
            color: 'var(--clr-text-muted)',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
          title="Refresh booking list"
        >
          ↻ Refresh
        </button>
      </div>

      {loading ? (
        <p className="loading-text">Loading bookings…</p>
      ) : bookings.length === 0 ? (
        <p className="empty-text">No bookings yet. Be the first!</p>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Seat</th>
                <th>Passenger</th>
                <th>Booked At</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td>
                    <span className="chip chip--small">{b.seatNumber}</span>
                  </td>
                  <td>{b.passengerName}</td>
                  <td>{new Date(b.bookedAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
