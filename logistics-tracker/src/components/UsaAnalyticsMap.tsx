"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { GoogleMap, useJsApiLoader } from "@react-google-maps/api";
import { STATE_NAME_TO_ABBR, STATE_CENTERS } from "@/lib/stateUtils";
import { StateInsightsModal } from "@/components/StateInsightsModal";
import { getHeatmapData } from "@/lib/supabase/analytics";

const GEOJSON_URL =
  "https://raw.githubusercontent.com/PublicaMundi/MappingAPI/master/data/geojson/us-states.json";

/** Heatmap colors: low=amber, medium=yellow, high=green, no data=gray */
const HEATMAP_COLORS = {
  low: "#f59e0b",
  medium: "#eab308",
  high: "#22c55e",
  noData: "#64748b",
};

const HOVER_STYLE = {
  fillColor: "#4285f4",
  fillOpacity: 0.8,
  strokeColor: "#1d4ed8",
  strokeWeight: 2,
};

const mapContainerStyle = { width: "100%", height: "100%", minHeight: 320 };

const defaultCenter = { lat: 39.5, lng: -98.35 };
const defaultZoom = 4;

/** Silver/light map style — minimal POI, clean look */
const mapStyles = [
  { featureType: "poi", elementType: "labels", stylers: [{ visibility: "off" }] },
  { featureType: "transit", stylers: [{ visibility: "off" }] },
  {
    featureType: "water",
    elementType: "geometry.fill",
    stylers: [{ color: "#d3d3d3" }],
  },
  {
    featureType: "landscape",
    elementType: "geometry.fill",
    stylers: [{ color: "#f5f5f5" }],
  },
];

function getColorForRpm(
  rpm: number,
  p33: number,
  p66: number
): string {
  if (rpm <= 0) return HEATMAP_COLORS.noData;
  if (p66 === p33) return HEATMAP_COLORS.high; // all same
  if (rpm < p33) return HEATMAP_COLORS.low;
  if (rpm < p66) return HEATMAP_COLORS.medium;
  return HEATMAP_COLORS.high;
}

