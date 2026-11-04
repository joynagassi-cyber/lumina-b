#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Active les workflows GitHub Actions
# =============================================================================
# Les workflows vivent dans ci/workflows/ car le jeton de l'agent n'a pas la
# permission "workflows". GitHub ne les exécute que depuis .github/workflows/.
#
# Usage :
#   bash scripts/install-ci.sh            # copie + commit local
#   bash scripts/install-ci.sh --push     # copie + commit + push
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

SRC="ci/workflows"
DEST=".github/workflows"

if [ ! -d "$SRC" ]; then
  echo "❌ Dossier $SRC introuvable."
  exit 1
fi

mkdir -p "$DEST"
cp -v "$SRC"/*.yml "$DEST"/

echo
echo "✅ Workflows copiés dans $DEST :"
ls -1 "$DEST"
echo
echo "Prochaines étapes :"
echo "  git add $DEST"
echo "  git commit -m 'ci: activer les workflows Android'"
echo "  git push"

if [ "${1:-}" = "--push" ]; then
  git add "$DEST"
  git commit -m "ci: activer les workflows Android (CI, release signée, bootstrap keystore)" || true
  git push
  echo "✅ Poussé : onglet Actions du dépôt."
fi
