#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Pipeline de build pour GitHub Actions (runner ubuntu-latest)
# =============================================================================
# Appelé par le workflow .github/workflows/<nom>.yml (coquille stable de ~20 lignes) :
#
#   - run: bash ci/run-build.sh
#     env:
#       KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
#       STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
#       KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
#       GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
#
# Tout l'entretien du pipeline se fait ICI (fichier hors .github/workflows/,
# donc modifiable par l'agent), sans jamais retoucher le workflow.
#
# Ce que fait le script :
#   1. sélectionne un JDK 21 (préinstallé sur les runners GitHub)
#   2. localise le SDK Android, accepte les licences, installe la plateforme 36
#   3. régénère debug.keystore et décode le keystore de release depuis les secrets
#   4. build : APK debug toujours, APK + AAB signés si le keystore est disponible
#   5. sur un tag vX.Y.Z : attache les binaires à la GitHub Release et la publie
#
# Variables optionnelles : VERSION_NAME, VERSION_CODE, MIN_SDK, SKIP_RELEASE=1
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

log() { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }

# --- 1. JDK ------------------------------------------------------------------
log "JDK"
jdk_major() {
  [ -x "$1/bin/java" ] || { echo 0; return; }
  "$1/bin/java" -version 2>&1 | head -1 \
    | sed -E 's/.*"([0-9]+)(\.[0-9]+)?.*/\1/' | grep -E '^[0-9]+$' || echo 0
}

CANDIDATES=()
[ -n "${JAVA_HOME:-}" ] && CANDIDATES+=("$JAVA_HOME")
for d in /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/*/x64 \
         /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/*/arm64 \
         /opt/hostedtoolcache/Java_*/*/x64 /usr/lib/jvm/*; do
  [ -d "$d" ] && CANDIDATES+=("$d")
done

pick_jdk() { # $1 = version majeure souhaitée -> affiche le JAVA_HOME correspondant
  local want="$1" c
  for c in "${CANDIDATES[@]}"; do
    [ "$(jdk_major "$c")" = "$want" ] && { echo "$c"; return 0; }
  done
  return 1
}

if ! { [ -n "${JAVA_HOME:-}" ] && [ "$(jdk_major "$JAVA_HOME")" -ge 17 ]; }; then
  if JDK21=$(pick_jdk 21); then export JAVA_HOME="$JDK21"
  elif JDK17=$(pick_jdk 17); then export JAVA_HOME="$JDK17"
  else
    echo "::error::Aucun JDK 17+ trouvé (candidats : ${CANDIDATES[*]:-aucun})."
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME ($(jdk_major "$JAVA_HOME"))"
java -version 2>&1 | head -1

# --- 2. SDK Android ----------------------------------------------------------
log "SDK Android"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"

SDKMANAGER=""
for c in "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
         "$ANDROID_HOME"/cmdline-tools/*/bin/sdkmanager; do
  if [ -x "$c" ]; then SDKMANAGER="$c"; break; fi
done
if [ -z "$SDKMANAGER" ] && command -v sdkmanager >/dev/null 2>&1; then
  SDKMANAGER="$(command -v sdkmanager)"
fi

if [ -n "$SDKMANAGER" ]; then
  yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" "platform-tools" >/dev/null 2>&1 || true
  "$SDKMANAGER" "platforms;android-36.1" >/dev/null 2>&1 \
    || "$SDKMANAGER" "platforms;android-36" >/dev/null 2>&1 \
    || echo "::warning::Plateforme 36 non installée — AGP tentera le téléchargement."
else
  echo "::warning::sdkmanager introuvable — on laisse AGP gérer le SDK."
fi

# --- 3. Keystores ------------------------------------------------------------
log "Keystores"
chmod +x gradlew scripts/*.sh 2>/dev/null || true
bash scripts/generate-debug-keystore.sh

if [ -n "${KEYSTORE_BASE64:-}" ]; then
  echo "$KEYSTORE_BASE64" | base64 -d > my-upload-key.jks
  chmod 600 my-upload-key.jks
  export KEYSTORE_PATH="$PWD/my-upload-key.jks"
  echo "Keystore de release : décodé depuis les secrets"
  keytool -list -keystore my-upload-key.jks -storepass "${STORE_PASSWORD:?}" | sed -n '2,6p'
else
  echo "::warning::KEYSTORE_BASE64 absent — APK de release non signé ignoré (debug uniquement)."
fi

# --- 4. Build ----------------------------------------------------------------
log "Build"
if [ "${GITHUB_REF_TYPE:-}" = "tag" ]; then
  export VERSION_NAME="${GITHUB_REF_NAME#v}"
fi
export VERSION_CODE="${VERSION_CODE:-${GITHUB_RUN_NUMBER:-1}}"
echo "VERSION_NAME=${VERSION_NAME:-1.0}  VERSION_CODE=$VERSION_CODE  MIN_SDK=${MIN_SDK:-24}"

TASKS="assembleDebug"
if [ -n "${KEYSTORE_PATH:-}" ]; then
  TASKS="assembleDebug assembleRelease bundleRelease"
fi
./gradlew $TASKS --no-configuration-cache --stacktrace

ls -lh app/build/outputs/apk/debug/*.apk 2>/dev/null || true
ls -lh app/build/outputs/apk/release/*.apk 2>/dev/null || true

# Vérification de la signature si un APK de release existe
APK=$(ls app/build/outputs/apk/release/*.apk 2>/dev/null | head -1 || true)
if [ -n "$APK" ]; then
  APKSIGNER=$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)
  if [ -n "$APKSIGNER" ]; then
    log "Vérification de la signature"
    "$APKSIGNER" verify --print-certs "$APK"
  fi
fi

# --- 5. GitHub Release (tags uniquement) -------------------------------------
if [ "${SKIP_RELEASE:-0}" = "1" ]; then
  log "Release ignorée (SKIP_RELEASE=1)"
  exit 0
fi

if [ "${GITHUB_REF_TYPE:-}" != "tag" ]; then
  log "Pas un tag : artifacts uniquement (pas de release)"
  exit 0
fi
if [ -z "${KEYSTORE_PATH:-}" ]; then
  echo "::warning::Release non créée : APK signé indisponible."
  exit 0
fi

log "GitHub Release $GITHUB_REF_NAME"
TAG="$GITHUB_REF_NAME"
FILES=$(ls app/build/outputs/apk/release/*.apk app/build/outputs/bundle/release/*.aab 2>/dev/null || true)
if [ -z "$FILES" ]; then
  echo "::error::Aucun binaire à publier."
  exit 1
fi

NOTES_LINE="CHANGELOG.md"
if gh release view "$TAG" >/dev/null 2>&1; then
  gh release upload "$TAG" $FILES --clobber
else
  if [ -f "$NOTES_LINE" ]; then
    gh release create "$TAG" $FILES --title "Lumina-beta ${TAG#v}" \
      --notes-file "$NOTES_LINE" --verify-tag
  else
    gh release create "$TAG" $FILES --title "Lumina-beta ${TAG#v}" --generate-notes --verify-tag
  fi
fi

gh release edit "$TAG" --draft=false 2>/dev/null || true
echo
echo "✅ Release publiée : $(gh release view "$TAG" --json url --jq .url 2>/dev/null || echo "$TAG")"
