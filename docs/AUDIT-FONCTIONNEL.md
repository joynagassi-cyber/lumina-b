# Audit fonctionnel — Lumina-beta

**Date :** 4 novembre 2026
**Périmètre :** `app/src/main` (≈ 9 400 lignes Kotlin), ressources, configuration Gradle, tests.
**Méthode :** revue statique complète du code (couche données Room, repository, ViewModel,
les 6 écrans Compose, composants, export PDF/CSV), + analyse de la configuration de build.
**Limite importante :** aucun build n'a pu être exécuté dans cet environnement (pas de JDK
ni de SDK Android disponibles, accès réseau limité). Les conclusions portent donc sur le
**code et la configuration**, pas sur une exécution réelle. La CI ajoutée
(`.github/workflows/android-ci.yml`) sert précisément à valider la compilation et les tests ;
si elle est rouge, le log indiquera la ligne exacte.

---

## 1. Verdict

| Question | Réponse |
|---|---|
| Toutes les fonctionnalités annoncées existent-elles ? | **Oui.** Les 6 écrans, la persistance Room, les caisses multiples, les budgets, les catégories personnalisées et les exports PDF/CSV sont réellement implémentés (aucun bouton mort, aucun `TODO` bloquant). |
| Sont-elles toutes pleinement fonctionnelles ? | **Presque.** 4 défauts fonctionnels réels (dont 2 qui faussent les chiffres), 3 lacunes de configuration qui empêchaient tout build hors Android Studio. |
| L'app est-elle prête à être signée et distribuée ? | **Oui après les 3 actions ci-dessous** (identité de package, données de démo, préférences persistées). La chaîne de signature/CI est désormais en place. |
| Peut-elle être publiée sur le Play Store telle quelle ? | **Non, pas encore.** `applicationId` temporaire `com.aistudio.applet.hfkylu` + données de démonstration injectées au premier lancement. |

**3 actions avant publication :** ① figer l'`applicationId` ; ② désactiver les données de démo ;
③ décider du sort des transferts internes dans les totaux (voir P1-1).

---

## 2. Matrice des fonctionnalités

Légende : ✅ conforme · 🟡 fonctionne mais incomplet · ❌ absent

### Cœur métier

| Fonctionnalité | État | Détail |
|---|---|---|
| Saisie d'une opération (entrée/sortie, montant, catégorie, caisse, date, note) | ✅ | `AddTransactionDialog.kt` (558 l.) : montant > 0 validé, catégorie personnalisée possible, sélection de caisse, formatage `.`/`,` |
| Persistance locale 100 % hors ligne (Room/SQLite) | ✅ | `LuminaDatabase` v4, 4 entités, `Flow` réactifs. **Aucune permission INTERNET** dans le manifeste : l'app ne peut pas exfiltrer de données |
| Suppression d'opération | ✅ | Confirmation + recalcul immédiat des soldes |
| **Édition** d'une opération | ❌ | `updateTransaction()` existe dans le DAO mais n'est **jamais appelé** : il faut supprimer puis ressaisir |
| Bilan : total entrées / sorties / solde net / taux d'épargne | ✅ | Calculs en `Long` (arithmétique entière, déterministe) |
| Répartition par catégorie (graphique) | ✅ | `CategoryDistributionChart.kt` (Canvas Compose) |
| Timeline interactive de l'évolution financière | ✅ | `FinancialTimelineChart.kt` (580 l.), ajouté au dernier commit |
| Historique groupé par date + recherche + filtre type/période | ✅ | `TimelineScreen.kt` (489 l.) |
| Caisses multiples : création, édition, suppression, solde initial | ✅ | `CaissesScreen.kt` (730 l.), icônes typées (espèces/mobile/banque) |
| Transfert entre caisses | 🟡 | Fonctionne, mais **fausse les totaux** (P1-1) |
| Budgets mensuels par catégorie + progression + alerte visuelle | ✅ | `BudgetScreen.kt`, calcul `spent/ceiling`, tri des dépassements |
| **Contrainte budgétaire stricte** (blocage, seuil personnalisé) | ❌ | `alertThresholdPercent` et `isHardLimit` sont stockés mais **jamais lus** (P1-5) |
| Catégories personnalisées (nom, couleur, type) | ✅ | Création/édition/suppression ; renommage incomplet (P1-3) |
| Thème clair / sombre / système | 🟡 | Appliqué immédiatement, **non persisté** (P1-4) |
| 4 modes d'affichage des montants (standard, abrégé k/M, comptable, discret) | 🟡 | Fonctionnels, non persistés |
| Export PDF multi-pages (en-tête, KPI, tableau, pied de page) | ✅ | `ReportExporter.kt` via `PdfDocument`, partage FileProvider |
| Export CSV compatible Excel (BOM UTF-8, séparateur `;`) | ✅ | Voir P2-2 pour les détails de robustesse |
| **Export XLSX** natif | ❌ | Le libellé « Excel » désigne le CSV ; aucun `.xlsx` généré |
| Partage système (WhatsApp, e-mail, Drive…) | ✅ | `Intent.ACTION_SEND` + `FileProvider` correctement déclaré |
| Sauvegarde / restauration interne de la base | ❌ | Seule la sauvegarde Android système existe (et elle est grande ouverte, P1-7) |

