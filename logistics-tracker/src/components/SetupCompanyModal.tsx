"use client";

import { useState } from "react";
import { Building2 } from "lucide-react";
import { useLogisticsActions } from "@/components/LogisticsSyncProvider";

export function SetupCompanyModal() {
  const [name, setName] = useState("");
  const [startDate, setStartDate] = useState(
    new Date().toISOString().slice(0, 10)
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { addCompany } = useLogisticsActions();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || loading) return;
    setError("");
    setLoading(true);
    try {
      await addCompany(name.trim(), `${startDate}T12:00:00.000Z`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ошибка. Проверьте подключение.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/80 backdrop-blur-sm p-4">
      <div className="w-full max-w-md rounded-2xl bg-slate-800 shadow-xl shadow-slate-900/50 p-6 sm:p-8 border border-slate-700">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-3 rounded-xl bg-sky-500/20">
            <Building2 className="w-8 h-8 text-sky-400" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-slate-50">
              Первая компания
            </h2>
            <p className="text-sm text-slate-400">
              Добавьте компанию для учёта грузов
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label
              htmlFor="company-name"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Название компании
            </label>
            <input
              id="company-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. ABC Logistics"
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="start-date"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Дата начала
            </label>
            <input
              id="start-date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
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
            {loading ? "Сохранение…" : "Сохранить и начать"}
          </button>
        </form>
      </div>
    </div>
  );
}
