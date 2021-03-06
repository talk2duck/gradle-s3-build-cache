#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "$SET_ENV_DIR/set-environment.sh"

cd "$PROJECT_ROOT_DIR" || exit 1

CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)

if [[ "$CHANGED_FILES" != *version.json* ]]; then
  echo "Version did not change on this commit. Ignoring"
  exit 0
fi

git tag -a "$PROJECT_VERSION" -m "${PROJECT_ID} version $PROJECT_VERSION"
git push origin "$PROJECT_VERSION"
