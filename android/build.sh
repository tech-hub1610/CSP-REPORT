#!/usr/bin/env bash
# CSP Daily Report — native Android APK build pipeline (no Gradle, no AndroidX, arm64-native tools).
#
# Pipeline: aapt (resources + R.java) -> javac -> dx (dalvik-exchange) -> aapt package
#           -> append classes.dex -> zipalign -> apksigner (v1+v2).
#
# The output APK is fully standalone: no INTERNET permission, no WebView, no assets.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB="$ROOT/lib"
RES="$ROOT/res"
SRC="$ROOT/src"
BUILD="$ROOT/build"
DIST="$ROOT/dist"

AAPT="$(command -v aapt)"
ZIPALIGN="$(command -v zipalign)"
APKSIGNER="$(command -v apksigner)"
DX="${DX:-/usr/bin/dalvik-exchange}"

die() { echo "BUILD ERROR: $*" >&2; exit 1; }

# ----------------------------- version --------------------------------------
# Track semantic versioning persistently in version.properties
VERSION_PROP="$ROOT/version.properties"
if [ -f "$VERSION_PROP" ]; then
    # Load existing version
    V_MAJOR=$(grep -E "^VERSION_MAJOR=" "$VERSION_PROP" | cut -d'=' -f2 | tr -d ' \r\n')
    V_MINOR=$(grep -E "^VERSION_MINOR=" "$VERSION_PROP" | cut -d'=' -f2 | tr -d ' \r\n')
    V_PATCH=$(grep -E "^VERSION_PATCH=" "$VERSION_PROP" | cut -d'=' -f2 | tr -d ' \r\n')
    V_CODE=$(grep -E "^VERSION_CODE=" "$VERSION_PROP" | cut -d'=' -f2 | tr -d ' \r\n')
else
    V_MAJOR="2"
    V_MINOR="0"
    V_PATCH="1"
    V_CODE="20260921"
fi

# Allow CLI override if specified, otherwise use version.properties
VERSION_NAME="${VERSION_NAME:-$V_MAJOR.$V_MINOR.$V_PATCH}"
VERSION_CODE="${VERSION_CODE:-$V_CODE}"

echo "==> Building Release: versionName=$VERSION_NAME (versionCode=$VERSION_CODE)"

# Save current version to version.properties
cat <<EOF > "$VERSION_PROP"
VERSION_MAJOR=$V_MAJOR
VERSION_MINOR=$V_MINOR
VERSION_PATCH=$V_PATCH
VERSION_CODE=$VERSION_CODE
EOF

# Update root AndroidManifest.xml as well as build manifest
sed -i -e "s/android:versionCode=\"[0-9]*\"/android:versionCode=\"$VERSION_CODE\"/" \
       -e "s/android:versionName=\"[^\"]*\"/android:versionName=\"$VERSION_NAME\"/" \
       "$ROOT/AndroidManifest.xml"

mkdir -p "$BUILD" "$DIST"
cp -f "$ROOT/AndroidManifest.xml" "$BUILD/AndroidManifest.xml"
MANIFEST="$BUILD/AndroidManifest.xml"

# ----------------------------- tool checks -----------------------------------
for t in aapt javac java zipalign apksigner; do
  command -v "$t" >/dev/null 2>&1 || die "'$t' not found in PATH (apt install aapt zipalign apksigner)"
done
[ -f "$LIB/android.jar" ] || die "lib/android.jar missing (API-34 platform stubs)"
if [ ! -f "$LIB/r8.jar" ] && [ ! -x "$DX" ]; then
  die "dexer missing: install lib/r8.jar or apt install dalvik-exchange"
fi

# ----------------------------- 1. aapt: resources + R.java -------------------
rm -rf "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"
"$AAPT" package -f -m -J "$BUILD/gen" -M "$MANIFEST" -S "$RES" -I "$LIB/android.jar"
echo "==> [1/6] aapt: R.java generated"

# ----------------------------- 2. javac --------------------------------------
find "$BUILD/gen" "$SRC" -name '*.java' > "$BUILD/sources.txt"
javac --release 8 -encoding UTF-8 -nowarn \
  -cp "$LIB/android.jar" \
  -d "$BUILD/classes" \
  @"$BUILD/sources.txt"
echo "==> [2/6] javac: $(grep -c . "$BUILD/sources.txt") source files"

# ----------------------------- 3. dex ----------------------------------------
if [ -f "$LIB/r8.jar" ]; then
  find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
  java -cp "$LIB/r8.jar" com.android.tools.r8.D8 --min-api 24 --lib "$LIB/android.jar" --output "$BUILD/dex" @"$BUILD/classes.txt"
else
  "$DX" --dex --min-sdk-version=24 --output "$BUILD/dex/classes.dex" "$BUILD/classes"
fi
echo "==> [3/6] dx: classes.dex ($(stat -c%s "$BUILD/dex/classes.dex") bytes)"

# ----------------------------- 4. package ------------------------------------
rm -f "$BUILD/app.unsigned.apk"
"$AAPT" package -f -M "$MANIFEST" -S "$RES" -I "$LIB/android.jar" -F "$BUILD/app.unsigned.apk"
(cd "$BUILD/dex" && "$AAPT" add ../app.unsigned.apk classes.dex >/dev/null)
echo "==> [4/6] aapt: packaged ($(stat -c%s "$BUILD/app.unsigned.apk") bytes)"

# ----------------------------- 5. zipalign -----------------------------------
rm -f "$BUILD/app.aligned.apk"
"$ZIPALIGN" -f 4 "$BUILD/app.unsigned.apk" "$BUILD/app.aligned.apk"
echo "==> [5/6] zipalign: done"

