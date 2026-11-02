import { useState, useEffect, useCallback, useRef } from 'react';
import { fetchSeats } from '../api/seatApi';

const POLL_INTERVAL = 3000;

/**
 * Custom hook that polls seat status every 3 seconds.
 * Returns { seats, loading, error, refresh }.
 */
export function useSeats() {
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const intervalRef = useRef(null);

  const refresh = useCallback(async () => {
    try {
      const data = await fetchSeats();
      setSeats(data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load seats. Is the server running?');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    intervalRef.current = setInterval(refresh, POLL_INTERVAL);
    return () => clearInterval(intervalRef.current);
  }, [refresh]);

  return { seats, loading, error, refresh };
}
