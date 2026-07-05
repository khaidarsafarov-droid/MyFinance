"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { DollarSign, TrendingUp, Fuel, Loader2 } from "lucide-react";
import { BottomNav } from "@/components/BottomNav";
import { useStore } from "@/store/useStore";
import { useStoreHydration } from "@/hooks/useHydration";
import { useLogisticsSync } from "@/components/LogisticsSyncProvider";
import { formatCurrency } from "@/lib/utils";
import { UsaAnalyticsMap } from "@/components/UsaAnalyticsMap";

export default function AnalyticsPage() {
  const hydrated = useStoreHydration();
  const sync = useLogisticsSync();
  const isDataReady = sync?.isDataReady ?? true;
  const loads = useStore((s) => s.loads);

  const totals = hydrated
    ? loads.reduce(
        (acc, l) => ({
          gross: acc.gross + l.gross,
          profit: acc.profit + l.profit,
          diesel: acc.diesel + l.diesel,
        }),
        { gross: 0, profit: 0, diesel: 0 }
      )
    : { gross: 0, profit: 0, diesel: 0 };

  const chartData = hydrated
    ? [...loads]
        .sort(
          (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
        )
        .map((l) => ({
          date: new Date(l.date).toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
          }),
          gross: l.gross,
          profit: l.profit,
          diesel: l.diesel,
        }))
    : [];

  if (!hydrated) return null;

  return (
    <div className="min-h-screen pb-24">
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-700/50">
        <div className="max-w-lg mx-auto px-4 py-4">
          <h1 className="text-xl font-bold text-slate-50">Analytics</h1>
          <p className="text-sm text-slate-400 mt-0.5">Financial overview</p>
        </div>
      </header>

      <main className="max-w-lg mx-auto px-4 py-6 space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-1.5 rounded-lg bg-emerald-500/20">
                <DollarSign className="w-4 h-4 text-emerald-400" />
              </div>
              <span className="text-xs text-slate-500 uppercase tracking-wider">
                Gross
              </span>
            </div>
            <p className="text-lg sm:text-xl font-bold text-emerald-400">
              {formatCurrency(totals.gross)}
            </p>
          </div>
          <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-1.5 rounded-lg bg-sky-500/20">
                <TrendingUp className="w-4 h-4 text-sky-400" />
              </div>
              <span className="text-xs text-slate-500 uppercase tracking-wider">
                Profit
              </span>
            </div>
            <p className="text-lg sm:text-xl font-bold text-sky-400">
              {formatCurrency(totals.profit)}
            </p>
          </div>
          <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-1.5 rounded-lg bg-amber-500/20">
                <Fuel className="w-4 h-4 text-amber-400" />
              </div>
              <span className="text-xs text-slate-500 uppercase tracking-wider">
                Diesel
              </span>
            </div>
            <p className="text-lg sm:text-xl font-bold text-amber-400">
              {formatCurrency(totals.diesel)}
            </p>
          </div>
        </div>

        <div className="space-y-4">
          <h3 className="text-sm font-medium text-slate-400">Map by State</h3>
          <UsaAnalyticsMap />
        </div>

        <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4 overflow-hidden">
          <h3 className="text-sm font-medium text-slate-400 mb-4">
            Dynamics Over Time
          </h3>
          {chartData.length > 0 ? (
            <div className="h-64 sm:h-80 -mx-2">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#334155"
                    opacity={0.5}
                  />
                  <XAxis
                    dataKey="date"
                    stroke="#94a3b8"
                    fontSize={12}
                    tickLine={false}
                  />
                  <YAxis
                    stroke="#94a3b8"
                    fontSize={12}
                    tickLine={false}
                    tickFormatter={(v) => `$${v}`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1e293b",
                      border: "1px solid #334155",
                      borderRadius: "12px",
                    }}
                    labelStyle={{ color: "#94a3b8" }}
                    formatter={(value: number, name: string) => [
                      formatCurrency(value),
                      name,
                    ]}
                    labelFormatter={(label) => `Date: ${label}`}
                  />
                  <Legend
                    wrapperStyle={{ fontSize: "12px" }}
                    formatter={(value) => (
                      <span className="text-slate-300">{value}</span>
                    )}
                  />
                  <Line
                    type="monotone"
                    dataKey="gross"
                    stroke="#34d399"
                    strokeWidth={2}
                    dot={{ fill: "#34d399", r: 4 }}
                    name="Gross"
                  />
                  <Line
                    type="monotone"
                    dataKey="profit"
                    stroke="#38bdf8"
                    strokeWidth={2}
                    dot={{ fill: "#38bdf8", r: 4 }}
                    name="Profit"
                  />
                  <Line
                    type="monotone"
                    dataKey="diesel"
                    stroke="#fbbf24"
                    strokeWidth={2}
                    dot={{ fill: "#fbbf24", r: 4 }}
                    name="Diesel"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-48 flex items-center justify-center text-slate-500 text-sm">
              No data yet. Add loads to see the chart.
            </div>
          )}
        </div>
      </main>

      <BottomNav />
    </div>
  );
}
