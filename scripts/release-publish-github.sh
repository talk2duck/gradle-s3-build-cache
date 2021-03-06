#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SET_ENV_DIR/set-environment.sh"

export PUBLISH_REQUEST_BODY=" $(mktemp)"

cat >"${PUBLISH_REQUEST_BODY}" <<EOL
{
  "tag_name": "${PROJECT_VERSION}",
  "target_commitish": "main",
  "name": "${PROJECT_VERSION}",
  "body": "Description of the release",
  "draft": false,
  "prerelease": false
}
EOL

curl -v -i -X POST \
    -H "Content-Type:application/json" \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    "https://api.github.com/repos/${PROJECT_OWNER}/${PROJECT_ID}/releases" \
    -d "@${PUBLISH_REQUEST_BODY}"
