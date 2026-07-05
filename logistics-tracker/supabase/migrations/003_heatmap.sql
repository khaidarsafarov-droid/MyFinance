-- ============================================================
-- Heatmap: avg RPM per state for map coloring
-- Run after 002_crowdsourcing_analytics.sql
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_all_states_heatmap()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_result JSONB;
BEGIN
  -- Returns array of { stateCode, avgRpm } for states with data in last 14 days
  -- Uses origin_state first, falls back to state for legacy loads
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
    GROUP BY UPPER(TRIM(COALESCE(l.origin_state, l.state)))
  ) sub;

  RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_all_states_heatmap() TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_all_states_heatmap() TO service_role;
