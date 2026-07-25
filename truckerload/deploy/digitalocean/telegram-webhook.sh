#!/usr/bin/env bash
set -euo pipefail

# Never allow shell tracing to print webhook credentials.
set +x

command_name="${1:-status}"
: "${TELEGRAM_BOT_TOKEN:?TELEGRAM_BOT_TOKEN is required}"

if [[ ! "$TELEGRAM_BOT_TOKEN" =~ ^[0-9]+:[A-Za-z0-9_-]+$ ]]; then
  echo "TELEGRAM_BOT_TOKEN has an invalid format" >&2
  exit 2
fi

telegram_request() {
  local method="$1"
  shift
  curl --silent --show-error --fail-with-body --config - <<EOF
url = "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/${method}"
request = "POST"
$*
EOF
  printf '\n'
}

case "$command_name" in
  set)
    : "${TELEGRAM_WEBHOOK_URL:?TELEGRAM_WEBHOOK_URL is required for set}"
    : "${TELEGRAM_WEBHOOK_SECRET:?TELEGRAM_WEBHOOK_SECRET is required for set}"
    if [[ ! "$TELEGRAM_WEBHOOK_URL" =~ ^https://[^[:space:]\"]+$ || "$TELEGRAM_WEBHOOK_URL" == *\\* ]]; then
      echo "TELEGRAM_WEBHOOK_URL must be an HTTPS URL without whitespace" >&2
      exit 2
    fi
    if [[ ! "$TELEGRAM_WEBHOOK_SECRET" =~ ^[A-Za-z0-9_-]{1,256}$ ]]; then
      echo "TELEGRAM_WEBHOOK_SECRET must use Telegram's allowed characters" >&2
      exit 2
    fi
    telegram_request setWebhook \
      "data-urlencode = \"url=${TELEGRAM_WEBHOOK_URL}\"
data-urlencode = \"secret_token=${TELEGRAM_WEBHOOK_SECRET}\"
data-urlencode = \"allowed_updates=[\\\"message\\\"]\""
    ;;
  status)
    telegram_request getWebhookInfo
    ;;
  delete)
    drop_pending_updates="${DROP_PENDING_UPDATES:-false}"
    if [[ "$drop_pending_updates" != "true" && "$drop_pending_updates" != "false" ]]; then
      echo "DROP_PENDING_UPDATES must be true or false" >&2
      exit 2
    fi
    telegram_request deleteWebhook \
      "data-urlencode = \"drop_pending_updates=${drop_pending_updates}\""
    ;;
  *)
    echo "Usage: $0 {set|status|delete}" >&2
    exit 2
    ;;
esac
