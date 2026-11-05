package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChurchWorkspaceCatalog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ChurchWorkspaceCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences by lazy {
        context.getSharedPreferences("lumina_church_workspaces", Context.MODE_PRIVATE)
    }

    @Before
    fun clearCatalogAndLegacyDb() {
        preferences.edit().clear().commit()
        context.deleteDatabase("lumina_financial.db")
        context.deleteDatabase("lumina_church_default.db")
        context.databaseList().filter { it.startsWith("lumina_church_") }
            .forEach(context::deleteDatabase)
    }

    @After
    fun cleanup() {
        preferences.edit().clear().commit()
        context.deleteDatabase("lumina_financial.db")
        context.deleteDatabase("lumina_church_default.db")
        context.databaseList().filter { it.startsWith("lumina_church_") }
            .forEach(context::deleteDatabase)
    }

    @Test
    fun creation_de_dossiers_produit_des_ids_et_bases_distincts() {
        val catalog = ChurchWorkspaceCatalog(context)
        val first = catalog.ensureInitialWorkspace()
        val second = catalog.createWorkspace()

        assertNotEquals(first.id, second.id)
        assertNotEquals(first.databaseName, second.databaseName)
        assertEquals(second.id, catalog.getActiveWorkspace()?.id)
        assertEquals(2, catalog.getWorkspaces().size)

        catalog.setActiveWorkspace(first.id)
        assertEquals(first.id, catalog.getActiveWorkspace()?.id)
    }

    @Test
    fun la_migration_conserve_le_nom_du_fichier_historique() {
        val legacyFile = context.getDatabasePath("lumina_financial.db")
        legacyFile.parentFile?.mkdirs()
        legacyFile.writeText("placeholder legacy database")

        val workspace = ChurchWorkspaceCatalog(context).ensureInitialWorkspace()

        assertEquals("lumina_financial.db", workspace.databaseName)
        assertTrue(legacyFile.exists())
    }

    @Test
    fun le_nom_affiche_est_enregistre_par_dossier() {
        val catalog = ChurchWorkspaceCatalog(context)
        val workspace = catalog.ensureInitialWorkspace()

        catalog.updateDisplayName(workspace.id, "Assemblée Lumière")

        assertEquals("Assemblée Lumière", catalog.getWorkspaces().single().displayName)
    }
}
