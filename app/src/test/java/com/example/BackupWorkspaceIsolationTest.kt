package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.export.BackupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupWorkspaceIsolationTest {
    @Test
    fun copie_automatique_ne_lit_que_le_dossier_actif() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.filesDir, "copies-automatiques")
        root.deleteRecursively()
        try {
            val churchA = File(root, "church-a").apply { mkdirs() }
            val churchB = File(root, "church-b").apply { mkdirs() }
            val fileA = File(churchA, "copie-auto-20261105.json").apply { writeText("A") }
            File(churchB, "copie-auto-20261106.json").writeText("B")

            assertEquals(fileA.canonicalFile, BackupService.latestAutoFile(context, "church-a")?.canonicalFile)
            assertNull(BackupService.latestAutoFile(context, "church-c"))
        } finally {
            root.deleteRecursively()
        }
    }
}
