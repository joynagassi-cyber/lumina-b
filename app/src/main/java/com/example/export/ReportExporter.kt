package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val fullDateTimeFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)

    fun generatePdfReport(
        context: Context,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        netBalanceCents: Long
    ): File {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Lumina_Bilan_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(cacheDir, fileName)

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        // Configuration du style monochrome & indigo sombre
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(55, 48, 163) // Indigo sombre #3730A3
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = AndroidColor.rgb(113, 113, 122) // Zinc monochrome
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = AndroidColor.rgb(24, 24, 27) // Noir zinc
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerTextPaint = Paint().apply {
            color = AndroidColor.rgb(255, 255, 255)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val regularPaint = Paint().apply {
            color = AndroidColor.rgb(63, 63, 70)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = AndroidColor.rgb(9, 9, 11)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val incomePaint = Paint().apply {
            color = AndroidColor.rgb(55, 48, 163) // Indigo sombre
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val expensePaint = Paint().apply {
            color = AndroidColor.rgb(113, 113, 122) // Monochrome zinc
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = AndroidColor.rgb(248, 250, 252)
            isAntiAlias = true
        }

        val cardBorderPaint = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val rowBgAltPaint = Paint().apply {
            color = AndroidColor.rgb(248, 250, 252)
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeaderAndKPIs() {
            // Bandeau supérieur décoratif
            val bannerPaint = Paint().apply {
                color = AndroidColor.rgb(55, 48, 163) // Indigo sombre
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, bannerPaint)

            // Titre & En-tête
            canvas.drawText("LUMINA-BETA", 36f, 38f, titlePaint)
            canvas.drawText("RAPPORT FINANCIER & BILAN DE TRÉSORERIE DÉTERMINISTE", 36f, 50f, subtitlePaint)
            val metaInfo = "Période : $periodLabel  |  Édité le : ${fullDateTimeFormat.format(Date())}"
            canvas.drawText(metaInfo, 36f, 62f, subtitlePaint)

            // KPI Cards (3 cartes : Entrées, Sorties, Solde Net)
            val cardY = 74f
            val cardHeight = 52f
            val cardSpacing = 10f
            val cardWidth = (pageWidth - 72f - (cardSpacing * 2)) / 3f

            // Carte 1 : Total Entrées
            val x1 = 36f
            canvas.drawRoundRect(x1, cardY, x1 + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(x1, cardY, x1 + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            canvas.drawText("TOTAL ENTRÉES", x1 + 10f, cardY + 18f, subtitlePaint)
            val incomeText = com.example.util.CurrencyFormatter.format(totalIncomeCents, withSign = true, isPositive = true)
            canvas.drawText(incomeText, x1 + 10f, cardY + 38f, incomePaint.apply { textSize = 11.5f })
            incomePaint.textSize = 8.5f

            // Carte 2 : Total Sorties
            val x2 = x1 + cardWidth + cardSpacing
            canvas.drawRoundRect(x2, cardY, x2 + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(x2, cardY, x2 + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            canvas.drawText("TOTAL SORTIES", x2 + 10f, cardY + 18f, subtitlePaint)
            val expenseText = com.example.util.CurrencyFormatter.format(totalExpenseCents, withSign = true, isPositive = false)
            canvas.drawText(expenseText, x2 + 10f, cardY + 38f, expensePaint.apply { textSize = 11.5f })
            expensePaint.textSize = 8.5f

            // Carte 3 : Solde Net
            val x3 = x2 + cardWidth + cardSpacing
            canvas.drawRoundRect(x3, cardY, x3 + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(x3, cardY, x3 + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            canvas.drawText("SOLDE NET DE PÉRIODE", x3 + 10f, cardY + 18f, subtitlePaint)
            val netText = com.example.util.CurrencyFormatter.format(netBalanceCents, withSign = true, isPositive = netBalanceCents >= 0)
            val netPaint = if (netBalanceCents >= 0) incomePaint else expensePaint
            canvas.drawText(netText, x3 + 10f, cardY + 38f, netPaint.apply { textSize = 11.5f })
            netPaint.textSize = 8.5f
        }

        drawHeaderAndKPIs()

        var currentY = 145f
        canvas.drawText("DÉTAIL CHRONOLOGIQUE PAR CAISSE", 36f, currentY, sectionPaint)
        currentY += 12f

        // Table Header
        val colDate = 36f
        val colCaisse = 92f
        val colType = 175f
        val colCat = 222f
        val colTitle = 308f
        val colAmount = 559f // Alignement droite

        val tableHeaderPaint = Paint().apply {
            color = AndroidColor.rgb(30, 41, 59) // Dark Slate
        }
        canvas.drawRect(36f, currentY, pageWidth - 36f, currentY + 18f, tableHeaderPaint)

        canvas.drawText("DATE", colDate + 4f, currentY + 12f, headerTextPaint)
        canvas.drawText("CAISSE", colCaisse, currentY + 12f, headerTextPaint)
        canvas.drawText("TYPE", colType, currentY + 12f, headerTextPaint)
        canvas.drawText("CATÉGORIE", colCat, currentY + 12f, headerTextPaint)
        canvas.drawText("INTITULÉ", colTitle, currentY + 12f, headerTextPaint)

        val rightAlignHeaderPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("MONTANT", colAmount - 6f, currentY + 12f, rightAlignHeaderPaint)

        currentY += 18f

        val rightAlignRegular = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightAlignIncome = Paint(incomePaint).apply { textAlign = Paint.Align.RIGHT }
        val rightAlignExpense = Paint(expensePaint).apply { textAlign = Paint.Align.RIGHT }

        val rowHeight = 20f

        for ((index, tx) in transactions.withIndex()) {
            if (currentY + rowHeight > pageHeight - 50f) {
                // Footer de page
                val footerPaint = Paint().apply {
                    color = AndroidColor.rgb(148, 163, 184)
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    "Page $pageNumber  •  Lumina-beta  •  Calculs déterministes certifiés en FCFA",
                    pageWidth / 2f,
                    pageHeight - 20f,
                    footerPaint
                )

                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                currentY = 40f
                // Redessiner entête de tableau
                canvas.drawRect(36f, currentY, pageWidth - 36f, currentY + 18f, tableHeaderPaint)
                canvas.drawText("DATE", colDate + 4f, currentY + 12f, headerTextPaint)
                canvas.drawText("CAISSE", colCaisse, currentY + 12f, headerTextPaint)
                canvas.drawText("TYPE", colType, currentY + 12f, headerTextPaint)
                canvas.drawText("CATÉGORIE", colCat, currentY + 12f, headerTextPaint)
                canvas.drawText("INTITULÉ", colTitle, currentY + 12f, headerTextPaint)
                canvas.drawText("MONTANT", colAmount - 6f, currentY + 12f, rightAlignHeaderPaint)
                currentY += 18f
            }

            if (index % 2 == 1) {
                canvas.drawRect(36f, currentY, pageWidth - 36f, currentY + rowHeight, rowBgAltPaint)
            }

            val dateStr = dateFormat.format(Date(tx.timestamp))
            val caisseStr = if (tx.cashRegisterName.length > 14) tx.cashRegisterName.take(13) + "…" else tx.cashRegisterName
            val typeStr = if (tx.type == TransactionType.INCOME) "Entrée" else "Sortie"
            val titleText = if (tx.note.isNotBlank()) "${tx.title} (${tx.note})" else tx.title
            val truncatedTitle = if (titleText.length > 28) titleText.take(26) + "…" else titleText

            canvas.drawText(dateStr, colDate + 4f, currentY + 13f, regularPaint)
            canvas.drawText(caisseStr, colCaisse, currentY + 13f, boldPaint)
            val typeColor = if (tx.type == TransactionType.INCOME) incomePaint else expensePaint
            canvas.drawText(typeStr, colType, currentY + 13f, typeColor)
            canvas.drawText(tx.category.take(13), colCat, currentY + 13f, regularPaint)
            canvas.drawText(truncatedTitle, colTitle, currentY + 13f, regularPaint)

            val amountStr = com.example.util.CurrencyFormatter.format(
                amount = tx.amountCents,
                withSign = true,
                isPositive = tx.type == TransactionType.INCOME
            )
            val amountDrawPaint = if (tx.type == TransactionType.INCOME) rightAlignIncome else rightAlignExpense
            canvas.drawText(amountStr, colAmount - 6f, currentY + 13f, amountDrawPaint)

            currentY += rowHeight
        }

        // Ligne de synthèse finale
        currentY += 8f
        val linePaint = Paint().apply {
            color = AndroidColor.rgb(203, 213, 225)
            strokeWidth = 1f
        }
        canvas.drawLine(36f, currentY, pageWidth - 36f, currentY, linePaint)
        currentY += 14f

        val totalOpsText = "Total opérations enregistrées : ${transactions.size}"
        canvas.drawText(totalOpsText, 36f, currentY, boldPaint)

        // Footer dernière page
        val footerPaint = Paint().apply {
            color = AndroidColor.rgb(148, 163, 184)
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Page $pageNumber  •  Lumina-beta  •  Calculs déterministes certifiés",
            pageWidth / 2f,
            pageHeight - 20f,
            footerPaint
        )

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    fun generateCsvExport(
        context: Context,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        netBalanceCents: Long
    ): File {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Lumina_Export_${System.currentTimeMillis()}.csv"
        val csvFile = File(cacheDir, fileName)

        FileOutputStream(csvFile).use { fos ->
            // Écrire le BOM UTF-8 (\uFEFF) pour compatibilité immédiate avec Excel
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Métadonnées d'en-tête
                writer.append("# Lumina-beta - Relevé Comptable Déterministe\n")
                writer.append("# Période: ").append(periodLabel).append("\n")
                writer.append("# Date d'export: ").append(fullDateTimeFormat.format(Date())).append("\n\n")

                // En-têtes de colonnes (séparateur point-virgule adapté Excel Europe)
                writer.append("ID;Date;Heure;Caisse;Type;Catégorie;Intitulé;Montant (FCFA);Note\n")

                // Lignes de données
                for (tx in transactions) {
                    val dateStr = dateFormat.format(Date(tx.timestamp))
                    val timeStr = timeFormat.format(Date(tx.timestamp))
                    val caisseClean = tx.cashRegisterName.replace(";", ",")
                    val typeStr = if (tx.type == TransactionType.INCOME) "ENTREE" else "SORTIE"
                    val amountRaw = if (tx.type == TransactionType.INCOME) tx.amountCents else -tx.amountCents

                    val cleanTitle = tx.title.replace(";", ",")
                    val cleanCategory = tx.category.replace(";", ",")
                    val cleanNote = tx.note.replace(";", ",").replace("\n", " ")

                    writer.append("${tx.id};$dateStr;$timeStr;\"$caisseClean\";$typeStr;$cleanCategory;\"$cleanTitle\";$amountRaw;\"$cleanNote\"\n")
                }

                // Synthèse finale
                writer.append("\n# SYNTHESE DE LA PERIODE (FCFA)\n")
                writer.append("TOTAL ENTRÉES (FCFA);").append(totalIncomeCents.toString()).append("\n")
                writer.append("TOTAL SORTIES (FCFA);").append(totalExpenseCents.toString()).append("\n")
                writer.append("SOLDE NET (FCFA);").append(netBalanceCents.toString()).append("\n")
                writer.append("DEVISE;FCFA (Afrique de l'Ouest - XOF)\n")
            }
        }

        return csvFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Partager le rapport Lumina-beta").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
