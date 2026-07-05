"use client";

import { useEffect, useState } from "react";
import { X, DollarSign, MapPin, Clock, Loader2, Circle } from "lucide-react";
import { getStateAnalytics, type StateAnalytics } from "@/lib/supabase/analytics";

interface StateInsightsModalProps {
  stateCode: string;
  stateName: string;
  onClose: () => void;
}

export function StateInsightsModal({
  stateCode,
  stateName,
  onClose,
}: StateInsightsModalProps) {
  const [data, setData] = useState<StateAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getStateAnalytics(stateCode)
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Ошибка загрузки");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [stateCode]);

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <button
        onClick={onClose}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        aria-label="Закрыть"
      />
      <div className="relative w-full max-w-lg rounded-t-2xl sm:rounded-2xl bg-slate-800 border-t sm:border border-slate-700 shadow-xl max-h-[85vh] overflow-y-auto">
        <div className="sticky top-0 flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-slate-100">
              {stateName} ({stateCode}) Live Insights
            </h2>
            {data?.isDataFresh && (
              <span
                className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-xs"
                title="Данные за последние 24 часа"
              >
                <Circle className="w-2 h-2 fill-emerald-400" />
                Fresh
              </span>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 transition-colors"
            aria-label="Закрыть"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 pb-8">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-12 gap-3">
              <Loader2 className="w-10 h-10 text-sky-400 animate-spin" />
              <p className="text-slate-400 text-sm">Загрузка live-данных...</p>
            </div>
          ) : error ? (
            <div className="py-8 text-center">
              <p className="text-red-400 text-sm">{error}</p>
              <button
                onClick={() => window.location.reload()}
                className="mt-4 text-sky-400 text-sm hover:text-sky-300"
              >
                Обновить
              </button>
            </div>
          ) : data ? (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-6">
                <div className="rounded-xl bg-slate-700/30 p-4 flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-emerald-500/20 shrink-0">
                    <DollarSign className="w-5 h-5 text-emerald-400" />
                  </div>
                  <div>
                    <p className="text-slate-500 text-xs uppercase tracking-wider">
                      Avg. Market Rate
                    </p>
                    <p className="text-emerald-400 font-bold text-xl">
                      ${Number(data.avgRpm).toFixed(2)}/mi
                    </p>
                  </div>
                </div>
                <div className="rounded-xl bg-slate-700/30 p-4 flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-sky-500/20 shrink-0">
                    <MapPin className="w-5 h-5 text-sky-400" />
                  </div>
                  <div>
                    <p className="text-slate-500 text-xs uppercase tracking-wider">
                      Main Route
                    </p>
                    <p className="text-sky-400 font-bold text-xl">
                      {data.topDestination}
                    </p>
                  </div>
                </div>
                <div className="rounded-xl bg-slate-700/30 p-4 flex items-start gap-3 sm:col-span-2">
                  <div className="p-2 rounded-lg bg-amber-500/20 shrink-0">
                    <Clock className="w-5 h-5 text-amber-400" />
                  </div>
                  <div>
                    <p className="text-slate-500 text-xs uppercase tracking-wider">
                      Last Load
                    </p>
                    <p className="text-slate-200 font-semibold">
                      {data.lastLoadRecency}
                      {data.lastLoadExample && (
                        <span className="block text-slate-400 text-sm font-normal mt-1">
                          {data.lastLoadExample}
                        </span>
                      )}
                    </p>
                  </div>
                </div>
              </div>

              <p className="text-slate-400 text-sm leading-relaxed">
                Средняя цена в этом штате сейчас составляет{" "}
                <span className="text-emerald-400 font-medium">
                  ${Number(data.avgRpm).toFixed(2)}/милю
                </span>
                . Чаще всего отсюда уезжают в{" "}
                <span className="text-sky-400 font-medium">{data.topDestination}</span>
                {data.topDestinationCount > 0 && (
                  <span className="text-slate-500">
                    {" "}({data.topDestinationCount} выездов за 14 дней)
                  </span>
                )}
                .
              </p>
            </>
          ) : null}
        </div>
      </div>
    </div>
  );
}
