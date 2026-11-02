import { useState, useCallback } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { lockSeats, confirmBooking, cancelLock } from '../api/seatApi';

const SESSION_KEY = 'seatlock_session_id';

function getOrCreateSessionId() {
  let id = localStorage.getItem(SESSION_KEY);
  if (!id) {
    id = uuidv4();
    localStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

function newSessionId() {
  const id = uuidv4();
  localStorage.setItem(SESSION_KEY, id);
  return id;
}

/**
 * Custom hook encapsulating the booking flow:
 *   select → lock → confirm / cancel.
 */
export function useBooking({ onToast, refreshSeats, onBookingConfirmed }) {
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [lockedSeatIds, setLockedSeatIds] = useState([]);
  const [sessionId, setSessionId] = useState(getOrCreateSessionId);
  const [expiresAt, setExpiresAt] = useState(null);
  const [phase, setPhase] = useState('select'); // select | locked | confirmed
  const [busy, setBusy] = useState(false);

  /** Toggle a seat in the selection list (only during select phase). */
  const toggleSeat = useCallback(
    (seatId) => {
      if (phase !== 'select') return;
      setSelectedSeatIds((prev) =>
        prev.includes(seatId)
          ? prev.filter((id) => id !== seatId)
          : [...prev, seatId],
      );
    },
    [phase],
  );

  /** Lock the selected seats. */
  const handleLock = useCallback(async () => {
    if (selectedSeatIds.length === 0) {
      onToast('Please select at least one seat.', 'warning');
      return;
    }
    setBusy(true);
    try {
      const res = await lockSeats(selectedSeatIds, sessionId);
      const lockedIds = res.lockedSeatIds || res.lockedSeats || [];
      setLockedSeatIds(lockedIds);
      setExpiresAt(res.expiresAt);
      setPhase('locked');
      onToast(`Locked ${lockedIds.length} seat(s). Complete checkout within 2 minutes!`, 'success');
    } catch (err) {
      const data = err.response?.data;
      if (data?.unavailableSeatIds || err.response?.status === 409) {
        onToast(`Some seats are no longer available: ${(data.unavailableSeatIds || []).join(', ')}`, 'error');
      } else {
        onToast(data?.message || 'Failed to lock seats.', 'error');
      }
    } finally {
      setBusy(false);
      refreshSeats();
    }
  }, [selectedSeatIds, sessionId, onToast, refreshSeats]);

  /** Confirm the booking. */
  const handleConfirm = useCallback(
    async (passengerName) => {
      if (!passengerName.trim()) {
        onToast('Please enter a passenger name.', 'warning');
        return;
      }
      setBusy(true);
      try {
        const res = await confirmBooking(sessionId, passengerName.trim());
        setPhase('confirmed');
        onToast(
          `Booking confirmed! Seats: ${(res.bookedSeatIds || res.confirmedSeats || []).join(', ')} for ${res.passengerName}`,
          'success',
        );
        onBookingConfirmed?.();
        // Prepare a fresh session for next booking
        setTimeout(() => {
          resetFlow();
        }, 2000);
      } catch (err) {
        const data = err.response?.data;
        if (data?.error === 'Gone' || err.response?.status === 410) {
          onToast('Your reservation has expired. Please try again.', 'error');
          resetFlow();
        } else {
          onToast(data?.message || 'Failed to confirm booking.', 'error');
        }
      } finally {
        setBusy(false);
        refreshSeats();
        onBookingConfirmed?.();
      }
    },
    [sessionId, onToast, refreshSeats],
  );

  /** Cancel / release lock. */
  const handleCancel = useCallback(async () => {
    setBusy(true);
    try {
      await cancelLock(sessionId);
      onToast('Reservation cancelled.', 'info');
    } catch {
      // even if cancel call fails, reset local state
    }
    resetFlow();
    setBusy(false);
    refreshSeats();
  }, [sessionId, onToast, refreshSeats]);

  /** Called when countdown expires. */
  const handleExpiry = useCallback(async () => {
    onToast('⏰ Your reservation has expired!', 'error');
    try {
      await cancelLock(sessionId);
    } catch {
      // even if cancel fails, proceed with reset
    }
    resetFlow();
    refreshSeats();
  }, [sessionId, onToast, refreshSeats]);

  function resetFlow() {
    setSelectedSeatIds([]);
    setLockedSeatIds([]);
    setExpiresAt(null);
    setPhase('select');
    const id = newSessionId();
    setSessionId(id);
  }

  return {
    selectedSeatIds,
    lockedSeatIds,
    sessionId,
    expiresAt,
    phase,
    busy,
    toggleSeat,
    handleLock,
    handleConfirm,
    handleCancel,
    handleExpiry,
  };
}
