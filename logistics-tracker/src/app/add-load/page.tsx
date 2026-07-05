"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useStore } from "@/store/useStore";
import { useStoreHydration } from "@/hooks/useHydration";
import { useLogisticsActions } from "@/components/LogisticsSyncProvider";
import { STATE_NAME_TO_ABBR } from "@/lib/stateUtils";

const US_STATES = Object.entries(STATE_NAME_TO_ABBR)
  .map(([name, abbr]) => ({ name, abbr }))
  .sort((a, b) => a.name.localeCompare(b.name));

export default function AddLoadPage() {
  const router = useRouter();
  const hydrated = useStoreHydration();
  const { addLoad } = useLogisticsActions();
  const getCurrentCompany = useStore((s) => s.getCurrentCompany);
  const hasCompanies = useStore((s) => s.hasCompanies);

  const [date, setDate] = useState("");
  const [gross, setGross] = useState("");
  const [profit, setProfit] = useState("");
  const [diesel, setDiesel] = useState("");
  const [originState, setOriginState] = useState("");
  const [destinationState, setDestinationState] = useState("");
  const [miles, setMiles] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (hydrated) setDate(new Date().toISOString().slice(0, 10));
  }, [hydrated]);

  if (!hydrated) return null;

  if (!hasCompanies()) {
    router.push("/");
    return null;
  }

  const currentCompany = getCurrentCompany();
  if (!currentCompany) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const g = parseFloat(gross);
    const p = parseFloat(profit);
    const d = parseFloat(diesel);
    if (isNaN(g) || isNaN(p) || isNaN(d)) return;

    setError("");
    setLoading(true);
    try {
      const m = miles.trim() ? parseFloat(miles) : undefined;
      await addLoad({
        date: new Date(date).toISOString(),
        gross: g,
        profit: p,
        diesel: d,
        companyId: currentCompany.id,
        ...(originState && { originState }),
        ...(destinationState && { destinationState }),
        ...(m != null && !isNaN(m) && m > 0 && { miles: m }),
        ...(destinationState && { state: destinationState }),
      });
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ошибка сохранения");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen pb-24">
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-700/50">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center gap-3">
          <Link
            href="/"
            className="p-2 -ml-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div className="min-w-0">
            <h1 className="text-xl font-bold text-slate-50">Добавить груз</h1>
            <p className="text-sm text-slate-400 truncate">{currentCompany.name}</p>
          </div>
        </div>
      </header>

      <main className="max-w-lg mx-auto px-4 py-6">
        <form
          onSubmit={handleSubmit}
          className="rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-6 space-y-5"
        >
          <div>
            <label
              htmlFor="date"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Date
            </label>
            <input
              id="date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="gross"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Gross Amount ($)
            </label>
            <input
              id="gross"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0"
              value={gross}
              onChange={(e) => setGross(e.target.value)}
              placeholder="0"
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="profit"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Profit Amount ($)
            </label>
            <input
              id="profit"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0"
              value={profit}
              onChange={(e) => setProfit(e.target.value)}
              placeholder="0"
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="diesel"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Diesel Expenses ($)
            </label>
            <input
              id="diesel"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0"
              value={diesel}
              onChange={(e) => setDiesel(e.target.value)}
              placeholder="0"
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div className="space-y-2 border-t border-slate-600/50 pt-4">
            <p className="text-xs text-slate-500 uppercase tracking-wider">
              Route (для коллективной аналитики)
            </p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="originState" className="block text-sm text-slate-400 mb-1">
                  From (origin)
                </label>
                <select
                  id="originState"
                  value={originState}
                  onChange={(e) => setOriginState(e.target.value)}
                  className="w-full px-3 py-2.5 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 text-sm"
                >
                  <option value="">—</option>
                  {US_STATES.map(({ name, abbr }) => (
                    <option key={abbr} value={abbr}>{abbr}</option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="destinationState" className="block text-sm text-slate-400 mb-1">
                  To (destination)
                </label>
                <select
                  id="destinationState"
                  value={destinationState}
                  onChange={(e) => setDestinationState(e.target.value)}
                  className="w-full px-3 py-2.5 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 text-sm"
                >
                  <option value="">—</option>
                  {US_STATES.map(({ name, abbr }) => (
                    <option key={abbr} value={abbr}>{abbr}</option>
                  ))}
                </select>
              </div>
            </div>
            <div>
              <label htmlFor="miles" className="block text-sm text-slate-400 mb-1">
                Miles (optional)
              </label>
              <input
                id="miles"
                type="number"
                inputMode="decimal"
                step="1"
                min="0"
                value={miles}
                onChange={(e) => setMiles(e.target.value)}
                placeholder="e.g. 850"
                className="w-full px-3 py-2.5 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 text-sm"
              />
            </div>
          </div>

          {error && (
            <p className="text-sm text-red-400 bg-red-500/10 rounded-xl px-4 py-2">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl bg-sky-500 hover:bg-sky-600 disabled:opacity-60 disabled:cursor-not-allowed text-white font-semibold shadow-lg shadow-sky-500/25 transition-colors"
          >
            {loading ? "Сохранение…" : "Сохранить"}
          </button>
        </form>
      </main>
    </div>
  );
}
