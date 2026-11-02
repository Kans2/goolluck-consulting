import React from 'react';

export default function StatusLegend() {
  const items = [
    { color: '#4CAF50', label: 'Available' },
    { color: '#2196F3', label: 'Selected' },
    { color: '#FF9800', label: 'Locked (you)' },
    { color: '#FFC107', label: 'Locked (others)' },
    { color: '#9E9E9E', label: 'Booked' },
  ];

  return (
    <div className="legend">
      {items.map((item) => (
        <div className="legend-item" key={item.label}>
          <span
            className="legend-swatch"
            style={{ backgroundColor: item.color }}
          />
          <span className="legend-label">{item.label}</span>
        </div>
      ))}
    </div>
  );
}
