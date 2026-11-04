# Intégration continue — installation des workflows

Les 3 workflows sont **prêts à l'emploi** mais sont stockés ici (`ci/workflows/`) et non dans
`.github/workflows/` : le jeton GitHub utilisé par l'agent du dépôt n'a pas la permission
`workflows`, ce qui bloque tout push de fichier dans `.github/workflows/`.

GitHub n'active un workflow que s'il se trouve dans **`.github/workflows/`**. Il reste donc une
action d'une minute pour les activer.

## Option 1 — en local (recommandé)

```bash
bash scripts/install-ci.sh --push
# → copie les 3 fichiers dans .github/workflows/, commit et push

bash scripts/check-ci.sh        # vérifie workflows + secrets
bash scripts/check-ci.sh --run  # déclenche le build et suit le résultat
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

Actions utilisées (versions vérifiées comme existantes, entrées compatibles) :
`actions/checkout@v7`, `actions/setup-java@v6`, `actions/upload-artifact@v7`,
`android-actions/setup-android@v4`, `gradle/actions/setup-gradle@v6`.

La CI est découpée en **deux jobs indépendants** : `APK debug` (produit toujours l'artifact) et
`Tests unitaires` (Room/Robolectric, avec `robolectric.properties` fixé sur l'API 34). Un échec
de test ne prive donc jamais d'APK.

Prérequis pour `android-release.yml` : secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`,
`KEY_PASSWORD` — voir [`docs/SIGNING-ET-RELEASE.md`](../docs/SIGNING-ET-RELEASE.md).
