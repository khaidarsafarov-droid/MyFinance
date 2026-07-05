"use client";

import { useEffect, useCallback, useState } from "react";
import { createContext, useContext } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useStore } from "@/store/useStore";
import {
  fetchLoads,
  fetchCompanies,
  fetchCompanyChanges,
  insertLoad,
  insertCompany,
  setCurrentCompany as setCurrentCompanyDb,
  updateLoad as updateLoadDb,
  updateCompany as updateCompanyDb,
  deleteLoad as deleteLoadDb,
} from "@/lib/supabase/logistics";
import type { Load, Company, CompanyChange } from "@/types";

type LogisticsSyncContextValue = {
  isDataReady: boolean;
  dataError: string | null;
  refetch: () => Promise<void>;
};

const LogisticsSyncContext = createContext<LogisticsSyncContextValue | null>(null);

export function LogisticsSyncProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const replaceData = useStore((s) => s.replaceData);
  const [isDataReady, setIsDataReady] = useState(false);
  const [dataError, setDataError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    if (!user?.id) {
      replaceData({ loads: [], companies: [], companyChanges: [] });
      setIsDataReady(true);
      setDataError(null);
      return;
    }
    setDataError(null);
    try {
      const [loads, companies, companyChanges] = await Promise.all([
        fetchLoads(user.id),
        fetchCompanies(user.id),
        fetchCompanyChanges(user.id),
      ]);
      replaceData({ loads, companies, companyChanges });
      setIsDataReady(true);
    } catch (err) {
      setDataError(err instanceof Error ? err.message : "Ошибка загрузки данных");
      setIsDataReady(true); // still show UI, user can retry
    }
  }, [user?.id, replaceData]);

  useEffect(() => {
    if (!user?.id) {
      replaceData({ loads: [], companies: [], companyChanges: [] });
      setIsDataReady(true);
      return;
    }
    let cancelled = false;
    setIsDataReady(false);
    (async () => {
      try {
        const [loads, companies, companyChanges] = await Promise.all([
          fetchLoads(user.id),
          fetchCompanies(user.id),
          fetchCompanyChanges(user.id),
        ]);
        if (!cancelled) {
          replaceData({ loads, companies, companyChanges });
          setIsDataReady(true);
          setDataError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setDataError(err instanceof Error ? err.message : "Ошибка загрузки данных");
          setIsDataReady(true);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user?.id, replaceData]);

  return (
    <LogisticsSyncContext.Provider
      value={{
        isDataReady,
        dataError,
        refetch: fetchData,
      }}
    >
      {children}
    </LogisticsSyncContext.Provider>
  );
}

export function useLogisticsSync() {
  return useContext(LogisticsSyncContext);
}

export function useLogisticsActions() {
  const { user, profile } = useAuth();
  const store = useStore.getState();

  const addLoad = useCallback(
    async (load: Omit<Load, "id">) => {
      if (!user?.id) throw new Error("Не авторизован");
      const equipmentType = profile?.equipment_type?.trim();
      const loadWithEquipment = equipmentType
        ? { ...load, equipmentType }
        : load;
      const created = await insertLoad(user.id, loadWithEquipment);
      store.addLoadFromRemote(created);
    },
    [user?.id, profile?.equipment_type]
  );

  const addCompany = useCallback(
    async (name: string, startDate?: string) => {
      if (!user?.id) throw new Error("Не авторизован");
      await insertCompany(user.id, name, true, startDate);
      const [loads, companies, companyChanges] = await Promise.all([
        fetchLoads(user.id),
        fetchCompanies(user.id),
        fetchCompanyChanges(user.id),
      ]);
      useStore.getState().replaceData({ loads, companies, companyChanges });
    },
    [user?.id]
  );

  const setCurrentCompany = useCallback(
    async (companyId: string) => {
      if (!user?.id) throw new Error("Не авторизован");
      const company = useStore.getState().companies.find((c) => c.id === companyId);
      if (!company) return;
      await setCurrentCompanyDb(user.id, companyId, company.name);
      const [loads, companies, companyChanges] = await Promise.all([
        fetchLoads(user.id),
        fetchCompanies(user.id),
        fetchCompanyChanges(user.id),
      ]);
      useStore.getState().replaceData({ loads, companies, companyChanges });
    },
    [user?.id]
  );

  const updateLoad = useCallback(
    async (id: string, updates: Partial<Omit<Load, "id" | "companyId">>) => {
      if (!user?.id) throw new Error("Не авторизован");
      await updateLoadDb(user.id, id, updates);
      store.updateLoad(id, updates);
    },
    [user?.id]
  );

  const updateCompany = useCallback(
    async (id: string, name: string) => {
      if (!user?.id) throw new Error("Не авторизован");
      await updateCompanyDb(user.id, id, name);
      store.updateCompany(id, name);
    },
    [user?.id]
  );

  const deleteLoad = useCallback(
    async (id: string) => {
      if (!user?.id) throw new Error("Не авторизован");
      await deleteLoadDb(user.id, id);
      store.deleteLoad(id);
    },
    [user?.id]
  );

  return {
    addLoad,
    addCompany,
    setCurrentCompany,
    updateLoad,
    updateCompany,
    deleteLoad,
  };
}
