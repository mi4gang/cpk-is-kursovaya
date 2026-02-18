#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JDK_HOME="$ROOT_DIR/.tools/jdk/Contents/Home"
ARCHIVE="$ROOT_DIR/.tools/jdk17.tar.gz"
URL="https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"

if [[ -x "$JDK_HOME/bin/java" ]]; then
  echo "Local JDK already installed: $JDK_HOME"
  "$JDK_HOME/bin/java" -version
  exit 0
fi

mkdir -p "$ROOT_DIR/.tools"

echo "Downloading local JDK 17..."
curl -L "$URL" -o "$ARCHIVE"

echo "Extracting JDK..."
rm -rf "$ROOT_DIR/.tools/jdk"
mkdir -p "$ROOT_DIR/.tools/jdk"
tar -xzf "$ARCHIVE" -C "$ROOT_DIR/.tools/jdk" --strip-components=1

if [[ ! -x "$JDK_HOME/bin/java" ]]; then
  echo "Failed to install local JDK"
  exit 1
fi

echo "Local JDK installed: $JDK_HOME"
"$JDK_HOME/bin/java" -version
