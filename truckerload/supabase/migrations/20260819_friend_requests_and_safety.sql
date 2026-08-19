-- Friend requests, reports, and PII-safe public identity.
-- Apply AFTER 20260818_community_live.sql. Safe to re-run.

-- Public peers must never read email / phone / legal name from profiles.
drop policy if exists "profiles_select_nickname_public" on public.profiles;

create or replace function public.community_handle(p_user uuid)
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(nullif(trim(p.nickname), ''), 'Driver')
  from public.profiles p
  where p.id = p_user;
$$;

revoke all on function public.community_handle(uuid) from public;
grant execute on function public.community_handle(uuid) to authenticated;

drop function if exists public.search_profile_by_nickname(text);
create function public.search_profile_by_nickname(p_nickname text)
returns table (
  user_id uuid,
  nickname text
)
language sql
security definer
set search_path = public
as $$
  select p.id, p.nickname
  from public.profiles p
  where p.nickname is not null
    and lower(p.nickname) = lower(trim(p_nickname))
  limit 1;
$$;

grant execute on function public.search_profile_by_nickname(text) to authenticated;

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
  peer_name := public.community_handle(p_peer);
  my_name := public.community_handle(me);
  insert into public.community_chats (id, type, title, creator_id, is_public)
  values (new_id, 'PRIVATE', coalesce(peer_name, 'Chat'), me, false);
  insert into public.community_chat_members (chat_id, user_id, display_name, role)
  values
    (new_id, me, coalesce(my_name, 'You'), 'MEMBER'),
    (new_id, p_peer, coalesce(peer_name, 'Driver'), 'MEMBER');
  return new_id;
end;
$$;

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
    public.community_handle(p.id),
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

create table if not exists public.friend_requests (
  id uuid primary key default gen_random_uuid(),
  from_id uuid not null references auth.users (id) on delete cascade,
  to_id uuid not null references auth.users (id) on delete cascade,
  status text not null default 'pending'
    check (status in ('pending', 'accepted', 'declined', 'cancelled')),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check (from_id <> to_id)
);

create unique index if not exists friend_requests_pending_uidx
  on public.friend_requests (from_id, to_id)
  where status = 'pending';

create index if not exists friend_requests_to_pending_idx
  on public.friend_requests (to_id)
  where status = 'pending';

alter table public.friend_requests enable row level security;

drop policy if exists "friend_requests_participants" on public.friend_requests;
create policy "friend_requests_participants"
  on public.friend_requests for select to authenticated
  using (auth.uid() = from_id or auth.uid() = to_id);

grant select on public.friend_requests to authenticated;

