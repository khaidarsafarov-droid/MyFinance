-- Voice room description/moderator + creator-only delete.
-- Group chats: description, creator delete, assign moderator.

alter table public.community_voice_rooms
  add column if not exists description text not null default '';

alter table public.community_voice_rooms
  add column if not exists moderator_id uuid references auth.users (id) on delete set null;

drop policy if exists "community_voice_rooms_auth" on public.community_voice_rooms;

create policy "community_voice_rooms_select"
  on public.community_voice_rooms for select to authenticated
  using (true);

create policy "community_voice_rooms_insert"
  on public.community_voice_rooms for insert to authenticated
  with check (creator_id = auth.uid());

create policy "community_voice_rooms_update"
  on public.community_voice_rooms for update to authenticated
  using (creator_id = auth.uid() or moderator_id = auth.uid())
  with check (creator_id = auth.uid() or moderator_id = auth.uid());

create policy "community_voice_rooms_delete"
  on public.community_voice_rooms for delete to authenticated
  using (creator_id = auth.uid());

create or replace function public.delete_community_voice_room(p_room uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
begin
  if me is null then
    raise exception 'not authenticated';
  end if;
  delete from public.community_voice_rooms
   where id = p_room and creator_id = me;
  if not found then
    raise exception 'not allowed';
  end if;
  delete from public.community_voice_signals
   where session_id = p_room::text
      or session_id like p_room::text || '%';
end;
$$;

grant execute on function public.delete_community_voice_room(uuid) to authenticated;

alter table public.community_chats
  add column if not exists description text not null default '';

drop policy if exists "community_chats_creator_delete" on public.community_chats;
create policy "community_chats_creator_delete"
  on public.community_chats for delete to authenticated
  using (creator_id = auth.uid());

grant delete on public.community_chats to authenticated;

create or replace function public.delete_community_group(p_chat uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
begin
  if me is null then
    raise exception 'not authenticated';
  end if;
  if not exists (
    select 1 from public.community_chats
     where id = p_chat and creator_id = me
  ) then
    raise exception 'not creator';
  end if;
  delete from public.community_chats where id = p_chat;
end;
$$;

grant execute on function public.delete_community_group(uuid) to authenticated;

create or replace function public.set_community_group_moderator(p_chat uuid, p_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
begin
  if me is null or p_user is null then
    raise exception 'invalid args';
  end if;
  if not exists (
    select 1 from public.community_chats
     where id = p_chat and creator_id = me
  ) then
    raise exception 'not creator';
  end if;
  if not exists (
    select 1 from public.community_chat_members
     where chat_id = p_chat and user_id = p_user
  ) then
    raise exception 'not a member';
  end if;
  update public.community_chat_members
     set role = 'MEMBER'
   where chat_id = p_chat
     and role = 'MODERATOR'
     and user_id <> me;
  update public.community_chat_members
     set role = 'MODERATOR'
   where chat_id = p_chat
     and user_id = p_user
     and role <> 'OWNER';
end;
$$;

grant execute on function public.set_community_group_moderator(uuid, uuid) to authenticated;
