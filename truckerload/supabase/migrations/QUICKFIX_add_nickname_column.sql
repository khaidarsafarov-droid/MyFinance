-- Quick fix for: Could not find the 'nickname' column of 'profiles' (PGRST204)
-- Prefer the full file 20260731_friend_nicknames.sql when possible.
-- Paste into Supabase → SQL Editor → Run.

alter table public.profiles
  add column if not exists nickname text;

create unique index if not exists profiles_nickname_lower_uidx
  on public.profiles (lower(nickname))
  where nickname is not null and nickname <> '';

notify pgrst, 'reload schema';
