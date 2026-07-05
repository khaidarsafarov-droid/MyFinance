"use client";

import Link from "next/link";
import { Plus } from "lucide-react";

export function FloatingActionButton() {
  return (
    <Link
      href="/add-load"
      className="flex items-center justify-center w-14 h-14 rounded-2xl bg-primary hover:bg-primary-700 text-white shadow-lg shadow-primary/30 transition-all hover:scale-105 active:scale-95"
      aria-label="Add new load"
    >
      <Plus className="w-7 h-7" />
    </Link>
  );
}
