#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME="$ROOT_DIR/.tools/jdk/Contents/Home"
PID_FILE="$ROOT_DIR/.run/cpk-is.pid"
LOG_FILE="$ROOT_DIR/.run/cpk-is.log"
JAR_PATH="$ROOT_DIR/target/cpk-is-0.0.1-SNAPSHOT.jar"

mkdir -p "$ROOT_DIR/.run"
mkdir -p "$ROOT_DIR/.tools"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  "$ROOT_DIR/scripts/setup-jdk.sh"
fi

if [[ -f "$PID_FILE" ]]; then
  PID="$(cat "$PID_FILE")"
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "Application already running with PID $PID"
    exit 0
  fi
fi

cd "$ROOT_DIR"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Building executable jar..."
  ./mvnw -q -DskipTests package
fi

nohup "$JAVA_HOME/bin/java" -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

echo "Starting application... PID=$PID"

for _ in {1..60}; do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login | grep -q "200"; then
    echo "Application is up: http://localhost:8080"
    exit 0
  fi
  sleep 1
done

echo "Application did not start in time. Check log: $LOG_FILE"
exit 1
