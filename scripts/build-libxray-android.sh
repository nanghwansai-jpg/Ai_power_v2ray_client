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
ACTUAL_COMMIT="$(git -C "$WORK_DIR" rev-parse HEAD)"
case "$LIBXRAY_VERSION:$ACTUAL_COMMIT" in
  v26.11.9:5a0f7d8*) ;;
  *)
    echo "Unexpected libXray source revision: $ACTUAL_COMMIT for $LIBXRAY_VERSION" >&2
    exit 1
    ;;
esac

(
  cd "$WORK_DIR"
  python3 build/main.py android
)

test -s "$WORK_DIR/libXray.aar"
unzip -p "$WORK_DIR/libXray.aar" classes.jar > "$WORK_DIR/classes.jar"
jar tf "$WORK_DIR/classes.jar" | grep -Fx "libXray/LibXray.class" >/dev/null
API_SIGNATURES="$(javap -classpath "$WORK_DIR/classes.jar" libXray.LibXray)"
for method in invoke registerDialerController setDNS resetDNS; do
  if ! grep -qi "$method" <<< "$API_SIGNATURES"; then
    echo "Generated libXray AAR is missing expected API method: $method" >&2
    exit 1
  fi
done

mkdir -p "$APP_LIBS_DIR"
install -m 0644 "$WORK_DIR/libXray.aar" "$APP_LIBS_DIR/libXray.aar"
echo "Built $APP_LIBS_DIR/libXray.aar from XTLS/libXray $LIBXRAY_VERSION"
