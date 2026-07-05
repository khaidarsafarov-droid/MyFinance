"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Building2, Check, Plus, Database, Loader2, LogOut } from "lucide-react";
import { AddCompanyModal } from "@/components/AddCompanyModal";
import { BottomNav } from "@/components/BottomNav";
import { useStore } from "@/store/useStore";
import { useStoreHydration } from "@/hooks/useHydration";
import { useLogisticsActions, useLogisticsSync } from "@/components/LogisticsSyncProvider";
import { useAuth } from "@/contexts/AuthContext";
import { updateProfileRpmThresholds } from "@/lib/supabase/profiles";

const SAMPLE_LOADS = [
  { gross: 2850, profit: 720, diesel: 380, state: "TX" },
  { gross: 3200, profit: 890, diesel: 420, state: "CA" },
  { gross: 2100, profit: 540, diesel: 290, state: "FL" },
  { gross: 1950, profit: 480, diesel: 260, state: "TX" },
  { gross: 4100, profit: 1120, diesel: 520, state: "IL" },
];

const RPM_DEFAULT_MIN = 2.0;
const RPM_DEFAULT_TARGET = 2.5;

export default function SettingsPage() {
  const router = useRouter();
  const [showAddCompany, setShowAddCompany] = useState(false);
  const hydrated = useStoreHydration();
  const { user, profile, signOut, refreshProfile } = useAuth();
  const companies = useStore((s) => s.companies);
  const { setCurrentCompany, addLoad } = useLogisticsActions();
  const hasCompanies = useStore((s) => s.hasCompanies);
  const getCurrentCompany = useStore((s) => s.getCurrentCompany);

  const defaultMin = profile?.rpm_min_threshold != null ? profile.rpm_min_threshold : RPM_DEFAULT_MIN;
  const defaultTarget = profile?.rpm_target_threshold != null ? profile.rpm_target_threshold : RPM_DEFAULT_TARGET;
  const [rpmMin, setRpmMin] = useState<string>(() => String(defaultMin));
  const [rpmTarget, setRpmTarget] = useState<string>(() => String(defaultTarget));
  const [rpmSaving, setRpmSaving] = useState(false);
  const [rpmError, setRpmError] = useState<string | null>(null);

  useEffect(() => {
    setRpmMin(String(profile?.rpm_min_threshold != null ? profile.rpm_min_threshold : RPM_DEFAULT_MIN));
    setRpmTarget(String(profile?.rpm_target_threshold != null ? profile.rpm_target_threshold : RPM_DEFAULT_TARGET));
  }, [profile?.rpm_min_threshold, profile?.rpm_target_threshold]);

  const [seeding, setSeeding] = useState(false);
  const handleSeedSampleData = async () => {
    const company = getCurrentCompany();
    if (!company || seeding) return;
    setSeeding(true);
    try {
      const baseDate = new Date();
      for (let i = 0; i < SAMPLE_LOADS.length; i++) {
        const load = SAMPLE_LOADS[i]!;
        const date = new Date(baseDate);
        date.setDate(date.getDate() - i * 3);
        await addLoad({
          date: date.toISOString(),
          gross: load.gross,
          profit: load.profit,
          diesel: load.diesel,
          companyId: company.id,
          state: load.state,
        });
      }
    } finally {
      setSeeding(false);
    }
  };

  const sync = useLogisticsSync();
  const isDataReady = sync?.isDataReady ?? false;

  const handleSaveRpm = async () => {
    const min = parseFloat(rpmMin);
    const target = parseFloat(rpmTarget);
    if (!user?.id || isNaN(min) || isNaN(target)) return;
    setRpmError(null);
    setRpmSaving(true);
    try {
      await updateProfileRpmThresholds(user.id, min, target);
      await refreshProfile();
    } catch (err) {
      setRpmError(err instanceof Error ? err.message : "Ошибка сохранения");
    } finally {
      setRpmSaving(false);
    }
  };

  if (!hydrated) return null;

  if (!isDataReady) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 px-4">
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
        <p className="text-slate-400">Загрузка данных...</p>
      </div>
    );
  }

  if (!hasCompanies()) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <p className="text-slate-400">No companies yet. Complete setup first.</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen pb-24">
      {showAddCompany && (
        <AddCompanyModal onClose={() => setShowAddCompany(false)} />
      )}
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-700/50">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center gap-3">
          <Link
            href="/"
            className="p-2 -ml-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-slate-50">Settings</h1>
            <p className="text-sm text-slate-400">Manage companies</p>
          </div>
        </div>
      </header>

      <main className="max-w-lg mx-auto px-4 py-6">
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-medium text-slate-500 uppercase tracking-wider">
              Companies
            </h2>
            <button
              onClick={() => setShowAddCompany(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 text-sm font-medium transition-colors"
            >
              <Plus className="w-4 h-4" /> Add
            </button>
          </div>
          <div className="space-y-2">
            {companies.map((company) => (
              <button
                key={company.id}
                onClick={() => setCurrentCompany(company.id).catch(console.error)}
                className="w-full flex items-center justify-between gap-4 rounded-2xl bg-slate-800/80 border border-slate-700/50 shadow-sm p-4 hover:border-slate-600/50 transition-colors text-left"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="p-2 rounded-xl bg-sky-500/20 shrink-0">
                    <Building2 className="w-5 h-5 text-sky-400" />
                  </div>
                  <span className="font-medium text-slate-200 truncate">
                    {company.name}
                  </span>
                </div>
                {company.isCurrent && (
                  <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-sky-500/20 text-sky-400 text-sm font-medium shrink-0">
                    <Check className="w-4 h-4" />
                    Current
                  </div>
                )}
              </button>
            ))}
          </div>
        </section>

        <p className="mt-6 text-xs text-slate-500">
          Tap a company to switch. Loads added after a change will be tagged with
          the new company. A divider will appear in the dashboard feed.
        </p>

        <section className="mt-8">
          <h2 className="text-sm font-medium text-slate-500 uppercase tracking-wider mb-3">
            Пороги прибыльности RPM
          </h2>
          <p className="text-xs text-slate-500 mb-3">
            Ниже красного порога — неприбыльно. Выше зелёного — высокая прибыль. Между — средняя (оранжевый).
          </p>
          <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 p-4 space-y-4">
            <div>
              <label className="block text-sm text-slate-400 mb-1">Красный порог ($/mi)</label>
              <input
                type="number"
                step="0.1"
                min="0"
                value={rpmMin}
                onChange={(e) => setRpmMin(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <label className="block text-sm text-slate-400 mb-1">Зелёный порог ($/mi)</label>
              <input
                type="number"
                step="0.1"
                min="0"
                value={rpmTarget}
                onChange={(e) => setRpmTarget(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            {rpmError && (
              <p className="text-sm text-red-400">{rpmError}</p>
            )}
            <button
              onClick={handleSaveRpm}
              disabled={rpmSaving}
              className="w-full py-3 rounded-xl bg-primary hover:bg-primary-700 disabled:opacity-60 text-white font-medium transition-colors"
            >
              {rpmSaving ? "Сохранение…" : "Сохранить пороги"}
            </button>
          </div>
        </section>

        <section className="mt-8">
          <h2 className="text-sm font-medium text-slate-500 uppercase tracking-wider mb-3">
            Demo data
          </h2>
          <button
            onClick={handleSeedSampleData}
            className="w-full flex items-center justify-center gap-2 py-3 rounded-xl border border-dashed border-slate-600 text-slate-400 hover:text-slate-300 hover:border-slate-500 transition-colors"
          >
            <Database className="w-4 h-4" />
            Load sample data (for map testing)
          </button>
        </section>

        <section className="mt-8">
          <h2 className="text-sm font-medium text-slate-500 uppercase tracking-wider mb-3">
            Аккаунт
          </h2>
          <button
            onClick={() => {
              signOut();
              router.push("/login");
            }}
            className="w-full flex items-center justify-center gap-2 py-3.5 rounded-xl border border-slate-600 text-slate-400 hover:text-red-400 hover:border-red-500/50 hover:bg-red-500/10 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Выйти
          </button>
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
