-- Add RPM profitability thresholds to profiles (per-user settings)
-- Red threshold: below = unprofitable (red). Green threshold: above = high profit (green). Between = medium (orange).

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS rpm_min_threshold NUMERIC(6,2) DEFAULT 2.00,
  ADD COLUMN IF NOT EXISTS rpm_target_threshold NUMERIC(6,2) DEFAULT 2.50;

COMMENT ON COLUMN public.profiles.rpm_min_threshold IS 'Red threshold $/mi - below this is unprofitable';
COMMENT ON COLUMN public.profiles.rpm_target_threshold IS 'Green threshold $/mi - above this is high profit';
