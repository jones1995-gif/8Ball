#!/usr/bin/env sh
# Lightweight bootstrap for environments where the Gradle wrapper JAR is not bundled.
set -eu
VERSION="8.9"
CACHE_BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual-distributions"
GRADLE_HOME="$CACHE_BASE/gradle-$VERSION"
ZIP_PATH="$CACHE_BASE/gradle-$VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$CACHE_BASE"
  if [ ! -f "$ZIP_PATH" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "$URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_PATH" "$URL"
    else
      echo "É necessário instalar curl ou wget para baixar o Gradle." >&2
      exit 1
    fi
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    EXPECTED="d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
    ACTUAL="$(sha256sum "$ZIP_PATH" | awk '{print $1}')"
    if [ "$ACTUAL" != "$EXPECTED" ]; then
      echo "Checksum do Gradle inválido. Apague $ZIP_PATH e tente novamente." >&2
      exit 1
    fi
  fi
  unzip -q -o "$ZIP_PATH" -d "$CACHE_BASE"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
