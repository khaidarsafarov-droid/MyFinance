import type { Load, CompanyChange } from "@/types";

export type FeedItem =
  | { type: "load"; data: Load }
  | { type: "companyChange"; data: CompanyChange };

export function buildFeed(
  loads: Load[],
  companyChanges: CompanyChange[]
): FeedItem[] {
  const items: FeedItem[] = [];

  loads.forEach((load) => items.push({ type: "load", data: load }));
  companyChanges.forEach((cc) =>
    items.push({ type: "companyChange", data: cc })
  );

  items.sort(
    (a, b) =>
      new Date(b.type === "load" ? b.data.date : b.data.date).getTime() -
      new Date(a.type === "load" ? a.data.date : a.data.date).getTime()
  );

  return items;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}
