#!/bin/bash

# ./publish.sh 82AC9F59 <gpg_pass> <sonatype_username> <sonatype_pass>

GPG_PRIVATE_KEY_ID=$1
GPG_PRIVATE_PASSWORD=$2
GPG_PRIVATE_KEY=$(echo "$GPG_PRIVATE_PASSWORD" | gpg --batch --yes --passphrase-fd 0 --pinentry-mode loopback --export-secret-key --armor "${GPG_PRIVATE_KEY_ID}!")

export GPG_PRIVATE_KEY
export GPG_PRIVATE_KEY_ID
export GPG_PRIVATE_PASSWORD

SONATYPE_USERNAME=$3
SONATYPE_PASSWORD=$4

export SONATYPE_USERNAME
export SONATYPE_PASSWORD

./gradlew publishMavenPublicationToMqRepository
