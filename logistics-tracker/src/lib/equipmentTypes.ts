/** Equipment types for market segmentation (onboarding + map analytics) */
export const EQUIPMENT_TYPES = [
  "Amazon",
  "Dry Van",
  "Reefer",
  "Flatbed",
  "Stepdeck",
  "USPS",
  "Power Only",
  "Box Truck",
] as const;

export type EquipmentType = (typeof EQUIPMENT_TYPES)[number];

export function isValidEquipmentType(s: string): s is EquipmentType {
  return EQUIPMENT_TYPES.includes(s as EquipmentType);
}

/** Options for UI dropdowns/selection: { label, value } */
export const EQUIPMENT_OPTIONS = EQUIPMENT_TYPES.map((v) => ({
  label: v,
  value: v,
}));
