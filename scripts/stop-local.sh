#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT_DIR/.run/cpk-is.pid"
JAR_PATTERN="cpk-is-0.0.1-SNAPSHOT.jar"

stop_pid() {
  local pid="$1"
  if [[ -z "${pid}" ]]; then
    return
  fi
  if ps -p "$pid" > /dev/null 2>&1; then
    kill "$pid" || true
    sleep 1
    if ps -p "$pid" > /dev/null 2>&1; then
      kill -9 "$pid" || true
    fi
    echo "Stopped PID $pid"
  fi
}

if [[ ! -f "$PID_FILE" ]]; then
  echo "PID file not found. Checking orphan processes..."
else
  PID="$(cat "$PID_FILE")"
  stop_pid "$PID"
fi

# Kill any orphan app process not tracked by PID file.
while IFS= read -r pid; do
  stop_pid "$pid"
done < <(pgrep -f "$JAR_PATTERN" || true)

# Extra safety: clear listeners on 8080 for the same app command.
while IFS= read -r pid; do
  if ps -p "$pid" -o command= | grep -q "cpk-is-0.0.1-SNAPSHOT.jar"; then
    stop_pid "$pid"
  fi
done < <(lsof -tiTCP:8080 -sTCP:LISTEN || true)

rm -f "$PID_FILE"
echo "Application is fully stopped."
