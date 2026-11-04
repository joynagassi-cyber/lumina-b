# Signature, keystore et publication — Lumina-beta

Guide complet pour signer l'application, générer l'APK/AAB et automatiser le build
avec GitHub Actions.

---

## 1. Ce qui est déjà en place dans le dépôt

| Élément | Rôle |
|---|---|
| `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | Wrapper Gradle 9.3.1 (manquait auparavant : la CI ne pouvait pas builder) |
| `scripts/generate-keystore.sh` | Crée le keystore **release** (`my-upload-key.jks`, alias `upload`) |
| `scripts/generate-debug-keystore.sh` | Crée le `debug.keystore` exigé par `app/build.gradle.kts` |
| `ci/workflows/android-ci.yml` | Compile + tests + APK debug à chaque push/PR |
| `ci/workflows/android-release.yml` | APK **signé** + AAB signé sur tag `v*`, publiés en GitHub Release |
| `ci/workflows/bootstrap-keystore.yml` | Génère le keystore **dans GitHub** (si vous n'avez pas de JDK local) |
| `.gitignore` | Bloque `*.jks`, `*.keystore`, `*.p12`, `*.jks.base64` |

> ⚠️ **Activer les workflows d'abord** : ils sont livrés dans `ci/workflows/` (contrainte de
> permission du dépôt). Lancez `bash scripts/install-ci.sh` puis poussez, ou déposez les 3
> fichiers dans `.github/workflows/` depuis l'interface GitHub. Détails : [`ci/README.md`](../ci/README.md).

Le build lit déjà ces variables d'environnement (bloc `signingConfigs` → `release` de
`app/build.gradle.kts`) :

| Variable | Valeur attendue |
|---|---|
| `KEYSTORE_PATH` | chemin du keystore (défaut : `./my-upload-key.jks`) |
| `STORE_PASSWORD` | mot de passe du store |
| `KEY_PASSWORD` | mot de passe de la clé (identique au store — contrainte PKCS12) |
| alias | **`upload`** (codé en dur dans le build, ne pas changer) |

`versionCode` / `versionName` peuvent être surchargés par `VERSION_CODE` / `VERSION_NAME`.

---

## 2. Générer le keystore

### Option A — en local (recommandé)

Prérequis : un JDK 17+ (`keytool`), par exemple le JDK embarqué d'Android Studio.

```bash
cd lumina-b

# Mot de passe passé en variable, ou saisie interactive (masquée)
STORE_PASSWORD='Choisissez-Un-Mot-De-Passe-Solide-2026' \
  bash scripts/generate-keystore.sh
```

Le script :
1. refuse d'écraser un keystore existant (écraser = perte définitive des mises à jour) ;
2. crée `my-upload-key.jks` (RSA 4096, 30 ans, alias `upload`) ;
3. vérifie la clé et affiche l'empreinte SHA-256 ;
4. produit `my-upload-key.jks.base64` pour les secrets GitHub.

> **Sauvegardez immédiatement** `my-upload-key.jks` + le mot de passe dans un
> gestionnaire de mots de passe et sur un support hors ligne. Sans ce fichier,
> aucune mise à jour de l'app ne pourra plus être publiée sous le même `applicationId`.

### Option B — sans JDK local, directement dans GitHub

1. Onglet **Actions** → **Bootstrap keystore (à lancer UNE seule fois)** → **Run workflow**.
2. Télécharger l'artefact `lumina-upload-keystore` (expire après 3 jours) :
   - `my-upload-key.jks` → à sauvegarder hors dépôt,
   - `my-upload-key.jks.base64` → contenu du secret `KEYSTORE_BASE64`,
   - `credentials.txt` → mot de passe généré aléatoirement.
3. Supprimer l'artefact du run une fois les secrets créés.

---

## 3. Enregistrer les secrets GitHub

Dépôt → **Settings → Secrets and variables → Actions → New repository secret** :

| Nom | Valeur |
|---|---|
| `KEYSTORE_BASE64` | contenu intégral de `my-upload-key.jks.base64` (une seule ligne) |
| `STORE_PASSWORD` | mot de passe du keystore |
| `KEY_PASSWORD` | **le même** mot de passe |

Avec GitHub CLI :

```bash
gh secret set KEYSTORE_BASE64 < my-upload-key.jks.base64
gh secret set STORE_PASSWORD  --body 'Choisissez-Un-Mot-De-Passe-Solide-2026'
gh secret set KEY_PASSWORD    --body 'Choisissez-Un-Mot-De-Passe-Solide-2026'

