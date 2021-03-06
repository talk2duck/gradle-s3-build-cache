#!/usr/bin/env bash

SET_ENV_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SET_ENV_DIR/set-environment.sh"

cd "$PROJECT_ROOT_DIR" || exit 1

./gradlew build
