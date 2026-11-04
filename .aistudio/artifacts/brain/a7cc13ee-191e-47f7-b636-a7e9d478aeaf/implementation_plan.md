# Lumina-beta - Gestion Financière Minimaliste & Déterministe

Lumina-beta est une application Android conçue pour offrir un suivi rigoureux, sans fioritures et déterministe des flux de trésorerie (entrées et sorties). Elle garantit une traçabilité intégrale via une timeline chronologique et génère un bilan financier détaillé avec exports structurés (Excel/CSV et PDF stylisé).

---

## Décisions Clés & Alignement Utilisateur

> [!IMPORTANT]
> Vos préférences confirmées ont été directement intégrées dans cette architecture :
> - **Stockage 100% Hors-Ligne & Déterministe** : Base de données locale Room (SQLite) sans dépendance réseau, garantissant rapidité instantanée, intégrité mathématique absolue et confidentialité totale.
> - **Rapports et Exports Hybrides** : Génération de fichiers Excel / CSV tabulaires (séparateur universel UTF-8) ainsi qu'un export PDF haute fidélité mis en page proprement avec en-tête, répartition par postes et solde net.
> - **Thème Automatique Adaptatif** : Interface épurée et moderne réactive au thème sombre et clair d'Android, avec touches d'accent émeraude (revenus) et rose terracotta/ambre (dépenses) sur surfaces Material 3 contrastées.

---

## 1. Vue d'Ensemble & Concept

- **Proposition de valeur** : Éliminer l'opacité et l'instabilité des applications financières complexes. Lumina-beta traite chaque transaction comme un fait immuable et calculé au centime près, permettant une vue cristalline de la santé financière.
- **Utilisateurs cibles** : Utilisateurs recherchant un outil sobre, rigoureux et autonome pour piloter leur budget personnel ou d'activité sans pub ni télémétrie superflue.
- **Fonctionnalités maîtresses** :
  1. **Tableau de Bord & Bilan Dynamique** : Solde global en temps réel, total des entrées, total des sorties, taux d'épargne net et répartition par catégories.
  2. **Timeline Chronologique (Historique Précis)** : Journal paginé/filtrable par période (Mois en cours, 30 derniers jours, Année, Période personnalisée) avec indication claire de la date, de l'heure et des notes.
  3. **Saisie Rapide Déterministe** : Formulaire intuitif avec sélecteur Entrée/Sortie, saisie monétaire précise, choix de catégorie prédéfinie ou personnalisée, date et mémo.
  4. **Centre d'Export & Rapports** : Génération en un clic d'un PDF élégant (avec synthèse visuelle) et d'un export CSV/Excel directement partageable via le sélecteur Android natif.

---

## 2. Expérience Utilisateur & Design Visuel

### Parcours & Écrans Principaux
- **Écran 1 : Bilan (Dashboard Financier)**
  - En-tête statutaire : Solde net actuel mis en valeur en grand format typographique.
  - Cartes métriques jumelles : Total Entrées (+ vert émeraude) et Total Sorties (- rouge corail/ambre).
  - Graphique de répartition catégorielle épuré (Canvas Compose haute performance) et ratios de flux.
  - Actions rapides : Bouton flottant d'ajout (+) accessible d'une seule main et raccourci d'export de rapport.
- **Écran 2 : Timeline (Historique des Transactions)**
  - Sélecteur de période fluide (filtre temporel : Tout, Mois actuel, Mois précédent).
  - Liste chronologique groupée par date avec indicateurs visuels distincts (flèche montante/descendante, badges de catégories).
  - Recherche instantanée par intitulé ou note, et suppression/édition avec dialogue de confirmation sécurisé.
- **Écran 3 : Centre d'Export & Rapports**
  - Aperçu synthétique de la période sélectionnée.
  - Option 1 : **Télécharger / Partager PDF** (rendu via `android.graphics.pdf.PdfDocument` avec logo stylisé, tableau des opérations, totaux et solde net).
  - Option 2 : **Exporter CSV / Excel** (compatible Microsoft Excel, Google Sheets, LibreOffice avec encodage UTF-8 et délimiteur adapté).
- **Dialogue d'Ajout / Modification** :
  - Clavier numérique dédié, bascule rapide Revenu/Dépense, tags de catégorie rapides (Salaire, Vente, Alimentation, Transport, Loyer, Loisirs, etc.).

