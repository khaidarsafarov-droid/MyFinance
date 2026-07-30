-- Nicknames + per-friend share settings (Truck Log friends on map)
-- Apply after 20260730_friends_live_routes.sql

-- Unique handle for search / add-friend
alter table public.profiles
  add column if not exists nickname text;

create unique index if not exists profiles_nickname_lower_uidx
  on public.profiles (lower(nickname))
  where nickname is not null and nickname <> '';

-- Allow authenticated users to look up public nickname + display name (not email/phone)
drop policy if exists "profiles_select_nickname_public" on public.profiles;
create policy "profiles_select_nickname_public"
  on public.profiles for select to authenticated
  using (
    auth.uid() = id
    or (nickname is not null and nickname <> '')
  );

-- Search RPC (exact match, case-insensitive)
create or replace function public.search_profile_by_nickname(p_nickname text)
returns table (
  user_id uuid,
  nickname text,
  full_name text
)
language sql
security definer
set search_path = public
as $$
  select p.id, p.nickname, p.full_name
  from public.profiles p
  where p.nickname is not null
    and lower(p.nickname) = lower(trim(p_nickname))
  limit 1;
$$;

grant execute on function public.search_profile_by_nickname(text) to authenticated;

-- Who I share with + what I share (owner = me, friend = them)
create table if not exists public.friend_links (
  owner_id uuid not null references auth.users (id) on delete cascade,
  friend_id uuid not null references auth.users (id) on delete cascade,
  friend_nickname text not null default '',
  friend_display_name text not null default '',
  share_my_location boolean not null default true,
  share_my_route boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (owner_id, friend_id),
  check (owner_id <> friend_id)
);

create index if not exists friend_links_friend_idx on public.friend_links (friend_id);

alter table public.friend_links enable row level security;

drop policy if exists "friend_links_own" on public.friend_links;
create policy "friend_links_own"
  on public.friend_links for all to authenticated
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

-- Friend can read the row that grants them access (to know prefs exist)
drop policy if exists "friend_links_select_as_friend" on public.friend_links;
create policy "friend_links_select_as_friend"
  on public.friend_links for select to authenticated
  using (auth.uid() = friend_id);

grant select, insert, update, delete on public.friend_links to authenticated;

-- Tighten presence: follower + owner grants share_my_location
drop policy if exists "presence_select_friends" on public.driver_presence;
create policy "presence_select_friends"
  on public.driver_presence for select to authenticated
  using (
    auth.uid() = user_id
    or (
      share_path_enabled = true
      and exists (
        select 1 from public.friend_links fl
        where fl.owner_id = driver_presence.user_id
          and fl.friend_id = auth.uid()
          and fl.share_my_location = true
      )
    )
  );

drop policy if exists "routes_select_friends" on public.active_route_shares;
create policy "routes_select_friends"
  on public.active_route_shares for select to authenticated
  using (
    auth.uid() = user_id
    or (
      share_path_enabled = true
      and status = 'active'
      and exists (
        select 1 from public.friend_links fl
        where fl.owner_id = active_route_shares.user_id
          and fl.friend_id = auth.uid()
          and fl.share_my_route = true
      )
    )
  );

-- Keep friendships in sync helpers: followee visibility still useful for mutual graph
-- (optional insert when adding friend from app)

notify pgrst, 'reload schema';
