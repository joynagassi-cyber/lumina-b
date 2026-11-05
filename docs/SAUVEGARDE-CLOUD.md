# Sauvegardes cloud Lumina — état et limites

## Projet Supabase dédié

- Projet : **Lumina Cloud Backups**
- Référence : `ybfebchvsgbgpeesrqld`
- Région : `eu-west-1`
- Plan : **Free**, conservé par choix de l'utilisateur pour l'usage envisagé (une ou plusieurs églises)
- Bucket privé : `lumina-backups` (140 MiB maximum par objet)
- Espace partagé : `church_cloud_workspaces`, membres, invitations à usage unique, manifests de versions et pointeur du backup courant
- Migrations appliquées : `20261105155150_setup_private_cloud_backup_storage`, `20261105160603_shared_church_backup_workspace`, `20261105160632_grant_minimal_church_backup_access`

Les personnes ont des comptes cloud individuels, puis rejoignent le même espace d'église avec une invitation. Tous les membres lisent le même pointeur de sauvegarde courant. Les objets et versions précédentes sont immuables; il n'y a pas de règle client de suppression. Un éditeur peut publier une nouvelle version seulement si la génération qu'il a lue est toujours courante : une mise à jour concurrente est refusée au lieu d'écraser silencieusement le travail d'un autre appareil. Les rôles d'invitation disponibles sont `editor` et `viewer`.

Les règles RLS limitent les données et les objets aux membres de l'église concernée; `service_role` n'est pas requis dans l'application. Le SQL reproductible est dans `supabase/migrations/`.

Le plan Free est celui demandé et peut suffire en volume pour quelques églises, mais le volume réel (notamment les photos de justificatifs) et les limites de disponibilité/rétention du fournisseur restent à surveiller. Cela ne constitue pas une garantie absolue contre une panne ou une perte. Prévoir des exports externes et tester régulièrement la restauration.

## Chiffrement retenu

Les archives seront transmises par HTTPS et protégées par le chiffrement géré par le fournisseur au repos. Cela simplifie la restauration, mais **ce n'est pas du chiffrement de bout en bout** : le fournisseur, et toute personne disposant d'un accès administratif approprié au projet, pourrait techniquement accéder au contenu en clair. Ne pas présenter ce choix comme une confidentialité où seul l'utilisateur peut lire ses données.

Les exports portables Lumina retirent déjà les comptes locaux et PIN. Il faudra conserver cette règle pour les sauvegardes cloud.

## Ce qui n'est pas encore fait

La création du projet et du stockage ne relie pas encore l'application Android à Supabase. **Aucune donnée n'a été téléversée et aucune restauration cloud automatique n'est active à ce stade.** Il reste à implémenter et tester dans l'application :

1. connexion cloud indépendante du PIN local, configuration d'authentification/e-mail de production et invitation d'un compte à l'espace de son église;
2. création d'archives sans comptes locaux/PIN, téléversement en arrière-plan avec reprise après absence de réseau;
3. lecture du pointeur commun et des versions, contrôle SHA-256, restauration avec sauvegarde locale préalable;
4. traitement dans l'application des conflits de génération : ne jamais écraser silencieusement une version publiée depuis un autre appareil;
5. tests réels d'isolation entre deux églises, d'accès entre membres d'une même église, d'invitation à usage unique et de restauration après désinstallation/formatage simulé;
6. politique de rétention, surveillance des quotas Free et procédure de suppression de compte.

Le service cloud réduit fortement le risque de perte du téléphone, mais ne permet pas de garantir une absence absolue de perte : il faut garder une copie externe/export périodique, tester les restaurations et surveiller le volume consommé par les églises.