gh secret list   # vérification
```

Aucun binaire de signature ne doit être committé : `.gitignore` bloque déjà
`*.jks`, `*.keystore` et `*.p12`.

---

## 4. Builder en local

### APK de debug

```bash
bash scripts/generate-debug-keystore.sh   # une seule fois
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### APK de release signé

```bash
export KEYSTORE_PATH="$PWD/my-upload-key.jks"
export STORE_PASSWORD='Choisissez-Un-Mot-De-Passe-Solide-2026'
export KEY_PASSWORD="$STORE_PASSWORD"

./gradlew clean assembleRelease        # APK
./gradlew bundleRelease                # AAB (Play Store)
# → app/build/outputs/apk/release/  et  app/build/outputs/bundle/release/
```

Si `KEYSTORE_PATH` n'est pas défini, le build cherche `my-upload-key.jks` à la racine
du projet. S'il est absent, Gradle échoue avec `Keystore file ... not found`.

### Vérifier la signature

```bash
apksigner verify --print-certs app/build/outputs/apk/release/*.apk
# apksigner se trouve dans $ANDROID_HOME/build-tools/<version>/
```

La sortie doit afficher votre certificat (SHA-256 identique à celui du keystore) et
`Verified using v2/v3 scheme: true`.

---

## 5. Publier une release via GitHub Actions

```bash
# 1. Mettre à jour le code, puis :
git add .
git commit -m "feat: ..."
git push

# 2. Créer le tag de version
git tag v1.0.1
git push origin v1.0.1
```

Le workflow **Android Release** :
1. décode `KEYSTORE_BASE64` dans le runner et vérifie la présence de l'alias `upload` ;
2. calcule `versionName` depuis le tag (`v1.0.1` → `1.0.1`) et `versionCode` = numéro de run ;
3. exécute `assembleRelease` + `bundleRelease` ;
4. vérifie la signature avec `apksigner verify --print-certs` ;
5. publie `Lumina-<version>.apk`, `Lumina-<version>.aab` et `SHA256SUMS.txt`
   en artifacts **et** dans une GitHub Release ;
6. supprime le keystore du runner.

Sans tag, vous pouvez lancer le workflow manuellement (Actions → *Run workflow*) pour
obtenir un APK signé téléchargeable dans les artifacts (30 jours), sans créer de Release.

---

## 6. Installer l'APK sur le téléphone

1. Télécharger `Lumina-<version>.apk` (Release ou artifact).
2. Autoriser « Installer des applications inconnues » pour le navigateur/Gestionnaire de fichiers.
3. Ouvrir le fichier → Installer.

> ⚠️ Si une version **debug** de Lumina est déjà installée (build AI Studio / Android
> Studio), l'installation de l'APK release échouera avec
> `INSTALL_FAILED_UPDATE_INCOMPATIBLE` : les signatures diffèrent.
> Désinstallez d'abord l'ancienne version (cela supprime ses données locales).

`adb install -r Lumina-1.0.1-42.apk` fonctionne aussi, et
`adb shell dumpsys package <applicationId> | grep signatures` permet de comparer.

---

## 7. Google Play Console

