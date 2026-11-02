import React from 'react';
import Seat from './Seat.jsx';

/**
 * Renders a train-coach style seat map: 5 rows × 4 columns with an aisle.
 *
 * Expected seat numbering: 1A 1B | 1C 1D  …  5A 5B | 5C 5D
 *
 * Props:
 *  - seats           Array of seat objects
 *  - selectedSeatIds Array of selected seat ids
 *  - lockedSeatIds   Array of ids locked by this session
 *  - phase           'select' | 'locked' | 'confirmed'
 *  - onToggle        (seatId) => void
 */
export default function SeatMap({ seats, selectedSeatIds, lockedSeatIds, phase, onToggle }) {
  // Group seats into rows (4 per row)
  const rows = [];
  const sorted = [...seats].sort((a, b) => a.id - b.id);
  for (let i = 0; i < sorted.length; i += 4) {
    rows.push(sorted.slice(i, i + 4));
  }

  return (
    <div className="seat-map-wrapper">
      {/* Coach shell */}
      <div className="coach">
        <div className="coach-top">
          <span className="coach-direction">← Direction of travel</span>
        </div>

        <div className="coach-body">
          {rows.map((row, ri) => (
            <div className="seat-row" key={ri}>
              {/* Left pair */}
              <div className="seat-pair">
                {row.slice(0, 2).map((seat) => (
                  <Seat
                    key={seat.id}
                    seat={seat}
                    isSelected={selectedSeatIds.includes(seat.id)}
                    isMyLock={lockedSeatIds.includes(seat.id)}
                    onClick={() => onToggle(seat.id)}
                    disabled={phase !== 'select'}
                  />
                ))}
              </div>

              {/* Aisle */}
              <div className="aisle">
                <span className="row-number">{ri + 1}</span>
              </div>

              {/* Right pair */}
              <div className="seat-pair">
                {row.slice(2, 4).map((seat) => (
                  <Seat
                    key={seat.id}
                    seat={seat}
                    isSelected={selectedSeatIds.includes(seat.id)}
                    isMyLock={lockedSeatIds.includes(seat.id)}
                    onClick={() => onToggle(seat.id)}
                    disabled={phase !== 'select'}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="coach-bottom" />
      </div>
    </div>
  );
}
