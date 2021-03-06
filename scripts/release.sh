#!/usr/bin/env bash

SET_ENV_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SET_ENV_DIR/set-environment.sh"

VERSION=${1:-}

if [ "$VERSION" == "" ]; then
  echo "Version is missing"
  exit 1
fi

echo "Creating release $VERSION"
echo "Scripts $SCRIPTS_DIR"
