#!/usr/bin/env bash

set -e

if [ -z "$1" ]; then
  echo "Usage: ./scripts/generate-api-version.sh <version>"
  echo "Example: ./scripts/generate-api-version.sh 1.5.0"
  exit 1
fi

VERSION=${1//./_}

echo "Generating API snapshot for version ${VERSION}"

./gradlew apiDump

find . -path "*/api/*.api" -type f | while read -r file; do
  dir=$(dirname "$file")

  output="${dir}/${VERSION}.api"

  cp "$file" "$output"

  echo "Created ${output}"
done

echo "Done"