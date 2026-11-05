package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.ChurchProfileEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.export.ReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
class ReportExporterTest {
    @Test
    fun csv_protege_les_cellules_formulees_et_les_separateurs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val csv = ReportExporter.generateCsvExport(
            context = context,
            periodLabel = "Novembre",
            transactions = listOf(
                TransactionEntity(
                    title = "=HYPERLINK(\"https://bad\")",
                    type = TransactionType.EXPENSE,
                    amountCents = 100,
                    category = "A; B",
                    note = "ligne 1\nligne 2"
                )
            ),
            totalIncomeCents = 0,
            totalExpenseCents = 100,
            netBalanceCents = -100
        )
        val text = csv.readText(Charsets.UTF_8)
        assertTrue(text.contains("'=HYPERLINK"))
        assertTrue(text.contains("\"A; B\""))
        assertTrue(text.contains("ligne 1 ligne 2"))
        csv.delete()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rapport_tableau_refuse_un_tableau_sans_colonnes() {
        ReportExporter.generateTablePdf(
            context = ApplicationProvider.getApplicationContext<Context>(),
            profile = ChurchProfileEntity(churchName = "Église de Kandi"),
            title = "Rapport des membres",
            periodLabel = "Tout le registre",
            columns = emptyList(),
            rows = emptyList()
        )
    }

    @Test
    fun excel_est_un_classeur_ooxml_distinct_du_csv_et_echappe_les_donnees() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val excel = ReportExporter.generateExcelExport(
            context = context,
            periodLabel = "Novembre 2026",
            transactions = listOf(
                TransactionEntity(
                    title = "Dîme & offrande",
                    type = TransactionType.INCOME,
                    amountCents = 12345,
                    category = "Dîmes",
                    timestamp = 1_790_000_000_000L,
                    note = "Don <spécial>",
                    cashRegisterName = "Caisse principale"
                )
            ),
            totalIncomeCents = 12345,
            totalExpenseCents = 0,
            netBalanceCents = 12345,
            profile = ChurchProfileEntity(churchName = "Église de Kandi")
        )

        assertTrue(excel.name.endsWith(".xlsx"))
        ZipFile(excel).use { workbook ->
            assertTrue(workbook.getEntry("[Content_Types].xml") != null)
            assertTrue(workbook.getEntry("xl/workbook.xml") != null)
            val sheet = workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet1.xml"))
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(sheet.contains("Dîme &amp; offrande"))
            assertTrue(sheet.contains("Don &lt;spécial&gt;"))
            assertTrue(sheet.contains("<c r=\"G4\"><v>12345</v></c>"))
            val parser = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            val document = parser.parse(workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet1.xml")))
            assertEquals("worksheet", document.documentElement.localName)
            assertEquals(7, document.getElementsByTagNameNS(
                "http://schemas.openxmlformats.org/spreadsheetml/2006/main", "row"
            ).length)
        }
        excel.delete()
    }
}
