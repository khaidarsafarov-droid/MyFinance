-- Friends live location + active route sharing (Truck Log)
-- Apply in Supabase SQL editor after profiles migration.
-- Used by SupabaseFriendsRealtimeService (REST; Realtime optional on these tables).

-- Who follows whom (cross-device). Local Room driver_follows remains a cache.
create table if not exists public.friendships (
  follower_id uuid not null references auth.users (id) on delete cascade,
  followee_id uuid not null references auth.users (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, followee_id),
  check (follower_id <> followee_id)
);

create index if not exists friendships_followee_idx on public.friendships (followee_id);

alter table public.friendships enable row level security;

drop policy if exists "friendships_select_involved" on public.friendships;
create policy "friendships_select_involved"
  on public.friendships for select to authenticated
  using (auth.uid() = follower_id or auth.uid() = followee_id);

drop policy if exists "friendships_insert_own" on public.friendships;
create policy "friendships_insert_own"
  on public.friendships for insert to authenticated
  with check (auth.uid() = follower_id);

drop policy if exists "friendships_delete_own" on public.friendships;
create policy "friendships_delete_own"
  on public.friendships for delete to authenticated
  using (auth.uid() = follower_id);

-- Live GPS presence (only while share_path_enabled = true on client).
create table if not exists public.driver_presence (
  user_id uuid primary key references auth.users (id) on delete cascade,
  display_name text,
  latitude double precision not null,
  longitude double precision not null,
  heading double precision,
  speed_mps double precision,
  share_path_enabled boolean not null default true,
  updated_at timestamptz not null default now()
);

create index if not exists driver_presence_updated_idx
  on public.driver_presence (updated_at desc);

alter table public.driver_presence enable row level security;

-- Owner full control
drop policy if exists "presence_upsert_own" on public.driver_presence;
create policy "presence_upsert_own"
  on public.driver_presence for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Friends can read presence only when the peer is sharing
drop policy if exists "presence_select_friends" on public.driver_presence;
create policy "presence_select_friends"
  on public.driver_presence for select to authenticated
  using (
    auth.uid() = user_id
    or (
      share_path_enabled = true
      and exists (
        select 1 from public.friendships f
        where f.follower_id = auth.uid()
          and f.followee_id = driver_presence.user_id
      )
    )
  );

-- Active route share for map polylines (anonymized enough: no trip id required publicly).
create table if not exists public.active_route_shares (
  user_id uuid primary key references auth.users (id) on delete cascade,
  load_ref text,
  origin_label text not null default '',
  destination_label text not null default '',
  origin_lat double precision,
  origin_lng double precision,
  dest_lat double precision,
  dest_lng double precision,
  start_date date not null,
  end_date date not null,
  status text not null default 'active'
    check (status in ('active', 'completed', 'cancelled')),
  track_points jsonb not null default '[]'::jsonb,
  -- [{lat,lng,t}] chronological GPS crumbs while sharing
  share_path_enabled boolean not null default true,
  updated_at timestamptz not null default now()
);

create index if not exists active_route_shares_dates_idx
  on public.active_route_shares (start_date, end_date)
  where status = 'active';

alter table public.active_route_shares enable row level security;

drop policy if exists "routes_upsert_own" on public.active_route_shares;
create policy "routes_upsert_own"
  on public.active_route_shares for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "routes_select_friends" on public.active_route_shares;
create policy "routes_select_friends"
  on public.active_route_shares for select to authenticated
  using (
    auth.uid() = user_id
    or (
      share_path_enabled = true
      and status = 'active'
      and exists (
        select 1 from public.friendships f
        where f.follower_id = auth.uid()
          and f.followee_id = active_route_shares.user_id
      )
    )
  );

grant select, insert, update, delete on public.friendships to authenticated;
grant select, insert, update, delete on public.driver_presence to authenticated;
grant select, insert, update, delete on public.active_route_shares to authenticated;

-- Optional: enable Realtime for these tables in Dashboard → Database → Replication
-- alter publication supabase_realtime add table public.driver_presence;
-- alter publication supabase_realtime add table public.active_route_shares;

notify pgrst, 'reload schema';
