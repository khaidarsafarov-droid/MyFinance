-- Keep denormalized friend_links.friend_nickname in sync when a user changes
-- their profiles.nickname. Friends who already added them see the new handle
-- without re-adding. Also expose a live-join RPC for list refresh.

-- 1) Trigger: profiles.nickname / full_name → friend_links rows pointing at me
create or replace function public.sync_friend_link_identity()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'UPDATE'
     and new.nickname is not distinct from old.nickname
     and new.full_name is not distinct from old.full_name then
    return new;
  end if;

  update public.friend_links fl
  set
    friend_nickname = coalesce(nullif(trim(new.nickname), ''), fl.friend_nickname),
    friend_display_name = coalesce(
      nullif(trim(new.full_name), ''),
      nullif(trim(new.nickname), ''),
      fl.friend_display_name
    ),
    updated_at = now()
  where fl.friend_id = new.id;

  return new;
end;
$$;

drop trigger if exists trg_sync_friend_link_identity on public.profiles;
create trigger trg_sync_friend_link_identity
  after insert or update of nickname, full_name
  on public.profiles
  for each row
  execute function public.sync_friend_link_identity();

-- 2) Live list: prefer current profiles.nickname over denormalized snapshot
create or replace function public.list_my_friend_links()
returns table (
  friend_id uuid,
  friend_nickname text,
  friend_display_name text,
  share_my_location boolean,
  share_my_route boolean
)
language sql
security definer
set search_path = public
as $$
  select
    fl.friend_id,
    coalesce(nullif(trim(p.nickname), ''), fl.friend_nickname) as friend_nickname,
    coalesce(
      nullif(trim(p.full_name), ''),
      nullif(trim(p.nickname), ''),
      fl.friend_display_name
    ) as friend_display_name,
    fl.share_my_location,
    fl.share_my_route
  from public.friend_links fl
  left join public.profiles p on p.id = fl.friend_id
  where fl.owner_id = auth.uid()
  order by fl.created_at desc;
$$;

grant execute on function public.list_my_friend_links() to authenticated;

notify pgrst, 'reload schema';
