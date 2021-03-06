#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

PROJECT_NAME="Gradle S3 Build Cache"
PROJECT_ID="gradle-s3-build-cache"
BINTRAY_LATEST_VERSION_URL="https://bintray.com/api/v1/packages/talk2duck/maven/${PROJECT_ID}/versions/_latest"
SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TOOLS_DIR="$SCRIPTS_DIR/tools"
PROJECT_ROOT_DIR=$(realpath "$SCRIPTS_DIR/..")
