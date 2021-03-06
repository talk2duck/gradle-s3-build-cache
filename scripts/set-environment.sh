#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

PROJECT_NAME="Gradle S3 Build Cache"
PROJECT_ID="gradle-s3-build-cache"
PROJECT_ROOT_DIR=$(realpath "$SCRIPTS_DIR/..")

TOOLS_DIR="$SCRIPTS_DIR/tools"
JQ="$TOOLS_DIR/jq"

BINTRAY_LATEST_VERSION_URL="https://bintray.com/api/v1/packages/talk2duck/maven/${PROJECT_ID}/versions/_latest"

