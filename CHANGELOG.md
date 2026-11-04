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
D0:27:13:1B:71:B1:ED:54:08:CC:3B:C0:2A:D9:53:95:4D:65:51:70:E1:3B:6B:86:4D:C5:F1:37:4D:09:D0:E6
```
