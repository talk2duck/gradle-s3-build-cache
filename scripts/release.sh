#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

SET_ENV_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SET_ENV_DIR/set-environment.sh"
printProjectInfo

cd "$PROJECT_ROOT_DIR" || exit 1

NEW_VERSION=${1:-}

if [ "$NEW_VERSION" == "" ]; then
  echo "New version is missing"
  exit 1
fi

echo "Release $PROJECT_ID version $NEW_VERSION"

git stash

BINTRAY_VERSION=$(curl -s "$BINTRAY_LATEST_VERSION_URL" | $JQ -r .name)

echo "Current version in Bintray is $BINTRAY_VERSION"

sed -i -- s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g "README.md"
sed -i -- s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g "version.json"

git add "README.md"
git add "version.json"

git commit -am"Release $NEW_VERSION"

git push origin

git push

git stash apply

