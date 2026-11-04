#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Build d'un APK + AAB de release SIGNÉS, en local
# =============================================================================
# Lit keystore.properties (ou les variables d'environnement) et produit :
#   app/build/outputs/apk/release/app-release.apk
#   app/build/outputs/bundle/release/app-release.aab
#
# Usage :
#   bash scripts/build-release.sh
#   VERSION_NAME=1.0.1 VERSION_CODE=2 bash scripts/build-release.sh
#   MIN_SDK=31 VERSION_NAME=1.0.1 bash scripts/build-release.sh   # plancher Android 12
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

# --- 1. Charger les identifiants de signature --------------------------------
if [ -f keystore.properties ] && [ -z "${STORE_PASSWORD:-}" ]; then
  # shellcheck disable=SC1091
  set -a; . ./keystore.properties; set +a
fi

export KEYSTORE_PATH="${KEYSTORE_PATH:-${storeFile:-}}"
if [ -z "$KEYSTORE_PATH" ]; then
  echo "❌ Aucun keystore : ni keystore.properties, ni KEYSTORE_PATH."
  echo "   Générez-le : bash scripts/generate-keystore.sh"
  exit 1
fi
case "$KEYSTORE_PATH" in
  /*) ;;
  *) KEYSTORE_PATH="$PWD/$KEYSTORE_PATH" ;;
esac
export KEYSTORE_PATH

export STORE_PASSWORD="${STORE_PASSWORD:-${storePassword:-}}"
export KEY_PASSWORD="${KEY_PASSWORD:-${keyPassword:-$STORE_PASSWORD}}"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "❌ Keystore introuvable : $KEYSTORE_PATH"
  exit 1
fi
if [ -z "$STORE_PASSWORD" ]; then
  read -r -s -p "Mot de passe du keystore : " STORE_PASSWORD; echo
  export STORE_PASSWORD
  export KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"
fi

echo "▶ Keystore : $KEYSTORE_PATH"
echo "▶ Version  : ${VERSION_NAME:-1.0} (code ${VERSION_CODE:-1}) — minSdk ${MIN_SDK:-24 par défaut}"

# --- 2. Build ----------------------------------------------------------------
./gradlew clean assembleRelease bundleRelease --no-configuration-cache --stacktrace

APK=$(ls app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)
AAB=$(ls app/build/outputs/bundle/release/*.aab 2>/dev/null | head -1)

echo
echo "✅ APK : $APK  ($(du -h "$APK" | cut -f1))"
echo "✅ AAB : $AAB  ($(du -h "$AAB" | cut -f1))"

# --- 3. Vérification de la signature ----------------------------------------
APKSIGNER=""
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
  APKSIGNER=$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
fi

if [ -n "$APKSIGNER" ]; then
  echo
  echo "▶ Vérification de la signature :"
  "$APKSIGNER" verify --print-certs "$APK"
else
  echo
  echo "ℹ️  apksigner introuvable (ANDROID_HOME non défini) — vérification ignorée."
fi

echo
echo "▶ SHA-256 :"
sha256sum "$APK" "$AAB" 2>/dev/null || true

cat <<EOF

Installation sur téléphone :
  adb install -r "$APK"
  # ou copier l'APK sur l'appareil et l'ouvrir (autoriser les sources inconnues)

⚠️  Si une version debug de Lumina est déjà installée, la désinstaller d'abord
    (signatures différentes → INSTALL_FAILED_UPDATE_INCOMPATIBLE).
EOF
