#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SET_ENV_DIR/set-environment.sh"

NEW_VERSION=${1:-}

if [ "$NEW_VERSION" == "" ]; then
  echo "New version is missing"
  exit 1
fi

echo "Creating release $PROJECT_NAME:$NEW_VERSION"

#git tag -a "$NEW_VERSION" -m "$PROJECT_NAME $NEW_VERSION"
#git push origin "$NEW_VERSION"

#git stash

BINTRAY_VERSION=$(curl -s "$BINTRAY_LATEST_VERSION_URL" | "$TOOLS_DIR"/jq -r .name)

ls -al "$PROJECT_ROOT_DIR/VERSION"

sed -i -- s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g "$PROJECT_ROOT_DIR/README.md"
sed -i -- s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g "$PROJECT_ROOT_DIR/VERSION"

#git add README.md
#
#git commit -am"Release $NEW_VERSION"
#
#git push origin
#
#git push
#
#git stash apply

