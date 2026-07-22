-- TruckerLoad auth schema for Supabase
-- Project: jsptulbjtesnphrbxsil
-- Required by SupabaseAuthService (profiles + check_registration RPC)

create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  full_name text,
  phone_number text,
  email text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists profiles_email_lower_uidx
  on public.profiles (lower(email))
  where email is not null and email <> '';

create unique index if not exists profiles_phone_uidx
  on public.profiles (phone_number)
  where phone_number is not null and phone_number <> '';

alter table public.profiles enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
  on public.profiles for select
  to authenticated
  using (auth.uid() = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
  on public.profiles for insert
  to authenticated
  with check (auth.uid() = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

grant select, insert, update on public.profiles to authenticated;
grant select on public.profiles to anon;

-- Auto-create profile row on signup / Google sign-in
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, phone_number, email)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name'),
    new.raw_user_meta_data->>'phone_number',
    new.email
  )
  on conflict (id) do update set
    full_name = coalesce(excluded.full_name, public.profiles.full_name),
    phone_number = coalesce(excluded.phone_number, public.profiles.phone_number),
    email = coalesce(excluded.email, public.profiles.email),
    updated_at = now();
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- App calls: POST /rest/v1/rpc/check_registration  { p_email, p_phone }
create or replace function public.check_registration(p_email text, p_phone text)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  email_norm text := lower(trim(coalesce(p_email, '')));
  phone_norm text := trim(coalesce(p_phone, ''));
  email_taken boolean := false;
  phone_taken boolean := false;
begin
  if email_norm <> '' then
    select exists (
      select 1 from auth.users u where lower(u.email) = email_norm
    ) or exists (
      select 1 from public.profiles p where lower(p.email) = email_norm
    ) into email_taken;
  end if;

  if phone_norm <> '' then
    select exists (
      select 1 from public.profiles p
      where p.phone_number = phone_norm
         or regexp_replace(coalesce(p.phone_number, ''), '[^0-9]', '', 'g')
            = regexp_replace(phone_norm, '[^0-9]', '', 'g')
    ) into phone_taken;
  end if;

  return json_build_object(
    'email_taken', email_taken,
    'phone_taken', phone_taken
  );
end;
$$;

revoke all on function public.check_registration(text, text) from public;
grant execute on function public.check_registration(text, text) to anon, authenticated, service_role;

-- Backfill profiles for any existing auth users
insert into public.profiles (id, full_name, phone_number, email)
select
  u.id,
  coalesce(u.raw_user_meta_data->>'full_name', u.raw_user_meta_data->>'name'),
  u.raw_user_meta_data->>'phone_number',
  u.email
from auth.users u
on conflict (id) do nothing;

notify pgrst, 'reload schema';