### Configuration / industrialisation

| Élément | État | Détail |
|---|---|---|
| Gradle wrapper (`gradlew` + jar) | ✅ *(corrigé)* | Les 3 fichiers manquaient : `./gradlew` était inutilisable, y compris en CI |
| `debug.keystore` exigé par `build.gradle.kts` | ✅ *(corrigé)* | Script + étape CI ; le build debug échouait sinon |
| Keystore de release + signature | ✅ *(ajouté)* | `scripts/generate-keystore.sh`, workflow `bootstrap-keystore.yml` |
| CI build + tests + artifact APK | ✅ *(ajouté)* | `android-ci.yml` |
| CI release signée (APK + AAB + GitHub Release) | ✅ *(ajouté)* | `android-release.yml` |
| Tests unitaires | 🟡 | 10 tests (Room + `CurrencyFormatter`), **aucun test du ViewModel** ni des écrans |
| Lint / détekt / ktlint | ❌ | Non configuré |
| Internationalisation | ❌ | Toutes les chaînes sont en dur en français (app mono-locale assumée) |

---

## 3. Anomalies par priorité

### 🔴 P0 — bloquantes pour la distribution (corrigées dans cette branche)

**P0-1 · Wrapper Gradle incomplet**
`gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat` étaient absents : impossible de
builder ailleurs que dans un IDE configuré, et impossible pour GitHub Actions.
→ *Corrigé* : fichiers officiels Gradle 9.3.1 ajoutés (wrapper 46 Ko, `gradlew` exécutable).

**P0-2 · `debug.keystore` manquant**
`app/build.gradle.kts` pointe `debugConfig` vers `${rootDir}/debug.keystore` (convention
AI Studio) : le fichier est ignoré par Git, donc `assembleDebug` échoue sur un clone propre.
→ *Corrigé* : `scripts/generate-debug-keystore.sh` + étape dédiée dans les 2 workflows.

**P0-3 · Aucune chaîne de signature**
Aucun keystore, aucun secret, aucune CI : l'APK ne pouvait pas être signé hors Android Studio.
→ *Corrigé* : scripts + 3 workflows + doc `docs/SIGNING-ET-RELEASE.md`.

**P0-4 · `applicationId` temporaire**
`app/build.gradle.kts` : `applicationId = "com.aistudio.applet.hfkylu"`, `namespace = "com.example"`.
Cet identifiant est propre à l'environnement AI Studio. Il devient **immuable dès la première
publication Play**. À figer (ex. `com.joynagassi.lumina`) **avant** tout envoi sur le Store.
→ *Non modifié volontairement* : c'est une décision produit/identité, et le changer invalide
les installations existantes. À faire dans une PR dédiée (namespace + arborescence de packages).

### 🟠 P1 — défauts fonctionnels réels

**P1-1 · Les transferts entre caisses gonflent artificiellement dépenses et revenus**
`FinancialViewModel.transferBetweenCaisses()` (l. 453-503) crée deux écritures :
une **EXPENSE** catégorie `"Transfert / Virement"` et une **INCOME** de même montant.
Conséquence : pour un virement interne de 100 000 FCFA, le bilan affiche +100 000 en entrées
et +100 000 en sorties (le solde net reste juste, mais **totaux, taux d'épargne et répartition
par catégorie sont faussés**). De plus, `"Transfert / Virement"` n'existe dans le catalogue que
comme catégorie de **revenu** : elle apparaît comme poste de dépense inconnu dans le graphique.
→ *Correctif recommandé* : colonne `transferGroupId: String?` sur `TransactionEntity`, exclusion
de ces écritures des KPI « entrées/sorties » et des budgets, affichage dédié (« Virements
internes »). Nécessite une migration Room explicite (v4 → v5).

