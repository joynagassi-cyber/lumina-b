package com.example

import com.example.data.DemoDataset
import com.example.data.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDatasetTest {
    @Test
    fun jeu_de_demo_est_fictif_complet_et_sans_permission_d_ecriture_reelle() {
        assertEquals(UserRole.LECTURE, DemoDataset.user.roleValue)
        assertFalse(DemoDataset.user.roleValue.canWrite)
        assertFalse(DemoDataset.user.roleValue.canDelete)
        assertTrue(DemoDataset.profile.churchName.contains("Démonstration"))
        assertTrue(DemoDataset.members.isNotEmpty())
        assertTrue(DemoDataset.budgets.isNotEmpty())
        assertTrue(DemoDataset.projects.any { it.kind == "CAMPAIGN" })
        assertTrue(DemoDataset.projects.any { it.kind == "EVENT" })
        assertTrue(DemoDataset.transactions.any { it.projectId > 0L })
        assertTrue(DemoDataset.transactions.all { it.uid.startsWith("demo-") })
    }
}
