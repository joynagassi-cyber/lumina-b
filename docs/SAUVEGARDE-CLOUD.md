# Sauvegardes cloud Lumina — état et limites

## Projet Supabase de test

- Projet dédié : **Lumina Cloud Backups**
- Référence : `ybfebchvsgbgpeesrqld`
- Région : `eu-west-1`
- Plan actuel : **Free (tests uniquement)**
- Bucket privé : `lumina-backups` (140 MiB maximum par objet)
- Manifeste : `public.cloud_backup_manifests`
- Migration appliquée : `20261105155150_setup_private_cloud_backup_storage`

Le bucket n'est pas public. Les règles RLS imposent une session Supabase authentifiée et limitent lecture/téléversement au préfixe de l'utilisateur (`auth.uid()`). Les objets sont immuables via les règles actuelles : aucune mise à jour ni suppression n'est autorisée. Les manifests sont également protégés par RLS. Le SQL reproductible est dans `supabase/migrations/`.

## Chiffrement retenu

Pour ce prototype, les archives seront transmises par HTTPS et protégées par le chiffrement géré par le fournisseur au repos. Cela simplifie la restauration, mais **ce n'est pas du chiffrement de bout en bout** : le fournisseur, et toute personne disposant d'un accès administratif approprié au projet, pourrait techniquement accéder au contenu en clair. Ne pas présenter ce choix comme une confidentialité où seul l'utilisateur peut lire ses données.

Les exports portables Lumina retirent déjà les comptes locaux et PIN. Il faudra conserver cette règle pour les sauvegardes cloud.

## Ce qui n'est pas encore fait

La création du projet et du stockage ne relie pas encore l'application Android à Supabase. **Aucune donnée n'a été téléversée et aucune restauration cloud automatique n'est active à ce stade.** Il reste à implémenter et tester dans l'application :

1. connexion cloud indépendante du PIN local;
2. création d'archives sans comptes locaux/PIN, téléversement en arrière-plan avec reprise après absence de réseau;
3. historique des sauvegardes, contrôle SHA-256, sélection et restauration avec sauvegarde locale préalable;
4. vérification sur un second appareil, après désinstallation/formatage simulé et sur une connexion interrompue;
5. politique de rétention et procédure de suppression de compte;
6. passage à un plan adapté avant d'y conserver des données de production, et vérification des sauvegardes/restaurations du fournisseur.

Le service cloud réduit fortement le risque de perte du téléphone, mais ne permet pas de garantir une absence absolue de perte : il faut garder une copie externe/export périodique, tester les restaurations et éviter de compter sur le projet Free pour les données de production.
