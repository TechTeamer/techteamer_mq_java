#!/bin/bash

SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# ./bin/publish.sh <sonatype_username> <sonatype_pass> 82AC9F59 <gpg_key_pass> <gpg_key_enc_pass>

# Travis does a shallow clone by default
# so `master` is not present in the local metadata
# in a build for another branch
if ! (git rev-parse --abbrev-ref HEAD | grep -qxE 'main');
then
  echo "Fetching main branch (possibly missing due to shallow clone)"
  git fetch origin main:main
fi
if ! (git branch --contains "latest" | grep -qxE '. main'); then
  echo "Skipping build: main branch does not contain latest tag"
  exit 1
fi

SONATYPE_USERNAME=$1
SONATYPE_PASSWORD=$2
GPG_PRIVATE_KEY_ID=$3
GPG_PRIVATE_PASSWORD=$4
GPG_PRIVATE_KEY_ENC_PASS=$5
GPG_PRIVATE_KEY=$6

if [[ -z $SONATYPE_USERNAME ]]; then
  echo "Skipping publish: no sonatype username provided"
  exit 1
fi
if [[ -z $SONATYPE_PASSWORD ]]; then
  echo "Skipping publish: no sonatype password provided"
  exit 1
fi
if [[ -z $GPG_PRIVATE_KEY_ID ]]; then
  echo "Skipping publish: no GPG KEY ID provided"
  exit 1
fi
if [[ -z $GPG_PRIVATE_PASSWORD ]]; then
  echo "Skipping publish: no GPG KEY PASS provided"
  exit 1
fi
if [[ -z $GPG_PRIVATE_KEY ]]; then
  if [[ -n $GPG_PRIVATE_KEY_ENC_PASS ]]; then
    echo "Attempting to decrypt GPG PRIVATE KEY from repo"
    echo "$GPG_PRIVATE_KEY_ENC_PASS" | gpg --batch --yes --passphrase-fd 0 --pinentry-mode loopback "$SCRIPT_DIR/FaceKomDev-maven-signing.armored.key.gpg"
    GPG_PRIVATE_KEY=$(cat "$SCRIPT_DIR/FaceKomDev-maven-signing.armored.key")
    rm "$SCRIPT_DIR/FaceKomDev-maven-signing.armored.key"
  fi
fi
if [[ -z $GPG_PRIVATE_KEY ]]; then
  echo "Attempting to load GPG KEY by KEY ID from local machine"
  GPG_PRIVATE_KEY=$(echo "$GPG_PRIVATE_PASSWORD" | gpg --batch --yes --passphrase-fd 0 --pinentry-mode loopback --export-secret-key --armor "${GPG_PRIVATE_KEY_ID}!")
fi
if [[ -z $GPG_PRIVATE_KEY ]]; then
  echo "Skipping publish: no GPG KEY found"
  exit 1
fi

echo "Releasing latest tag with GPG KEY $GPG_PRIVATE_KEY_ID as $SONATYPE_USERNAME to sonatype"

export SONATYPE_USERNAME
export SONATYPE_PASSWORD
export GPG_PRIVATE_KEY
export GPG_PRIVATE_KEY_ID
export GPG_PRIVATE_PASSWORD

./gradlew publishMavenPublicationToMqRepository --info
