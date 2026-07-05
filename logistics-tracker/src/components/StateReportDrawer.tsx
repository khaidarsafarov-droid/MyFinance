"use client";

import { X } from "lucide-react";
import { formatCurrency } from "@/lib/utils";
import type { StateReport } from "@/lib/stateUtils";

interface StateReportDrawerProps {
  report: StateReport;
  onClose: () => void;
}

export function StateReportDrawer({ report, onClose }: StateReportDrawerProps) {
  const isEmpty = report.trips === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <button
        onClick={onClose}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        aria-label="Close"
      />
      <div className="relative w-full max-w-lg rounded-t-2xl bg-slate-800 border-t border-slate-700 shadow-xl">
        <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
          <h3 className="font-semibold text-slate-100">
            {report.stateName} ({report.stateCode})
          </h3>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-4 pb-8">
          {isEmpty ? (
            <p className="py-8 text-center text-slate-500">
              No loads recorded in this state yet.
            </p>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-xl bg-slate-700/30 p-3">
                <p className="text-slate-500 text-xs uppercase tracking-wider">Gross</p>
                <p className="text-emerald-400 font-semibold">{formatCurrency(report.gross)}</p>
              </div>
              <div className="rounded-xl bg-slate-700/30 p-3">
                <p className="text-slate-500 text-xs uppercase tracking-wider">Profit</p>
                <p className="text-sky-400 font-semibold">{formatCurrency(report.profit)}</p>
              </div>
              <div className="rounded-xl bg-slate-700/30 p-3">
                <p className="text-slate-500 text-xs uppercase tracking-wider">Diesel</p>
                <p className="text-amber-400 font-semibold">{formatCurrency(report.diesel)}</p>
              </div>
              <div className="rounded-xl bg-slate-700/30 p-3">
                <p className="text-slate-500 text-xs uppercase tracking-wider">Trips</p>
                <p className="text-slate-200 font-semibold">{report.trips}</p>
              </div>
              <div className="rounded-xl bg-slate-700/30 p-3 col-span-2">
                <p className="text-slate-500 text-xs uppercase tracking-wider">Avg rate</p>
                <p className="text-slate-200 font-semibold">{formatCurrency(report.avgRate)}</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
