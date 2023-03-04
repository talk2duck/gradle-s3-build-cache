#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
TOOLS_DIR="$SCRIPTS_DIR/tools"

if ! command -v jq &>/dev/null; then
  JQ="$TOOLS_DIR/jq"
else
  JQ=jq
fi

function printProjectInfo() {
  echo "Project (${PROJECT_ID})"
  echo "  Name: ${PROJECT_NAME}"
  echo "  Directory: ${PROJECT_ROOT_DIR}"
  echo "  Local version: ${PROJECT_VERSION}"
  echo "  Owner: ${PROJECT_OWNER}"
  if [ "$PROJECT_ENV_FILE_LOADED" == "Yes" ]; then
    echo "  Env loaded: $PROJECT_ENV_FILE_LOADED"
  else
    echo "  Env loaded: $PROJECT_ENV_FILE_LOADED (missing at $PROJECT_ENV_FILE, using system)"
  fi
  echo
}

PROJECT_NAME="Gradle S3 Build Cache"
PROJECT_ID="gradle-s3-build-cache"
PROJECT_OWNER="talk2duck"
PROJECT_ROOT_DIR=$(realpath "$SCRIPTS_DIR/..")
PROJECT_VERSION=$($JQ -r ".project.version" "$PROJECT_ROOT_DIR/version.json")
PROJECT_ENV_FILE="$HOME/dev/talk2duck.env"
PROJECT_ENV_FILE_LOADED="No"

if [[ -f "$PROJECT_ENV_FILE" ]]; then
    PROJECT_ENV_FILE_LOADED="Yes"
    source "$PROJECT_ENV_FILE"
fi
