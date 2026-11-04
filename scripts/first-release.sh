#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Publier la release (APK + AAB signés) sur GitHub
# =============================================================================
# Enchaîne :
#   1. vérification des prérequis (workflows installés, 3 secrets présents)
#   2. déclenchement du workflow « Android Release » (build signé)
#   3. suivi du run jusqu'à la fin
#   4. téléchargement de l'artifact, création/publication de la GitHub Release
#      avec l'APK, l'AAB et les sommes SHA-256
#
# Usage :
#   bash scripts/first-release.sh            # version 1.0.0
#   bash scripts/first-release.sh 1.0.1      # autre version
#   bash scripts/first-release.sh 1.0.1 --draft-only   # ne publie pas la release
#
# Prérequis : gh authentifiée avec les droits d'administration du dépôt.
# =============================================================================
set -uo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:-1.0.0}"
MODE="${2:-}"
TAG="v$VERSION"
REPO=$(git remote get-url origin 2>/dev/null | sed -E 's#\.git$##' | awk -F'[:/]' '{print $(NF-1)"/"$NF}')
REF=$(git rev-parse --abbrev-ref HEAD)

if [ -z "${REPO:-}" ]; then
  echo "❌ Dépôt GitHub introuvable (git remote origin)."
  exit 1
fi

echo "═══════════════════════════════════════════════════════════════"
echo " Release Lumina-beta $VERSION  ($TAG)"
echo " Dépôt : $REPO   Branche : $REF"
echo "═══════════════════════════════════════════════════════════════"
echo

RC=0

# --- 1. Prérequis ------------------------------------------------------------
echo "1) Workflows installés ?"
if gh api "repos/$REPO/contents/.github/workflows?ref=$REF" >/dev/null 2>&1; then
  echo "   ✓ .github/workflows présent sur $REF"
else
  echo "   ❌ .github/workflows absent sur '$REF'."
  echo "      → bash scripts/install-ci.sh --push"
  echo "      → puis relancer ce script."
  RC=1
fi

echo "2) Secrets de signature ?"
if SECRETS=$(gh secret list --repo "$REPO" 2>/dev/null); then
  MISSING=""
  for s in KEYSTORE_BASE64 STORE_PASSWORD KEY_PASSWORD; do
    echo "$SECRETS" | grep -q "^$s" || MISSING="$MISSING $s"
  done
  if [ -n "$MISSING" ]; then
    echo "   ❌ Secrets manquants :$MISSING"
    echo "      → bash scripts/set-github-secrets.sh"
    RC=1
  else
    echo "   ✓ KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD"
  fi
else
  echo "   ❌ Impossible de lire les secrets (permission refusée)."
  echo "      → https://github.com/$REPO/settings/secrets/actions"
  RC=1
fi

if [ "$RC" -ne 0 ]; then
  echo
  echo "⛔ Build impossible : les deux prérequis ci-dessus sont obligatoires."
  echo "   Alternative 100 % locale (sans GitHub) : bash scripts/build-release.sh"
  exit 1
fi
echo

# --- 2. Déclenchement --------------------------------------------------------
echo "3) Déclenchement du build signé …"
if ! gh workflow run android-release.yml --repo "$REPO" --ref "$REF" -f version_name="$VERSION" 2>/tmp/rel.err; then
  echo "   ❌ Échec du déclenchement :"
  sed 's/^/      /' /tmp/rel.err
  exit 1
fi
sleep 10

RUN_ID=$(gh run list --repo "$REPO" --workflow=android-release.yml --limit 1 \
  --json databaseId,headBranch --jq ".[] | select(.headBranch==\"$REF\") | .databaseId" | head -1)

if [ -z "$RUN_ID" ]; then
  echo "   ⚠️  Run introuvable — voir https://github.com/$REPO/actions"
  exit 1
fi

echo "   ▶ run #$RUN_ID — suivi :"
gh run watch "$RUN_ID" --repo "$REPO" --exit-status || {
  echo "   ❌ Le build a échoué. Logs : gh run view $RUN_ID --repo $REPO --log-failed"
  exit 1
}

# --- 3. Récupération de l'artifact ------------------------------------------
echo "4) Téléchargement de l'artifact signé …"
DIST="dist/$TAG"
rm -rf "$DIST"; mkdir -p "$DIST"
if ! gh run download "$RUN_ID" --repo "$REPO" -D "$DIST" 2>/tmp/dl.err; then
  echo "   ❌ Téléchargement impossible :"; sed 's/^/      /' /tmp/dl.err; exit 1
fi
find "$DIST" -type f -exec ls -lh {} \; | sed 's/^/   /'

# --- 4. Release --------------------------------------------------------------
echo "5) Publication de la release $TAG …"
NOTES="CHANGELOG.md"
if ! gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release create "$TAG" --repo "$REPO" --target main \
    --title "Lumina-beta $VERSION" \
    --notes-file "$NOTES" \
    --draft
  echo "   ✓ release brouillon créée"
else
  echo "   ✓ release existante réutilisée"
fi

gh release upload "$TAG" -R "$REPO" $(find "$DIST" -type f) --clobber
echo "   ✓ APK / AAB / SHA256 ajoutés"

if [ "$MODE" = "--draft-only" ]; then
  echo
  echo "✅ Release $TAG prête en brouillon (non publiée)."
  echo "   Publier : gh release edit $TAG --repo $REPO --draft=false"
  exit 0
fi

gh release edit "$TAG" --repo "$REPO" --draft=false
echo
echo "✅ Release publiée : https://github.com/$REPO/releases/tag/$TAG"
