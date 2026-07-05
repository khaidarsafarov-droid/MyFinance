import type { Load } from "@/types";

/** US state full name -> two-letter abbreviation */
export const STATE_NAME_TO_ABBR: Record<string, string> = {
  Alabama: "AL",
  Alaska: "AK",
  Arizona: "AZ",
  Arkansas: "AR",
  California: "CA",
  Colorado: "CO",
  Connecticut: "CT",
  Delaware: "DE",
  "District of Columbia": "DC",
  Florida: "FL",
  Georgia: "GA",
  Hawaii: "HI",
  Idaho: "ID",
  Illinois: "IL",
  Indiana: "IN",
  Iowa: "IA",
  Kansas: "KS",
  Kentucky: "KY",
  Louisiana: "LA",
  Maine: "ME",
  Maryland: "MD",
  Massachusetts: "MA",
  Michigan: "MI",
  Minnesota: "MN",
  Mississippi: "MS",
  Missouri: "MO",
  Montana: "MT",
  Nebraska: "NE",
  Nevada: "NV",
  "New Hampshire": "NH",
  "New Jersey": "NJ",
  "New Mexico": "NM",
  "New York": "NY",
  "North Carolina": "NC",
  "North Dakota": "ND",
  Ohio: "OH",
  Oklahoma: "OK",
  Oregon: "OR",
  Pennsylvania: "PA",
  "Rhode Island": "RI",
  "South Carolina": "SC",
  "South Dakota": "SD",
  Tennessee: "TN",
  Texas: "TX",
  Utah: "UT",
  Vermont: "VT",
  Virginia: "VA",
  Washington: "WA",
  "West Virginia": "WV",
  Wisconsin: "WI",
  Wyoming: "WY",
  "Puerto Rico": "PR",
};

/** State abbreviation -> full name (for modals) */
export const STATE_ABBR_TO_NAME: Record<string, string> = Object.fromEntries(
  Object.entries(STATE_NAME_TO_ABBR).map(([name, abbr]) => [abbr, name])
);

/** Approximate geographic center per state for map panTo */
export const STATE_CENTERS: Record<string, { lat: number; lng: number }> = {
  Alabama: { lat: 32.3, lng: -86.9 },
  Alaska: { lat: 64.2, lng: -152.5 },
  Arizona: { lat: 34.3, lng: -111.7 },
  Arkansas: { lat: 34.9, lng: -92.4 },
  California: { lat: 36.7, lng: -119.4 },
  Colorado: { lat: 39.1, lng: -105.3 },
  Connecticut: { lat: 41.6, lng: -72.8 },
  Delaware: { lat: 38.9, lng: -75.5 },
  "District of Columbia": { lat: 38.9, lng: -77.0 },
  Florida: { lat: 28.6, lng: -82.4 },
  Georgia: { lat: 32.2, lng: -83.6 },
  Hawaii: { lat: 20.8, lng: -156.4 },
  Idaho: { lat: 44.4, lng: -114.6 },
  Illinois: { lat: 40.0, lng: -89.2 },
  Indiana: { lat: 40.3, lng: -86.1 },
  Iowa: { lat: 42.0, lng: -93.5 },
  Kansas: { lat: 38.5, lng: -98.4 },
  Kentucky: { lat: 37.7, lng: -85.5 },
  Louisiana: { lat: 31.2, lng: -92.0 },
  Maine: { lat: 45.4, lng: -69.4 },
  Maryland: { lat: 39.0, lng: -76.6 },
  Massachusetts: { lat: 42.4, lng: -71.4 },
  Michigan: { lat: 44.0, lng: -84.5 },
  Minnesota: { lat: 46.3, lng: -94.2 },
  Mississippi: { lat: 32.7, lng: -89.6 },
  Missouri: { lat: 37.9, lng: -91.8 },
  Montana: { lat: 46.9, lng: -110.4 },
  Nebraska: { lat: 41.5, lng: -99.7 },
  Nevada: { lat: 39.3, lng: -116.4 },
  "New Hampshire": { lat: 44.0, lng: -71.5 },
  "New Jersey": { lat: 40.2, lng: -74.6 },
  "New Mexico": { lat: 34.5, lng: -105.9 },
  "New York": { lat: 43.0, lng: -75.5 },
  "North Carolina": { lat: 35.6, lng: -79.4 },
  "North Dakota": { lat: 47.5, lng: -100.5 },
  Ohio: { lat: 40.4, lng: -82.8 },
  Oklahoma: { lat: 35.6, lng: -97.5 },
  Oregon: { lat: 44.0, lng: -120.6 },
  Pennsylvania: { lat: 41.0, lng: -77.5 },
  "Rhode Island": { lat: 41.7, lng: -71.5 },
  "South Carolina": { lat: 34.0, lng: -81.0 },
  "South Dakota": { lat: 44.4, lng: -99.9 },
  Tennessee: { lat: 35.9, lng: -86.6 },
  Texas: { lat: 31.5, lng: -99.5 },
  Utah: { lat: 39.3, lng: -111.6 },
  Vermont: { lat: 44.1, lng: -72.6 },
  Virginia: { lat: 37.5, lng: -78.5 },
  Washington: { lat: 47.4, lng: -120.5 },
  "West Virginia": { lat: 38.6, lng: -80.6 },
  Wisconsin: { lat: 44.6, lng: -89.6 },
  Wyoming: { lat: 43.1, lng: -107.3 },
  "Puerto Rico": { lat: 18.2, lng: -66.4 },
};

export interface StateReport {
  stateCode: string;
  stateName: string;
  gross: number;
  profit: number;
  diesel: number;
  trips: number;
  avgRate: number;
}

export function getStateReport(
  stateCode: string,
  loads: Load[],
  stateName?: string
): StateReport {
  const filtered = loads.filter((l) => {
    const loadState = l.state?.toUpperCase();
    return loadState === stateCode.toUpperCase();
  });
  const gross = filtered.reduce((s, l) => s + l.gross, 0);
  const profit = filtered.reduce((s, l) => s + l.profit, 0);
  const diesel = filtered.reduce((s, l) => s + l.diesel, 0);
  const trips = filtered.length;
  const avgRate = trips > 0 ? gross / trips : 0;
  return {
    stateCode,
    stateName: stateName ?? stateCode,
    gross,
    profit,
    diesel,
    trips,
    avgRate,
  };
}
