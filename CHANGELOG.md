# Lumina — journal des versions

## v1.5.0 — clôture, contre-écriture, budget réalisé

### Annulation par contre-écriture
- Une écriture ne se supprime plus : elle s'**annule**. L'originale reste au
  journal, marquée « annulée », et une **contre-écriture inverse** est créée au
  même montant. Les soldes restent justes, la piste d'audit est intacte.
- Un virement interne s'annule en une fois : contre-écriture dans les **deux**
  caisses, les deux lignes d'origine restant visibles.
- L'historique affiche une pastille « annulée » / « contre-écriture » et les
  écritures neutralisées ne pèsent plus sur les produits, les charges, le
  résultat ni les budgets.

### Clôture des comptes
- Paramètres → **Clôture des comptes** : « Clôturer le mois dernier » fige toutes
  les écritures jusqu'à la fin du mois précédent ; « Réouvrir » lève le verrou.
- Une période clôturée refuse toute saisie, modification, annulation ou virement
  daté dans cette période, avec un message explicite.

### Rapport de gestion
- Nouvelle section **« Budget prévu / réalisé »** : plafond, réalisé, écart
  (vert si marge, rouge si dépassement) et taux d'usage par catégorie.
- La zone de validation indique le nombre d'**écritures annulées** sur la
  période, en plus du nombre d'écritures rapprochées.

### Technique
- Base de données **version 7**, migration sans perte : deux colonnes ajoutées
  aux écritures (`reversalOfUid`, `isCancelled`) et une au profil de l'église
  (`booksClosedUntilMillis`).

## v1.4.0 — rapport de gestion auditable

### Vocabulaire et références du document
- Le document s'appelle désormais **« Rapport de gestion mensuel — situation de
  trésorerie »** : le mot *bilan* est réservé au patrimoine. L'écran d'accueil et
  la barre de navigation suivent (« Rapport »).
- En-tête complété : **numéro de document** (`RG-AAAAMMJJ-HHmm`), **période
  exacte** (date de début → date de fin), **date et heure d'édition**, **devise**,
  **base comptable** (trésorerie : encaissements et décaissements) et **auteur de
  la saisie**.
- La mention « membres au registre » ne figure plus dans le rapport financier.

### Soldes d'ouverture et de clôture
- Nouveau tableau **Situation de trésorerie par caisse** : type de caisse
  (espèces, banque, mobile money, coffre), **solde d'ouverture**, entrées,
  sorties et **solde de clôture**, avec ligne de total.
- Ligne de **contrôle** imprimée : ouverture + entrées − sorties = clôture, et
  rappel que les virements internes ne touchent pas le résultat.

### Anomalies corrigées
- **Taux d'épargne** : affiche « N/A (aucun produit enregistré sur la période) »
  au lieu de « 0,0 % » sans signification ; même règle dans l'application.
- **Répartition des charges** : pourcentage et montant occupent des colonnes
  séparées (plus de « 000,0 % » illisible), total à 100 % ajouté.
- Journal des écritures : nouvelle colonne **« Saisie par »** (traçabilité de
  l'auteur).

### Caisses
- **Refus des doublons** : deux caisses ne peuvent plus porter le même nom
  (comparaison insensible à la casse, aux accents et aux espaces).
- **Alerte visible** dans l'écran Caisses quand deux noms sont très proches
  (« Caisse principale » / « Caisse principal »), avec la consigne de renommer ou
  supprimer.

### Plan de comptes
- Catégories ajoutées : *Frais bancaires & commissions*, *Construction &
  équipement*, *Événements & fêtes*, *Cotisations des membres*, *Dons affectés à
  un projet*.

## v1.3.1 — filtres rattachés, écritures modifiables

### Historique : deux icônes de filtre dans la page
- Les filtres ne sont plus un panneau dépliant qui pousse le contenu : **deux
  icônes sont fixées à l'en-tête de la page** (période/nature/tri, et
  caisse/catégorie/montant). Un appui ouvre une **liste flottante** ancrée à
  l'icône, qui se referme d'un geste. Chaque icône porte une pastille avec le
  nombre de filtres actifs.

### Écritures modifiables
- Un appui sur une opération de l'historique ouvre le formulaire **pré-rempli** :
  intitulé, montant, nature, catégorie, caisse, date, note et justificatif
  peuvent être corrigés. L'identité de l'écriture et l'auteur de la modification
  sont conservés.
- Les virements internes restent non modifiables (ils s'annulent d'un bloc).

### Correction de calcul
- Le filtre **montant minimum / maximum** comparait des centimes à des francs :
  il multipliait la saisie par 100 et ne trouvait presque jamais rien. Il
  travaille désormais dans la même unité que les écritures.

