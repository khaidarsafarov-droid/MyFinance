#!/usr/bin/env bash
# Apply profiles.nickname (+ related friends nicknames SQL) to the linked Supabase project.
# Requires one of:
#   SUPABASE_DB_URL   postgres://... (Database settings → URI)
#   DATABASE_URL      same
# Optional local.properties keys (never commit real values):
#   SUPABASE_DB_URL=...
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL_FILE="${1:-$ROOT/supabase/migrations/20260731_friend_nicknames.sql}"
QUICKFIX="$ROOT/supabase/migrations/QUICKFIX_add_nickname_column.sql"

if [[ -f "$ROOT/local.properties" ]]; then
  # shellcheck disable=SC1090
  while IFS='=' read -r k v; do
    case "$k" in
      SUPABASE_DB_URL|DATABASE_URL|SUPABASE_ACCESS_TOKEN|SUPABASE_PROJECT_REF)
        export "$k=$v"
        ;;
    esac
  done < <(grep -E '^(SUPABASE_DB_URL|DATABASE_URL|SUPABASE_ACCESS_TOKEN|SUPABASE_PROJECT_REF)=' "$ROOT/local.properties" || true)
fi

DB_URL="${SUPABASE_DB_URL:-${DATABASE_URL:-}}"
PROJECT_REF="${SUPABASE_PROJECT_REF:-jsptulbjtesnphrbxsil}"

if [[ -n "$DB_URL" ]]; then
  if ! command -v psql >/dev/null 2>&1; then
    sudo apt-get update -qq && sudo apt-get install -y -qq postgresql-client >/dev/null
  fi
  echo "Applying $SQL_FILE via psql…"
  psql "$DB_URL" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
  echo "OK: nickname migration applied."
  exit 0
fi

if [[ -n "${SUPABASE_ACCESS_TOKEN:-}" ]]; then
  echo "Applying via Supabase Management API (project $PROJECT_REF)…"
  BODY=$(python3 - <<PY
import json, pathlib
sql = pathlib.Path("$SQL_FILE").read_text()
print(json.dumps({"query": sql, "name": "friend_nicknames"}))
PY
)
  CODE=$(curl -sS -o /tmp/sb_mig_out.json -w '%{http_code}' \
    -X POST "https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query" \
    -H "Authorization: Bearer ${SUPABASE_ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$BODY")
  echo "HTTP $CODE"
  cat /tmp/sb_mig_out.json
  echo
  [[ "$CODE" == "200" || "$CODE" == "201" ]] || {
    # Fallback: dedicated migrations endpoint (restricted)
    CODE2=$(curl -sS -o /tmp/sb_mig_out2.json -w '%{http_code}' \
      -X POST "https://api.supabase.com/v1/projects/${PROJECT_REF}/database/migrations" \
      -H "Authorization: Bearer ${SUPABASE_ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$BODY")
    echo "migrations HTTP $CODE2"
    cat /tmp/sb_mig_out2.json
    echo
    [[ "$CODE2" == "200" || "$CODE2" == "201" ]] || exit 1
  }
  echo "OK: nickname migration applied via Management API."
  exit 0
fi

echo "Missing credentials."
echo "Add to truckerload/local.properties (gitignored) one of:"
echo "  SUPABASE_DB_URL=postgresql://postgres.…@db.${PROJECT_REF}.supabase.co:5432/postgres"
echo "  SUPABASE_ACCESS_TOKEN=<personal access token from supabase.com/dashboard/account/tokens>"
echo "Then re-run: sh scripts/apply-nickname-migration.sh"
echo "Quickfix SQL also at: $QUICKFIX"
exit 2