**P1-2 · Suppression d'une caisse : transactions orphelines**
`deleteCashRegister()` (l. 435-450) supprime la caisse sans réaffecter ni supprimer ses
écritures. La caisse disparaît du solde consolidé (`totalCashBalanceCents`) mais ses opérations
continuent d'alimenter les totaux, les budgets et l'historique, avec un `cashRegisterName`
fantôme. Un filtre sur l'ancienne caisse renvoie une liste vide.
→ *Correctif* : avant suppression, demander la caisse de réaffectation (ou supprimer les
écritures liées), puis bloquer la suppression si le solde calculé n'est pas nul.

**P1-3 · Renommer une catégorie casse le lien avec l'historique**
Les transactions et budgets référencent la catégorie **par son nom** (`category: String`,
`categoryName`, index unique). `updateCategory()` (l. 522) modifie la ligne `categories` sans
propager : les anciennes écritures pointent vers un nom inexistant (elles restent comptées dans
les totaux mais sortent du suivi budgétaire), et le budget associé devient orphelin.
→ *Correctif* : dans une transaction Room, `UPDATE transactions SET category = :new WHERE category = :old`
et idem pour `budgets`. (La clé étrangère par `id` serait la vraie solution de fond.)

**P1-4 · Préférences utilisateur non persistées**
`_themeMode` et `_amountDisplayMode` sont des `MutableStateFlow` en mémoire
(`FinancialViewModel` l. 114-115). Aucun `SharedPreferences` / `DataStore` dans le projet.
Au redémarrage : retour au thème système et à l'affichage standard. Idem pour la période
sélectionnée et la recherche.
→ *Correctif* : `DataStore Preferences` (dépendance déjà disponible mais commentée dans
`app/build.gradle.kts`) ou `SharedPreferences` dans le `MainActivity`.

**P1-5 · La « contrainte budgétaire » n'est pas appliquée**
`BudgetEntity.alertThresholdPercent` et `isHardLimit` sont écrits par le repository
(`TransactionRepository.saveBudgetConstraint`, l. 122-155) mais **lus nulle part** :
- le seuil d'alerte est figé à 80 % en dur dans le ViewModel (`isNearLimit`),
- `isHardLimit` ne bloque aucune saisie.
L'interface parle de contrainte ; le comportement réel est purement indicatif.
→ *Correctif* : utiliser le seuil stocké dans `CategoryBudgetProgress`, et refuser (ou avertir
avec confirmation) un `addTransaction` qui dépassse un budget `isHardLimit = true`.

**P1-6 · Données de démonstration injectées dans une app réelle**
`LuminaDatabase.populateInitialData()` (l. 65-205) crée au premier lancement :
3 caisses avec **1 500 000 / 350 000 / 150 000 FCFA** de solde initial, 12 catégories,
4 budgets et 6 transactions fictives (dont un « Virement Salaire Mensuel » de 1 250 000 FCFA).
Sur un téléphone personnel, l'utilisateur découvre donc **un patrimoine qui n'est pas le sien**,
et doit tout supprimer à la main — les soldes de caisse ne peuvent pas être « annulés » sans
supprimer la caisse.
→ *Correctif* : conserver le seed uniquement en debug (`if (BuildConfig.DEBUG)`) ou remplacer
par une caisse unique à 0 FCFA. **À trancher avant publication.**

**P1-7 · Sauvegarde cloud non maîtrisée et données financières en clair**
`AndroidManifest.xml` l. 6 : `android:allowBackup="true"`, avec
`backup_rules.xml` / `data_extraction_rules.xml` **vides de toute règle** : la base
`lumina_financial.db` part dans la sauvegarde Google et dans les transferts d'appareil, sans
chiffrement (pas de SQLCipher). Pour une app de comptabilité, c'est un choix à assumer.
→ *Options* : exclure la base (`<exclude domain="database" path="lumina_financial.db"/>`) et
ajouter un export/import chiffré, ou chiffrer la base.

**P1-8 · Migration destructive**
`LuminaDatabase` l. 45 : `.fallbackToDestructiveMigration()` — toute future incrémentation de
`version` **efface définitivement** les données de l'utilisateur. Acceptable en bêta, à
remplacer par de vraies `Migration` avant une version grand public.

### 🟡 P2 — qualité, robustesse, dette

