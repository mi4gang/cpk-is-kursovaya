#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT_DIR/.run/cpk-is.pid"
JAR_PATTERN="cpk-is-0.0.1-SNAPSHOT.jar"

RUNNING_PIDS="$(pgrep -f "$JAR_PATTERN" || true)"
LISTENER_PIDS="$(lsof -tiTCP:8080 -sTCP:LISTEN || true)"

if [[ -f "$PID_FILE" ]]; then
  PID="$(cat "$PID_FILE")"
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "RUNNING PID=$PID (tracked)"
    exit 0
  fi
fi

if [[ -n "$RUNNING_PIDS" || -n "$LISTENER_PIDS" ]]; then
  echo "RUNNING (orphan)"
  if [[ -n "$RUNNING_PIDS" ]]; then
    echo "app pids: $RUNNING_PIDS"
  fi
  if [[ -n "$LISTENER_PIDS" ]]; then
    echo "listener pids: $LISTENER_PIDS"
  fi
    exit 0
fi

echo "STOPPED"
