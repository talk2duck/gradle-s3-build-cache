#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "$SET_ENV_DIR/set-environment.sh"

cd "$PROJECT_ROOT_DIR" || exit 1

CHANGELOG_VERSION=$1

echo "Changelog:"
TAG=$(echo "refs/tags/$CHANGELOG_VERSION" | sed "s/.*tags\///g")
START="### v$TAG"
END="###"
sed -n "/^$START$/,/$END/p" CHANGELOG.md | sed '1d' | sed '$d' | sed '$d'
