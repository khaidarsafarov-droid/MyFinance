-- ============================================================
-- Crowdsourcing Analytics: Route-based stats for map insights
-- Run in Supabase SQL Editor after 001_initial_schema.sql
-- ============================================================

-- 1. Add route columns to loads (backward compatible)
ALTER TABLE public.loads
  ADD COLUMN IF NOT EXISTS origin_state TEXT,
  ADD COLUMN IF NOT EXISTS destination_state TEXT,
  ADD COLUMN IF NOT EXISTS miles NUMERIC(10,2),
  ADD COLUMN IF NOT EXISTS total_rate NUMERIC(12,2);

-- Backfill: for existing loads with state, use as destination_state
UPDATE public.loads
SET destination_state = UPPER(TRIM(state)), total_rate = gross
WHERE state IS NOT NULL AND destination_state IS NULL AND total_rate IS NULL;

-- Indexes for analytics queries
CREATE INDEX IF NOT EXISTS idx_loads_origin_state ON public.loads(origin_state);
CREATE INDEX IF NOT EXISTS idx_loads_destination_state ON public.loads(destination_state);
CREATE INDEX IF NOT EXISTS idx_loads_created_at ON public.loads(created_at);

-- 2. Function: get_state_analytics (returns aggregated data for map)
-- SECURITY DEFINER allows reading ALL loads for aggregates (bypasses RLS)
CREATE OR REPLACE FUNCTION public.get_state_analytics(p_state_code TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_state TEXT := UPPER(TRIM(p_state_code));
  v_result JSONB;
  v_avg_rpm NUMERIC;
  v_top_dest TEXT;
  v_top_dest_count BIGINT;
  v_last_load_at TIMESTAMPTZ;
  v_last_load_origin TEXT;
  v_last_load_dest TEXT;
  v_last_load_rpm NUMERIC;
  v_recency_hours NUMERIC;
  v_recency_text TEXT;
  v_is_fresh BOOLEAN;
BEGIN
  -- Avg RPM: loads where origin_state = state, last 14 days, with miles > 0
  -- Use total_rate or gross (legacy)
  SELECT COALESCE(AVG(COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0)), 0)
  INTO v_avg_rpm
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.created_at >= NOW() - INTERVAL '14 days'
    AND l.miles IS NOT NULL AND l.miles > 0;

  -- Legacy: state column (single state) — treat as origin for RPM
  IF v_avg_rpm IS NULL OR v_avg_rpm = 0 THEN
    SELECT COALESCE(AVG(l.gross / NULLIF(l.miles, 0)), 0)
    INTO v_avg_rpm
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.created_at >= NOW() - INTERVAL '14 days'
      AND l.miles IS NOT NULL AND l.miles > 0;
  END IF;

  -- Top destination from this state (origin_state = state)
  SELECT l.destination_state, COUNT(*) INTO v_top_dest, v_top_dest_count
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.destination_state IS NOT NULL AND TRIM(l.destination_state) <> ''
    AND l.created_at >= NOW() - INTERVAL '14 days'
  GROUP BY l.destination_state
  ORDER BY COUNT(*) DESC
  LIMIT 1;

  -- Legacy: state column as "destination" - for top dest we need origin->destination
  IF v_top_dest IS NULL THEN
    SELECT l.state, COUNT(*) INTO v_top_dest, v_top_dest_count
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.created_at >= NOW() - INTERVAL '14 days'
    GROUP BY l.state
    LIMIT 1;
  END IF;

  -- Last load from this state (as origin)
  SELECT l.created_at, l.origin_state, l.destination_state, (COALESCE(l.total_rate, l.gross) / NULLIF(l.miles, 0))
  INTO v_last_load_at, v_last_load_origin, v_last_load_dest, v_last_load_rpm
  FROM public.loads l
  WHERE UPPER(TRIM(l.origin_state)) = v_state
    AND l.miles IS NOT NULL AND l.miles > 0
  ORDER BY l.created_at DESC
  LIMIT 1;

  -- Fallback: legacy state as destination (load ended in state)
  IF v_last_load_at IS NULL THEN
    SELECT l.created_at, l.state, l.state, (l.gross / NULLIF(l.miles, 0))
    INTO v_last_load_at, v_last_load_origin, v_last_load_dest, v_last_load_rpm
    FROM public.loads l
    WHERE UPPER(TRIM(l.state)) = v_state
      AND l.miles IS NOT NULL AND l.miles > 0
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

  -- Build last load example string
  v_result := jsonb_build_object(
    'avgRpm', COALESCE(v_avg_rpm, 0),
    'topDestination', COALESCE(v_top_dest, '-'),
    'topDestinationCount', COALESCE(v_top_dest_count, 0),
    'lastLoadAt', v_last_load_at,
    'lastLoadRecency', v_recency_text,
    'lastLoadExample', CASE
      WHEN v_last_load_at IS NOT NULL AND v_last_load_origin IS NOT NULL AND v_last_load_dest IS NOT NULL THEN
        v_recency_text || ': ' || COALESCE(v_last_load_origin, '?') || ' → ' || COALESCE(v_last_load_dest, '?') || ' @ $' || ROUND(COALESCE(v_last_load_rpm, 0)::NUMERIC, 2) || '/mile'
      ELSE NULL
    END,
    'isDataFresh', v_is_fresh
  );

  RETURN v_result;
END;
$$;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.get_state_analytics(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_state_analytics(TEXT) TO service_role;
