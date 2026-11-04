# Design System Lumina • Architecture Visuelle & Spécifications

Ce document formalise les principes du Design System de **Lumina-beta**, issus de la synthèse des 4 références visuelles (Android One UI Settings, Findex, Winvest, et Dashboards financiers modernes) adaptés à l'identité propre de Lumina (Palette Monochrome Pure OLED / Blanc Épuré, Accent Indigo sombre et arithmétique entière déterministe).

---

## 1. Principes Fondamentaux de Conception

### A. Hiérarchie des Surfaces & Profondeur Tonale
1. **Fond de page** : 
   - Mode Sombre : Noir absolu OLED pur (`#000000`).
   - Mode Clair : Blanc pur (`#FFFFFF`).
2. **Cartes Principales (Niveau 1)** :
   - Surfaces tonales douces (`#0C0C0E` en sombre, `#F8F8FA` en clair) avec bordure subtile (`#27272A` / `#E4E4E7`).
   - Rayon de courbure généreux : **22 dp à 26 dp**.
3. **Éléments Imbriqués (Niveau 2 - Sous-cartes, Tuiles, Badges)** :
   - Surfaces plus contrastées (`#18181B` en sombre, `#F1F1F4` en clair).
   - Rayon de courbure interne : **12 dp à 16 dp**.
4. **Séparation par superposition et espacement** : Pas de bordures épaisses ou criardes. L'espacement (8, 12, 16, 20, 24 dp) et la luminance définissent la structure.

---

## 2. Typographie & Affichage des Nombres

- **Montants Clés (Hero Amount)** : 32 sp à 36 sp, `FontWeight.Black`, espacement resserré (`-0.5 sp`).
- **Affichage Tabulaire & Déterministe** : Chiffres alignés sans variation de largeur.
- **Symbole Devise & Décimales** : Taille harmonisée et atténuée pour donner la priorité au montant principal.
- **Pastilles de Variation (DeltaBadge)** :
  - Fond tonal doux semi-transparent.
  - Flèche sémantique (`ArrowUpward` / `ArrowDownward`).
  - Couleur : Indigo sombre / clair pour les flux réguliers, rouge feutré en cas de déficit.

---

## 3. Composants Clés Réutilisables

| Composant | Description | Rayon | Usage dans Lumina |
|---|---|---|---|
| **`HeroCard`** | Carte maîtresse en tête d'écran avec indicateur financier principal, delta et sous-cartes | 24 dp | Dashboard (Solde net / Trésorerie), Caisses, Exports |
| **`IconTile`** | Tuile carrée à coins arrondis contenant une icône centrée | 12 dp | Listes d'opérations, options de paramètres, caisses |
| **`ListRow`** | Ligne groupée (IconTile + Titre + Sous-titre + Valeur/Chevron) | 0 dp (dans carte) | Historique, Paramètres, Sélecteurs |
| **`GroupedCard`** | Carte conteneur pour regrouper plusieurs `ListRow` avec séparateurs insets | 22 dp | Paramètres, Timeline quotidienne |
| **`StatTrio`** | Rangée de 3 colonnes de statistiques alignées (Libellé + Grand chiffre) | 14 dp | Détail d'une caisse, Bilan export, Suivi budget |
| **`DeltaBadge`** | Pastille de pourcentage ou variation avec icône fléchée | 10 dp | Carte héro, cartes de caisses, comparaison mensuelle |
| **`SegmentedControl`** | Contrôle à bascule en pilule pour le filtrage rapide | 16 dp | Périodes (Ce mois / Mois dernier...), Types de flux |
| **`PillButton`** | Bouton pleine largeur ou en duo à coins arrondis complets (pilule) | 999 dp | Actions primaires / secondaires (Enregistrer, Exporter) |
| **`SectionHeader`** | Titre de section typographique avec action secondaire ("Voir tout") | N/A | Entêtes de sections du Dashboard, Caisses, Budgets |
| **`HorizontalCarousel`** | Défilement horizontal fluide de cartes compactes | 18 dp | Carrousel "Mes Caisses & Comptes" sur le Dashboard |
| **`SparklineMiniChart`**| Mini-courbe vectorielle Canvas intégrée | N/A | Aperçu visuel de tendance sur les cartes et lignes |
| **`FloatingPillBottomNav`**| Barre de navigation flottante en pilule respectant les insets système | 28 dp | Navigation principale de l'application |

---

## 4. Tokens Centralisés (`LuminaTokens`)

- **Rayons de courbure** :
  - `CornerPill`: `999.dp`
  - `CornerHero`: `24.dp`
  - `CornerCard`: `20.dp`
  - `CornerSubCard`: `14.dp`
  - `CornerTile`: `12.dp`
  - `CornerBadge`: `8.dp`
- **Espacements & Paddings** :
  - `SpacingMicro`: `4.dp`
  - `SpacingSmall`: `8.dp`
  - `SpacingMedium`: `12.dp`
  - `SpacingDefault`: `16.dp`
  - `SpacingLarge`: `20.dp`
  - `SpacingXLarge`: `24.dp`
  - `SpacingSection`: `28.dp`
- **Cible Tactile Minimale** : `48.dp` sur tous les éléments cliquables.

---

## 5. Correspondance des Écrans Lumina

1. **Dashboard** :
   - `HeroCard` en haut avec solde net de la période, `DeltaBadge` de taux d'épargne, et sous-tuile de trésorerie consolidée.
   - `SectionHeader` ("Mes Caisses & Liquidités" + "Gérer").
   - `HorizontalCarousel` avec cartes de caisses compactes (solde, icône, delta).
   - Carte budget avec barre de progression et métriques.
   - Actions rapides en tuiles (PDF, Excel, Nouvelle opération).
   - `SectionHeader` ("Dernières opérations" + "Voir tout") avec liste de `ListRow` contenant `IconTile` et `SparklineMiniChart`.
2. **Caisses (Trésorerie)** :
   - `HeroCard` Trésorerie Totale avec nombre de caisses actives et solde consolidé.
   - Duo d'actions en pilules ("Virement Inter-caisses", "Nouvelle Caisse").
   - Cartes de caisses enrichies avec `StatTrio` (Solde initial, Entrées, Sorties) et badge de filtre actif.
3. **Budgets & Suivi** :
   - Sélecteur de mois en contrôle segmenté avec flèches.
   - Carte de budget global avec `StatTrio` (Dépensé / Plafond / Reste) et jauge de progression sémantique.
   - Liste des budgets par catégorie avec `IconTile`, pourcentage consommé et seuil d'alerte.
4. **Timeline (Historique)** :
   - `SegmentedControl` pour périodes et types (Tous, Entrées, Sorties).
   - Champ de recherche épuré avec rayon 14 dp.
   - Groupement par jour dans des `GroupedCard` avec séparateurs insets et tuiles d'icônes.
5. **Paramètres** :
   - Fidèle au modèle One UI / iOS Settings : sections logiques groupées dans des `GroupedCard` avec tuiles d'icônes colorées (`IconTile`), titre + sous-titre explicatif, et commandes intégrées (radios, bascules, chevrons).
6. **Exports** :
   - `HeroCard` de récapitulatif comptable avec `StatTrio`.
   - Cartes immersives PDF et Excel avec `PillButton` d'export direct.
7. **Navigation** :
   - `FloatingPillBottomNav` flottante au-dessus du contenu, avec capsule active fluide et gestion stricte des marges système (`navigationBarsPadding`).