create or replace function public.link_community_friends(a uuid, b uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  nick_a text := public.community_handle(a);
  nick_b text := public.community_handle(b);
begin
  if a is null or b is null or a = b then
    return;
  end if;
  insert into public.friend_links (
    owner_id, friend_id, friend_nickname, friend_display_name,
    share_my_location, share_my_route, updated_at
  ) values (
    a, b, nick_b, nick_b, true, true, now()
  )
  on conflict (owner_id, friend_id) do update set
    friend_nickname = excluded.friend_nickname,
    friend_display_name = excluded.friend_nickname,
    updated_at = now();
  insert into public.friend_links (
    owner_id, friend_id, friend_nickname, friend_display_name,
    share_my_location, share_my_route, updated_at
  ) values (
    b, a, nick_a, nick_a, true, true, now()
  )
  on conflict (owner_id, friend_id) do update set
    friend_nickname = excluded.friend_nickname,
    friend_display_name = excluded.friend_nickname,
    updated_at = now();
  insert into public.friendships (follower_id, followee_id)
  values (a, b)
  on conflict do nothing;
  insert into public.friendships (follower_id, followee_id)
  values (b, a)
  on conflict do nothing;
end;
$$;

revoke all on function public.link_community_friends(uuid, uuid) from public;

create or replace function public.send_friend_request(p_peer uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  reverse_id uuid;
begin
  if me is null or p_peer is null or me = p_peer then
    raise exception 'invalid request';
  end if;
  if exists (
    select 1 from public.community_blocks bl
    where (bl.blocker_id = me and bl.blocked_id = p_peer)
       or (bl.blocker_id = p_peer and bl.blocked_id = me)
  ) then
    return 'blocked';
  end if;
  if public.are_community_peers(me, p_peer) then
    return 'already_friends';
  end if;
  select r.id into reverse_id
  from public.friend_requests r
  where r.from_id = p_peer and r.to_id = me and r.status = 'pending'
  limit 1;
  if reverse_id is not null then
    update public.friend_requests
      set status = 'accepted', responded_at = now()
      where id = reverse_id;
    perform public.link_community_friends(me, p_peer);
    return 'accepted';
  end if;
  if exists (
    select 1 from public.friend_requests
    where from_id = me and to_id = p_peer and status = 'pending'
  ) then
    return 'already_sent';
  end if;
  insert into public.friend_requests (from_id, to_id, status)
  values (me, p_peer, 'pending');
  return 'sent';
end;
$$;

grant execute on function public.send_friend_request(uuid) to authenticated;

create or replace function public.list_my_friend_requests()
returns table (
  request_id uuid,
  peer_id uuid,
  peer_nickname text,
  direction text,
  created_at timestamptz
)
language sql
security definer
set search_path = public
as $$
  select
    r.id,
    case when r.from_id = auth.uid() then r.to_id else r.from_id end,
    public.community_handle(
      case when r.from_id = auth.uid() then r.to_id else r.from_id end
    ),
    case when r.from_id = auth.uid() then 'outgoing' else 'incoming' end,
    r.created_at
  from public.friend_requests r
  where r.status = 'pending'
    and (r.from_id = auth.uid() or r.to_id = auth.uid());
$$;

grant execute on function public.list_my_friend_requests() to authenticated;

create or replace function public.accept_friend_request(p_request uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  rec public.friend_requests%rowtype;
begin
  select * into rec from public.friend_requests where id = p_request;
  if rec.id is null or rec.to_id <> auth.uid() or rec.status <> 'pending' then
    raise exception 'invalid request';
  end if;
  update public.friend_requests
    set status = 'accepted', responded_at = now()
    where id = p_request;
  perform public.link_community_friends(rec.from_id, rec.to_id);
end;
$$;

grant execute on function public.accept_friend_request(uuid) to authenticated;

create or replace function public.decline_friend_request(p_request uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.friend_requests
    set status = 'declined', responded_at = now()
    where id = p_request
      and to_id = auth.uid()
      and status = 'pending';
  if not found then
    raise exception 'invalid request';
  end if;
end;
$$;

grant execute on function public.decline_friend_request(uuid) to authenticated;

create or replace function public.cancel_friend_request(p_request uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.friend_requests
    set status = 'cancelled', responded_at = now()
    where id = p_request
      and from_id = auth.uid()
      and status = 'pending';
  if not found then
    raise exception 'invalid request';
  end if;
end;
$$;

grant execute on function public.cancel_friend_request(uuid) to authenticated;

create table if not exists public.community_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users (id) on delete cascade,
  reported_user_id uuid not null references auth.users (id) on delete cascade,
  chat_id uuid,
  message_id uuid,
  reason text not null check (reason in (
    'spam', 'harassment', 'hate', 'sexual', 'scam', 'other'
  )),
  details text not null default '',
  created_at timestamptz not null default now(),
  check (reporter_id <> reported_user_id)
);

create index if not exists community_reports_reporter_day_idx
  on public.community_reports (reporter_id, created_at desc);

alter table public.community_reports enable row level security;

drop policy if exists "community_reports_own" on public.community_reports;
create policy "community_reports_own"
  on public.community_reports for select to authenticated
  using (auth.uid() = reporter_id);

grant select on public.community_reports to authenticated;

create or replace function public.submit_community_report(
  p_user uuid,
  p_reason text,
  p_details text default '',
  p_chat uuid default null,
  p_message uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  new_id uuid := gen_random_uuid();
  reason_norm text := lower(trim(coalesce(p_reason, 'other')));
begin
  if me is null or p_user is null or me = p_user then
    raise exception 'invalid report';
  end if;
  if reason_norm not in ('spam', 'harassment', 'hate', 'sexual', 'scam', 'other') then
    reason_norm := 'other';
  end if;
  if (
    select count(*) from public.community_reports
    where reporter_id = me and created_at > now() - interval '1 day'
  ) >= 20 then
    raise exception 'report rate limited';
  end if;
  insert into public.community_reports (
    id, reporter_id, reported_user_id, chat_id, message_id, reason, details
  ) values (
    new_id, me, p_user, p_chat, p_message, reason_norm,
    left(coalesce(p_details, ''), 500)
  );
  return new_id;
end;
$$;

grant execute on function public.submit_community_report(uuid, text, text, uuid, uuid) to authenticated;

create or replace function public.community_block_cleanup()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.friend_links
  where (owner_id = new.blocker_id and friend_id = new.blocked_id)
     or (owner_id = new.blocked_id and friend_id = new.blocker_id);
  delete from public.friendships
  where (follower_id = new.blocker_id and followee_id = new.blocked_id)
     or (follower_id = new.blocked_id and followee_id = new.blocker_id);
  update public.friend_requests
    set status = 'cancelled', responded_at = now()
    where status = 'pending'
      and (
        (from_id = new.blocker_id and to_id = new.blocked_id)
        or (from_id = new.blocked_id and to_id = new.blocker_id)
      );
  return new;
end;
$$;

drop trigger if exists community_blocks_cleanup on public.community_blocks;
create trigger community_blocks_cleanup
  after insert on public.community_blocks
  for each row execute function public.community_block_cleanup();

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
  my_name := public.community_handle(me);
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

notify pgrst, 'reload schema';
