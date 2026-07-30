# Friends live routes (Supabase)

## Apply SQL
Run in Supabase SQL editor:
`supabase/migrations/20260730_friends_live_routes.sql`

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
1. Menu → **Друзья на карте**
2. Toggle **Показывать мой путь друзьям** (starts location FGS; off clears presence)
3. Markers = live positions; **Показать его путь** = gray (past) + blue (remaining)
4. **С кем пересекается мой путь** = date/corridor overlap vs your active load

## Active load rule
Today ∈ [startDate, endDate], not finished early (`actualFinishDate`), not future.
