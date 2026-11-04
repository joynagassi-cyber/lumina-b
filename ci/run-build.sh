#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Pipeline de build pour GitHub Actions (runner ubuntu-latest)
# =============================================================================
# Appelé par la coquille stable .github/workflows/<nom>.yml :
#
#   - run: bash ci/run-build.sh
#     env:
#       KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
#       STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
#       KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
#       GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
#
# Toute la logique vit ici (fichier hors .github/workflows/, donc modifiable
# sans retoucher le workflow).
#
# Chaque phase est isolée : en cas d'échec, les 30 dernières lignes de sortie
# sont publiées dans une *annotation* GitHub — seul canal lisible par l'agent
# (les logs bruts du run ne sont pas téléchargeables depuis sa sandbox).
#
# Variables optionnelles : VERSION_NAME, VERSION_CODE, MIN_SDK, SKIP_RELEASE=1
# =============================================================================
set -Eeuo pipefail

cd "$(dirname "$0")/.."
ROOT="$PWD"

# fd 3 = stdout d'origine : les annotations doivent y être écrites même quand
# la phase en cours a redirigé sa sortie vers un fichier de capture.
exec 3>&1

log() { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }

# --- Plomberie d'échec -------------------------------------------------------
MASK_VALUES=()
mask() { # masque une valeur sensible dans les annotations
  if [ -n "${1:-}" ]; then MASK_VALUES+=("$1"); fi
  return 0
}