export function UsaAnalyticsMap() {
  const [selectedState, setSelectedState] = useState<{
    code: string;
    name: string;
  } | null>(null);
  const [hoveredState, setHoveredState] = useState<{
    code: string;
    name: string;
  } | null>(null);
  const [heatmapRpm, setHeatmapRpm] = useState<Record<string, number>>({});
  const [percentiles, setPercentiles] = useState<{ p33: number; p66: number }>({
    p33: 0,
    p66: 0,
  });
  const dataLayerRef = useRef<google.maps.Data | null>(null);

  const apiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY?.trim() ?? "";
  const hasApiKey = apiKey.length > 0 && !apiKey.startsWith("your_");

  const { isLoaded, loadError } = useJsApiLoader({
    id: "google-map-script",
    googleMapsApiKey: hasApiKey ? apiKey : " ",
    ...(hasApiKey ? {} : { disableLoading: true }),
  });

  useEffect(() => {
    if (!hasApiKey) return;
    getHeatmapData()
      .then((data) => {
        setHeatmapRpm(data);
        const values = Object.values(data).filter((v) => v > 0).sort((a, b) => a - b);
        const n = values.length;
        const p33 = n > 0 ? values[Math.floor(n * 0.33)] ?? 0 : 0;
        const p66 = n > 0 ? values[Math.floor(n * 0.66)] ?? 0 : 0;
        setPercentiles({ p33, p66 });
      })
      .catch(() => {});
  }, [hasApiKey]);

  const onMapLoad = useCallback(
    (mapInstance: google.maps.Map) => {
      mapInstance.setOptions({ styles: mapStyles });
      const dataLayer = mapInstance.data;
      dataLayerRef.current = dataLayer;

      dataLayer.loadGeoJson(GEOJSON_URL);
      dataLayer.setStyle((feature: google.maps.Data.Feature) => {
        const name = feature.getProperty("name") as string | undefined;
        const code = name ? STATE_NAME_TO_ABBR[name] ?? "" : "";
        const rpm = code ? (heatmapRpm[code.toUpperCase()] ?? 0) : 0;
        const color = getColorForRpm(rpm, percentiles.p33, percentiles.p66);
        return {
          fillColor: color,
          fillOpacity: 0.65,
          strokeColor: "#1e293b",
          strokeWeight: 1,
        };
      });

      dataLayer.addListener("mouseover", (e: google.maps.Data.MouseEvent) => {
        const name = e.feature.getProperty("name") as string | undefined;
        const code = name ? STATE_NAME_TO_ABBR[name] ?? name : "";
        setHoveredState(code ? { code, name: name ?? code } : null);
        dataLayer.overrideStyle(e.feature, HOVER_STYLE);
      });

      dataLayer.addListener("mouseout", () => {
        setHoveredState(null);
        dataLayer.revertStyle();
      });

      dataLayer.addListener("click", (e: google.maps.Data.MouseEvent) => {
        e.stop();
        const name = e.feature.getProperty("name") as string | undefined;
        const code = name ? STATE_NAME_TO_ABBR[name] ?? name : "";
        if (code) {
          setSelectedState({ code, name: name ?? code });
          const center = name && STATE_CENTERS[name];
          if (center) {
            mapInstance.panTo(center);
          }
        }
      });
    },
    [heatmapRpm, percentiles]
  );

  // Re-apply style when heatmap data arrives (map loads first, then data fetches)
  useEffect(() => {
    const dl = dataLayerRef.current;
    if (!dl || Object.keys(heatmapRpm).length === 0) return;
    dl.setStyle((feature: google.maps.Data.Feature) => {
      const name = feature.getProperty("name") as string | undefined;
      const code = name ? STATE_NAME_TO_ABBR[name] ?? "" : "";
      const rpm = code ? (heatmapRpm[code.toUpperCase()] ?? 0) : 0;
      const color = getColorForRpm(rpm, percentiles.p33, percentiles.p66);
      return {
        fillColor: color,
        fillOpacity: 0.65,
        strokeColor: "#1e293b",
        strokeWeight: 1,
      };
    });
  }, [heatmapRpm, percentiles]);

  if (loadError) {
    return (
      <div className="rounded-xl bg-slate-700/50 border border-slate-600 p-6 text-center text-slate-400">
        Failed to load map. Check your API key.
      </div>
    );
  }

  if (!isLoaded) {
    return (
      <div className="rounded-xl bg-slate-700/50 border border-slate-600 p-6 text-center text-slate-400">
        Loading map...
      </div>
    );
  }

  return (
    <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 overflow-hidden">
      <div style={mapContainerStyle} className="relative">
        {hoveredState && (
          <div className="absolute top-3 left-1/2 -translate-x-1/2 z-10 px-3 py-2 rounded-lg bg-slate-900/95 border border-slate-600 text-sm text-slate-200 pointer-events-none shadow-lg">
            <span className="font-medium">{hoveredState.name} ({hoveredState.code})</span>
            {heatmapRpm[hoveredState.code.toUpperCase()] != null && heatmapRpm[hoveredState.code.toUpperCase()] > 0 && (
              <span className="text-emerald-400 ml-2">
                ${heatmapRpm[hoveredState.code.toUpperCase()].toFixed(2)}/mi
              </span>
            )}
          </div>
        )}
        <div className="absolute bottom-3 left-3 right-3 z-10 flex flex-wrap gap-2 justify-center pointer-events-none">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900/90 text-xs text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: HEATMAP_COLORS.high }} />
            High
          </span>
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900/90 text-xs text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: HEATMAP_COLORS.medium }} />
            Medium
          </span>
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900/90 text-xs text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: HEATMAP_COLORS.low }} />
            Low
          </span>
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900/90 text-xs text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: HEATMAP_COLORS.noData }} />
            No data
          </span>
        </div>
        <GoogleMap
          mapContainerStyle={mapContainerStyle}
          center={defaultCenter}
          zoom={defaultZoom}
          onLoad={onMapLoad}
          options={{
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: true,
            zoomControl: true,
            disableDefaultUI: false,
          }}
        />
      </div>
      {selectedState && (
        <StateInsightsModal
          stateCode={selectedState.code}
          stateName={selectedState.name}
          onClose={() => setSelectedState(null)}
        />
      )}
    </div>
  );
}
