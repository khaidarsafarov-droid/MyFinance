# Friends live routes (Supabase)

## Apply SQL
Run in Supabase SQL editor (in order):
1. `supabase/migrations/20260730_friends_live_routes.sql`
2. `supabase/migrations/20260731_friend_nicknames.sql` (nicknames + friend_links + RLS)

### Fix: `Could not find the 'nickname' column of 'profiles'`
That HTTP 400 / `PGRST204` means migration **#2 was not applied** on the Supabase project.
1. Open [Supabase Dashboard](https://supabase.com/dashboard) → your project → **SQL Editor**
2. Paste the full contents of `supabase/migrations/20260731_friend_nicknames.sql`
3. Run it (it ends with `notify pgrst, 'reload schema';`)
4. Retry **Добавить никнейм** in the app — no APK rebuild required for the server fix

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

`GOOGLE_MAPS_API_KEY` must allow both **Maps SDK for Android** (map tiles) and
**Directions API** (road polylines). Without Directions enabled, the map falls
back to a straight line between you and the destination.

### Road routing behaviour
- Blue path = Google driving route from your GPS (or PU) to DEL, along roads.
- Gray path = GPS crumbs already driven (when sharing).
- If you leave the corridor (~800 m / 0.5 mi), the app recalculates a new
  driving route (throttled to about once every 45s).
- Destination change (new active load) also triggers a fresh Directions fetch.

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
