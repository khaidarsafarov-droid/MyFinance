"use server";

import { createClient } from "./server";

export interface StateAnalytics {
  avgRpm: number;
  topDestination: string;
  topDestinationCount: number;
  lastLoadAt: string | null;
  lastLoadRecency: string;
  lastLoadExample: string | null;
  isDataFresh: boolean;
}

export async function getStateAnalytics(
  stateCode: string
): Promise<StateAnalytics> {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("get_state_analytics", {
    p_state_code: stateCode.toUpperCase().trim(),
  });
  if (error) throw error;

  const raw = data as {
    avgRpm?: number;
    topDestination?: string;
    topDestinationCount?: number;
    lastLoadAt?: string | null;
    lastLoadRecency?: string;
    lastLoadExample?: string | null;
    isDataFresh?: boolean;
  } | null;

  if (!raw) {
    return {
      avgRpm: 0,
      topDestination: "-",
      topDestinationCount: 0,
      lastLoadAt: null,
      lastLoadRecency: "no data",
      lastLoadExample: null,
      isDataFresh: false,
    };
  }

  return {
    avgRpm: Number(raw.avgRpm) ?? 0,
    topDestination: raw.topDestination ?? "-",
    topDestinationCount: Number(raw.topDestinationCount) ?? 0,
    lastLoadAt: raw.lastLoadAt ?? null,
    lastLoadRecency: raw.lastLoadRecency ?? "no data",
    lastLoadExample: raw.lastLoadExample ?? null,
    isDataFresh: Boolean(raw.isDataFresh),
  };
}

/** Heatmap: state code -> avg RPM for last 14 days. Used to color the map. */
export async function getHeatmapData(): Promise<Record<string, number>> {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("get_all_states_heatmap");
  if (error) throw error;
  const arr = (data ?? []) as { stateCode: string; avgRpm: number }[];
  return Object.fromEntries(
    arr.map((r) => [r.stateCode.toUpperCase(), Number(r.avgRpm) || 0])
  );
}