### Identité Visuelle & Ergonomie
- **Typographie** : Clarté comptable, chiffres tabulaires lisibles et hiérarchie typographique stricte.
- **Couleurs & Contraste** :
  - Mode Sombre : Fond noir profond / graphite (`#121316`), cartes surfaces sombres (`#1C1E24`), accents Vert Menthe / Émeraude (`#00E676`) et Corail doux (`#FF5252`).
  - Mode Clair : Fond blanc doux cassé (`#F8F9FA`), cartes blanc pur avec ombres douces, accents Émeraude foncée (`#00897B`) et Cramoisi sobre (`#D32F2F`).
- **Touch Targets** : Minimum 48dp pour tous les boutons et éléments cliquables, respect strict des insets système (`WindowInsets.safeDrawing`).

---

## 3. Décisions Produit & Justifications

1. **Persistance Room (SQLite) avec Flow Réactif** :
   - *Choix* : Utilisation des tables SQLite typées via Room et requêtes DAO réactives `Flow<List<TransactionEntity>>`.
   - *Pourquoi* : Déterminisme absolu, pas de latence réseau, fonctionnement hors-ligne garanti, atomicité des transactions.
2. **Calculs Monétaires en Centimes (Long / Int)** :
   - *Choix* : Stockage des montants sous forme de centimes entiers (`Long amountInCents`), convertis à l'affichage en devise (`€` ou unité locale).
   - *Pourquoi* : Évite toute erreur d'arrondi à virgule flottante (`Double` / `Float`) inhérente au format binaire IEEE 754, garantissant l'exactitude comptable au centime près.
3. **Moteur d'Export PDF Natif Android** :
   - *Choix* : Rendu vectoriel via `android.graphics.pdf.PdfDocument` et partage via `FileProvider` natif d'Android.
   - *Pourquoi* : Rendu parfait sans dépendance externe lourde, support d'impression standard et partage instantané par email, Drive ou messagerie.

---

## 4. Architecture Technique & Flux de Données

```
┌───────────────────────────────────────────────────────────────┐
│                    Interface Utilisateur (M3)                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐  │
│  │   Écran Bilan    │  │  Écran Timeline  │  │ Fiche Ajout │  │
│  │ (Stats & Ratios) │  │  (Historique)    │  │  (Dialogue) │  │
│  └────────▲─────────┘  └────────▲─────────┘  └──────▲──────┘  │
└───────────┼─────────────────────┼───────────────────┼─────────┘
            │                     │                   │
┌───────────┴─────────────────────┴───────────────────┴─────────┐
│                     FinancialViewModel                        │
│   - stateFlow: UI State (Transactions, Totaux, Période)       │
│   - Actions: addTransaction(), deleteTransaction(), filter()  │
│   - Export: generatePdfReport(), generateCsvExport()          │
└───────────────────────────────┬───────────────────────────────┘
                                │
┌───────────────────────────────▼───────────────────────────────┐
│                  FinancialRepository (Clean)                  │
│       Abstrait les requêtes et fournit les flux réactifs      │
└───────────────────────────────┬───────────────────────────────┘
                                │
┌───────────────────────────────▼───────────────────────────────┐
│             Room Database (LuminaDatabase / SQLite)           │
│   - Entity: TransactionEntity (id, type, amountCents, date...)│
│   - DAO: TransactionDao (queries, filtres temporels, Flow)    │
└───────────────────────────────────────────────────────────────┘
```

### Modèle de Données Principal
```kotlin
enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val amountCents: Long, // Déterminisme : évite les erreurs de flottants
    val category: String,
    val timestamp: Long,   // Heure exacte de l'opération
    val note: String = ""
)
```

### Plan de Déploiement & Tests
1. **Initialisation** : Mise à jour du nom `Lumina-beta` dans `metadata.json` et `strings.xml`.
2. **Couche Données** : Implémentation de Room (Entity, DAO, Database, Repository, FileProvider).
3. **Logique Métier & Calculs** : ViewModel gérant le solde net, les entrées, sorties, calculs par catégorie et génération de rapports.
4. **Interface Graphique M3** : Navigation entre Bilan, Timeline et Exports avec interface soignée, animations discrètes et composants accessibles.
5. **Vérification** : Compilation complète avec `compile_applet`.
