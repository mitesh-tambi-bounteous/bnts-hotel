"use client";

import { useState, type KeyboardEvent } from "react";

/** Shows exactly the tabs the property catalogue defines for this hotel, nothing more. */
export function HotelTabs({ tabs }: { tabs: { name: string; body: string }[] }) {
  const [active, setActive] = useState(0);
  if (tabs.length === 0) return null;

  function onKeyDown(event: KeyboardEvent, index: number) {
    if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
    event.preventDefault();
    const next = (index + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
    setActive(next);
    document.getElementById(`tab-${next}`)?.focus();
  }

  return (
    <div className="stack">
      <div className="tabs" role="tablist">
        {tabs.map((tab, index) => (
          <button
            key={tab.name}
            id={`tab-${index}`}
            className="tab"
            type="button"
            role="tab"
            aria-selected={index === active}
            aria-controls="tab-panel"
            tabIndex={index === active ? 0 : -1}
            onClick={() => setActive(index)}
            onKeyDown={(event) => onKeyDown(event, index)}
          >
            {tab.name}
          </button>
        ))}
      </div>
      <div className="tab-panel" id="tab-panel" role="tabpanel" aria-labelledby={`tab-${active}`}>
        {tabs[active].body}
      </div>
    </div>
  );
}
