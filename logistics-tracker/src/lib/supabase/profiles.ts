import { createClient } from "./client";

export async function updateProfileEquipment(
  userId: string,
  equipmentType: string
): Promise<void> {
  const supabase = createClient();
  const { error } = await supabase
    .from("profiles")
    .update({ equipment_type: equipmentType })
    .eq("id", userId);
  if (error) throw error;
}

export async function updateProfileRpmThresholds(
  userId: string,
  rpmMin: number,
  rpmTarget: number
): Promise<void> {
  if (rpmMin < 0 || rpmTarget < 0 || rpmMin > rpmTarget) {
    throw new Error("Невалидные пороги RPM: красный должен быть меньше зелёного");
  }
  const supabase = createClient();
  const { error } = await supabase
    .from("profiles")
    .update({
      rpm_min_threshold: rpmMin,
      rpm_target_threshold: rpmTarget,
    })
    .eq("id", userId);
  if (error) throw error;
}
