"use client";

import { useState } from "react";
import { Truck, Trash2, MapPin } from "lucide-react";
import { formatDate, formatCurrency } from "@/lib/utils";
import type { Load } from "@/types";
import { useLogisticsActions } from "@/components/LogisticsSyncProvider";
import { EditLoadStateModal } from "@/components/EditLoadStateModal";

interface LoadCardProps {
  load: Load;
  companyName?: string;
}

export function LoadCard({ load, companyName }: LoadCardProps) {
  const { deleteLoad } = useLogisticsActions();
  const [showEditState, setShowEditState] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await deleteLoad(load.id);
      setShowConfirm(false);
    } catch (e) {
      console.error(e);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4 sm:p-5 hover:border-slate-600/50 transition-colors">
      <div className="flex justify-between items-start gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <div className="p-2 rounded-xl bg-emerald-500/20 shrink-0">
            <Truck className="w-5 h-5 text-emerald-400" />
          </div>
          <div className="min-w-0">
            <p className="font-medium text-slate-200 truncate">
              {formatDate(load.date)}
            </p>
            {companyName && (
              <p className="text-xs text-slate-500 truncate">{companyName}</p>
            )}
          </div>
        </div>
        <button
          onClick={() => setShowConfirm(true)}
          className="p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
          aria-label="Удалить"
          disabled={deleting}
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {(load.originState || load.destinationState || load.state) ? (
        <button
          onClick={() => setShowEditState(true)}
          className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500 hover:text-slate-300 transition-colors"
        >
          <MapPin className="w-3.5 h-3.5 shrink-0" />
          {load.originState && load.destinationState ? (
            <>
              <span>{load.originState} → {load.destinationState}</span>
              {load.miles != null && load.miles > 0 && (
                <span className="text-slate-600">
                  · ${(load.gross / load.miles).toFixed(2)}/mi
                </span>
              )}
            </>
          ) : load.state ? (
            load.state
          ) : (
            "—"
          )}
        </button>
      ) : (
        <button
          onClick={() => setShowEditState(true)}
          className="mt-2 text-xs text-slate-500 hover:text-sky-400 transition-colors"
        >
          + Add route
        </button>
      )}
      {showEditState && (
        <EditLoadStateModal load={load} onClose={() => setShowEditState(false)} />
      )}

      {showConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-slate-800 border border-slate-700 p-5">
            <p className="text-slate-200 font-medium mb-1">Удалить рейс?</p>
            <p className="text-slate-500 text-sm mb-4">{formatDate(load.date)} — {formatCurrency(load.gross)}</p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowConfirm(false)}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl border border-slate-600 text-slate-300 hover:bg-slate-700/50 transition-colors"
              >
                Отмена
              </button>
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl bg-red-500 hover:bg-red-600 disabled:opacity-60 text-white font-medium transition-colors"
              >
                {deleting ? "..." : "Удалить"}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="mt-4 grid grid-cols-3 gap-3">
        <div className="rounded-xl bg-slate-700/30 p-2.5">
          <p className="text-[10px] sm:text-xs text-slate-500 uppercase tracking-wider">
            Gross
          </p>
          <p className="text-sm sm:text-base font-semibold text-emerald-400">
            {formatCurrency(load.gross)}
          </p>
        </div>
        <div className="rounded-xl bg-slate-700/30 p-2.5">
          <p className="text-[10px] sm:text-xs text-slate-500 uppercase tracking-wider">
            Profit
          </p>
          <p className="text-sm sm:text-base font-semibold text-sky-400">
            {formatCurrency(load.profit)}
          </p>
        </div>
        <div className="rounded-xl bg-slate-700/30 p-2.5">
          <p className="text-[10px] sm:text-xs text-slate-500 uppercase tracking-wider">
            Diesel
          </p>
          <p className="text-sm sm:text-base font-semibold text-amber-400">
            {formatCurrency(load.diesel)}
          </p>
        </div>
      </div>
    </div>
  );
}
