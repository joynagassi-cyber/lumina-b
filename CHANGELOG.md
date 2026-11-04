# Lumina — journal des versions

## v1.1.0 — refonte de la gestion financière d'église

### Onboarding et identité
- Assistant de premier lancement en trois étapes : présentation, **identité de
  l'église** (nom obligatoire, dénomination, ville, adresse, contacts,
  responsable, trésorier), puis **logo et couleur d'accent** avec aperçu réel de
  l'en-tête des rapports.
- Profil local enregistré sur l'appareil, modifiable depuis les paramètres :
  le nom, le logo et la couleur habillent l'application **et** les bilans PDF.
- Sept palettes d'accent proposées, bleu indigo par défaut.

### Comptabilité
- **Virements internes déterministes** : deux écritures indissociables, exclues
  des produits et des charges ; confirmation avec solde avant → après des deux
  caisses ; solde insuffisant refusé ; suppression groupée.
- Suppression d'une caisse avec ses écritures (plus d'opérations orphelines).
- Renommage d'une catégorie propagé aux opérations et aux budgets.
- Plafonds budgétaires : alerte locale à 80 % et au dépassement.
- Le premier lancement ne crée plus d'écritures fictives : plan de comptes
  d'église (dîmes, offrandes, diaconie, missions…) et une caisse vide.

### Membres
- Nouveau registre : prénom et nom obligatoires ; situation matrimoniale,
  conjoint, contact, adresse, lieu de résidence, profession et notes optionnels,
  avec recherche et édition.

### Interface
- Historique : page entièrement défilante, filtres repliés derrière deux icônes
  (période, nature, caisse, catégorie, montant min/max, tri).
- Filtre par caisse de l'historique corrigé (il n'était pas transmis).
- Caisses et budgets : l'en-tête ne coupe plus le contenu de la liste.
- Justificatif photo ou scan, facultatif, sur chaque opération.
- Notifications locales : confirmations, virements, alertes de budget.
- Mentions techniques retirées de l'interface.

### Rapports
- Bilan PDF restructuré : en-tête institutionnel, synthèse, compte de résultat
  par nature, trésorerie par caisse, graphique sur six mois, répartition des
  charges, détail des écritures et bloc de signatures.

# Journal des versions — Lumina-beta

## v1.0.0 — première release

Première version publiée de **Lumina-beta**, application Android de gestion financière
déterministe et 100 % hors ligne, en FCFA (XOF).

### Fonctionnalités

- **Bilan** : solde net de la période, total des entrées/sorties, taux d'épargne, répartition
  par catégorie, timeline financière interactive, dernières opérations.
- **Caisses multiples** : espèces, mobile money, banque — solde initial, création/modification/
  suppression, transferts internes entre caisses, filtre par caisse sur tous les écrans.
- **Budgets mensuels** : plafond par catégorie et par mois, progression, alertes de dépassement,
  navigation mois précédent/suivant.
- **Catégories personnalisées** : nom, type (entrée/sortie), couleur, 12 catégories par défaut.
- **Historique** : journal groupé par jour, recherche instantanée, filtres période et type.
- **Exports** : PDF multi-pages mis en page (en-tête, KPI, tableau, pied de page) et CSV
  compatible Excel (UTF-8 BOM, séparateur `;`), partage via le sélecteur Android natif.
- **Interface** : thème clair/sombre/système, 4 modes d'affichage des montants
  (standard, abrégé k/M, comptable, discret), design system monochrome + accent indigo.

### Technique

- Kotlin 2.2 · Jetpack Compose (Material 3) · Room 2.7 (SQLite local) · AGP 9.1 · Gradle 9.3.1
- `minSdk 24` (Android 7.0) → `targetSdk 36` : **compatible Android 7 à 16**, Android 12 inclus
- Écran de démarrage natif Android 12+ (`values-v31/themes.xml`)
- Aucune permission réseau : les données ne quittent jamais l'appareil
- Calculs monétaires en arithmétique entière (`Long`), sans arrondi flottant
- APK signé (keystore dédié, RSA 4096, alias `upload`) + AAB pour Google Play

### Corrections de cette version

- Cohérence des exports : le PDF/CSV exporte exactement la période et la caisse sélectionnées
  (la liste filtrée par la recherche n'est plus utilisée pour les documents comptables).
- Android 12 : thème de démarrage explicite, activité `exported` conforme.

### Limitations connues

- Pas d'édition d'une opération existante (supprimer puis ressaisir).
- Préférences d'affichage (thème, mode montants) non conservées entre deux lancements.
- Les transferts entre caisses apparaissent à la fois en entrée et en sortie dans les totaux.
- Pas de sauvegarde/restauration interne (seule la sauvegarde Android système s'applique).

Voir `docs/AUDIT-FONCTIONNEL.md` pour le détail et les priorités des prochaines versions.

### Installation

1. Télécharger `Lumina-1.0.0-<versionCode>.apk` depuis les assets de la release.
2. Autoriser l'installation depuis une source inconnue pour le navigateur ou le gestionnaire
   de fichiers, puis ouvrir le fichier.
3. Vérifier l'intégrité : `sha256sum -c SHA256SUMS.txt`.

Empreinte du certificat de signature (SHA-256) :

```
9A:71:27:AD:2D:C1:CD:E3:64:E2:77:78:46:E4:7C:20:CB:E8:62:3A:35:E7:1C:D3:89:D3:79:4D:91:13:D6:56
```

### Publication effective — 2026-11-04

Première release publiée avec ses binaires signés :

| Fichier | Taille |
|---|---|
| `app-release.apk` | 15,4 Mo |
| `app-release.aab` | 15,0 Mo |

- empreinte SHA-256 de l'APK au moment du build : `30982b84…f0e41593`
- certificat de signature vérifié par `apksigner verify --print-certs` :
  `CN=Lumina-beta, OU=Mobile, O=Lumina, L=Kandi, ST=Alibori, C=BJ`,
  SHA-256 `9A:71:27:AD:…:13:D6:56`
- run de publication : https://github.com/joynagassi-cyber/lumina-b/actions/runs/41809739025

Deux défauts bloquants ont été corrigés pour y parvenir : `java.util.Properties()`
ne se compile pas dans un script Gradle Kotlin DSL (`java` y désigne l'extension
`java { }`, d'où `import java.util.Properties`), et `run_step` neutralisait
`set -e` en appelant les phases dans une liste `||` (l'échec Gradle était avalé).
