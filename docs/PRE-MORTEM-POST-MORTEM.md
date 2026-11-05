# Pré-mortem et post-mortem de mise en production — Lumina

**État :** revue préventive, aucune mise en production ni incident observé dans cette branche.

## Pré-mortem — scénarios d'échec à éviter

| Risque | Impact | Garde-fou et test avant publication |
|---|---|---|
| Mauvaise couleur d'accent appliquée à une carte ou un rapport | Écran illisible ou identité incohérente | Palette unique du profil, dégradé héro dérivé du thème ; vérifier les 13 accents en clair et OLED, notamment le contraste des textes blancs. |
| Courbe financière trompeuse avec une seule journée ou des soldes identiques | Décision comptable erronée | Une observation s'affiche comme un point centré sans ligne de sélection verticale ; série constante au milieu de l'échelle. Tester 0, 1, 2 et plusieurs dates, valeurs positives et négatives. |
| Excel exporté sous un faux format ou confondu avec CSV | Import cassé / données mal interprétées | Deux actions explicites : classeur OOXML `.xlsx` empaqueté en ZIP avec cellules texte/numériques typées et CSV UTF-8 `.csv`. Vérifier l'archive, ses relations XML et l'ouverture dans Excel/LibreOffice/Sheets avant release. |
| Champs CSV pris pour des formules | Exécution de contenu de cellule à l'ouverture dans un tableur | Les cellules textuelles sont citées, échappées, ramenées sur une ligne et préfixées d'une apostrophe si elles commencent par `=`, `+`, `-` ou `@`. Garder un test de non-régression. |
| Mise à jour partielle de l'identité de l'église | Informations recouvertes ou navigation confuse | Écran de fiche opaque au-dessus du contenu parent ; tester sur petit écran, avec texte agrandi et navigation système. |
| Données financières attribuées au mauvais profil | Incident majeur de confidentialité/comptabilité | Les dossiers locaux utilisent maintenant des fichiers Room distincts, un sélecteur d'église, des UID stables et des répertoires distincts pour les sauvegardes, exports, échanges et justificatifs. Les imports de sauvegarde/synchronisation vérifient l'identité du dossier. **À valider impérativement** par migration de l'ancien fichier, tests d'étanchéité A↔B, changements répétés de dossier et imports croisés avant toute release. |
| Migration Room 8→10 incomplète | Perte ou blocage à la mise à jour | Tester une vraie base v8 contenant projets et écritures, migrer jusqu'à v10, vérifier les UID d'église générés, les types de projets, les liens et la sauvegarde/restauration. Confirmer que le fichier historique reste intact et que les nouveaux dossiers ouvrent d'autres fichiers. |
| Régression invisible faute d'environnement de build local | Défaut livré malgré les changements | Exécuter `./gradlew :app:testDebugUnitTest` et `./gradlew :app:assembleDebug` dans CI avec JDK + SDK Android ; traiter tout échec avant release. L'environnement de cette session n'a pas de Java. |

## Plan post-mortem après chaque release

1. Enregistrer la version, le commit, les appareils/versions Android testés et les résultats CI (sans données personnelles ni finances réelles).
2. Pendant la recette, comparer les exports PDF, XLSX et CSV d'un même jeu de données synthétique et vérifier totaux, signes, dates, accents et périodes.
3. Rejouer les cas de graphiques vides, à un point, constants, négatifs et avec plusieurs points ; vérifier la navigation et le scroll avec une taille de police Android élevée.
4. En cas d'anomalie : suspendre la diffusion, conserver les journaux techniques expurgés, documenter cause racine, impact, détection manquée, correction et test de non-régression.
5. Ne pas déclarer la release « validée en production » sans retour de recette réelle ; cette branche ne dispose pas d'une observation post-déploiement.

## Éléments de cette branche

- Les cartes d'en-tête utilisent désormais la couleur d'accent active.
- Six accents supplémentaires ont été ajoutés aux sept existants ; leur aperçu est accessible dans Paramètres et l'application du choix est immédiate.
- Le thème clair utilise des surfaces douces, distinctes du blanc pur.
- La sélection de la courbe a été alignée avec les positions réellement dessinées ; les séries constantes et mono-point ne dessinent plus un curseur vertical trompeur.
- L'export Excel est séparé du CSV et produit un vrai classeur OOXML `.xlsx` avec les parties workbook/worksheet et cellules typées ; les cellules CSV sont protégées contre l'injection de formules.
- Les dossiers peuvent être typés Projet, Campagne, Événement ou Collecte ; la migration Room 8→9 ajoute le type sans supprimer les dossiers existants, et le format de sauvegarde conserve cette donnée.
- Les pages Caisses et Budgets proposent une recherche et des filtres d'état accessibles depuis deux icônes ; les projets ouvrent une fiche détaillée avec leurs écritures affectées.
- L'onboarding compte six étapes, avec des illustrations vectorielles Compose.
- Les dossiers d'église locaux ont un catalogue avec sélection/création et un fichier Room distinct par église. Les sauvegardes, rapports, fichiers d'échange et justificatifs sont séparés par dossier ; les restaurations et imports de synchronisation contrôlent un UID d'église stable. La migration conserve `lumina_financial.db`. Le code est ajouté mais les scénarios d'isolation ne sont pas encore exécutés.
- Un profil de démonstration fictif en mémoire est accessible depuis l'écran de connexion ; il bloque les écritures et sauvegardes du dossier réel, mais propose un essai d'opération conservé uniquement en mémoire. Ses justificatifs et exports utilisent le namespace `demo` et sont effacés à la sortie ; la session locale est restaurée, ou l'écran de connexion réapparaît si la démo est ouverte depuis cet écran. Un test vérifie que l'opération d'essai ne persiste pas dans Room ; les tests ne sont pas encore exécutés.
- Les rapports PDF autonomes couvrent le registre des membres, les budgets et l'historique ; les périodes incluent 4 et 6 mois ainsi qu'une plage personnalisée.

Ce document est un garde-fou de livraison, pas une certification de sécurité ni une preuve de disponibilité « sans faille ».
