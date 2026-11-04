#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Envoi du keystore et des mots de passe vers les secrets GitHub
# =============================================================================
# Crée (ou met à jour) les 3 secrets nécessaires au workflow de release :
#   KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD
#
# Prérequis : GitHub CLI authentifiée avec le droit d'administration du dépôt
#   gh auth login          # ou : export GH_TOKEN=...
#
# Usage :
#   bash scripts/set-github-secrets.sh
#   bash scripts/set-github-secrets.sh --repo owner/repo
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

REPO=""
if [ "${1:-}" = "--repo" ] && [ -n "${2:-}" ]; then
  REPO="$2"
elif git remote get-url origin >/dev/null 2>&1; then
  REPO=$(git remote get-url origin | sed -E 's#\.git$##' | awk -F'[:/]' '{print $(NF-1)"/"$NF}')
fi

if [ -z "$REPO" ]; then
  echo "❌ Dépôt GitHub introuvable. Relancez avec : --repo owner/repo"
  exit 1
fi

# --- Identifiants ------------------------------------------------------------
if [ -f keystore.properties ]; then
  # shellcheck disable=SC1091
  set -a; . ./keystore.properties; set +a
fi

STORE_PASSWORD="${STORE_PASSWORD:-${storePassword:-}}"
KEY_PASSWORD="${KEY_PASSWORD:-${keyPassword:-$STORE_PASSWORD}}"
STORE_FILE="${KEYSTORE_PATH:-${storeFile:-my-upload-key.jks}}"
BASE64_FILE="$STORE_FILE.base64"

if [ -z "$STORE_PASSWORD" ]; then
  read -r -s -p "Mot de passe du keystore : " STORE_PASSWORD; echo
  KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"
fi

if [ ! -f "$STORE_FILE" ]; then
  echo "❌ Keystore introuvable : $STORE_FILE"
  exit 1
fi

if [ ! -f "$BASE64_FILE" ]; then
  echo "▶ Génération de $BASE64_FILE"
  base64 -w0 "$STORE_FILE" > "$BASE64_FILE" 2>/dev/null || base64 "$STORE_FILE" | tr -d '\n' > "$BASE64_FILE"
  chmod 600 "$BASE64_FILE"
fi

echo "▶ Dépôt cible : $REPO"
echo "▶ Keystore    : $STORE_FILE ($(wc -c < "$STORE_FILE") octets)"

# --- Envoi -------------------------------------------------------------------
TMP_ERR=$(mktemp)
trap 'rm -f "$TMP_ERR"' EXIT

push_secret() {
  local name="$1" value="$2"
  if gh secret set "$name" --repo "$REPO" --body "$value" 2>"$TMP_ERR"; then
    echo "✓ secret $name"
    return 0
  fi
  if grep -qi "403\|not accessible" "$TMP_ERR"; then
    return 2   # permission refusée
  fi
  echo "✗ $name"
  cat "$TMP_ERR"
  return 1
}

set +e
push_secret KEYSTORE_BASE64 "$(cat "$BASE64_FILE")"; RC1=$?
if [ "$RC1" -eq 0 ]; then
  push_secret STORE_PASSWORD "$STORE_PASSWORD"; RC2=$?
  push_secret KEY_PASSWORD "$KEY_PASSWORD"; RC3=$?
else
  RC2=0; RC3=0
fi
set -e

FAILED=0
[ "$RC1" -ne 0 ] && FAILED=$RC1
[ "$RC2" -ne 0 ] && FAILED=$RC2
[ "$RC3" -ne 0 ] && FAILED=$RC3
if [ "$RC1" -eq 2 ] || [ "$RC2" -eq 2 ] || [ "$RC3" -eq 2 ]; then
  FAILED=403
fi

if [ "$FAILED" -eq 403 ]; then
  cat <<EOF

❌ Le jeton GitHub utilisé n'a pas la permission « Secrets » (HTTP 403).

Deux solutions :

A) Depuis votre machine, avec votre propre authentification :
     gh auth login
     bash scripts/set-github-secrets.sh

B) Via l'interface web (2 minutes) :
     https://github.com/$REPO/settings/secrets/actions
     → New repository secret
        1. KEYSTORE_BASE64  = contenu intégral de $(basename "$BASE64_FILE") (une seule ligne)
        2. STORE_PASSWORD   = votre mot de passe
        3. KEY_PASSWORD     = le même mot de passe
EOF
  exit 1
fi

if [ "$FAILED" -ne 0 ]; then
  exit 1
fi

echo
gh secret list --repo "$REPO" 2>/dev/null || true
echo
echo "✅ Secrets en place. Poussez un tag pour lancer la release signée :"
echo "   git tag v1.0.1 && git push origin v1.0.1"
