#!/bin/bash

# ./publish.sh 82AC9F59 <gpg_pass> <sonatype_username> <sonatype_pass>

# Travis does a shallow clone by default
# so `master` is not present in the local metadata
# in a build for another branch
git fetch origin main:main
if ! (git branch --contains "latest" | grep -qxE '. main'); then
  echo "Skipping build: main branch does not contain latest tag"
  exit 1    # quit the build early
fi

echo "Releasing latest tag with GPG KEY $GPG_PRIVATE_KEY_ID as $SONATYPE_USERNAME to sonatype"

#GPG_PRIVATE_KEY_ID=$1
#GPG_PRIVATE_PASSWORD=$2
#GPG_PRIVATE_KEY=$(echo "$GPG_PRIVATE_PASSWORD" | gpg --batch --yes --passphrase-fd 0 --pinentry-mode loopback --export-secret-key --armor "${GPG_PRIVATE_KEY_ID}!")
#
#export GPG_PRIVATE_KEY
#export GPG_PRIVATE_KEY_ID
#export GPG_PRIVATE_PASSWORD
#
#SONATYPE_USERNAME=$3
#SONATYPE_PASSWORD=$4
#
#export SONATYPE_USERNAME
#export SONATYPE_PASSWORD
#
#./gradlew publishMavenPublicationToMqRepository
