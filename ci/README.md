# Intégration continue — installation des workflows

Les 3 workflows sont **prêts à l'emploi** mais sont stockés ici (`ci/workflows/`) et non dans
`.github/workflows/` : le jeton GitHub utilisé par l'agent du dépôt n'a pas la permission
`workflows`, ce qui bloque tout push de fichier dans `.github/workflows/`.

GitHub n'active un workflow que s'il se trouve dans **`.github/workflows/`**. Il reste donc une
action d'une minute pour les activer.

## Option 1 — en local (recommandé)

```bash
bash scripts/install-ci.sh
# → copie les 3 fichiers dans .github/workflows/, commit et push
```

## Option 2 — directement sur github.com

1. Ouvrir <https://github.com/joynagassi-cyber/lumina-b> → bouton **Add file → Upload files**.
2. Glisser-déposer les 3 fichiers de `ci/workflows/` dans un dossier `.github/workflows`
   (créer le chemin en tapant `.github/workflows/` dans le champ du nom de fichier).
3. Commit sur la branche `main`.
4. Onglet **Actions** : les workflows « Android CI », « Android Release » et
   « Bootstrap keystore » apparaissent.

## Contenu

| Fichier | Déclencheur | Effet |
|---|---|---|
| `android-ci.yml` | push / PR / manuel | tests unitaires + APK debug en artifact |
| `android-release.yml` | tag `v*` / manuel | APK + AAB signés, vérifiés avec `apksigner`, GitHub Release |
| `bootstrap-keystore.yml` | manuel, une seule fois | génère le keystore de signature et le publie en artifact |

Prérequis pour `android-release.yml` : secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`,
`KEY_PASSWORD` — voir [`docs/SIGNING-ET-RELEASE.md`](../docs/SIGNING-ET-RELEASE.md).
