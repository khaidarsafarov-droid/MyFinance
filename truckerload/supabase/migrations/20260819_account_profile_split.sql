-- Split User / DriverProfile / CommunityProfile (privacy boundary).
-- Community rows are readable by other authenticated users per visibility_settings.
-- Professional rows (CDL, company) are owner-only (and optional dispatcher).

create table if not exists public.driver_professional_profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  role text not null default 'OWNER_OPERATOR',
  company_name text,
  cdl_number_ciphertext text,
  cdl_document_url_ciphertext text,
  vehicle_type text not null default '',
  primary_region text not null default '',
  dispatcher_user_id uuid references auth.users (id) on delete set null,
  skipped boolean not null default false,
  updated_at timestamptz not null default now()
);

create index if not exists driver_professional_dispatcher_idx
  on public.driver_professional_profiles (dispatcher_user_id);

alter table public.driver_professional_profiles enable row level security;

drop policy if exists "driver_pro_select_own_or_dispatcher" on public.driver_professional_profiles;
create policy "driver_pro_select_own_or_dispatcher"
  on public.driver_professional_profiles for select
  to authenticated
  using (auth.uid() = user_id or auth.uid() = dispatcher_user_id);

drop policy if exists "driver_pro_write_own" on public.driver_professional_profiles;
create policy "driver_pro_write_own"
  on public.driver_professional_profiles for all
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create table if not exists public.community_profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  nickname text not null default '',
  avatar_url text,
  bio text,
  visibility_settings jsonb not null default '{"nickname":true,"bio":false,"avatar":true}'::jsonb,
  skipped boolean not null default false,
  updated_at timestamptz not null default now()
);

alter table public.community_profiles enable row level security;

drop policy if exists "community_profiles_select_authenticated" on public.community_profiles;
create policy "community_profiles_select_authenticated"
  on public.community_profiles for select
  to authenticated
  using (true);

drop policy if exists "community_profiles_write_own" on public.community_profiles;
create policy "community_profiles_write_own"
  on public.community_profiles for all
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Community API must never expose professional columns: no view joining the two tables.
create or replace view public.community_profiles_public as
select
  user_id,
  nickname,
  case when coalesce((visibility_settings->>'avatar')::boolean, true) then avatar_url else null end as avatar_url,
  case when coalesce((visibility_settings->>'bio')::boolean, false) then bio else null end as bio
from public.community_profiles;

grant select on public.community_profiles_public to authenticated;
grant select, insert, update, delete on public.driver_professional_profiles to authenticated;
grant select, insert, update, delete on public.community_profiles to authenticated;

notify pgrst, 'reload schema';
