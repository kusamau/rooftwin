#!/usr/bin/env sh
# Run Maven inside the official Maven + Temurin 21 image, so the host needs only Docker.
#   ops/scripts/mvn-docker.sh verify
#   ops/scripts/mvn-docker.sh spotless:apply
# The local ~/.m2 is mounted as a cache; files are written as your own user, not root.
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$HOME/.m2"

TTY=""
if [ -t 1 ]; then TTY="-t"; fi

exec docker run --rm -i $TTY \
  -u "$(id -u):$(id -g)" \
  -e MAVEN_CONFIG=/var/maven/.m2 \
  -v "$HOME/.m2":/var/maven/.m2 \
  -v "$ROOT":/workspace -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -Duser.home=/var/maven -B "$@"
