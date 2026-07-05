"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Load, Company, CompanyChange } from "@/types";

const STORAGE_KEY = "logistics-tracker-storage";

export interface StoreState {
  companies: Company[];
  loads: Load[];
  companyChanges: CompanyChange[];
  addCompany: (name: string, startDate?: string) => void;
  setCurrentCompany: (companyId: string) => void;
  addLoad: (load: Omit<Load, "id">) => void;
  addLoadFromRemote: (load: Load) => void;
  updateLoad: (id: string, updates: Partial<Omit<Load, "id" | "companyId">>) => void;
  deleteLoad: (id: string) => void;
  updateCompany: (id: string, name: string) => void;
  hasCompanies: () => boolean;
  getCurrentCompany: () => Company | undefined;
  replaceData: (data: { loads: Load[]; companies: Company[]; companyChanges: CompanyChange[] }) => void;
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export const useStore = create<StoreState>()(
  persist(
    (set, get) => ({
      companies: [],
      loads: [],
      companyChanges: [],

      addCompany: (name: string, startDate?: string) => {
        const id = generateId();
        set((state) => {
          const newCompanies = state.companies.map((c) => ({
            ...c,
            isCurrent: false,
          }));
          newCompanies.push({
            id,
            name,
            isCurrent: true,
          });
          return {
            companies: newCompanies,
            companyChanges: [
              ...state.companyChanges,
              {
                id: generateId(),
                date: startDate || new Date().toISOString(),
                companyId: id,
                companyName: name,
              },
            ],
          };
        });
      },

      setCurrentCompany: (companyId: string) => {
        const company = get().companies.find((c) => c.id === companyId);
        if (!company) return;

        set((state) => {
          const newCompanies = state.companies.map((c) => ({
            ...c,
            isCurrent: c.id === companyId,
          }));
          return {
            companies: newCompanies,
            companyChanges: [
              ...state.companyChanges,
              {
                id: generateId(),
                date: new Date().toISOString(),
                companyId: company.id,
                companyName: company.name,
              },
            ],
          };
        });
      },

      addLoad: (load: Omit<Load, "id">) => {
        const newLoad: Load = {
          ...load,
          id: generateId(),
        };
        set((state) => ({
          loads: [newLoad, ...state.loads],
        }));
      },

      addLoadFromRemote: (load: Load) => {
        set((state) => ({
          loads: [load, ...state.loads.filter((l) => l.id !== load.id)],
        }));
      },

      replaceData: (data) => {
        set({
          loads: data.loads,
          companies: data.companies,
          companyChanges: data.companyChanges,
        });
      },

      updateLoad: (id: string, updates: Partial<Omit<Load, "id" | "companyId">>) => {
        set((state) => ({
          loads: state.loads.map((l) =>
            l.id === id ? { ...l, ...updates } : l
          ),
        }));
      },

      deleteLoad: (id: string) => {
        set((state) => ({
          loads: state.loads.filter((l) => l.id !== id),
        }));
      },

      updateCompany: (id: string, name: string) => {
        set((state) => ({
          companies: state.companies.map((c) =>
            c.id === id ? { ...c, name } : c
          ),
          companyChanges: state.companyChanges.map((cc) =>
            cc.companyId === id ? { ...cc, companyName: name } : cc
          ),
        }));
      },

      hasCompanies: () => get().companies.length > 0,

      getCurrentCompany: () =>
        get().companies.find((c) => c.isCurrent),
    }),
    { name: STORAGE_KEY }
  )
);
