-- ============================================================
-- Logistics Tracker: Multi-user schema for Supabase
-- Запустите в Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- ============================================================

-- 1. Таблица профилей (связана с auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
  id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
  first_name TEXT,
  last_name TEXT,
  truck_number TEXT,
  company_name TEXT,
  updated_at TIMESTAMPTZ DEFAULT (now() AT TIME ZONE 'utc')
);

-- 2. RLS для profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Пользователи могут смотреть только свой профиль"
  ON public.profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "Пользователи могут обновлять только свой профиль"
  ON public.profiles FOR UPDATE
  USING (auth.uid() = id);

-- 3. Триггер: автосоздание профиля при регистрации (Email или Google)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, first_name, last_name)
  VALUES (
    NEW.id,
    COALESCE(
      NEW.raw_user_meta_data->>'first_name',
      NEW.raw_user_meta_data->>'given_name'
    ),
    COALESCE(
      NEW.raw_user_meta_data->>'last_name',
      NEW.raw_user_meta_data->>'family_name'
    )
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- ============================================================
-- Таблицы для грузов, компаний (per user)
-- ============================================================

-- Companies
CREATE TABLE IF NOT EXISTS public.companies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  is_current BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, name)
);

-- Company changes (история смен компании)
CREATE TABLE IF NOT EXISTS public.company_changes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  date TIMESTAMPTZ NOT NULL,
  company_id UUID NOT NULL REFERENCES public.companies(id) ON DELETE CASCADE,
  company_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Loads (грузы)
CREATE TABLE IF NOT EXISTS public.loads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  date TIMESTAMPTZ NOT NULL,
  gross NUMERIC(12,2) NOT NULL,
  profit NUMERIC(12,2) NOT NULL,
  diesel NUMERIC(12,2) NOT NULL,
  company_id UUID NOT NULL REFERENCES public.companies(id) ON DELETE CASCADE,
  state TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_companies_user_id ON public.companies(user_id);
CREATE INDEX IF NOT EXISTS idx_company_changes_user_id ON public.company_changes(user_id);
CREATE INDEX IF NOT EXISTS idx_loads_user_id ON public.loads(user_id);
CREATE INDEX IF NOT EXISTS idx_loads_date ON public.loads(date);

-- ============================================================
-- RLS: каждый водитель видит только свои данные
-- ============================================================

ALTER TABLE public.companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.company_changes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loads ENABLE ROW LEVEL SECURITY;

-- Companies
CREATE POLICY "Users can view own companies"
  ON public.companies FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own companies"
  ON public.companies FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own companies"
  ON public.companies FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own companies"
  ON public.companies FOR DELETE USING (auth.uid() = user_id);

-- Company changes
CREATE POLICY "Users can view own company_changes"
  ON public.company_changes FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own company_changes"
  ON public.company_changes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own company_changes"
  ON public.company_changes FOR DELETE USING (auth.uid() = user_id);

-- Loads
CREATE POLICY "Users can view own loads"
  ON public.loads FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own loads"
  ON public.loads FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own loads"
  ON public.loads FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own loads"
  ON public.loads FOR DELETE USING (auth.uid() = user_id);
