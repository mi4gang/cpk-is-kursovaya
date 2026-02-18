#!/usr/bin/env bash
set -euo pipefail

TAG_NAME="${1:-v1.0-kursovaya}"
MESSAGE="${2:-Release for coursework defense}"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Error: current directory is not a git repository." >&2
  exit 1
fi

if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
  echo "Error: tag '$TAG_NAME' already exists." >&2
  exit 1
fi

git tag -a "$TAG_NAME" -m "$MESSAGE"
echo "Created tag: $TAG_NAME"
