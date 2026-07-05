import { createClient } from "./client";
import type { Load, Company, CompanyChange } from "@/types";

export async function fetchLoads(userId: string): Promise<Load[]> {
  const supabase = createClient();
  const { data, error } = await supabase
    .from("loads")
    .select("*")
    .eq("user_id", userId)
    .order("date", { ascending: false });
  if (error) throw error;
  return (data ?? []).map((r) => ({
    id: r.id,
    date: r.date,
    gross: r.gross,
    profit: r.profit,
    diesel: r.diesel,
    companyId: r.company_id,
    state: r.state ?? undefined,
    originState: r.origin_state ?? undefined,
    destinationState: r.destination_state ?? undefined,
    miles: r.miles ?? undefined,
    equipmentType: r.equipment_type ?? undefined,
  }));
}

export async function fetchCompanies(userId: string): Promise<Company[]> {
  const supabase = createClient();
  const { data, error } = await supabase
    .from("companies")
    .select("*")
    .eq("user_id", userId)
    .order("created_at", { ascending: true });
  if (error) throw error;

  const companies = (data ?? []).map((r) => ({
    id: r.id,
    name: r.name,
    isCurrent: r.is_current,
  }));

  if (companies.length > 0 && !companies.some((c) => c.isCurrent)) {
    companies[0]!.isCurrent = true;
  }
  return companies;
}

export async function fetchCompanyChanges(userId: string): Promise<CompanyChange[]> {
  const supabase = createClient();
  const { data, error } = await supabase
    .from("company_changes")
    .select("*")
    .eq("user_id", userId)
    .order("date", { ascending: true });
  if (error) throw error;
  return (data ?? []).map((r) => ({
    id: r.id,
    date: r.date,
    companyId: r.company_id,
    companyName: r.company_name,
  }));
}

export async function insertLoad(
  userId: string,
  load: Omit<Load, "id">
): Promise<Load> {
  const supabase = createClient();
  const payload: Record<string, unknown> = {
    user_id: userId,
    date: load.date,
    gross: load.gross,
    profit: load.profit,
    diesel: load.diesel,
    company_id: load.companyId,
    state: load.state ?? null,
  };
  if (load.originState != null) payload.origin_state = load.originState;
  if (load.destinationState != null) payload.destination_state = load.destinationState;
  if (load.miles != null) payload.miles = load.miles;
  if (load.miles != null && load.miles > 0) payload.total_rate = load.gross;
  if (load.equipmentType != null) payload.equipment_type = load.equipmentType;

  const { data, error } = await supabase
    .from("loads")
    .insert(payload)
    .select()
    .single();
  if (error) throw error;
  return {
    id: data.id,
    date: data.date,
    gross: data.gross,
    profit: data.profit,
    diesel: data.diesel,
    companyId: data.company_id,
    state: data.state ?? undefined,
    originState: data.origin_state ?? undefined,
    destinationState: data.destination_state ?? undefined,
    miles: data.miles ?? undefined,
    equipmentType: data.equipment_type ?? undefined,
  };
}

export async function updateLoad(
  userId: string,
  id: string,
  updates: Partial<Omit<Load, "id" | "companyId">>
): Promise<void> {
  const supabase = createClient();
  const payload: Record<string, unknown> = {};
  if (updates.date != null) payload.date = updates.date;
  if (updates.gross != null) payload.gross = updates.gross;
  if (updates.profit != null) payload.profit = updates.profit;
  if (updates.diesel != null) payload.diesel = updates.diesel;
  if (updates.state !== undefined) payload.state = updates.state ?? null;
  if (updates.originState !== undefined) payload.origin_state = updates.originState ?? null;
  if (updates.destinationState !== undefined) payload.destination_state = updates.destinationState ?? null;
  if (updates.miles !== undefined) payload.miles = updates.miles ?? null;
  if (updates.equipmentType !== undefined) payload.equipment_type = updates.equipmentType ?? null;
  if (updates.equipmentType !== undefined) payload.equipment_type = updates.equipmentType ?? null;
  const { error } = await supabase
    .from("loads")
    .update(payload)
    .eq("id", id)
    .eq("user_id", userId);
  if (error) throw error;
}

export async function deleteLoad(userId: string, id: string): Promise<void> {
  const supabase = createClient();
  const { error } = await supabase
    .from("loads")
    .delete()
    .eq("id", id)
    .eq("user_id", userId);
  if (error) throw error;
}

export async function insertCompany(
  userId: string,
  name: string,
  isCurrent: boolean,
  startDate?: string
): Promise<Company> {
  const supabase = createClient();
  if (isCurrent) {
    await supabase
      .from("companies")
      .update({ is_current: false })
      .eq("user_id", userId);
  }
  const { data, error } = await supabase
    .from("companies")
    .insert({ user_id: userId, name, is_current: isCurrent })
    .select()
    .single();
  if (error) throw error;
  const { error: ccError } = await supabase.from("company_changes").insert({
    user_id: userId,
    date: startDate ?? new Date().toISOString(),
    company_id: data.id,
    company_name: name,
  });
  if (ccError) throw ccError;
  return {
    id: data.id,
    name: data.name,
    isCurrent: data.is_current,
  };
}

export async function setCurrentCompany(
  userId: string,
  companyId: string,
  companyName: string
): Promise<void> {
  const supabase = createClient();
  await supabase
    .from("companies")
    .update({ is_current: false })
    .eq("user_id", userId);
  const { error: e1 } = await supabase
    .from("companies")
    .update({ is_current: true })
    .eq("id", companyId)
    .eq("user_id", userId);
  if (e1) throw e1;
  const { error: e2 } = await supabase.from("company_changes").insert({
    user_id: userId,
    date: new Date().toISOString(),
    company_id: companyId,
    company_name: companyName,
  });
  if (e2) throw e2;
}

export async function insertCompanyChange(
  userId: string,
  change: Omit<CompanyChange, "id">
): Promise<CompanyChange> {
  const supabase = createClient();
  const { data, error } = await supabase
    .from("company_changes")
    .insert({
      user_id: userId,
      date: change.date,
      company_id: change.companyId,
      company_name: change.companyName,
    })
    .select()
    .single();
  if (error) throw error;
  return {
    id: data.id,
    date: data.date,
    companyId: data.company_id,
    companyName: data.company_name,
  };
}

export async function updateCompany(
  userId: string,
  id: string,
  name: string
): Promise<void> {
  const supabase = createClient();
  const { error } = await supabase
    .from("companies")
    .update({ name })
    .eq("id", id)
    .eq("user_id", userId);
  if (error) throw error;
  await supabase
    .from("company_changes")
    .update({ company_name: name })
    .eq("company_id", id)
    .eq("user_id", userId);
}
