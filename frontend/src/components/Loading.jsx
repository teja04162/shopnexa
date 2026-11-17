import React from "react";

export default function Loading({ label = "Loading products..." }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <span className="loading-emblem" aria-hidden="true"><span /></span>
      <span className="loading-label">{label}</span>
      <span className="loading-track" aria-hidden="true"><span /></span>
    </div>
  );
}
