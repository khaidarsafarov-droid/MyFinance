-- Friend search must see both account handles (profiles.nickname) and
-- community handles (community_profiles.nickname) after the account split.
-- Safe to re-run.

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
  with wanted as (
    select lower(trim(p_nickname)) as handle
  )
  select p.id, p.nickname
  from public.profiles p, wanted w
  where p.nickname is not null
    and trim(p.nickname) <> ''
    and lower(p.nickname) = w.handle
  union all
  select c.user_id, c.nickname
  from public.community_profiles c, wanted w
  where c.nickname is not null
    and trim(c.nickname) <> ''
    and lower(c.nickname) = w.handle
  limit 1;
$$;

grant execute on function public.search_profile_by_nickname(text) to authenticated;

create or replace function public.community_handle(p_user uuid)
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(
    nullif(trim(c.nickname), ''),
    nullif(trim(p.nickname), ''),
    'Driver'
  )
  from (select p_user as id) u
  left join public.community_profiles c on c.user_id = u.id
  left join public.profiles p on p.id = u.id;
$$;

notify pgrst, 'reload schema';
