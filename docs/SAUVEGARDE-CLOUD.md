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

Les règles RLS limitent les données et les objets aux membres de l'église concernée; `service_role` n'est pas requis dans l'application. Le SQL reproductible est dans `supabase/migrations/`. Les migrations enregistrées couvrent le bucket privé, l'espace commun par église, les droits minimaux, l'acceptation idempotente des invitations, l'unicité de l'identité locale, les invitations éditeur et la RPC `github_keepalive` sans accès aux données.

Le plan Free est celui demandé et peut suffire en volume pour quelques églises, mais le volume réel (notamment les photos de justificatifs) et les limites de disponibilité/rétention du fournisseur restent à surveiller. Cela ne constitue pas une garantie absolue contre une panne ou une perte. Prévoir des exports externes et tester régulièrement la restauration.

## Maintien d'activité du projet Free

Le workflow `ci/workflows/supabase-keepalive.yml` (à installer dans `.github/workflows/`) appelle deux fois par semaine une fonction SQL qui ne fait que renvoyer l'heure du serveur. Elle n'accède à aucune sauvegarde et ne nécessite pas de clé privée Supabase : seuls l'URL du projet et la clé publishable, déjà publique pour l'application, sont utilisés. Il dispose aussi d'un lancement manuel et vérifie le retour HTTP.

**Le workflow planifié ne s'exécutera qu'après présence du fichier sur la branche par défaut GitHub (`main`).** L'action réduit le risque d'inactivité, mais GitHub peut retarder/sauter une exécution et le fournisseur peut changer ses règles : ce n'est pas une garantie contre la mise en pause. Un projet déjà en pause devra être réactivé dans Supabase.

## Chiffrement retenu

Les archives seront transmises par HTTPS et protégées par le chiffrement géré par le fournisseur au repos. Cela simplifie la restauration, mais **ce n'est pas du chiffrement de bout en bout** : le fournisseur, et toute personne disposant d'un accès administratif approprié au projet, pourrait techniquement accéder au contenu en clair. Ne pas présenter ce choix comme une confidentialité où seul l'utilisateur peut lire ses données.

Les exports portables Lumina retirent déjà les comptes locaux et PIN. Il faudra conserver cette règle pour les sauvegardes cloud.

## État de l'intégration Android

Le dépôt contient maintenant le client REST Supabase et le parcours de base : session anonyme technique (aucun e-mail, mot de passe ou écran de connexion cloud), création/association d'un espace, invitation, archive sans comptes locaux/PIN, téléversement versionné, comparaison SHA-256, détection de conflits et restauration avec copie locale de sécurité. Dans Paramètres → Sauvegarde, **Activer la sauvegarde** associe l'espace et programme le travail en arrière-plan; **Vérifier la copie** est une action séparée qui télécharge la dernière archive, contrôle son empreinte SHA-256 et son format sans restaurer ni modifier les données. Les codes d'invitation cloud sont dans Équipe & échange.

Une tentative immédiate est mise en file après activation et l'application planifie ensuite un contrôle/téléversement quotidien avec contrainte réseau. Les empreintes logiques évitent de créer une nouvelle version si le contenu n'a pas changé. Pendant un vrai transfert, une barre de progression dans l'en-tête de l'application indique l'étape et le pourcentage d'octets envoyés. Les sessions cloud sont renouvelées avec le jeton de rafraîchissement. L'archive n'est pas chiffrée de bout en bout; seuls HTTPS et le chiffrement géré par Supabase sont utilisés, comme demandé.

**Limites importantes pour les essais :** WorkManager vise une tentative quotidienne (donc plus de trois jours distincts par semaine en fonctionnement normal), avec contrainte réseau et reprise exponentielle; Android peut différer l'exécution selon le réseau, l'économie d'énergie ou l'état de l'appareil. Ce n'est pas une garantie d'heure précise ni d'envoi instantané. La barre mesure le transfert des octets, pas le temps restant. Les jetons anonymes sont liés à l'installation et exclus des sauvegardes Android; après formatage ou perte de l'appareil, il faut qu'un autre membre encore connecté génère un nouveau code d'invitation. Si tous les appareils et tous les codes sont perdus, ces comptes anonymes ne permettent pas de récupérer l'accès au cloud. Il faudra concevoir une vraie procédure de récupération (compte récupérable ou clé/code conservé hors du téléphone) avant de promettre une restauration garantie. Les installations existantes avec une identité d'église différente et des données locales non vides doivent d'abord être rapprochées manuellement; une restauration qui détecte une autre église est refusée.

Avant de considérer le parcours prêt pour la production, il faut encore compiler et tester sur Android, puis valider avec deux appareils : première sauvegarde, invitation/acceptation, même espace partagé, conflits simultanés, restauration après effacement simulé, accès refusé entre deux églises et comportement hors ligne. La CI seule ne prouve pas la restauration réelle. Le plan Free et le stockage doivent aussi être surveillés; conserver des exports indépendants reste recommandé.

Le service cloud réduit le risque de perte du téléphone, mais ne permet pas de garantir une absence absolue de perte.
