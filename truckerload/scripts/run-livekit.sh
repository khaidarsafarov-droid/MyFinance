#!/bin/sh
# Start a local LiveKit SFU in --dev mode (API key=devkey, secret=secret).
set -eu
ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
BIN="${LIVEKIT_BIN:-$HOME/.local/bin/livekit-server}"
VERSION="${LIVEKIT_VERSION:-1.13.5}"

install_livekit() {
  if [ -x "$BIN" ]; then
    return 0
  fi
  mkdir -p "$(dirname "$BIN")"
  tmp="$(mktemp -d)"
  url="https://github.com/livekit/livekit/releases/download/v${VERSION}/livekit_${VERSION}_linux_amd64.tar.gz"
  echo "Downloading $url"
  curl -fsSL "$url" -o "$tmp/livekit.tgz"
  tar -xzf "$tmp/livekit.tgz" -C "$tmp"
  if [ -x "$tmp/livekit-server" ]; then
    mv "$tmp/livekit-server" "$BIN"
  else
    found="$(find "$tmp" -type f -name 'livekit-server' | head -n 1)"
    mv "$found" "$BIN"
  fi
  chmod +x "$BIN"
  rm -rf "$tmp"
}

install_livekit
echo "Starting LiveKit at ws://0.0.0.0:7880 (devkey/secret)"
exec "$BIN" --dev --bind 0.0.0.0
