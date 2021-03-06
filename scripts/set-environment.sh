#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TOOLS_DIR="$SCRIPTS_DIR/tools"

if ! command -v jq &> /dev/null
then
    JQ="$TOOLS_DIR/jq"
else
    JQ=jq
fi

PROJECT_NAME="Gradle S3 Build Cache"
PROJECT_ID="gradle-s3-build-cache"
PROJECT_OWNER="talk2duck"
PROJECT_ROOT_DIR=$(realpath "$SCRIPTS_DIR/..")
PROJECT_VERSION=$($JQ -r ".[\"@${PROJECT_ID}\"].version" version.json)
BINTRAY_LATEST_VERSION_URL="https://bintray.com/api/v1/packages/${PROJECT_OWNER}/maven/${PROJECT_ID}/versions/_latest"



