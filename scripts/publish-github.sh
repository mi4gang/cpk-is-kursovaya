#!/usr/bin/env bash
set -euo pipefail

OWNER="${1:-mi4gang}"
REPO="${2:-cpk-is-kursovaya}"
VISIBILITY="${3:-public}"
REPO_FULL="$OWNER/$REPO"

if ! command -v gh >/dev/null 2>&1; then
  echo "Error: GitHub CLI (gh) is not installed." >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Error: current directory is not a git repository." >&2
  exit 1
fi

if git remote get-url origin >/dev/null 2>&1; then
  echo "Remote 'origin' already exists: $(git remote get-url origin)"
else
  if gh repo view "$REPO_FULL" >/dev/null 2>&1; then
    git remote add origin "https://github.com/$REPO_FULL.git"
    echo "Connected to existing repository: $REPO_FULL"
  else
    gh repo create "$REPO_FULL" --"$VISIBILITY" --source=. --remote=origin --description "Курсовая работа: ИС центра повышения квалификации"
    echo "Created repository: $REPO_FULL"
  fi
fi

current_branch="$(git rev-parse --abbrev-ref HEAD)"
git push -u origin "$current_branch"
git push origin --tags

echo "Published to https://github.com/$REPO_FULL"
