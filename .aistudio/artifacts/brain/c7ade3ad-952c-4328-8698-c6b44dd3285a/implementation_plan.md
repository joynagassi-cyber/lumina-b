# Composant Réutilisable LuminaCard & Carte Héro Bilan Bleu Électrique Compacte

Création d'un composant de base `LuminaCard` en Jetpack Compose avec coins arrondis (24px / 24dp), gestion fluide de l'élévation par superposition tonale et ombrage de profondeur, ainsi que la refonte de la carte héro de bilan dans un format ultra-compact habillé d'un dégradé bleu indigo électrique profond.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Les deux décisions clés ont été confirmées lors de la phase d'échange interactive :
> - **Couleur et Finition de la Carte Héro** : Dégradé bleu indigo électrique (`#4338CA` vers `#312E81` ou `#4F46E5` vers `#1E1B4B`) avec ombre portée douce multi-niveaux pour créer une forte impression de relief et de profondeur.
> - **Compacité et Agencement** : Format ultra-compact optimisant l'espace vertical : montant principal net mis en valeur, pastilles de delta et statut alignées horizontalement, et sous-métriques d'entrées/sorties condensées sur une ligne avec typographie tabulaire sans perte d'information.

---

### 1. Overview & Core Concept

- **Problématique résolue** : Offrir un composant de carte générique et universel (`LuminaCard`) pour l'ensemble de l'application Lumina, garantissant le respect strict des tokens de surface (coins 24px, bordure subtile, gestion de l'élévation tonale selon le thème Clair ou OLED), tout en fournissant une variante Héro spécialisée au bleu indigo saisissant pour la carte maîtresse du tableau de bord.
- **Bénéfice utilisateur** : Une hiérarchie visuelle immédiate dès l'ouverture de l'application : le solde net de la période ressort instantanément avec élégance et profondeur grâce au dégradé bleu électrique et à l'ombre portée, tout en libérant de l'espace sur l'écran pour les caisses, les budgets et les dernières opérations.

---

### 2. User Experience & Visual Design

- **Composant `LuminaCard`** :
  - Rayon de courbure fixe de **24 dp** (`LuminaTokens.CornerHero` ou 24.dp).
  - Gestion de l'élévation par superposition tonale (`tonalElevation`) et ombre physique (`shadowElevation` ou `Modifier.shadow`) configurable par niveau (Flat, Subtle, Raised, Hero).
  - Prise en charge des états cliquables optionnels avec retour tactile Material ripple.
  - Support de conteneur monochrome ou dégradé personnalisé.
- **Carte Héro de Bilan en Bleu Indigo Électrique** :
  - **Atmosphère & Gradient** : Dégradé linéaire subtil (angle doux) allant d'un indigo lumineux à un bleu nuit profond (`#4F46E5` / `#4338CA` vers `#1E1B4B` / `#312E81`).
  - **Ombrage de profondeur** : Ombre portée diffuse (`shadowElevation = 8.dp`, `ambientColor`, `spotColor`) qui détache la carte du fond noir OLED ou blanc pur.
  - **Format Ultra-Compact** :
    - En-tête : Label "SOLDE NET" et sélecteur de période condensé.
    - Ligne principale : Très grand montant en centimes FCFA tabulaires (`FontWeight.Black`, 30-32 sp) avec `DeltaBadge` compacte intégrée sur la droite.
    - Ligne secondaire imbriquée : Bandeau compact à fond translucide (`alpha = 0.2f`) affichant côte à côte le total des Entrées (`+`), des Sorties (`-`) et la mini jauge d'épargne sans gonfler la hauteur de carte.

---

### 3. Key Product Decisions & Trade-Offs

- **Décision 1 : `LuminaCard` comme fondation unifiée** :
  - *Approche* : Remplacer l'usage direct de `CardDefaults` dispersé par `LuminaCard` dotée d'une énumération `LuminaCardElevation` (`FLAT`, `LOW`, `MEDIUM`, `HIGH`) et d'un support direct pour les gradients.
  - *Raison* : Cohérence absolue de la courbure à 24px et centralisation de la superposition tonale sans duplication de code.
- **Décision 2 : Lisibilité du texte sur fond bleu indigo** :
  - *Approche* : Utiliser des typographies blanches (`Color.White`) et blanches adoucies (`Color.White.copy(alpha = 0.75f)`) pour tous les éléments textuels de la carte héro, garantissant un contraste conforme WCAG AAA sur le dégradé bleu.
  - *Raison* : Éviter les conflits de contraste avec le texte standard du thème lorsque la carte bascule sur le fond bleu électrique.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   DashboardScreen                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │     FinancialSummaryCard (LuminaCard Hero)       │  │
│  │                                                  │  │
│  │  [Bleu Indigo Électrique + Gradient + Ombrage]   │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │ En-tête : "SOLDE NET • Période" + Badge    │  │  │
│  │  │ Montant : 81 590 FCFA       [+34.5% Épargne]│  │  │
│  │  ├────────────────────────────────────────────┤  │  │
│  │  │ Ligne compacte : Entrées (+) | Sorties (-) │  │  │
│  │  │ Jauge de trésorerie ultra-fine             │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────┐  ┌─────────────────────────────┐ │
│  │ Carrousel Caisses│  │ Dernières Opérations        │ │
│  │ (LuminaCard Low) │  │ (LuminaCard Grouped)        │ │
│  └──────────────────┘  └─────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

- **Composant `LuminaCard`** :
  - Fichier : `app/src/main/java/com/example/ui/designsystem/LuminaCard.kt`.
  - Paramètres : `modifier`, `shape = RoundedCornerShape(24.dp)`, `elevation: LuminaCardElevation`, `backgroundBrush: Brush?`, `containerColor: Color?`, `border: BorderStroke?`, `onClick: (() -> Unit)?`, `content`.
- **Mise à jour de `FinancialSummaryCard`** :
  - Utilise `LuminaCard` avec dégradé bleu indigo électrique (`Brush.linearGradient`), ombre portée de profondeur (8.dp) et disposition ultra-compacte.
- **Intégrité des tests et identifiants** :
  - Conservation de `testTag("financial_summary_card")` et compatibilité avec l'ensemble des écrans et tests automatisés existants.
