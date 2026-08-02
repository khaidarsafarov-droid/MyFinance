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

### Google key — only free-tier products
`GOOGLE_MAPS_API_KEY` is used for **Maps SDK for Android** (map tiles on the friends
map and analytics heatmap). Enable only **Maps SDK for Android** in Google Cloud —
**do not enable Directions API** (paid beyond small quota).

Route polylines on the friends map use **OSRM** (free public server, no key).

### Road routing behaviour
- Blue path = OSRM driving route from GPS (or PU) to DEL, along roads.
- Gray path = GPS crumbs already driven (when sharing).
- Off-route reroute: >50 m from corridor for 10+ seconds → new route (throttled ~5s).

## In-app
1. Menu → **Друзья на карте**
2. Toggle **Показывать мой путь друзьям** (starts location FGS)
3. Markers = live positions; **Показать его путь** = gray (past) + blue (remaining)
