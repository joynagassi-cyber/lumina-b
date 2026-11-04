#!/usr/bin/env bash
# =============================================================================
# Lumina-beta - Génère les liens GitHub « créer le fichier » pour les workflows
# =============================================================================
# GitHub n'autorise l'écriture dans .github/workflows/ qu'aux comptes disposant
# de la permission « Workflows ». Ce script produit des liens vers l'éditeur
# GitHub avec le CHEMIN et le CONTENU préremplis : un clic, puis « Commit changes ».
#
# Usage :
#   bash scripts/print-workflow-links.sh            # fichier INSTALLER-WORKFLOWS.md
#   bash scripts/print-workflow-links.sh --stdout   # affiche seulement
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

REPO=$(git remote get-url origin 2>/dev/null | sed -E 's#\.git$##' | awk -F'[:/]' '{print $(NF-1)"/"$NF}')
BRANCH="${BRANCH:-main}"
OUT="INSTALLER-WORKFLOWS.md"

if [ -z "${REPO:-}" ]; then
  echo "❌ Dépôt GitHub introuvable."
  exit 1
fi

python3 - "$REPO" "$BRANCH" "$OUT" "${1:-}" <<'PY'
import sys, os, urllib.parse

repo, branch, out, mode = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
files = sorted(f for f in os.listdir('ci/workflows') if f.endswith('.yml'))
order = ['release-lite.yml', 'android-ci.yml', 'android-release.yml', 'bootstrap-keystore.yml']
files = [f for f in order if f in files] + [f for f in files if f not in order]

lines = [
    "# Installer les workflows GitHub Actions",
    "",
    "Le projet est **entièrement configuré** (Gradle, signature, scripts, workflows).",
    "Seule l'écriture dans `.github/workflows/` est réservée au propriétaire du dépôt.",
    "",
    "## Méthode 1 — lien avec contenu prérempli (2 clics par fichier)",
    "",
    "Cliquer, vérifier que le contenu est bien présent, puis **Commit changes**.",
    "",
]
fallbacks = []
for f in files:
    content = open(f'ci/workflows/{f}').read()
    url = (f"https://github.com/{repo}/new/{branch}"
           f"?filename={urllib.parse.quote('.github/workflows/' + f)}"
           f"&value={urllib.parse.quote(content)}")
    lines.append(f"- **`{f}`** → [ouvrir l'éditeur]({url})")
    fallbacks.append((f, len(url)))

lines += [
    "",
    "> Si un lien est tronqué par le navigateur (URL longue), utiliser la méthode 2.",
    "",
    "## Méthode 2 — copier / coller",
    "",
    "Créer le fichier avec le chemin prérempli, puis coller le contenu de `ci/workflows/…` :",
    "",
]
for f in files:
    lines.append(f"- `{f}` → https://github.com/{repo}/new/{branch}?filename={urllib.parse.quote('.github/workflows/' + f)}")

lines += [
    "",
    "## Méthode 3 — terminal (le plus simple si le dépôt est cloné)",
    "",
    "```bash",
    "bash scripts/install-ci.sh --push",
    "```",
    "",
    "## Après l'installation",
    "",
    "1. **Secrets** (obligatoires pour un APK signé) : `bash scripts/set-github-secrets.sh`",
    "   ou coller les 3 valeurs de `SECRETS-A-COPIER.md` dans",
    f"   https://github.com/{repo}/settings/secrets/actions",
    "2. **Lancer un build** : onglet Actions → « Android CI » → *Run workflow*,",
    f"   ou https://github.com/{repo}/actions/workflows/android-ci.yml",
    "3. **Release complète** : `bash scripts/first-release.sh 1.0.0`",
    "",
]

text = "\n".join(lines)
if mode == "--stdout":
    print(text)
else:
    open(out, 'w').write(text)
    print(f"✅ {out} généré")
    for f, n in fallbacks:
        print(f"   {f}: lien de {n} caractères")
PY
