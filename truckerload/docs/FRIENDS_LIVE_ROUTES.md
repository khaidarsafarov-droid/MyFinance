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

`GOOGLE_MAPS_API_KEY` — **Maps SDK for Android** (map tiles). Often Android-
restricted (package + SHA-1).

`GOOGLE_DIRECTIONS_API_KEY` (optional) — **Directions API** HTTP key. Prefer an
unrestricted or IP-restricted key: Android package restrictions frequently
return `REQUEST_DENIED` for Directions even when tiles load. If Google
Directions fails or the key is empty, the app falls back to **OSRM** public
road routing (still along roads, car profile). Only if both fail does the map
draw a straight line (and shows a red status under the route label).

### Road routing behaviour
- Blue path = road polyline from your GPS (or PU) to DEL (Google Directions →
  OSRM fallback). Default request profile is **truck** (Google: `mode=driving`
  + `avoid=ferries`; full height/weight/hazmat needs TomTom/HERE in production).
- Gray path = GPS crumbs already driven (when sharing).
- If you leave the corridor (~100 m) for **10+ seconds**, the app recalculates
  a new driving route (throttled ~5s between fetches).
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