### Navigation
- **Membres** entre dans la barre du bas à la place d'*Exports* ; les rapports
  restent accessibles par le menu ☰.

## v1.3.0 — une page, un plan, des fiches

### Défilement unifié
- **Caisses** et **Budgets** ne sont plus coupées en deux plans : l'en-tête, la
  synthèse et les cartes défilent ensemble, d'un seul geste. Plus d'effet
  « carte figée au-dessus » sur les petits écrans.

### Fiches détaillées
- **Membre** : un appui sur la carte ouvre une fiche complète (identité,
  situation familiale, contact et résidence, vie d'église), alignée et lisible,
  avec les boutons *Modifier* et *Retirer*. Le formulaire ne réapparaît qu'à la
  demande.
- **Caisse** : appui sur la carte → fiche avec solde, entrées, sorties, jauge et
  derniers mouvements ; virement, modification et suppression au même endroit.
- **Budget** : appui sur la carte → fiche avec plafond, consommation, reste et
  liste des dépenses de la catégorie.
- **Église** : Paramètres → *Profil de l'église* ouvre désormais une fiche
  présentable (logo, identité, localisation, contacts, responsables, couleur),
  et non plus directement le formulaire.

### Bouton flottant contextuel
- Chaque page crée ce qu'elle affiche : **Caisse** dans Caisses, **Budget** dans
  Budgets, **Membre** dans Membres, **Opération** ailleurs.

### Virement inter-caisses repensé
- Deux menus déroulants (caisse source, caisse destinataire) avec nom et solde,
  bouton d'inversion, dialogue défilant qui ne déborde plus. Refus si le solde
  de la source est insuffisant.

### Navigation et couleurs
- Barre de navigation agrandie : icônes 24 px, libellés toujours visibles,
  cibles tactiles de 56 px minimum.
- Les **cartes d'en-tête prennent la couleur d'accent** choisie (dégradé calculé
  à partir de teinte, plus clair en haut, plus profond en bas).
- Graphiques : **vert** pour les entrées et les courbes ascendantes, **rouge**
  pour les sorties et les courbes descendantes ; répartitions sémantiques par
  type.

## v1.2.1 — nettoyage des données d'essai

- Paramètres → **Données d'essai** : deux actions claires, chacune protégée par
  une confirmation.
  - **Effacer les écritures d'essai** : supprime opérations, membres, budgets et
    caisses ; le nom de l'église, le logo et l'équipe restent.
  - **Tout effacer et repartir de zéro** : supprime aussi le profil et l'équipe,
    rétablit le plan de comptes de départ et rouvre l'assistant de premier
    lancement.
- Les rapports PDF/Excel générés, les fichiers d'échange et les photos de
  justificatifs sont également supprimés du téléphone.

## v1.2.0 — plusieurs personnes, un seul dossier

### Une personne, un accès
- Au premier lancement (et à chaque ouverture), l'application demande **qui**
  l'utilise : chaque personne de l'équipe choisit son nom et saisit son code
  PIN. Le premier accès créé est celui du trésorier.
- Trois profils : **Trésorier** (tout gérer, y compris l'équipe),
  **Assistant** (saisir et corriger, sans supprimer ni gérer l'équipe),
  **Lecture seule** (consulter et exporter : pasteur, commission de contrôle).
- Le code n'est jamais conservé en clair : seuls un sel aléatoire et son
  empreinte sont enregistrés sur l'appareil.
- Chaque écriture retient **l'auteur de la saisie**, visible dans l'historique.

### Échange entre appareils, sans internet
- Nouvel écran **Équipe & partage** : exporter le dossier dans un fichier
  `.lumina` protégé par une phrase de passe, l'envoyer (WhatsApp, Bluetooth,
  câble, clé USB), puis l'importer sur un autre téléphone.
- Le fichier est **chiffré** (AES-256-GCM, clé dérivée par PBKDF2) : illisible
  sans la phrase de passe, et inutilisable si elle est fausse.
- La fusion est **déterministe** : chaque écriture, membre, caisse et catégorie
  porte une identité stable ; en cas de doublon, la version la plus récemment
  modifiée l'emporte. Rien n'est écrasé, rien n'est dupliqué.
- Après import, un **rapport de fusion** indique précisément ce qui a été
  ajouté, mis à jour ou déjà présent.
- Les **suppressions voyagent aussi** : une écriture supprimée sur un téléphone
  disparaît partout, sans casser les soldes.
- Les personnes et leurs codes PIN **ne quittent jamais l'appareil**.

### Mise à jour sans perte
- Migration de la base vers la nouvelle structure **sans effacer** les données
  déjà saisies : écritures, caisses, catégories, budgets, membres et profil
  sont conservés et reçoivent une identité stable.

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
