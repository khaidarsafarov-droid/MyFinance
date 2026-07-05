"use client";

import { SetupCompanyModal } from "@/components/SetupCompanyModal";
import { useStoreHydration } from "@/hooks/useHydration";
import { LoadCard } from "@/components/LoadCard";
import { CompanyChangeBadge } from "@/components/CompanyChangeBadge";
import { FloatingActionButton } from "@/components/FloatingActionButton";
import { BottomNav } from "@/components/BottomNav";
import { useStore } from "@/store/useStore";
import { buildFeed } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useLogisticsSync } from "@/components/LogisticsSyncProvider";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { LogOut, Loader2, RefreshCw } from "lucide-react";

export default function DashboardPage() {
  const router = useRouter();
  const hydrated = useStoreHydration();
  const { user, profile, isLoading: authLoading, signOut } = useAuth();
  const sync = useLogisticsSync();
  const isDataReady = sync?.isDataReady ?? false;
  const dataError = sync?.dataError ?? null;
  const refetch = sync?.refetch ?? (async () => {});
  const companies = useStore((s) => s.companies);
  const loads = useStore((s) => s.loads);
  const companyChanges = useStore((s) => s.companyChanges);
  const hasCompanies = useStore((s) => s.hasCompanies);

  if (!hydrated || authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
      </div>
    );
  }

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
      <>
        <SetupCompanyModal />
        <div className="min-h-screen" />
      </>
    );
  }

  const feed = buildFeed(loads, companyChanges);
  const getCompanyName = (id: string) =>
    companies.find((c) => c.id === id)?.name ?? "—";

  return (
    <div className="min-h-screen pb-24">
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-700/50">
        <div className="max-w-lg mx-auto px-4 py-4 flex items-center justify-between gap-3">
          <div className="min-w-0 flex-1">
            <h1 className="text-xl font-bold text-slate-50">Logistics Tracker</h1>
            <p className="text-sm text-slate-400 mt-0.5 truncate">
              {companies.find((c) => c.isCurrent)?.name ?? "—"}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <span className="hidden sm:inline text-sm text-slate-400 truncate max-w-[120px]">
              {profile?.first_name ?? user?.email?.split("@")[0] ?? "Профиль"}
            </span>
            <button
              onClick={() => { signOut(); router.push("/login"); }}
              className="p-2.5 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 transition-colors"
              aria-label="Выйти"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-lg mx-auto px-4 py-6 space-y-4">
        {dataError && (
          <div className="rounded-2xl bg-red-500/10 border border-red-500/30 p-4 flex items-center justify-between gap-3">
            <p className="text-sm text-red-400">{dataError}</p>
            <button
              onClick={() => refetch()}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 text-sm font-medium transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
              Повторить
            </button>
          </div>
        )}
        {feed.length === 0 ? (
          <div className="rounded-2xl border-2 border-dashed border-slate-600 p-12 text-center">
            <p className="text-slate-400 mb-2">Пока нет грузов</p>
            <p className="text-slate-500 text-sm mb-6">Нажмите + чтобы добавить первый рейс</p>
            <FloatingActionButton />
          </div>
        ) : (
          <>
            {feed.map((item) =>
              item.type === "load" ? (
                <LoadCard
                  key={`load-${item.data.id}`}
                  load={item.data}
                  companyName={getCompanyName(item.data.companyId)}
                />
              ) : (
                <CompanyChangeBadge key={`cc-${item.data.id}`} change={item.data} />
              )
            )}
            <div className="pt-2">
              <FloatingActionButton />
            </div>
          </>
        )}
      </main>

      <BottomNav />
    </div>
  );
}
