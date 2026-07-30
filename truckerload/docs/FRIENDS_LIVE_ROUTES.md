# Friends live routes (Supabase)

## Apply SQL
Run in Supabase SQL editor (in order):
1. `supabase/migrations/20260730_friends_live_routes.sql`
2. `supabase/migrations/20260731_friend_nicknames.sql` (nicknames + friend_links + RLS)

Optional Realtime (Dashboard → Database → Replication):
- `driver_presence`
- `active_route_shares`

## App config
```
LOCAL_ONLY_MODE=false
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=...
GOOGLE_MAPS_API_KEY=...
```

## In-app
1. Menu → **Друзья на карте** (иконка «добавить человека»)
2. Set **мой никнейм** (unique handle)
3. Search friend by nickname → Add, or invite if not found
4. List **С кем я делюсь**: edit (show me / show route) or delete
5. Toggle **Показывать мой путь друзьям** (starts location FGS)
6. Markers = live positions; **Показать его путь** = gray (past) + blue (remaining)

## Active load rule
Today ∈ [startDate, endDate], not finished early (`actualFinishDate`), not future.

## Visibility
Friend B sees A only if A has a `friend_links` row for B with `share_my_location` / `share_my_route` and global share is ON.
