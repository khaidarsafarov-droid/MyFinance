-- TruckerLoad Community (chats, groups, weekly stats, status, voice)
-- Apply in Supabase SQL editor AFTER friend nickname migrations.
-- Safe to re-run: tables use IF NOT EXISTS, functions are CREATE OR REPLACE.
-- Dashboard → Database → Replication: add community_messages, community_voice_signals, community_calls.

create table if not exists public.community_blocks (
  blocker_id uuid not null references auth.users (id) on delete cascade,
  blocked_id uuid not null references auth.users (id) on delete cascade,
  blocked_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  check (blocker_id <> blocked_id)
);

alter table public.community_blocks enable row level security;

drop policy if exists "community_blocks_own" on public.community_blocks;
create policy "community_blocks_own"
  on public.community_blocks for all to authenticated
  using (auth.uid() = blocker_id)
  with check (auth.uid() = blocker_id);

grant select, insert, delete on public.community_blocks to authenticated;

create or replace function public.are_community_peers(a uuid, b uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select a is not null and b is not null and a <> b
    and not exists (
      select 1 from public.community_blocks bl
      where (bl.blocker_id = a and bl.blocked_id = b)
         or (bl.blocker_id = b and bl.blocked_id = a)
    )
    and exists (
      select 1 from public.friend_links fl
      where (fl.owner_id = a and fl.friend_id = b)
         or (fl.owner_id = b and fl.friend_id = a)
    );
$$;

grant execute on function public.are_community_peers(uuid, uuid) to authenticated;

create table if not exists public.community_chats (
  id uuid primary key default gen_random_uuid(),
  type text not null check (type in ('PRIVATE', 'GROUP')),
  title text not null default '',
  invite_code text,
  creator_id uuid references auth.users (id) on delete set null,
  category text not null default '',
  is_public boolean not null default false,
  last_message text not null default '',
  last_message_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create unique index if not exists community_chats_invite_code_uidx
  on public.community_chats (invite_code)
  where invite_code is not null and invite_code <> '';

alter table public.community_chats enable row level security;

create table if not exists public.community_chat_members (
  chat_id uuid not null references public.community_chats (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  display_name text not null default '',
  role text not null default 'MEMBER',
  joined_at timestamptz not null default now(),
  primary key (chat_id, user_id)
);

create index if not exists community_chat_members_user_idx
  on public.community_chat_members (user_id);

alter table public.community_chat_members enable row level security;

create table if not exists public.community_messages (
  id uuid primary key default gen_random_uuid(),
  chat_id uuid not null references public.community_chats (id) on delete cascade,
  sender_id uuid not null references auth.users (id) on delete cascade,
  sender_name text not null default '',
  body text not null default '',
  message_type text not null default 'TEXT',
  attachment_url text,
  reply_to_id uuid,
  location_label text,
  duration_ms bigint not null default 0,
  sent_at timestamptz not null default now()
);

create index if not exists community_messages_chat_sent_idx
  on public.community_messages (chat_id, sent_at);

alter table public.community_messages enable row level security;

create table if not exists public.community_reactions (
  message_id uuid not null references public.community_messages (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  reaction text not null,
  reacted_at timestamptz not null default now(),
  primary key (message_id, user_id, reaction)
);

alter table public.community_reactions enable row level security;

-- SECURITY DEFINER so RLS on community_chat_members does not recurse
-- when chats/messages policies ask "is the current user a member?".
create or replace function public.is_community_chat_member(p_chat uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select p_chat is not null
     and auth.uid() is not null
     and exists (
       select 1 from public.community_chat_members
       where chat_id = p_chat and user_id = auth.uid()
     );
$$;

grant execute on function public.is_community_chat_member(uuid) to authenticated;

drop policy if exists "community_chats_member_select" on public.community_chats;
create policy "community_chats_member_select"
  on public.community_chats for select to authenticated
  using (
    public.is_community_chat_member(id)
    or (type = 'GROUP' and is_public = true)
  );

drop policy if exists "community_chats_creator_update" on public.community_chats;
create policy "community_chats_creator_update"
  on public.community_chats for update to authenticated
  using (public.is_community_chat_member(id));

drop policy if exists "community_members_select" on public.community_chat_members;
create policy "community_members_select"
  on public.community_chat_members for select to authenticated
  using (user_id = auth.uid() or public.is_community_chat_member(chat_id));

drop policy if exists "community_members_self" on public.community_chat_members;
create policy "community_members_self"
  on public.community_chat_members for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists "community_messages_member_select" on public.community_messages;
create policy "community_messages_member_select"
  on public.community_messages for select to authenticated
  using (public.is_community_chat_member(chat_id));

drop policy if exists "community_messages_member_insert" on public.community_messages;
create policy "community_messages_member_insert"
  on public.community_messages for insert to authenticated
  with check (
    sender_id = auth.uid()
    and public.is_community_chat_member(chat_id)
  );

drop policy if exists "community_messages_member_update" on public.community_messages;
create policy "community_messages_member_update"
  on public.community_messages for update to authenticated
  using (sender_id = auth.uid())
  with check (sender_id = auth.uid());

drop policy if exists "community_reactions_member" on public.community_reactions;
create policy "community_reactions_member"
  on public.community_reactions for all to authenticated
  using (
    exists (
      select 1 from public.community_messages msg
      where msg.id = community_reactions.message_id
        and public.is_community_chat_member(msg.chat_id)
    )
  )
  with check (user_id = auth.uid());

grant select, insert, update on public.community_chats to authenticated;
grant select, insert, delete on public.community_chat_members to authenticated;
grant select, insert, update on public.community_messages to authenticated;
grant select, insert, delete on public.community_reactions to authenticated;

create or replace function public.create_or_get_dm(p_peer uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  existing uuid;
  new_id uuid;
  peer_name text;
  my_name text;
begin
  if me is null or p_peer is null or me = p_peer then
    raise exception 'invalid dm';
  end if;
  if not public.are_community_peers(me, p_peer) then
    raise exception 'not peers';
  end if;
  select c.id into existing
  from public.community_chats c
  join public.community_chat_members a on a.chat_id = c.id and a.user_id = me
  join public.community_chat_members b on b.chat_id = c.id and b.user_id = p_peer
  where c.type = 'PRIVATE'
  limit 1;
  if existing is not null then
    return existing;
  end if;
  new_id := gen_random_uuid();
  select coalesce(nullif(p.nickname, ''), nullif(p.full_name, ''), 'Driver')
    into peer_name from public.profiles p where p.id = p_peer;
  select coalesce(nullif(p.nickname, ''), nullif(p.full_name, ''), 'Driver')
    into my_name from public.profiles p where p.id = me;
  insert into public.community_chats (id, type, title, creator_id, is_public)
  values (new_id, 'PRIVATE', coalesce(peer_name, 'Chat'), me, false);
  insert into public.community_chat_members (chat_id, user_id, display_name, role)
  values
    (new_id, me, coalesce(my_name, 'You'), 'MEMBER'),
    (new_id, p_peer, coalesce(peer_name, 'Driver'), 'MEMBER');
  return new_id;
end;
$$;

grant execute on function public.create_or_get_dm(uuid) to authenticated;

create or replace function public.create_community_group(p_title text, p_category text default '')
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  new_id uuid := gen_random_uuid();
  code text;
  my_name text;
begin
  if me is null then
    raise exception 'not authenticated';
  end if;
  code := upper(substr(replace(new_id::text, '-', ''), 1, 8));
  select coalesce(nullif(p.nickname, ''), nullif(p.full_name, ''), 'You')
    into my_name from public.profiles p where p.id = me;
    insert into public.community_chats (
    id, type, title, invite_code, creator_id, category, is_public
  ) values (
    new_id, 'GROUP', coalesce(nullif(trim(p_title), ''), 'Group'),
    code, me, coalesce(p_category, ''), false
  );
  insert into public.community_chat_members (chat_id, user_id, display_name, role)
  values (new_id, me, coalesce(my_name, 'You'), 'OWNER');
  return new_id;
end;
$$;

grant execute on function public.create_community_group(text, text) to authenticated;

create or replace function public.join_group_by_invite(p_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  chat uuid;
  my_name text;
begin
  if me is null then
    raise exception 'not authenticated';
  end if;
  select id into chat
  from public.community_chats
  where type = 'GROUP' and upper(invite_code) = upper(trim(p_code))
  limit 1;
  if chat is null then
    raise exception 'code not found';
  end if;
  select coalesce(nullif(p.nickname, ''), nullif(p.full_name, ''), 'You')
    into my_name from public.profiles p where p.id = me;
  insert into public.community_chat_members (chat_id, user_id, display_name, role)
  values (chat, me, coalesce(my_name, 'You'), 'MEMBER')
  on conflict (chat_id, user_id) do nothing;
  update public.community_chats
    set last_message_at = now()
    where id = chat;
  return chat;
end;
$$;

grant execute on function public.join_group_by_invite(text) to authenticated;

alter table public.profiles
  add column if not exists share_weekly_stats boolean not null default false;

create table if not exists public.community_weekly_stats (
  user_id uuid not null references auth.users (id) on delete cascade,
  iso_year int not null,
  iso_week int not null,
  miles double precision not null default 0,
  loads int not null default 0,
  revenue double precision not null default 0,
  rpm double precision not null default 0,
  share_enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (user_id, iso_year, iso_week)
);

alter table public.community_weekly_stats enable row level security;

drop policy if exists "community_stats_own" on public.community_weekly_stats;
create policy "community_stats_own"
  on public.community_weekly_stats for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "community_stats_peers_select" on public.community_weekly_stats;
create policy "community_stats_peers_select"
  on public.community_weekly_stats for select to authenticated
  using (
    share_enabled = true
    and public.are_community_peers(auth.uid(), user_id)
  );

grant select, insert, update on public.community_weekly_stats to authenticated;

create table if not exists public.community_challenge_participation (
  challenge_id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  score double precision not null default 0,
  joined_at timestamptz not null default now(),
  primary key (challenge_id, user_id)
);

alter table public.community_challenge_participation enable row level security;

drop policy if exists "community_challenge_own_write" on public.community_challenge_participation;
create policy "community_challenge_own_write"
  on public.community_challenge_participation for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "community_challenge_peers_select" on public.community_challenge_participation;
create policy "community_challenge_peers_select"
  on public.community_challenge_participation for select to authenticated
  using (
    auth.uid() = user_id
    or public.are_community_peers(auth.uid(), user_id)
  );

grant select, insert, update on public.community_challenge_participation to authenticated;

create or replace function public.list_community_peers(p_year int, p_week int)
returns table (
  user_id uuid,
  display_name text,
  weekly_miles double precision,
  weekly_revenue double precision,
  weekly_loads int,
  weekly_rpm double precision
)
language sql
security definer
set search_path = public
as $$
  with peers as (
    select fl.friend_id as pid from public.friend_links fl where fl.owner_id = auth.uid()
    union
    select fl.owner_id as pid from public.friend_links fl where fl.friend_id = auth.uid()
  )
  select
    p.id,
    coalesce(nullif(p.nickname, ''), nullif(p.full_name, ''), 'Driver'),
    coalesce(s.miles, 0),
    coalesce(s.revenue, 0),
    coalesce(s.loads, 0),
    coalesce(s.rpm, 0)
  from peers
  join public.profiles p on p.id = peers.pid
  left join public.community_weekly_stats s
    on s.user_id = p.id
   and s.share_enabled = true
   and s.iso_year = p_year
   and s.iso_week = p_week
  where public.are_community_peers(auth.uid(), p.id);
$$;

grant execute on function public.list_community_peers(int, int) to authenticated;

create table if not exists public.community_statuses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  display_name text not null default '',
  type text not null default 'TEXT',
  body text,
  media_path text,
  duration_ms bigint not null default 0,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null
);

create index if not exists community_statuses_expires_idx
  on public.community_statuses (expires_at);

alter table public.community_statuses enable row level security;

drop policy if exists "community_statuses_own" on public.community_statuses;
create policy "community_statuses_own"
  on public.community_statuses for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "community_statuses_peers_select" on public.community_statuses;
create policy "community_statuses_peers_select"
  on public.community_statuses for select to authenticated
  using (
    expires_at > now()
    and (auth.uid() = user_id or public.are_community_peers(auth.uid(), user_id))
  );

grant select, insert, delete on public.community_statuses to authenticated;

create table if not exists public.community_voice_rooms (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  creator_id uuid not null references auth.users (id) on delete cascade,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

alter table public.community_voice_rooms enable row level security;

drop policy if exists "community_voice_rooms_auth" on public.community_voice_rooms;
create policy "community_voice_rooms_auth"
  on public.community_voice_rooms for all to authenticated
  using (true)
  with check (creator_id = auth.uid());

grant select, insert, update, delete on public.community_voice_rooms to authenticated;

create table if not exists public.community_voice_participants (
  room_id uuid not null references public.community_voice_rooms (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  display_name text not null default '',
  muted boolean not null default false,
  deafened boolean not null default false,
  joined_at timestamptz not null default now(),
  primary key (room_id, user_id)
);

alter table public.community_voice_participants enable row level security;

drop policy if exists "community_voice_participants_auth" on public.community_voice_participants;
create policy "community_voice_participants_auth"
  on public.community_voice_participants for all to authenticated
  using (true)
  with check (user_id = auth.uid());

grant select, insert, update, delete on public.community_voice_participants to authenticated;

create table if not exists public.community_voice_signals (
  id uuid primary key default gen_random_uuid(),
  session_id text not null,
  from_user_id uuid not null references auth.users (id) on delete cascade,
  type text not null,
  sdp text,
  candidate text,
  sdp_mid text,
  sdp_mline_index int,
  created_at timestamptz not null default now()
);

create index if not exists community_voice_signals_session_idx
  on public.community_voice_signals (session_id, created_at);

alter table public.community_voice_signals enable row level security;

drop policy if exists "community_voice_signals_auth" on public.community_voice_signals;
create policy "community_voice_signals_auth"
  on public.community_voice_signals for all to authenticated
  using (true)
  with check (from_user_id = auth.uid());

grant select, insert, delete on public.community_voice_signals to authenticated;

create table if not exists public.community_calls (
  id uuid primary key default gen_random_uuid(),
  caller_id uuid not null references auth.users (id) on delete cascade,
  callee_id uuid not null references auth.users (id) on delete cascade,
  caller_name text not null default '',
  callee_name text not null default '',
  status text not null default 'RINGING',
  created_at timestamptz not null default now()
);

alter table public.community_calls enable row level security;

drop policy if exists "community_calls_party" on public.community_calls;
create policy "community_calls_party"
  on public.community_calls for all to authenticated
  using (auth.uid() = caller_id or auth.uid() = callee_id)
  with check (auth.uid() = caller_id or auth.uid() = callee_id);

grant select, insert, update on public.community_calls to authenticated;

insert into storage.buckets (id, name, public)
values ('community', 'community', false)
on conflict (id) do nothing;

drop policy if exists "community_storage_own" on storage.objects;
create policy "community_storage_own"
  on storage.objects for all to authenticated
  using (
    bucket_id = 'community'
    and split_part(name, '/', 1) = auth.uid()::text
  )
  with check (
    bucket_id = 'community'
    and split_part(name, '/', 1) = auth.uid()::text
  );

drop policy if exists "community_storage_peer_read" on storage.objects;
create policy "community_storage_peer_read"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'community'
    and split_part(name, '/', 1) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    and public.are_community_peers(auth.uid(), split_part(name, '/', 1)::uuid)
  );

notify pgrst, 'reload schema';
