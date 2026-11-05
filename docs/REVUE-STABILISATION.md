# Revue de stabilisation — 5 novembre 2026

## Portée

Revue ciblée des chemins d'authentification locale, de saisie des montants, de sauvegarde/restauration, d'invitations, de synchronisation hors ligne, d'images et d'exports. L'objectif était de réduire les pertes silencieuses et les cas de corruption, sans exporter les comptes/PIN locaux ni masquer les divergences de synchronisation.

## Défauts corrigés

- **Contrôle d'accès côté ViewModel** : une session absente n'accorde plus implicitement les droits d'écriture. Les opérations d'administration exigent le rôle trésorier; les seules actions explicitement permises sans session sont l'onboarding et l'import réservé à un nouveau dossier. Les comptes désactivés/supprimés ne peuvent plus se connecter. La création du premier compte est vérifiée dans une transaction.
- **PIN locaux** : nouveaux PIN protégés par PBKDF2 (210 000 itérations, sel aléatoire), comparaison d'empreintes en temps constant, migration automatique des anciens SHA-256 après connexion réussie, et temporisation persistante après cinq erreurs (progression jusqu'à 30 minutes). Les PIN restent propres à l'appareil et ne sont pas ajoutés aux exports portables.
- **Sauvegardes** : l'import d'un ancien fichier partageable ignore les comptes/PIN qu'il pourrait contenir; les utilisateurs locaux existants sont conservés. Les sauvegardes Android système/cloud/transfert d'appareil sont désactivées explicitement pour éviter de migrer comptes et données financières. Les copies automatiques restent locales; l'écran demande désormais de conserver aussi un export externe. Les exports sont écrits de façon atomique avec des noms uniques.
- **Partage des invitations** : le chemin de cache `invitations/` est désormais autorisé par `FileProvider`, ce qui corrige l'échec de partage. Un test Robolectric couvre la résolution de l'URI.
- **Synchronisation** : une différence de contenu entre deux versions d'un même enregistrement devient un conflit à examiner, quel que soit l'ordre des horodatages. La copie locale n'est pas écrasée avant arbitrage; l'application forcée de la copie entrante reste possible. Cela évite de traiter l'horloge d'un téléphone comme preuve suffisante de causalité.
- **Montants** : saisie décimale exacte avec arrondi explicite, rejet des valeurs non finies/hors borne, mêmes bornes dans le ViewModel et validation des transactions importées depuis une sauvegarde ou un échange.
- **Fichiers et rapports** : noms d'exports uniques, écritures temporaires renommées atomiquement, flux sans progression traités sans boucle infinie, décodage des logos/justificatifs limité en dimensions, formatteurs de dates et de devises sûrs en concurrence, et neutralisation des retours à la ligne dans les métadonnées CSV.

## Tests ajoutés

- autorisations et rôles côté modèle;
- PIN PBKDF2, compatibilité et migration des anciennes empreintes;
- parsing/limites des montants;
- conflits de contenu indépendants des horodatages;
- couverture `FileProvider` pour les invitations;
- décodage d'images borné;
- formatage monétaire concurrent;
- rejet des versions invalides de sauvegarde et de synchronisation;
- séparation des comptes locaux lors d'une restauration portable.

## Limites et critères restant avant une déclaration « prêt production »

- Le dépôt ne contient pas les schémas historiques nécessaires pour migrer en sécurité les bases Room **versions 1 à 4**; le code actuel utilise explicitement `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)`. Il faut soit obtenir/tester des bases réelles de ces versions et écrire les migrations, soit confirmer qu'aucune installation à préserver ne les utilise avant release. Ne pas présenter ce risque comme résolu.
- Les tests CI sont des tests unitaires/Robolectric et des builds APK; ils ne remplacent pas des essais sur appareils Android 7–16, notamment restauration après réinstallation, partage réel de fichiers, navigation onboarding et arbitrage de conflits entre deux téléphones.
- La politique de synchronisation est testée au niveau de la règle de décision; un test d'intégration Room couvrant import, journal de conflit, choix local/entrant, pièces jointes et rollback reste souhaitable.
- Le PIN reste un secret de faible entropie (minimum métier actuel : 4 chiffres). PBKDF2 et la temporisation réduisent le risque, mais ne remplacent pas le verrouillage de l'appareil ni une politique d'au moins 6 chiffres si l'ergonomie le permet.
- Les copies automatiques sont locales et ne protègent pas contre la perte/destruction du téléphone. L'utilisateur doit enregistrer périodiquement une archive Lumina hors de l'appareil.
