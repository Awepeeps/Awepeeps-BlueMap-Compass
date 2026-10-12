#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="1.4.1-awepeeps.4"
IMAGE="gradle:9.1.0-jdk25@sha256:0a0fe3f8fb6e4ddd7ecf3f4d53b6f3f758add80e868bb95d72e3bbff420c1f68"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/awepeeps-bluemap-compass.XXXXXX")"
GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$GRADLE_CACHE"
trap 'rm -rf -- "$BUILD_DIR"' EXIT

docker run --rm --platform linux/amd64 \
  --user "$(id -u):$(id -g)" \
  --volume "$ROOT:/workspace:ro" \
  --volume "$BUILD_DIR:/build" \
  --volume "$GRADLE_CACHE:/gradle-cache" \
  --env GRADLE_USER_HOME=/gradle-cache \
  --workdir /workspace \
  "$IMAGE" \
  bash -euc '
    mkdir -p /build/source
    tar -C /workspace --exclude=.git --exclude=.gradle --exclude=build -cf - . |
      tar -C /build/source -xf -
    find /build/source -type d -exec chmod 0755 {} +
    find /build/source -type f -exec chmod 0644 {} +
    cr="$(printf "\r")"
    while IFS= read -r -d "" script; do
      tr -d "$cr" < "$script" > "$script.lf"
      mv -- "$script.lf" "$script"
    done < <(find /build/source -type f \( -name "*.sh" -o -name gradlew \) -print0)
    cd /build/source
    bash ./gradlew --no-daemon clean build
    install -D -m 0644 "build/libs/awepeeps-bluemap-compass-'"$VERSION"'.jar" \
      "/build/awepeeps-bluemap-compass-'"$VERSION"'.jar"
  '

mkdir -p "$ROOT/build/libs"
cp -- "$BUILD_DIR/awepeeps-bluemap-compass-$VERSION.jar" "$ROOT/build/libs/"
printf 'Built %s\n' "$ROOT/build/libs/awepeeps-bluemap-compass-$VERSION.jar"
sha256sum "$ROOT/build/libs/awepeeps-bluemap-compass-$VERSION.jar"