# ----------------------------- 6. sign ---------------------------------------
KS="$ROOT/csp-native.keystore"
[ -f "$KS" ] || die "keystore missing: $KS (see BUILDING.md for the keytool command)"
KS_PASS="${KS_PASS:-cspnative2026}"
OUT="$DIST/CSPDailyReport-$VERSION_NAME.apk"
rm -f "$OUT"
"$APKSIGNER" sign --ks "$KS" --ks-key-alias csp \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT" "$BUILD/app.aligned.apk"
"$APKSIGNER" verify --print-certs "$OUT" > "$BUILD/verify.txt" 2>&1 || die "signature verification failed"
cp -f "$OUT" "$ROOT/../CSPDailyReport-$VERSION_NAME.apk"
cp -f "$OUT" "$ROOT/../CSPDailyReport.apk"
echo "==> [6/6] SIGNED: $OUT ($(du -h "$OUT" | cut -f1))"
grep -E "DN:|Digest of" "$BUILD/verify.txt" | sed 's/^/    /' || true

# ----------------------------- forbidden-dependency audit --------------------
echo "==> auditing packaged APK for web-runtime dependencies"
FAIL=0
for pat in WebView assets/www "api.php" "index.php" "offline.html" "sw.js" "manifest.webmanifest"; do
  if unzip -l "$OUT" 2>/dev/null | grep -qi "$pat"; then
    echo "    HITS(in zip): $pat"; FAIL=1
  fi
done
# String-level sweep of the dex: no browser classes, no legacy web server credentials.
if strings "$BUILD/dex/classes.dex" | grep -qiE \
    "Landroid/webkit/|ServiceWorker|XMLHttpRequest|infinityfree|sql[0-9]+\.[a-z]+\.com|if0_[0-9]+|FNQfQO9YL2gp|\.php"; then
  echo "    HITS: forbidden web/credential string in dex:"
  strings "$BUILD/dex/classes.dex" | grep -ioE \
    "Landroid/webkit/|ServiceWorker|XMLHttpRequest|infinityfree|\.php" \
    | sort -u | sed 's/^/      /'
  FAIL=1
fi
[ "$FAIL" = 0 ] && echo "    clean: native runtime, no WebView, Supabase cloud sync ready"

echo "DONE: $OUT"
APK_SHA256="$(sha256sum "$OUT" | cut -d' ' -f1)"
APK_SIZE="$(stat -c%s "$OUT")"
echo "============================================================"
echo " APK RELEASE SUMMARY FOR SUPABASE"
echo "============================================================"
echo " File Name:     $(basename "$OUT")"
echo " Version Name:  $VERSION_NAME"
echo " Version Code:  $VERSION_CODE"
echo " File Size:     $APK_SIZE bytes"
echo " SHA-256:       $APK_SHA256"
echo "============================================================"
echo ""
echo "--- COPY-PASTE SQL FOR SUPABASE (app_updates) ---"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS minimum_supported_version_code INTEGER DEFAULT 0;"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS update_type TEXT DEFAULT 'major';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS title TEXT DEFAULT 'Update Available';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS message TEXT DEFAULT '';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_path TEXT DEFAULT '';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_file_name TEXT DEFAULT '';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_file_path TEXT DEFAULT '';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_size_bytes BIGINT DEFAULT 0;"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_sha256 TEXT DEFAULT '';"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS release_notes JSONB DEFAULT '[]'::jsonb;"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS is_mandatory BOOLEAN DEFAULT false;"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS released_at TEXT DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'));"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS created_at TEXT DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'));"
echo "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS updated_at TEXT DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'));"
echo ""
echo "INSERT INTO public.app_updates (version_code, version_name, minimum_supported_version_code, update_type, title, message, apk_path, apk_file_name, apk_file_path, apk_size_bytes, apk_sha256, release_notes, is_mandatory, is_active)"
echo "VALUES ($VERSION_CODE, '$VERSION_NAME', 0, 'major', 'Update Available: v$VERSION_NAME', 'Release $VERSION_NAME updates and improvements.', '$(basename "$OUT")', '$(basename "$OUT")', '$(basename "$OUT")', $APK_SIZE, '$APK_SHA256', jsonb_build_array('Release $VERSION_NAME updates and improvements.'), false, true)"
echo "ON CONFLICT (version_code) DO UPDATE SET"
echo "  version_name = EXCLUDED.version_name,"
echo "  update_type = EXCLUDED.update_type,"
echo "  title = EXCLUDED.title,"
echo "  message = EXCLUDED.message,"
echo "  apk_path = EXCLUDED.apk_path,"
echo "  apk_file_name = EXCLUDED.apk_file_name,"
echo "  apk_file_path = EXCLUDED.apk_file_path,"
echo "  apk_size_bytes = EXCLUDED.apk_size_bytes,"
echo "  apk_sha256 = EXCLUDED.apk_sha256,"
echo "  release_notes = EXCLUDED.release_notes,"
echo "  is_mandatory = EXCLUDED.is_mandatory,"
echo "  is_active = true,"
echo "  updated_at = (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'));"
echo "-------------------------------------------------"

# Auto-advance patch version and version code for the next build
NEXT_PATCH=$((V_PATCH + 1))
NEXT_CODE=$((VERSION_CODE + 1))
cat <<EOF > "$VERSION_PROP"
VERSION_MAJOR=$V_MAJOR
VERSION_MINOR=$V_MINOR
VERSION_PATCH=$NEXT_PATCH
VERSION_CODE=$NEXT_CODE
EOF
echo ""
echo "==> Prepared next build version: $V_MAJOR.$V_MINOR.$NEXT_PATCH (versionCode: $NEXT_CODE)"
echo "============================================================"

