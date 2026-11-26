#!/usr/bin/env bash
set -euo pipefail

# Pinned, reproducible XTLS/libXray source build for Android.
LIBXRAY_VERSION="${LIBXRAY_VERSION:-v26.11.9}"
WORK_DIR="${LIBXRAY_WORKDIR:-.build/libXray}"
APP_LIBS_DIR="android/app/libs"

command -v git >/dev/null
command -v go >/dev/null
command -v python3 >/dev/null
command -v gomobile >/dev/null || true

rm -rf "$WORK_DIR"
mkdir -p "$(dirname "$WORK_DIR")"
git clone --depth 1 --branch "$LIBXRAY_VERSION" https://github.com/XTLS/libXray.git "$WORK_DIR"

(
  cd "$WORK_DIR"
  python3 build/main.py android
)

test -s "$WORK_DIR/libXray.aar"
mkdir -p "$APP_LIBS_DIR"
install -m 0644 "$WORK_DIR/libXray.aar" "$APP_LIBS_DIR/libXray.aar"
echo "Built $APP_LIBS_DIR/libXray.aar from XTLS/libXray $LIBXRAY_VERSION"