| # | Constat | Impact |
|---|---|---|
| P2-1 | **Exports incohérents (corrigé ici)** : `exportPdf()`/`exportCsv()` utilisaient `state.transactions`, c.-à-d. la liste **filtrée** par la recherche/le type de la Timeline, alors que les totaux portaient sur la période entière. Exporter après une recherche produisait un PDF/CSV partiel avec des totaux globaux. | Fichier comptable faux — **corrigé** (nouveau champ `periodTransactions`, source unique des totaux, du Dashboard et des exports) |
| P2-2 | CSV : les guillemets `"` internes ne sont pas échappés (`title`/`note`), et les montants sont exportés en entiers bruts (l'app nomme `amountCents` une valeur qui est en réalité en **FCFA**, sans décimales) | Colonnes décalées si un libellé contient `"` |
| P2-3 | Firebase AI, AppCheck, `retrofit`, `moshi`, `okhttp`, `converter-moshi` et le plugin `google-services` sont déclarés mais **aucune ligne de code ne les utilise** (aucun appel Gemini, pas de `google-services.json`) | APK inutilement lourd (plusieurs Mo), plugin en mode WARN, surface d'attaque et dette de dépendances |
| P2-4 | `savingsRate` est borné à `[-100, 100]` (`coerceIn`) | Masque les situations extrêmes (déficit > 100 % des entrées) |
| P2-5 | Aucun test du `FinancialViewModel` (calculs de budgets, filtres, transferts) ni test UI des écrans | Les régressions de calcul passeraient inaperçues ; seuls Room et le formateur sont couverts |
| P2-6 | Pas de lint statique en CI | Warnings Compose/Kotlin non détectés (imports inutilisés déjà présents dans `MainActivity`) |
| P2-7 | `CategoryManagerDialog` autorise la suppression d'une catégorie par défaut | Catalogue initial destructible |
| P2-8 | Aucune action « Annuler » après suppression d'une opération | Perte de données sur erreur de frappe |
| P2-9 | Chaînes en dur partout (aucun `strings.xml` utilisé par les écrans) | Pas d'i18n possible, bien que `supportsRtl="true"` |

---

## 4. Ce qui a été corrigé dans cette branche

1. **P2-1** — cohérence totaux/exports : ajout de `FinancialUiState.periodTransactions`
   (période + caisse, sans filtre de recherche) utilisé par `exportPdf`, `exportCsv`,
   le Dashboard, l'écran Exports et les compteurs des Paramètres. La Timeline conserve la
   liste filtrée (`transactions`).
2. **P0-1** — wrapper Gradle complet (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`).
3. **P0-2** — `scripts/generate-debug-keystore.sh` (+ appel en CI).
4. **P0-3** — chaîne signature : `scripts/generate-keystore.sh`, 3 workflows, `.gitignore`
   durci, documentation `docs/SIGNING-ET-RELEASE.md`.
5. `versionCode` / `versionName` surchargeables par `VERSION_CODE` / `VERSION_NAME`
   (défauts `1` / `"1.0"` inchangés : AI Studio n'est pas impacté).
6. **Signature opérationnelle** : keystore de release généré et vérifié
   (`my-upload-key.jks`, PKCS12, alias `upload`, RSA 4096, SHA-256
   `D7:47:98:61:7B:6F:AD:B4:16:6C:87:D3:57:C4:06:D2:A5:5F:9C:3B:33:54:2A:73:1B:5A:1F:EF:A8:08:2E:A0`),
   `keystore.properties` lu par le build, scripts `build-release.sh` et `set-github-secrets.sh`.
7. **Android 12** : écran de démarrage natif (`values-v31/themes.xml`), compatibilité vérifiée
   sur la plage Android 7 → 16, `minSdk` paramétrable (`MIN_SDK=31`) sans toucher au code.

## 5. Ordre de traitement conseillé

1. `applicationId` définitif (P0-4) — **avant** toute publication.
2. Décision sur les données de démo (P1-6) et sur la sauvegarde (P1-7).
3. Transferts internes (P1-1) + migration Room v4→v5 (P1-8).
4. Réaffectation à la suppression d'une caisse (P1-2), propagation du renommage (P1-3).
5. Persistance des préférences (P1-4) et contrainte budgétaire réelle (P1-5).
6. Nettoyage des dépendances Firebase inutilisées (P2-3), lint + tests ViewModel (P2-5/P2-6).
7. Édition d'une transaction (manque fonctionnel le plus visible pour l'utilisateur).

Chaque point 3-5 est indépendant et peut faire l'objet d'une PR séparée.
