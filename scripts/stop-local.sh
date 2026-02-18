#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT_DIR/.run/cpk-is.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "PID file not found. Application is not marked as running."
  exit 0
fi

PID="$(cat "$PID_FILE")"
if ps -p "$PID" > /dev/null 2>&1; then
  kill "$PID" || true
  sleep 1
  if ps -p "$PID" > /dev/null 2>&1; then
    kill -9 "$PID" || true
  fi
  echo "Stopped PID $PID"
else
  echo "Process $PID already stopped"
fi

rm -f "$PID_FILE"
