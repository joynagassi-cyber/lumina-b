# Lumina-beta

Application Android de gestion financière **minimaliste, déterministe et 100 % hors ligne** :
bilan, caisses multiples, budgets mensuels, catégories personnalisées, historique et exports
PDF / CSV. Montants en **FCFA (XOF)**, calculs en arithmétique entière (`Long`), base locale
Room/SQLite, aucune permission réseau.

- Kotlin 2.2 · Jetpack Compose (Material 3) · Room 2.7 · AGP 9.1 · Gradle 9.3.1
- `minSdk 24` · `targetSdk 36` · `applicationId` actuel : `com.aistudio.applet.hfkylu`

## Écrans

| Écran | Contenu |
|---|---|
| **Bilan** | Solde net, entrées/sorties, taux d'épargne, graphique de répartition, timeline interactive, caisses, dernières opérations |
| **Caisses** | Comptes espèces / mobile money / banque, soldes consolidés, transferts internes |
| **Budgets** | Plafonds par catégorie et par mois, progression, dépassements |
| **Historique** | Journal groupé par date, recherche, filtres période/type, suppression |
| **Exports** | PDF multi-pages et CSV compatible Excel, partage système |
| **Paramètres** | Thème, mode d'affichage des montants, accès rapides |

## Démarrage rapide

```bash
# 1. Keystore de debug (une seule fois, convention AI Studio)
bash scripts/generate-debug-keystore.sh

# 2. APK de debug
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# 3. Tests unitaires
./gradlew testDebugUnitTest
```

Prérequis : JDK 17+ (21 recommandé) et un SDK Android (`ANDROID_HOME` ou `local.properties`).
Le wrapper Gradle est fourni : inutile d'installer Gradle.

## Signer et publier

```bash
# Keystore de release + secrets GitHub
STORE_PASSWORD='mot-de-passe-solide' bash scripts/generate-keystore.sh
gh secret set KEYSTORE_BASE64 < my-upload-key.jks.base64
gh secret set STORE_PASSWORD --body 'mot-de-passe-solide'
gh secret set KEY_PASSWORD   --body 'mot-de-passe-solide'

# Release signée (APK + AAB) via GitHub Actions
git tag v1.0.1 && git push origin v1.0.1
```

Guide détaillé (keystore, secrets, Play Store, dépannage) :
**[docs/SIGNING-ET-RELEASE.md](docs/SIGNING-ET-RELEASE.md)**

## Intégration continue

> Les workflows sont fournis dans **`ci/workflows/`** (le jeton de l'agent n'a pas la permission
> `workflows`). Activez-les avec `bash scripts/install-ci.sh` — voir [`ci/README.md`](ci/README.md).
> Une fois installés dans `.github/workflows/`, ils s'exécutent automatiquement.

| Workflow | Déclencheur | Résultat |
|---|---|---|
| `android-ci.yml` | push / PR / manuel | Tests + APK debug en artifact |
| `android-release.yml` | tag `v*` ou manuel | APK **et** AAB signés, vérifiés avec `apksigner`, publiés en GitHub Release |
| `bootstrap-keystore.yml` | manuel (une fois) | Génère le keystore de signature et le publie en artifact |

## État de l'application

Audit fonctionnel complet (matrice des fonctionnalités, anomalies classées P0/P1/P2, priorités) :
**[docs/AUDIT-FONCTIONNEL.md](docs/AUDIT-FONCTIONNEL.md)**

Résumé : toutes les fonctionnalités annoncées sont implémentées ; restent 4 points à traiter
avant une publication grand public (identité de package, données de démonstration, transferts
internes dans les totaux, persistance des préférences).

## Structure

```
app/src/main/java/com/example/
├── MainActivity.kt              navigation, drawer, FAB
├── data/                        Room : entités, DAO, repository, seed
├── ui/                          ViewModel + écrans Compose + composants + design system
├── export/ReportExporter.kt     génération PDF et CSV, partage FileProvider
└── util/CurrencyFormatter.kt    formatage FCFA (standard, abrégé, comptable, discret)
scripts/                         génération des keystores debug / release
docs/                            audit fonctionnel, design system, signature & release
.github/workflows/               CI, release signée, bootstrap keystore
```

## Sécurité

- Base SQLite locale, aucune permission `INTERNET`.
- **Ne jamais committer** de keystore : `.gitignore` bloque `*.jks`, `*.keystore`, `*.p12`.
- Sauvegardez `my-upload-key.jks` et son mot de passe hors du dépôt : sans eux, plus aucune
  mise à jour n'est possible sous le même `applicationId`.
