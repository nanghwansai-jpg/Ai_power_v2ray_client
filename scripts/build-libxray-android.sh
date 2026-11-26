#!/usr/bin/env bash
set -euo pipefail

# Pinned XTLS/libXray source plus the app-owned, isolated V4 overlay.
LIBXRAY_VERSION="${LIBXRAY_VERSION:-v26.11.9}"
LIBXRAY_COMMIT="5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3"
WORK_DIR="${LIBXRAY_WORKDIR:-.build/libXray}"
APP_LIBS_DIR="android/app/libs"
OVERLAY_DIR="native/libxray-v4"

command -v git >/dev/null
command -v go >/dev/null
command -v python3 >/dev/null
command -v sha256sum >/dev/null
command -v gomobile >/dev/null || true

rm -rf "$WORK_DIR"
mkdir -p "$(dirname "$WORK_DIR")"
git clone --depth 1 --branch "$LIBXRAY_VERSION" https://github.com/XTLS/libXray.git "$WORK_DIR"
ACTUAL_COMMIT="$(git -C "$WORK_DIR" rev-parse HEAD)"
if [[ "$ACTUAL_COMMIT" != "$LIBXRAY_COMMIT" ]]; then
  echo "Unexpected libXray source revision: $ACTUAL_COMMIT" >&2
  exit 1
fi

# Keep upstream Invoke v3 source untouched. The app-specific V4 entrypoint
# and tests are overlaid as separate same-package Go files.
install -m 0644 "$OVERLAY_DIR/invoke_v4.go" "$WORK_DIR/invoke_v4.go"
install -m 0644 "$OVERLAY_DIR/invoke_v4_test.go" "$WORK_DIR/invoke_v4_test.go"
gofmt -w "$WORK_DIR/invoke_v4.go" "$WORK_DIR/invoke_v4_test.go"

(
  cd "$WORK_DIR"
  go test -run '^(TestInvokeV4|TestInvokeAPIVersion|TestInvokeNoDataResponseShape)' .
  python3 build/main.py android
)

test -s "$WORK_DIR/libXray.aar"
unzip -p "$WORK_DIR/libXray.aar" classes.jar > "$WORK_DIR/classes.jar"
jar tf "$WORK_DIR/classes.jar" | grep -Fx "libXray/LibXray.class" >/dev/null
API_SIGNATURES="$(javap -classpath "$WORK_DIR/classes.jar" -s libXray.LibXray)"
for method in invoke registerDialerController setDNS resetDNS invokeV4; do
  if ! grep -qi "$method" <<< "$API_SIGNATURES"; then
    echo "Generated libXray AAR is missing expected API method: $method" >&2
    exit 1
  fi
done
if ! grep -A1 -E 'invokeV4\(java\.lang\.String\)' <<< "$API_SIGNATURES" | grep -Fq '(Ljava/lang/String;)Ljava/lang/String;'; then
  echo "Generated libXray AAR has an unexpected InvokeV4 signature" >&2
  exit 1
fi

mkdir -p "$APP_LIBS_DIR"
install -m 0644 "$WORK_DIR/libXray.aar" "$APP_LIBS_DIR/libXray.aar"
OVERLAY_TREE_HASH="$(sha256sum "$OVERLAY_DIR/invoke_v4.go" "$OVERLAY_DIR/invoke_v4_test.go" | sha256sum | awk '{print $1}')"
mkdir -p "$WORK_DIR/provenance"
{
  echo "upstream_repository=https://github.com/XTLS/libXray"
  echo "upstream_tag=$LIBXRAY_VERSION"
  echo "upstream_commit=$ACTUAL_COMMIT"
  echo "protocol_api_version=4"
  echo "invoke_v3_preserved=true"
  echo "invoke_v4_java_signature=$(grep -A1 -E 'invokeV4\(java\.lang\.String\)' <<< "$API_SIGNATURES" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  echo "native_overlay_sha256=$(sha256sum "$OVERLAY_DIR/invoke_v4.go" | awk '{print $1}')"
  echo "native_tests_sha256=$(sha256sum "$OVERLAY_DIR/invoke_v4_test.go" | awk '{print $1}')"
  echo "native_patch_sha256=$(sha256sum "$OVERLAY_DIR/invoke_v4.go" "$OVERLAY_DIR/invoke_v4_test.go" | sha256sum | awk '{print $1}')"
  echo "compiled_overlay_sha256=$(sha256sum "$WORK_DIR/invoke_v4.go" "$WORK_DIR/invoke_v4_test.go" | sha256sum | awk '{print $1}')"
  echo "patched_source_tree_identity=${ACTUAL_COMMIT}+${OVERLAY_TREE_HASH}"
  echo "aar_sha256=$(sha256sum "$WORK_DIR/libXray.aar" | awk '{print $1}')"
  echo "go_version=$(go version)"
  echo "python_version=$(python3 --version)"
  echo "javap_signatures_begin"
  printf '%s\n' "$API_SIGNATURES"
  echo "javap_signatures_end"
} > "$WORK_DIR/provenance/native-v4-build.txt"
cp "$WORK_DIR/provenance/native-v4-build.txt" "$WORK_DIR/native-v4-build.txt"
echo "Built $APP_LIBS_DIR/libXray.aar from XTLS/libXray $LIBXRAY_VERSION with InvokeV4"
cat "$WORK_DIR/native-v4-build.txt"