escape_ann() { # échappe une sortie multi-lignes pour une annotation GitHub
  local msg="$1" v
  for v in "${MASK_VALUES[@]:-}"; do [ -n "$v" ] && msg=${msg//"$v"/***}; done
  printf '%s' "$msg" | sed -e 's/%/%25/g' -e 's/\r//g' -e 's/\x1e//g' \
    | tr '\n' '\036' | sed 's/\x1e/ %0A /g' | cut -c1-3000
}

note() { # $1 = titre, $2 = contenu -> annotation "notice" lisible par l'agent
  echo "::notice title=${1}::$(escape_ann "${2:-}")"
}

CURRENT_STEP=""
CURRENT_OUT=""

summarize() { # sortie de build utile : sans les trames de pile (--stacktrace)
  grep -v -E '^[[:space:]]+at [a-zA-Z_$]' "$1" 2>/dev/null | grep -v -E '^[[:space:]]*$' \
    | tail -n 30 || true
}

on_error() { # errexit conserve son effet dans les phases ; l'annotation porte la sortie capturée
  local rc=$? step="${CURRENT_STEP:-script}" out="${CURRENT_OUT:-}" msg=""
  set +e
  if [ -n "$out" ] && [ -f "$out" ]; then msg=$(summarize "$out"); fi
  printf '::endgroup::\n' >&3
  printf '::error title=%s::code %s — %s\n' "$step" "$rc" "$(escape_ann "${msg:-(aucune sortie)}")" >&3
  exit "$rc"
}
trap on_error ERR

run_step() { # $1 = nom affiché, $2... = fonction de phase
  local name="$1"; shift
  local out
  out=$(mktemp)
  CURRENT_STEP="$name"
  CURRENT_OUT="$out"
  echo "::group::${name}"
  # Groupe de commandes + redirection : contrairement à `cmd || rc=$?`, cela ne
  # désactive PAS `set -e` dans la phase appelée — un `gradlew` en échec arrête
  # donc bien le pipeline et déclenche on_error().
  { "$@" 2>&1; } >>"$out"
  CURRENT_STEP=""
  CURRENT_OUT=""
  tail -n 25 "$out" || true
  note "${name} — OK" "$(tail -n 8 "$out")"
  echo "::endgroup::"
  rm -f "$out"
}

# --- 1. JDK ------------------------------------------------------------------
jdk_major() { # $1 = JAVA_HOME candidat -> version majeure sur stdout
  [ -x "$1/bin/java" ] || { printf '0'; return 0; }
  "$1/bin/java" -version 2>&1 | sed -n '1p' \
    | sed -E 's/.*"([0-9]+)(\.[0-9]+)?.*/\1/' | grep -E '^[0-9]+$' || printf '0'
}

phase_jdk() {
  local candidates=() d best="" best_v=0 v
  [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
  for d in /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/*/x64 \
           /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/*/arm64 \
           /opt/hostedtoolcache/Java_*/*/x64 /usr/lib/jvm/*; do
    if [ -d "$d" ]; then candidates+=("$d"); fi
  done
  for d in "${candidates[@]}"; do
    v=$(jdk_major "$d")
    if [ "$v" -eq 21 ]; then best="$d"; best_v=21; break; fi
    if [ "$v" -ge 17 ] && [ "$v" -gt "$best_v" ]; then best="$d"; best_v="$v"; fi
  done
  if [ -z "$best" ]; then
    echo "Candidats examinés : ${candidates[*]:-aucun}"
    echo "JAVA_HOME=${JAVA_HOME:-vide}"
    return 1
  fi
  export JAVA_HOME="$best"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "JAVA_HOME=$JAVA_HOME"
  echo "version majeure : $best_v"
  java -version 2>&1 | sed -n '1,3p'
  command -v keytool
}

# --- 2. SDK Android ----------------------------------------------------------
phase_sdk() {
  export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  echo "ANDROID_HOME=$ANDROID_HOME"
  [ -d "$ANDROID_HOME" ] || { echo "SDK introuvable dans $ANDROID_HOME"; return 1; }
  ls "$ANDROID_HOME" || true

  local sdkmanager="" c
  for c in "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
           "$ANDROID_HOME"/cmdline-tools/*/bin/sdkmanager; do
    if [ -x "$c" ]; then sdkmanager="$c"; break; fi
  done
  if [ -z "$sdkmanager" ] && command -v sdkmanager >/dev/null 2>&1; then
    sdkmanager="$(command -v sdkmanager)"
  fi
  echo "sdkmanager=$sdkmanager"
  if [ -n "$sdkmanager" ]; then
    yes 2>/dev/null | "$sdkmanager" --licenses >/dev/null 2>&1 || true
    "$sdkmanager" "platform-tools" >/dev/null 2>&1 || true
    "$sdkmanager" "platforms;android-36" >/dev/null 2>&1 || true
  fi
  return 0
}

# --- 3. Keystores ------------------------------------------------------------
phase_keystore() {
  bash scripts/generate-debug-keystore.sh
  echo "debug.keystore : $(ls -l debug.keystore 2>/dev/null || echo absent)"

  if [ -z "${KEYSTORE_BASE64:-}" ]; then
    echo "KEYSTORE_BASE64 vide -> build de release non signé impossible (debug seul)."
    return 0
  fi
  printf '%s' "$KEYSTORE_BASE64" | tr -d '\n\r ' | base64 -d > my-upload-key.jks
  chmod 600 my-upload-key.jks
  export KEYSTORE_PATH="$ROOT/my-upload-key.jks"
  echo "my-upload-key.jks : $(wc -c < my-upload-key.jks) octets"

  if [ -z "${STORE_PASSWORD:-}" ]; then
    echo "STORE_PASSWORD vide : secret absent ou vide."; return 1
  fi
  if [ -z "${KEY_PASSWORD:-}" ]; then
    echo "KEY_PASSWORD vide : secret absent ou vide."; return 1
  fi
  keytool -list -keystore my-upload-key.jks -storepass "$STORE_PASSWORD" \
    -alias upload | sed -n '1,12p'
}

# --- 4. Build ----------------------------------------------------------------
phase_build() {
  chmod +x gradlew
  if [ "${GITHUB_REF_TYPE:-}" = "tag" ] && [ -n "${GITHUB_REF_NAME:-}" ]; then
    export VERSION_NAME="${GITHUB_REF_NAME#v}"
  fi
  export VERSION_CODE="${VERSION_CODE:-${GITHUB_RUN_NUMBER:-1}}"
  echo "VERSION_NAME=${VERSION_NAME:-1.0} VERSION_CODE=$VERSION_CODE MIN_SDK=${MIN_SDK:-24}"

  local tasks="assembleDebug"
  if [ -n "${KEYSTORE_PATH:-}" ]; then
    tasks="assembleDebug assembleRelease bundleRelease"
  fi
  echo "Tâches Gradle : $tasks"
  ./gradlew $tasks --no-configuration-cache --stacktrace
  echo "--- binaires produits ---"
  find app/build -type f \( -name '*.apk' -o -name '*.aab' \) 2>/dev/null || true
  echo "--- arborescence app/build/outputs ---"
  find app/build/outputs -maxdepth 3 2>/dev/null | sed -n '1,40p' || true
}

# --- 5. Vérification de signature -------------------------------------------
phase_verify() {
  local apk
  apk=$(ls app/build/outputs/apk/release/*.apk 2>/dev/null | sed -n '1p' || true)
  [ -n "$apk" ] || { echo "Aucun APK de release (normal hors release)."; return 0; }
  local apksigner
  apksigner=$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n 1 || true)
  if [ -n "$apksigner" ]; then
    "$apksigner" verify --print-certs "$apk"
  else
    echo "apksigner introuvable — APK : $(ls -l "$apk")"
  fi
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$apk" | tee SHA256SUMS.txt; fi
  return 0
}

# --- 6. GitHub Release (tags uniquement) -------------------------------------
phase_release() {
  local tag="${GITHUB_REF_NAME:?}"
  local files
  files=$(ls app/build/outputs/apk/release/*.apk app/build/outputs/bundle/release/*.aab 2>/dev/null || true)
  [ -n "$files" ] || { echo "Aucun binaire à publier."; return 1; }
  if gh release view "$tag" >/dev/null 2>&1; then
    echo "Release $tag existante -> ajout des binaires"
    gh release upload "$tag" $files --clobber
  else
    if [ -f CHANGELOG.md ]; then
      gh release create "$tag" $files --title "Lumina-beta ${tag#v}" \
        --notes-file CHANGELOG.md --verify-tag
    else
      gh release create "$tag" $files --title "Lumina-beta ${tag#v}" \
        --generate-notes --verify-tag
    fi
  fi
  gh release edit "$tag" --draft=false 2>/dev/null || true
  gh release view "$tag" --json url,assets --jq '"\(.url) — \(.assets | length) fichier(s)"'
}

# --- Orchestration -----------------------------------------------------------
main() {
  log "Environnement"
  echo "ref=${GITHUB_REF:-local} type=${GITHUB_REF_TYPE:-local} run=${GITHUB_RUN_NUMBER:-0}"
  echo "répertoire : $ROOT"

  mask "${KEYSTORE_BASE64:-}"
  mask "${STORE_PASSWORD:-}"
  mask "${KEY_PASSWORD:-}"

  run_step "1. JDK"                 phase_jdk
  run_step "2. SDK Android"         phase_sdk
  run_step "3. Keystores"           phase_keystore
  run_step "4. Compilation Gradle"  phase_build
  run_step "5. Vérification APK"    phase_verify

  if [ "${SKIP_RELEASE:-0}" = "1" ]; then
    log "Release ignorée (SKIP_RELEASE=1)"
    return 0
  fi
  if [ "${GITHUB_REF_TYPE:-}" != "tag" ]; then
    log "Pas un tag : artifacts uniquement, pas de release"
    return 0
  fi
  if [ -z "${KEYSTORE_PATH:-}" ]; then
    echo "::warning::Release non publiée : keystore de signature absent."
    return 0
  fi
  log "GitHub Release"
  run_step "6. Publication de la release" phase_release
}

main "$@"
