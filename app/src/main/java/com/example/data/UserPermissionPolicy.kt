package com.example.data

/** Centralise les vérifications d'autorisation indépendamment de l'interface. */
object UserPermissionPolicy {
    /** Retourne le motif du refus, ou null si l'action est autorisée. */
    fun denialMessage(
        user: UserEntity?,
        needDelete: Boolean = false,
        needManageUsers: Boolean = false,
        allowUnauthenticated: Boolean = false
    ): String? {
        if (user == null) {
            return if (allowUnauthenticated) null else "Connectez-vous pour effectuer cette action"
        }
        if (!user.isActive || user.isDeleted) return "Ce compte n'est plus actif"

        val role = user.roleValue
        return when {
            needManageUsers && !role.canManageUsers -> "Votre profil ne permet pas d'administrer les utilisateurs"
            needDelete && !role.canDelete -> "Votre profil ne permet pas de supprimer une écriture"
            !role.canWrite -> "Votre profil est en lecture seule"
            else -> null
        }
    }
}
