#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "$SET_ENV_DIR/set-environment.sh"

echo "$PROJECT_VERSION"