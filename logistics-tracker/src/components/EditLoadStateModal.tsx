"use client";

import { useState } from "react";
import { X } from "lucide-react";
import type { Load } from "@/types";
import { useLogisticsActions } from "@/components/LogisticsSyncProvider";
import { STATE_NAME_TO_ABBR } from "@/lib/stateUtils";

const US_STATES = Object.entries(STATE_NAME_TO_ABBR)
  .map(([name, abbr]) => ({ name, abbr }))
  .sort((a, b) => a.name.localeCompare(b.name));

interface EditLoadStateModalProps {
  load: Load;
  onClose: () => void;
}

export function EditLoadStateModal({ load, onClose }: EditLoadStateModalProps) {
  const { updateLoad } = useLogisticsActions();
  const [state, setState] = useState(load.state ?? "");
  const [originState, setOriginState] = useState(load.originState ?? "");
  const [destinationState, setDestinationState] = useState(load.destinationState ?? "");
  const [miles, setMiles] = useState(load.miles != null ? String(load.miles) : "");

  const handleSave = async () => {
    const m = miles.trim() ? parseFloat(miles) : undefined;
    await updateLoad(load.id, {
      state: destinationState || state || undefined,
      originState: originState || undefined,
      destinationState: destinationState || undefined,
      miles: m != null && !isNaN(m) && m > 0 ? m : undefined,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        onClick={onClose}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        aria-label="Close"
      />
      <div className="relative w-full max-w-sm rounded-2xl bg-slate-800 border border-slate-700 shadow-xl p-5 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-slate-100">Edit route</h3>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-700/50"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm text-slate-400 mb-2">From (origin)</label>
            <select
              value={originState}
              onChange={(e) => setOriginState(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">— None —</option>
              {US_STATES.map(({ name, abbr }) => (
                <option key={abbr} value={abbr}>{name} ({abbr})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-slate-400 mb-2">To (destination)</label>
            <select
              value={destinationState}
              onChange={(e) => setDestinationState(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">— None —</option>
              {US_STATES.map(({ name, abbr }) => (
                <option key={abbr} value={abbr}>{name} ({abbr})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-slate-400 mb-2">Miles (for $/mi)</label>
            <input
              type="number"
              inputMode="decimal"
              step="1"
              min="0"
              value={miles}
              onChange={(e) => setMiles(e.target.value)}
              placeholder="e.g. 850"
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
        </div>

        <div className="flex gap-2 mt-6">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-slate-600 text-slate-300 hover:bg-slate-700/50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="flex-1 py-2.5 rounded-xl bg-sky-500 hover:bg-sky-600 text-white font-medium transition-colors"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  );
}
