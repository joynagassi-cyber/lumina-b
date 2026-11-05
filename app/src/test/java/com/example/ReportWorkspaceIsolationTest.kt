package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.ReportExporter
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReportWorkspaceIsolationTest {
    @Test
    fun rapports_generes_sont_ranges_par_dossier() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reportRoot = File(context.cacheDir, "reports")
        try {
            val fileA = ReportExporter.generateCsvExport(
                context, "Période", emptyList(), 0L, 0L, 0L,
                workspaceId = "church-a"
            )
            val fileB = ReportExporter.generateCsvExport(
                context, "Période", emptyList(), 0L, 0L, 0L,
                workspaceId = "church-b"
            )
            assertNotEquals(fileA.parentFile?.canonicalPath, fileB.parentFile?.canonicalPath)
            assertTrue(fileA.parentFile?.name == "church-a")
            assertTrue(fileB.parentFile?.name == "church-b")
        } finally {
            File(reportRoot, "church-a").deleteRecursively()
            File(reportRoot, "church-b").deleteRecursively()
        }
    }
}
