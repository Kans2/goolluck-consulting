import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

/**
 * Fetch all seats with their current status.
 * @returns {Promise<Array<{id: number, seatNumber: string, status: string}>>}
 */
export const fetchSeats = async () => {
  const { data } = await api.get('/seats');
  return data;
};

/**
 * Lock selected seats for a session.
 * @param {number[]} seatIds
 * @param {string} sessionId  UUID-v4
 * @returns {Promise<{sessionId: string, lockedSeats: number[], expiresAt: string, ttlSeconds: number}>}
 */
export const lockSeats = async (seatIds, sessionId) => {
  const { data } = await api.post('/seats/lock', { seatIds, sessionId });
  return data;
};

/**
 * Confirm booking for a locked session.
 * @param {string} sessionId
 * @param {string} passengerName
 * @returns {Promise<{bookingIds: number[], confirmedSeats: number[], passengerName: string}>}
 */
export const confirmBooking = async (sessionId, passengerName) => {
  const { data } = await api.post('/seats/confirm', { sessionId, passengerName });
  return data;
};

/**
 * Cancel / release a lock.
 * @param {string} sessionId
 */
export const cancelLock = async (sessionId) => {
  await api.delete(`/seats/lock/${sessionId}`);
};

/**
 * Fetch booking history.
 * @returns {Promise<Array<{id: number, seatNumber: string, passengerName: string, bookedAt: string}>>}
 */
export const fetchBookings = async () => {
  const { data } = await api.get('/bookings');
  return data;
};
