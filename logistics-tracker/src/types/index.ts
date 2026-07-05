export interface Load {
  id: string;
  date: string; // ISO format
  gross: number;
  profit: number;
  diesel: number;
  companyId: string;
  state?: string; // legacy: single state
  /** Route crowdsourcing: origin */
  originState?: string;
  /** Route crowdsourcing: destination */
  destinationState?: string;
  /** Miles for rate_per_mile (crowdsourcing) */
  miles?: number;
  /** Equipment type for market segmentation */
  equipmentType?: string;
}

export interface Company {
  id: string;
  name: string;
  isCurrent: boolean;
}

export interface CompanyChange {
  id: string;
  date: string; // ISO format
  companyId: string;
  companyName: string;
}

export interface Profile {
  id: string;
  first_name: string | null;
  last_name: string | null;
  truck_number: string | null;
  company_name: string | null;
  equipment_type: string | null;
  rpm_min_threshold: number | null;
  rpm_target_threshold: number | null;
  updated_at: string;
}
