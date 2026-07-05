-- ============================================================
-- Equipment Type segmentation: profiles + loads
-- Run after 002_crowdsourcing_analytics.sql
-- ============================================================

-- 1. Add equipment_type to profiles
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS equipment_type TEXT;

-- 2. Add equipment_type to loads
ALTER TABLE public.loads
  ADD COLUMN IF NOT EXISTS equipment_type TEXT;

CREATE INDEX IF NOT EXISTS idx_loads_equipment_type ON public.loads(equipment_type);

-- 3. Update get_state_analytics: add p_equipment_type, filter main stats, add comparative block
CREATE OR REPLACE FUNCTION public.get_state_analytics(
  p_state_code TEXT,
  p_equipment_type TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_state TEXT := UPPER(TRIM(p_state_code));
  v_equip TEXT := NULLIF(TRIM(p_equipment_type), '');
  v_result JSONB;
  v_avg_rpm NUMERIC;
  v_top_dest TEXT;
  v_top_dest_count BIGINT;
  v_last_load_at TIMESTAMPTZ;
  v_last_load_origin TEXT;
  v_last_load_dest TEXT;
  v_last_load_rpm NUMERIC;
  v_last_load_equip TEXT;
  v_recency_hours NUMERIC;
  v_recency_text TEXT;
  v_is_fresh BOOLEAN;
  v_rpm_by_equip JSONB;
BEGIN
  -- Main stats: filtered by equipment_type when provided
  SELECT COALESCE(AVG(COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0)), 0)
  INTO v_avg_rpm
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.created_at >= NOW() - INTERVAL '14 days'
    AND l.miles IS NOT NULL AND l.miles > 0
    AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip));

  IF v_avg_rpm IS NULL OR v_avg_rpm = 0 THEN
    SELECT COALESCE(AVG(l.gross / NULLIF(l.miles, 0)), 0)
    INTO v_avg_rpm
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.created_at >= NOW() - INTERVAL '14 days'
      AND l.miles IS NOT NULL AND l.miles > 0
      AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip));
  END IF;

  -- Top destination (filtered)
  SELECT l.destination_state, COUNT(*) INTO v_top_dest, v_top_dest_count
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.destination_state IS NOT NULL AND TRIM(l.destination_state) <> ''
    AND l.created_at >= NOW() - INTERVAL '14 days'
    AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip))
  GROUP BY l.destination_state
  ORDER BY COUNT(*) DESC
  LIMIT 1;

  IF v_top_dest IS NULL THEN
    SELECT l.state, COUNT(*) INTO v_top_dest, v_top_dest_count
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.created_at >= NOW() - INTERVAL '14 days'
      AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip))
    GROUP BY l.state
    LIMIT 1;
  END IF;

  -- Last load (filtered, include equipment for display)
  SELECT l.created_at, l.origin_state, l.destination_state,
         (COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0)),
         COALESCE(l.equipment_type, '')
  INTO v_last_load_at, v_last_load_origin, v_last_load_dest, v_last_load_rpm, v_last_load_equip
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.miles IS NOT NULL AND l.miles > 0
    AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip))
  ORDER BY l.created_at DESC
  LIMIT 1;

  IF v_last_load_at IS NULL THEN
    SELECT l.created_at, l.state, l.state, (l.gross / NULLIF(l.miles, 0)), COALESCE(l.equipment_type, '')
    INTO v_last_load_at, v_last_load_origin, v_last_load_dest, v_last_load_rpm, v_last_load_equip
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.miles IS NOT NULL AND l.miles > 0
      AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip))
    ORDER BY l.created_at DESC
    LIMIT 1;
  END IF;

  -- Recency
  IF v_last_load_at IS NOT NULL THEN
    v_recency_hours := EXTRACT(EPOCH FROM (NOW() - v_last_load_at)) / 3600;
    IF v_recency_hours < 1 THEN
      v_recency_text := 'less than 1h ago';
    ELSIF v_recency_hours < 24 THEN
      v_recency_text := ROUND(v_recency_hours)::TEXT || 'h ago';
    ELSE
      v_recency_text := ROUND(v_recency_hours / 24)::TEXT || ' days ago';
    END IF;
    v_is_fresh := v_recency_hours < 24;
  ELSE
    v_recency_text := 'no data';
    v_is_fresh := FALSE;
  END IF;

  -- Comparative: avg RPM by equipment type for this state (last 14 days)
  SELECT COALESCE(jsonb_object_agg(equip_key, rpm_val), '{}'::jsonb)
  INTO v_rpm_by_equip
  FROM (
    SELECT
      COALESCE(NULLIF(TRIM(l.equipment_type), ''), 'Unknown') AS equip_key,
      ROUND(AVG(COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0))::NUMERIC, 2) AS rpm_val
    FROM public.loads l
    WHERE (UPPER(TRIM(l.origin_state)) = v_state OR UPPER(TRIM(l.state)) = v_state)
      AND l.created_at >= NOW() - INTERVAL '14 days'
      AND l.miles IS NOT NULL AND l.miles > 0
      AND (l.origin_state IS NOT NULL OR l.state IS NOT NULL)
    GROUP BY COALESCE(NULLIF(TRIM(l.equipment_type), ''), 'Unknown')
  ) sub;

  v_result := jsonb_build_object(
    'avgRpm', COALESCE(v_avg_rpm, 0),
    'topDestination', COALESCE(v_top_dest, '-'),
    'topDestinationCount', COALESCE(v_top_dest_count, 0),
    'lastLoadAt', v_last_load_at,
    'lastLoadRecency', v_recency_text,
    'lastLoadEquipment', COALESCE(v_last_load_equip, ''),
    'lastLoadExample', CASE
      WHEN v_last_load_at IS NOT NULL AND v_last_load_origin IS NOT NULL AND v_last_load_dest IS NOT NULL THEN
        v_recency_text || ': ' || COALESCE(v_last_load_origin, '?') || ' → ' || COALESCE(v_last_load_dest, '?') ||
        ' @ $' || ROUND(COALESCE(v_last_load_rpm, 0)::NUMERIC, 2) || '/mile'
      ELSE NULL
    END,
    'isDataFresh', v_is_fresh,
    'rpmByEquipment', v_rpm_by_equip,
    'userEquipmentType', v_equip
  );

  RETURN v_result;
END;
$$;

-- 4. Update get_all_states_heatmap: add p_equipment_type
CREATE OR REPLACE FUNCTION public.get_all_states_heatmap(p_equipment_type TEXT DEFAULT NULL)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_equip TEXT := NULLIF(TRIM(p_equipment_type), '');
  v_result JSONB;
BEGIN
  SELECT COALESCE(jsonb_agg(
    jsonb_build_object('stateCode', UPPER(TRIM(state_key)), 'avgRpm', ROUND(avg_rpm::NUMERIC, 2))
  ), '[]'::jsonb)
  INTO v_result
  FROM (
    SELECT
      COALESCE(l.origin_state, l.state) AS state_key,
      AVG(COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0)) AS avg_rpm
    FROM public.loads l
    WHERE l.created_at >= NOW() - INTERVAL '14 days'
      AND l.miles IS NOT NULL AND l.miles > 0
      AND (l.origin_state IS NOT NULL OR l.state IS NOT NULL)
      AND TRIM(COALESCE(l.origin_state, l.state)) <> ''
      AND (v_equip IS NULL OR UPPER(TRIM(COALESCE(l.equipment_type, ''))) = UPPER(v_equip))
    GROUP BY UPPER(TRIM(COALESCE(l.origin_state, l.state)))
  ) sub;

  RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_state_analytics(TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_state_analytics(TEXT, TEXT) TO service_role;