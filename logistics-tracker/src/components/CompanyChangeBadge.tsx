"use client";

import { ArrowRightLeft } from "lucide-react";
import { formatDate } from "@/lib/utils";
import type { CompanyChange } from "@/types";

interface CompanyChangeBadgeProps {
  change: CompanyChange;
}

export function CompanyChangeBadge({ change }: CompanyChangeBadgeProps) {
  return (
    <div className="flex items-center justify-center py-4">
      <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-slate-700/60 border border-slate-600/50 text-slate-400 text-sm">
        <ArrowRightLeft className="w-4 h-4 shrink-0" />
        <span>
          Company changed to <strong className="text-slate-200">{change.companyName}</strong> on {formatDate(change.date)}
        </span>
      </div>
    </div>
  );
}