1. **Changer l'identité de publication avant la première mise en ligne** :
   `applicationId` actuel = `com.aistudio.applet.hfkylu` (identifiant temporaire AI Studio).
   Un `applicationId` ne peut plus être modifié après publication → choisissez par ex.
   `com.joynagassi.lumina` et alignez le `namespace` / les packages Kotlin si souhaité.
2. Créer l'app dans la Play Console, activer **Play App Signing** :
   Google conserve la clé d'app ; votre `my-upload-key.jks` devient la *upload key*.
3. Uploadez le fichier `.aab` (pas l'APK) dans une piste (interne → fermée → production).
4. Renseigner la fiche, la politique de confidentialité (l'app est 100 % hors ligne,
   aucune donnée collectée : c'est un argument à mettre en avant) et le questionnaire
   « Sécurité des données ».
5. `versionCode` doit **strictement augmenter** à chaque upload (le workflow utilise le
   numéro de run, qui est monotone).

---

## 8. Sécurité de la clé

- Ne jamais committer, envoyer par chat/e-mail ou stocker dans le cloud public le fichier `.jks`.
- Un keystore fuité = n'importe qui peut publier des mises à jour malveillantes en votre nom.
- Sauvegardes : gestionnaire de mots de passe (fichier) + copie chiffrée sur clé USB + un
  second emplacement physique. Testez la restauration une fois.
- Si le keystore est perdu **et** que vous n'utilisez pas Play App Signing : l'application
  ne peut plus être mise à jour (il faut publier une nouvelle app avec un nouvel `applicationId`).
- En cas de fuite, Play Console permet de demander un **reset de la upload key** (Play App
  Signing activé) — c'est la principale raison d'activer Play App Signing dès le départ.

---

## 9. Dépannage

| Erreur | Cause / solution |
|---|---|
| `Keystore file '.../my-upload-key.jks' not found` | Keystore absent : lancez `scripts/generate-keystore.sh` ou définissez `KEYSTORE_PATH`. |
| `Failed to read key upload from store` | L'alias n'est pas `upload`, ou mauvais `KEY_PASSWORD`. Vérifiez avec `keytool -list -keystore ... -storepass ...`. |
| `Given final block not properly padded` / `keystore password was incorrect` | `STORE_PASSWORD` erroné (piège : espaces ou guillemets dans le secret GitHub). |
| `Keystore was tampered with, or password was incorrect` | Fichier base64 tronqué (le secret doit tenir sur **une** ligne). |
| `debug.keystore` introuvable | `bash scripts/generate-debug-keystore.sh`. |
| `./gradlew: Permission denied` | `chmod +x gradlew`. |
| `Unsupported class file major version` / `Unsupported Java` | Utilisez JDK 21 (`JAVA_HOME`), comme dans la CI. |
| `google-services.json is missing` | Simple avertissement : le plugin est en `MissingGoogleServicesStrategy.WARN` et aucun code n'utilise Firebase. Voir `docs/AUDIT-FONCTIONNEL.md` (§ Firebase inutilisé). |
| `SDK location not found` | Définir `ANDROID_HOME` ou créer `local.properties` avec `sdk.dir=...`. |
| Build CI rouge sur `platforms;android-36.1` | AGP télécharge automatiquement la plateforme si les licences sont acceptées (déjà fait dans le workflow). |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Une version signée différemment est installée : désinstallez-la d'abord. |

---

## 10. Checklist avant publication

- [ ] Keystore généré, **sauvegardé en 2 endroits**, mot de passe dans le gestionnaire de mots de passe
- [ ] Secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` créés
- [ ] `applicationId` définitif choisi (avant la 1ʳᵉ publication)
- [ ] `versionName` / `versionCode` cohérents
- [ ] Données de démonstration désactivées (voir audit, P1-6)
- [ ] CI verte (tests + `assembleDebug`)
- [ ] APK release installé et testé sur un appareil réel (ajout, suppression, export PDF/CSV)
- [ ] `apksigner verify` OK et empreinte SHA-256 archivée
- [ ] AAB uploadée sur une piste interne Play Console
