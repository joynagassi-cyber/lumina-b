#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Active les workflows GitHub Actions
# =============================================================================
# Les workflows vivent dans ci/workflows/ car le jeton utilisé par l'agent du
# dépôt n'a pas la permission "workflows" (GitHub refuse tout push de fichier
# dans .github/workflows/ sans cette permission).
# Or GitHub n'exécute un workflow que s'il est dans .github/workflows/.
#
# Usage :
#   bash scripts/install-ci.sh              # copie + commit local
#   bash scripts/install-ci.sh --push       # copie + commit + push
#   bash scripts/install-ci.sh --no-push    # copie seulement (inspection)
#
# Si le push est refusé (permission workflow), le script affiche les liens
# directs vers l'éditeur GitHub pour créer les fichiers en 3 clics.
# =============================================================================
set -uo pipefail

cd "$(dirname "$0")/.."

SRC="ci/workflows"
DEST=".github/workflows"
PUSH=0
case "${1:-}" in
  --push) PUSH=1 ;;
  --no-push|"") PUSH=0 ;;
  *) echo "Option inconnue : $1"; exit 1 ;;
esac

REPO=$(git remote get-url origin 2>/dev/null | sed -E 's#\.git$##' | awk -F'[:/]' '{print $(NF-1)"/"$NF}')
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)

if [ ! -d "$SRC" ]; then
  echo "❌ Dossier $SRC introuvable."
  exit 1
fi

mkdir -p "$DEST"
cp -v "$SRC"/*.yml "$DEST"/

echo
echo "✅ Workflows copiés dans $DEST :"
ls -1 "$DEST" | sed 's/^/   /'

if [ "$PUSH" -eq 0 ]; then
  cat <<EOF

Prochaines étapes (ou relancez ce script avec --push) :

  git add $DEST
  git commit -m 'ci: activer les workflows Android'
  git push

Puis vérifiez :  bash scripts/check-ci.sh
EOF
  exit 0
fi

git add "$DEST"
git commit -m "ci: activer les workflows Android (CI, release signée, bootstrap keystore)" >/dev/null 2>&1 || true

echo
echo "▶ Push vers origin/$BRANCH …"
if git push origin "$BRANCH" 2>/tmp/push.err; then
  echo "✅ Workflows poussés."
  echo "   Onglet Actions : https://github.com/$REPO/actions"
  echo "   Vérification   : bash scripts/check-ci.sh --run"
  exit 0
fi

cat /tmp/push.err | sed 's/^/   /'
cat <<EOF

❌ Le push a été refusé (permission « Workflows » manquante sur le jeton utilisé).

Le contenu a été copié dans $DEST mais ne peut pas être poussé automatiquement.
Trois solutions (la plus rapide en premier) :

A) Sur votre machine, avec votre propre compte GitHub :
     git add $DEST && git commit -m "ci: activer les workflows" && git push

B) Via l'interface GitHub — créer chaque fichier avec le chemin prérempli :
     https://github.com/$REPO/new/$BRANCH?filename=.github/workflows/android-ci.yml
     https://github.com/$REPO/new/$BRANCH?filename=.github/workflows/android-release.yml
     https://github.com/$REPO/new/$BRANCH?filename=.github/workflows/bootstrap-keystore.yml
   → coller le contenu du fichier correspondant de $SRC/ puis « Commit changes ».

C) Reconnecter GitHub depuis Arena en autorisant la permission « Workflows »,
   puis relancer : bash scripts/install-ci.sh --push
EOF
exit 1
