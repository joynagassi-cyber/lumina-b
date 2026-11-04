#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Vérifie que les workflows GitHub Actions sont bien en place
# =============================================================================
# Contrôle, dans l'ordre :
#   1. présence des fichiers dans .github/workflows/ (branche visée)
#   2. présence des 3 secrets de signature
#   3. (avec --run) déclenche le workflow « Android CI » et suit l'exécution
#
# Usage :
#   bash scripts/check-ci.sh                 # état sur la branche par défaut
#   bash scripts/check-ci.sh --branch <ref>  # autre branche
#   bash scripts/check-ci.sh --run           # déclenche + suit le build
#
# Prérequis : gh authentifiée (gh auth login).
# =============================================================================
set -uo pipefail

cd "$(dirname "$0")/.."

REF=""
RUN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --branch) REF="${2:-}"; shift 2 ;;
    --run)    RUN=1; shift ;;
    *) echo "Option inconnue : $1"; exit 1 ;;
  esac
done

REPO=$(git remote get-url origin 2>/dev/null | sed -E 's#\.git$##' | awk -F'[:/]' '{print $(NF-1)"/"$NF}')
if [ -z "${REPO:-}" ]; then
  echo "❌ Dépôt GitHub introuvable (git remote origin)."
  exit 1
fi

if [ -z "$REF" ]; then
  REF=$(gh repo view "$REPO" --json defaultBranchRef --jq '.defaultBranchRef.name' 2>/dev/null || echo "main")
fi

echo "Dépôt  : $REPO"
echo "Branche: $REF"
echo

RC=0

# --- 1. Workflows installés ? ------------------------------------------------
echo "1) Workflows dans .github/workflows/ …"
if RAW=$(gh api "repos/$REPO/contents/.github/workflows?ref=$REF" 2>&1) \
   && ! printf '%s' "$RAW" | grep -q '"message"'; then
  FILES=$(printf '%s' "$RAW" | grep -o '"name": *"[^"]*"' | sed -E 's/.*"([^"]+)"$/\1/')
else
  FILES=""
fi
if [ -z "$FILES" ]; then
  echo "   ❌ Aucun workflow installé sur '$REF'."
  echo "      → bash scripts/install-ci.sh --push"
  RC=1
else
  echo "$FILES" | sed 's/^/   ✓ /'
  for wf in android-ci.yml android-release.yml; do
    echo "$FILES" | grep -qx "$wf" || { echo "   ⚠️  $wf manquant"; RC=1; }
  done
fi
echo

# --- 2. Secrets --------------------------------------------------------------
echo "2) Secrets de signature …"
SECRETS=$(gh secret list --repo "$REPO" 2>/dev/null)
if [ -z "$SECRETS" ]; then
  echo "   ⚠️  Impossible de lister les secrets (jeton sans permission, ou aucun secret)."
  echo "      → https://github.com/$REPO/settings/secrets/actions"
  echo "      Attendu : KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD"
else
  echo "$SECRETS" | awk '{print "   ✓ "$1}'
  for s in KEYSTORE_BASE64 STORE_PASSWORD KEY_PASSWORD; do
    echo "$SECRETS" | grep -q "^$s" || { echo "   ⚠️  $s manquant"; RC=1; }
  done
fi
echo

# --- 3. Déclenchement --------------------------------------------------------
if [ "$RUN" -eq 1 ]; then
  echo "3) Déclenchement du workflow « Android CI » sur '$REF' …"
  if ! gh workflow run android-ci.yml --repo "$REPO" --ref "$REF" 2>/tmp/ghrun.err; then
    echo "   ❌ Déclenchement impossible :"
    sed 's/^/      /' /tmp/ghrun.err
    RC=1
  else
    sleep 8
    RUN_ID=$(gh run list --repo "$REPO" --workflow=android-ci.yml --limit 1 --json databaseId --jq '.[0].databaseId')
    echo "   ▶ run #$RUN_ID — suivi en direct :"
    gh run watch "$RUN_ID" --repo "$REPO" --exit-status || RC=1
  fi
else
  echo "3) Pour lancer le build maintenant :"
  echo "   gh workflow run android-ci.yml --repo $REPO --ref $REF"
  echo "   bash scripts/check-ci.sh --run    # équivalent + suivi du résultat"
fi

echo
if [ "$RC" -eq 0 ]; then
  echo "✅ Tout est en place."
else
  echo "⚠️  Des éléments manquent (voir ci-dessus)."
fi
exit "$RC"
