"use client";

import { useState } from "react";
import { Building2 } from "lucide-react";
import { useLogisticsActions } from "@/components/LogisticsSyncProvider";

interface AddCompanyModalProps {
  onClose: () => void;
}

export function AddCompanyModal({ onClose }: AddCompanyModalProps) {
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { addCompany } = useLogisticsActions();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || loading) return;
    setError("");
    setLoading(true);
    try {
      await addCompany(name.trim());
      onClose();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Ошибка. Проверьте подключение.";
      if (msg.includes("duplicate") || msg.includes("unique")) {
        setError("Компания с таким именем уже существует");
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/80 backdrop-blur-sm p-4">
      <div className="w-full max-w-md rounded-2xl bg-slate-800 shadow-xl shadow-slate-900/50 p-6 border border-slate-700">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-sky-500/20">
              <Building2 className="w-6 h-6 text-sky-400" />
            </div>
            <h2 className="text-lg font-semibold text-slate-50">Add Company</h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-700/50"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="new-company-name"
              className="block text-sm font-medium text-slate-300 mb-1.5"
            >
              Company Name
            </label>
            <input
              id="new-company-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. XYZ Transport"
              required
              className="w-full px-4 py-3 rounded-xl bg-slate-700/50 border border-slate-600 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 rounded-xl border border-slate-600 text-slate-300 hover:bg-slate-700/50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 py-3 rounded-xl bg-sky-500 hover:bg-sky-600 text-white font-semibold transition-colors"
            >
              Add
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
