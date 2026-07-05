"use client";

import { useState } from "react";
import { Truck } from "lucide-react";
import { EQUIPMENT_OPTIONS } from "@/lib/equipmentTypes";
import { updateProfileEquipment } from "@/lib/supabase/profiles";

interface EquipmentSelectionModalProps {
  onSaved: () => void;
}

export function EquipmentSelectionModal({ onSaved }: EquipmentSelectionModalProps) {
  const [selected, setSelected] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    if (!selected) return;
    setError(null);
    setLoading(true);
    try {
      const { createClient } = await import("@/lib/supabase/client");
      const supabase = createClient();
      const { data: { user: u } } = await supabase.auth.getUser();
      if (!u?.id) throw new Error("Не авторизован");
      await updateProfileEquipment(u.id, selected);
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ошибка сохранения");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-2xl bg-slate-800 border border-slate-700 shadow-xl p-6">
        <div className="flex flex-col items-center text-center mb-6">
          <div className="p-4 rounded-2xl bg-primary/20 mb-4">
            <Truck className="w-10 h-10 text-primary-400" />
          </div>
          <h2 className="text-xl font-bold text-slate-100 mb-2">
            Выберите тип оборудования
          </h2>
          <p className="text-sm text-slate-400">
            Это поможет показывать релевантные цены на карте для вашего рынка
          </p>
        </div>

        <div className="grid grid-cols-2 gap-2 mb-6">
          {EQUIPMENT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setSelected(opt.value)}
              className={`px-4 py-3 rounded-xl border text-sm font-medium transition-colors ${
                selected === opt.value
                  ? "border-primary bg-primary/20 text-primary-300"
                  : "border-slate-600 bg-slate-700/50 text-slate-300 hover:border-slate-500"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        {error && (
          <p className="text-red-400 text-sm mb-4 text-center">{error}</p>
        )}

        <button
          onClick={handleSave}
          disabled={!selected || loading}
          className="w-full py-3.5 rounded-xl bg-primary hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold transition-colors"
        >
          {loading ? "Сохранение…" : "Сохранить"}
        </button>
      </div>
    </div>
  );
}
