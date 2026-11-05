package com.example

import com.example.data.UserEntity
import com.example.data.UserPermissionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserPermissionPolicyTest {
    @Test
    fun aucune_action_metier_n_est_autorisee_sans_session() {
        assertEquals(
            "Connectez-vous pour effectuer cette action",
            UserPermissionPolicy.denialMessage(user = null)
        )
        assertNull(UserPermissionPolicy.denialMessage(user = null, allowUnauthenticated = true))
    }

    @Test
    fun les_roles_et_l_etat_du_compte_sont_verifies_cote_modele() {
        val assistant = UserEntity(displayName = "Awa", role = "ASSISTANT")
        assertEquals(
            "Votre profil ne permet pas d'administrer les utilisateurs",
            UserPermissionPolicy.denialMessage(assistant, needManageUsers = true)
        )
        assertNull(UserPermissionPolicy.denialMessage(assistant))

        val reader = assistant.copy(role = "LECTURE")
        assertEquals(
            "Votre profil est en lecture seule",
            UserPermissionPolicy.denialMessage(reader)
        )
        assertEquals(
            "Ce compte n'est plus actif",
            UserPermissionPolicy.denialMessage(assistant.copy(isActive = false))
        )
    }
}
