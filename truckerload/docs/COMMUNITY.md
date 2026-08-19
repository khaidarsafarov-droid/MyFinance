# Community (Supabase)

Community chats, groups, weekly leaderboard, statuses, and voice signaling use
the same Supabase project as Friends on the map. Journal snapshots stay on Ktor.

## Apply SQL

In the Supabase SQL editor, in order:

1. `supabase/migrations/20260730_friends_live_routes.sql`
2. `supabase/migrations/20260731_friend_nicknames.sql`
3. `supabase/migrations/20260818_community_live.sql`
   (safe to re-run if an earlier attempt failed — re-run this latest file
   even if a previous Community script succeeded, so RLS helpers apply)

Optional Realtime (Dashboard → Database → Replication):

- `community_messages`
- `community_voice_signals`
- `community_calls`

The Android client currently polls REST every few seconds while Community
screens are open, so Replication is optional.

## App config

```
LOCAL_ONLY_MODE=false
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=...
```

Optional TURN for calls that fail on cellular NAT:

```
TURN_URI=turn:turn.example.com:3478
TURN_USERNAME=...
TURN_CREDENTIAL=...
```

## In-app

1. Sign in (Google / email) so the session has a Supabase JWT UUID.
2. Profile → set a unique nickname.
3. Friends on the map → add a friend by nickname.
4. Community → private chat, groups (invite code), leaderboard, weekly miles
   challenge (opt-in aggregate stats only), statuses, voice rooms / calls.

Groups are invite-code only (not a public directory). Share the code from
the group screen. Without friends, Community shows an empty-state CTA
instead of demo users.
Without a cloud JWT, Community stays on-device only (local sandbox).
