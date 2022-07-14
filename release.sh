#!/bin/bash

if ! (git rev-parse --abbrev-ref HEAD | grep -qxE 'main'); then
  echo "Skipping release: not on main branch"
  exit 1
fi

VERSION_TAG=$1

if [[ -z $VERSION_TAG ]]; then
  echo "Skipping release: no version tag provided"
  exit 1
fi

if (git branch --contains "$VERSION_TAG" | grep -qxE '. main'); then
  echo "Skipping release: main branch already tagged: $VERSION_TAG"
  exit 1
fi

echo "Tagging main branch: $VERSION_TAG"

git tag -d latest
git push --delete origin latest
git tag latest
git tag "$VERSION_TAG"
git push --tags
