package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.ChurchProfileEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.BilanSnapshot
import com.example.util.CurrencyFormatter
import com.example.util.ImageStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Génère le bilan financier d'une église.
 *
 * Le document suit une structure comptable lisible par un conseil
 * d'administration ou un organisme de contrôle : en-tête de l'organisation,
 * synthèse, compte de résultat par nature, situation de trésorerie par caisse,
 * graphique d'évolution, répartition des charges, détail des écritures puis
 * bloc de signatures. Aucune donnée ne quitte l'appareil.
 */
object ReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val ACCENT_DEFAULT = 0xFF3730A3.toInt()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val fullDateTimeFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)

    // ---------------------------------------------------------------------
    // Bilan PDF
    // ---------------------------------------------------------------------
    fun generatePdfReport(
        context: Context,
        profile: ChurchProfileEntity = ChurchProfileEntity(),
        periodLabel: String,
        transactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        netBalanceCents: Long,
        bilan: BilanSnapshot = BilanSnapshot(),
        memberCount: Int = 0
    ): File {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = profile.displayName
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim()
            .replace(" ", "_")
            .ifBlank { "Bilan" }
        val pdfFile = File(cacheDir, "${safeName}_${System.currentTimeMillis()}.pdf")

        val accent = runCatching {
            AndroidColor.parseColor(profile.accentColorHex)
        }.getOrDefault(ACCENT_DEFAULT)
        val logo: Bitmap? = ImageStore.base64ToBitmap(profile.logoBase64)

        val titlePaint = textPaint(18f, AndroidColor.rgb(17, 24, 39), bold = true)
        val orgPaint = textPaint(15f, AndroidColor.rgb(17, 24, 39), bold = true)
        val subtitlePaint = textPaint(8.5f, AndroidColor.rgb(100, 116, 139))
        val sectionPaint = textPaint(11f, AndroidColor.rgb(17, 24, 39), bold = true)
        val regularPaint = textPaint(8.5f, AndroidColor.rgb(63, 63, 70))
        val boldPaint = textPaint(8.5f, AndroidColor.rgb(9, 9, 11), bold = true)
        val smallPaint = textPaint(7.5f, AndroidColor.rgb(113, 113, 122))
        val headerTextPaint = textPaint(8f, AndroidColor.WHITE, bold = true)
        val accentTextPaint = textPaint(8.5f, accent, bold = true)
        val cardBgPaint = Paint().apply { color = AndroidColor.rgb(248, 250, 252); isAntiAlias = true }
        val cardBorderPaint = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val rulePaint = Paint().apply { color = AndroidColor.rgb(203, 213, 225); strokeWidth = 1f }
        val accentBarPaint = Paint().apply { color = accent; isAntiAlias = true }
        val headerBandPaint = Paint().apply { color = accent; isAntiAlias = true }
        val tableHeaderPaint = Paint().apply { color = AndroidColor.rgb(30, 41, 59); isAntiAlias = true }

        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun footer(text: String = "") {
            val footerPaint = textPaint(7.5f, AndroidColor.rgb(148, 163, 184))
            footerPaint.textAlign = Paint.Align.CENTER
            canvas.drawLine(MARGIN, PAGE_HEIGHT - 34f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 34f, rulePaint)
            val base = "$text Page $pageNumber  •  ${profile.displayName}  •  Édité le ${dateFormat.format(Date())}"
            canvas.drawText(base, PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
        }

        fun newPage(headerTitle: String) {
            footer()
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 4f, headerBandPaint)
            canvas.drawText(profile.displayName.uppercase(), MARGIN, 26f, smallPaint)
            canvas.drawText(headerTitle, MARGIN, 38f, sectionPaint)
            canvas.drawLine(MARGIN, 44f, PAGE_WIDTH - MARGIN, 44f, rulePaint)
        }

        // =================================================================
        // 1. En-tête institutionnel
        // =================================================================
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 96f, headerBandPaint)

        val logoSize = 52f
        val logoLeft = MARGIN
        val logoTop = 22f
        if (logo != null) {
            val bg = Paint().apply { color = AndroidColor.WHITE }
            canvas.drawRoundRect(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize, 8f, 8f, bg)
            val src = Rect(0, 0, logo.width, logo.height)
            val dst = Rect(
                (logoLeft + 4f).toInt(),
                (logoTop + 4f).toInt(),
                (logoLeft + logoSize - 4f).toInt(),
                (logoTop + logoSize - 4f).toInt()
            )
            canvas.drawBitmap(logo, src, dst, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
        } else {
            val white = Paint().apply { color = AndroidColor.WHITE }
            canvas.drawRoundRect(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize, 8f, 8f, white)
            val initialsPaint = textPaint(20f, accent, bold = true)
            initialsPaint.textAlign = Paint.Align.CENTER
            val initials = profile.displayName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "E" }
            canvas.drawText(initials, logoLeft + logoSize / 2f, logoTop + 34f, initialsPaint)
        }

        val textLeft = logoLeft + logoSize + 14f
        val whiteTitle = textPaint(17f, AndroidColor.WHITE, bold = true)
        val whiteSub = textPaint(8.5f, AndroidColor.rgb(219, 234, 254))

        canvas.drawText(profile.displayName.take(40), textLeft, 40f, whiteTitle)

        val orgDetails = buildList {
            if (profile.denomination.isNotBlank()) add(profile.denomination)
            if (profile.city.isNotBlank()) add(profile.city)
            if (profile.phone.isNotBlank()) add("Tél. ${profile.phone}")
            if (profile.email.isNotBlank()) add(profile.email)
        }.joinToString("  •  ")
        if (orgDetails.isNotBlank()) {
            canvas.drawText(orgDetails.take(95), textLeft, 54f, whiteSub)
        }
        if (profile.address.isNotBlank()) {
            canvas.drawText(profile.address.take(95), textLeft, 65f, whiteSub)
        }
        canvas.drawText(
            "BILAN FINANCIER ET COMPTE DE TRÉSORERIE — ${periodLabel.uppercase()}",
            textLeft,
            80f,
            textPaint(8.5f, AndroidColor.WHITE, bold = true)
        )

        // =================================================================
        // 2. Bandeau d'identification du document
        // =================================================================
        var currentY = 112f
        canvas.drawText("DOCUMENT DE GESTION", MARGIN, currentY, sectionPaint)
        currentY += 12f
        val identityLine = buildString {
            append(tableRow("Période analysée", periodLabel))
            if (profile.pastorName.isNotBlank()) append("   |   ").append(tableRow("Responsable", profile.pastorName))
            if (profile.treasurerName.isNotBlank()) append("   |   ").append(tableRow("Trésorier", profile.treasurerName))
            append("   |   ").append(tableRow("Membres au registre", memberCount.toString()))
        }
        canvas.drawText(identityLine.take(150), MARGIN, currentY, regularPaint)
        currentY += 8f
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
        currentY += 16f

        // =================================================================
        // 3. Synthèse (4 indicateurs)
        // =================================================================
        canvas.drawText("1. SYNTHÈSE DE LA PÉRIODE", MARGIN, currentY, sectionPaint)
        currentY += 10f

        val kpiWidth = (PAGE_WIDTH - 2 * MARGIN - 3 * 8f) / 4f
        val kpiHeight = 46f
        val kpis = listOf(
            Triple("PRODUITS", CurrencyFormatter.format(totalIncomeCents), accent),
            Triple("CHARGES", CurrencyFormatter.format(totalExpenseCents), AndroidColor.rgb(190, 24, 93)),
            Triple(
                "RÉSULTAT",
                CurrencyFormatter.format(netBalanceCents, withSign = true, isPositive = netBalanceCents >= 0),
                if (netBalanceCents >= 0) AndroidColor.rgb(4, 120, 87) else AndroidColor.rgb(190, 24, 93)
            ),
            Triple("TRÉSORERIE", CurrencyFormatter.format(bilan.tresorerieTotaleCents), AndroidColor.rgb(30, 41, 59))
        )
        kpis.forEachIndexed { index, kpi ->
            val x = MARGIN + index * (kpiWidth + 8f)
            canvas.drawRoundRect(x, currentY, x + kpiWidth, currentY + kpiHeight, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(x, currentY, x + kpiWidth, currentY + kpiHeight, 6f, 6f, cardBorderPaint)
            canvas.drawText(kpi.first, x + 8f, currentY + 14f, smallPaint)
            canvas.drawText(kpi.second.take(16), x + 8f, currentY + 32f, textPaint(9.5f, kpi.third, bold = true))
        }
        currentY += kpiHeight + 6f
        canvas.drawText(
            "Taux d'épargne de la période : ${String.format(Locale.FRANCE, "%.1f", bilan.tauxEpargnePercent)} %" +
                "   •   Virements internes : ${CurrencyFormatter.format(bilan.totalTransfersCents)} (hors produits et charges)",
            MARGIN,
            currentY + 8f,
            smallPaint
        )
        currentY += 22f

        // =================================================================
        // 4. Compte de résultat par nature
        // =================================================================
        canvas.drawText("2. COMPTE DE RÉSULTAT PAR NATURE", MARGIN, currentY, sectionPaint)
        currentY += 12f

        fun drawResultTable(
            title: String,
            rows: List<Pair<String, Long>>,
            total: Long,
            startY: Float,
            totalLabel: String
        ): Float {
            var y = startY
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f, tableHeaderPaint)
            canvas.drawText(title, MARGIN + 6f, y + 11f, headerTextPaint)
            val totalPaintHeader = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("MONTANT", PAGE_WIDTH - MARGIN - 6f, y + 11f, totalPaintHeader)
            y += 16f

            val rightPaint = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
            val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

            if (rows.isEmpty()) {
                canvas.drawText("Aucun mouvement sur la période", MARGIN + 6f, y + 12f, smallPaint)
                y += 16f
            } else {
                rows.forEachIndexed { index, row ->
                    if (index % 2 == 1) {
                        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 15f, cardBgPaint)
                    }
                    val label = if (row.first.length > 62) row.first.take(60) + "…" else row.first
                    canvas.drawText(label, MARGIN + 6f, y + 11f, regularPaint)
                    canvas.drawText(CurrencyFormatter.format(row.second), PAGE_WIDTH - MARGIN - 6f, y + 11f, rightPaint)
                    y += 15f
                }
            }
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
            y += 12f
            canvas.drawText(totalLabel, MARGIN + 6f, y, boldPaint)
            canvas.drawText(CurrencyFormatter.format(total), PAGE_WIDTH - MARGIN - 6f, y, rightBold)
            return y + 16f
        }

        currentY = drawResultTable(
            "PRODUITS (ENTRÉES)",
            bilan.productsByCategory,
            totalIncomeCents,
            currentY,
            "TOTAL DES PRODUITS"
        )
        currentY = drawResultTable(
            "CHARGES (SORTIES)",
            bilan.chargesByCategory,
            totalExpenseCents,
            currentY,
            "TOTAL DES CHARGES"
        )

        canvas.drawRoundRect(MARGIN, currentY - 6f, PAGE_WIDTH - MARGIN, currentY + 16f, 5f, 5f, cardBgPaint)
        canvas.drawText(
            if (netBalanceCents >= 0) "EXCÉDENT DE LA PÉRIODE" else "DÉFICIT DE LA PÉRIODE",
            MARGIN + 6f,
            currentY + 9f,
            boldPaint
        )
        val resultPaint = Paint(boldPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = if (netBalanceCents >= 0) AndroidColor.rgb(4, 120, 87) else AndroidColor.rgb(190, 24, 93)
        }
        canvas.drawText(
            CurrencyFormatter.format(netBalanceCents, withSign = true, isPositive = netBalanceCents >= 0),
            PAGE_WIDTH - MARGIN - 6f,
            currentY + 9f,
            resultPaint
        )
        currentY += 30f

        // =================================================================
        // 5. Situation de trésorerie par caisse
        // =================================================================
        if (bilan.registerBalances.isNotEmpty()) {
            canvas.drawText("3. SITUATION DE TRÉSORERIE PAR CAISSE", MARGIN, currentY, sectionPaint)
            currentY += 12f
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 16f, tableHeaderPaint)
            canvas.drawText("CAISSE", MARGIN + 6f, currentY + 11f, headerTextPaint)
            val rightHeader = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("SOLDE ACTUEL", PAGE_WIDTH - MARGIN - 6f, currentY + 11f, rightHeader)
            currentY += 16f
            bilan.registerBalances.forEachIndexed { index, entry ->
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, cardBgPaint)
                }
                canvas.drawText(entry.first.take(60), MARGIN + 6f, currentY + 11f, regularPaint)
                val rightPaint = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    CurrencyFormatter.format(entry.second),
                    PAGE_WIDTH - MARGIN - 6f,
                    currentY + 11f,
                    rightPaint
                )
                currentY += 15f
            }
            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
            currentY += 12f
            canvas.drawText("TOTAL TRÉSORERIE", MARGIN + 6f, currentY, boldPaint)
            val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(
                CurrencyFormatter.format(bilan.tresorerieTotaleCents),
                PAGE_WIDTH - MARGIN - 6f,
                currentY,
                rightBold
            )
            currentY += 22f

            // Mention de contrôle interne
            canvas.drawText(
                "Les virements internes déplacent la trésorerie d'une caisse à l'autre sans affecter le résultat.",
                MARGIN,
                currentY,
                smallPaint
            )
            currentY += 18f
        }

        // =================================================================
        // 6. Graphique d'évolution (6 derniers mois)
        // =================================================================
        val series = bilan.monthlySeries
        if (series.isNotEmpty()) {
            val chartHeight = 96f
            if (currentY + chartHeight + 40f > PAGE_HEIGHT - 60f) {
                newPage("BILAN FINANCIER — SUITE")
                currentY = 58f
            }
            canvas.drawText("4. ÉVOLUTION DES FLUX SUR 6 MOIS", MARGIN, currentY, sectionPaint)
            currentY += 10f

            val chartTop = currentY
            val chartBottom = chartTop + chartHeight
            val chartLeft = MARGIN + 30f
            val chartRight = PAGE_WIDTH - MARGIN - 10f
            val maxValue = series.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0L

            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, rulePaint)
            canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, rulePaint)

            val slotWidth = (chartRight - chartLeft) / series.size
            val barWidth = slotWidth * 0.28f
            val incomePaintBars = Paint().apply { color = accent; isAntiAlias = true }
            val expensePaintBars = Paint().apply {
                color = AndroidColor.rgb(203, 213, 225)
                isAntiAlias = true
            }

            series.forEachIndexed { index, point ->
                val slotLeft = chartLeft + index * slotWidth
                val usable = chartBottom - chartTop - 6f
                val incomeHeight = if (maxValue > 0L) {
                    (point.incomeCents.toFloat() / maxValue.toFloat()) * usable
                } else {
                    0f
                }
                val expenseHeight = if (maxValue > 0L) {
                    (point.expenseCents.toFloat() / maxValue.toFloat()) * usable
                } else {
                    0f
                }
                val incomeLeft = slotLeft + slotWidth * 0.18f
                canvas.drawRect(
                    incomeLeft,
                    chartBottom - incomeHeight,
                    incomeLeft + barWidth,
                    chartBottom,
                    incomePaintBars
                )
                canvas.drawRect(
                    incomeLeft + barWidth + 3f,
                    chartBottom - expenseHeight,
                    incomeLeft + 2 * barWidth + 3f,
                    chartBottom,
                    expensePaintBars
                )
                val labelPaint = textPaint(7f, AndroidColor.rgb(113, 113, 122))
                labelPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(point.label.take(7), slotLeft + slotWidth / 2f, chartBottom + 10f, labelPaint)
            }

            // Légende
            canvas.drawRect(chartLeft, chartBottom + 16f, chartLeft + 8f, chartBottom + 23f, incomePaintBars)
            canvas.drawText("Produits", chartLeft + 12f, chartBottom + 22f, smallPaint)
            canvas.drawRect(chartLeft + 62f, chartBottom + 16f, chartLeft + 70f, chartBottom + 23f, expensePaintBars)
            canvas.drawText("Charges", chartLeft + 74f, chartBottom + 22f, smallPaint)
            val scalePaint = textPaint(7f, AndroidColor.rgb(148, 163, 184))
            canvas.drawText(CurrencyFormatter.format(maxValue), chartLeft - 4f, chartTop + 6f, scalePaint)

            currentY = chartBottom + 34f
        }

        // =================================================================
        // 7. Répartition des charges
        // =================================================================
        val totalCharges = bilan.chargesByCategory.sumOf { it.second }
        if (totalCharges > 0L) {
            if (currentY + 90f > PAGE_HEIGHT - 80f) {
                newPage("BILAN FINANCIER — SUITE")
                currentY = 58f
            }
            canvas.drawText("5. RÉPARTITION DES CHARGES", MARGIN, currentY, sectionPaint)
            currentY += 12f

            bilan.chargesByCategory.take(6).forEach { entry ->
                val ratio = (entry.second.toDouble() / totalCharges.toDouble()).toFloat()
                canvas.drawText(entry.first.take(40), MARGIN, currentY, regularPaint)
                val barLeft = MARGIN + 190f
                val barRight = PAGE_WIDTH - MARGIN - 62f
                canvas.drawRect(barLeft, currentY - 7f, barRight, currentY - 1f, cardBorderPaint)
                canvas.drawRect(barLeft, currentY - 7f, barLeft + (barRight - barLeft) * ratio, currentY - 1f,
                    Paint().apply { color = accent; isAntiAlias = true })
                val percent = String.format(Locale.FRANCE, "%.1f %%", ratio * 100)
                val percentPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    "$percent  ${CurrencyFormatter.format(entry.second)}",
                    PAGE_WIDTH - MARGIN,
                    currentY,
                    percentPaint
                )
                currentY += 15f
            }
            currentY += 10f
        }

        // =================================================================
        // 8. Détail chronologique des écritures
        // =================================================================
        if (transactions.isNotEmpty()) {
            newPage("DÉTAIL DES ÉCRITURES")
            currentY = 58f

            val colDate = MARGIN
            val colCaisse = MARGIN + 52f
            val colCat = MARGIN + 150f
            val colTitle = MARGIN + 268f
            val colAmount = PAGE_WIDTH - MARGIN

            fun drawTableHeaderRow() {
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, tableHeaderPaint)
                canvas.drawText("DATE", colDate + 4f, currentY + 10f, headerTextPaint)
                canvas.drawText("CAISSE", colCaisse, currentY + 10f, headerTextPaint)
                canvas.drawText("CATÉGORIE", colCat, currentY + 10f, headerTextPaint)
                canvas.drawText("INTITULÉ", colTitle, currentY + 10f, headerTextPaint)
                val right = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("MONTANT", colAmount - 4f, currentY + 10f, right)
                currentY += 15f
            }
            drawTableHeaderRow()

            val rightRegular = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
            transactions.sortedByDescending { it.timestamp }.forEachIndexed { index, tx ->
                if (currentY + 16f > PAGE_HEIGHT - 45f) {
                    newPage("DÉTAIL DES ÉCRITURES (SUITE)")
                    currentY = 58f
                    drawTableHeaderRow()
                }
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 14f, cardBgPaint)
                }
                val title = if (tx.note.isNotBlank()) "${tx.title} — ${tx.note}" else tx.title
                canvas.drawText(dateFormat.format(Date(tx.timestamp)), colDate + 4f, currentY + 10f, regularPaint)
                canvas.drawText(tx.cashRegisterName.take(17), colCaisse, currentY + 10f, regularPaint)
                canvas.drawText(tx.category.take(19), colCat, currentY + 10f, regularPaint)
                canvas.drawText(title.take(38), colTitle, currentY + 10f, regularPaint)

                val amountColor = when {
                    tx.isTransfer -> AndroidColor.rgb(100, 116, 139)
                    tx.type == TransactionType.INCOME -> AndroidColor.rgb(4, 120, 87)
                    else -> AndroidColor.rgb(190, 24, 93)
                }
                val amountPaint = Paint(regularPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = amountColor
                }
                val prefix = when {
                    tx.isTransfer -> "⇄ "
                    tx.type == TransactionType.INCOME -> "+ "
                    else -> "− "
                }
                canvas.drawText(
                    "$prefix${CurrencyFormatter.format(tx.amountCents)}",
                    colAmount - 4f,
                    currentY + 10f,
                    amountPaint
                )
                currentY += 14f
            }
            currentY += 6f
            canvas.drawText("${transactions.size} écriture(s) — virements internes signalés par ⇄", MARGIN, currentY, smallPaint)
            currentY += 20f
        }

        // =================================================================
        // 9. Validation et signatures
        // =================================================================
        if (currentY + 110f > PAGE_HEIGHT - 60f) {
            newPage("VALIDATION DU DOCUMENT")
            currentY = 58f
        }
        canvas.drawText("6. VALIDATION", MARGIN, currentY, sectionPaint)
        currentY += 12f
        canvas.drawText(
            "Document établi à partir des écritures saisies dans l'application de gestion de l'église.",
            MARGIN,
            currentY,
            regularPaint
        )
        currentY += 14f
        canvas.drawText(
            "Les montants sont exprimés en ${profile.currencyCode.ifBlank { "FCFA" }} et arrêtés à la date d'édition.",
            MARGIN,
            currentY,
            regularPaint
        )
        currentY += 26f

        val signatureWidth = (PAGE_WIDTH - 2 * MARGIN - 20f) / 3f
        val labels = listOf(
            profile.treasurerName.ifBlank { "Trésorier" } to "Trésorier",
            profile.pastorName.ifBlank { "Responsable" } to "Responsable",
            "Contrôle / Audit" to "Visa"
        )
        labels.forEachIndexed { index, entry ->
            val x = MARGIN + index * (signatureWidth + 10f)
            canvas.drawRect(x, currentY, x + signatureWidth, currentY + 46f, cardBorderPaint)
            canvas.drawText(entry.first.take(26), x + 6f, currentY + 14f, smallPaint)
            canvas.drawText(entry.second, x + 6f, currentY + 40f, smallPaint)
        }
        currentY += 60f
        canvas.drawText(
            "Nom, date et signature",
            MARGIN,
            currentY,
            textPaint(7.5f, AndroidColor.rgb(148, 163, 184))
        )

        footer()
        document.finishPage(page)

        FileOutputStream(pdfFile).use { out -> document.writeTo(out) }
        document.close()
        if (logo != null && !logo.isRecycled) logo.recycle()

        return pdfFile
    }

    // ---------------------------------------------------------------------
    // Export CSV / tableur
    // ---------------------------------------------------------------------
    fun generateCsvExport(
        context: Context,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        netBalanceCents: Long,
        profile: ChurchProfileEntity = ChurchProfileEntity()
    ): File {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val csvFile = File(cacheDir, "Releve_${System.currentTimeMillis()}.csv")

        FileOutputStream(csvFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.append("# ${profile.displayName} — relevé des opérations\n")
                writer.append("# Période: ").append(periodLabel).append("\n")
                writer.append("# Édité le: ").append(fullDateTimeFormat.format(Date())).append("\n")
                writer.append("# Devise: ").append(profile.currencyCode.ifBlank { "FCFA" }).append("\n\n")

                writer.append("Date;Heure;Caisse;Nature;Catégorie;Intitulé;Montant;Virement interne;Justificatif;Note\n")

                for (tx in transactions) {
                    val dateStr = dateFormat.format(Date(tx.timestamp))
                    val timeStr = timeFormat.format(Date(tx.timestamp))
                    val nature = when {
                        tx.isTransfer -> "VIREMENT"
                        tx.type == TransactionType.INCOME -> "ENTREE"
                        else -> "SORTIE"
                    }
                    val amountRaw = when {
                        tx.type == TransactionType.INCOME -> tx.amountCents
                        else -> -tx.amountCents
                    }
                    writer.append(dateStr).append(';')
                        .append(timeStr).append(';')
                        .append('"').append(tx.cashRegisterName.replace(";", ",")).append('"').append(';')
                        .append(nature).append(';')
                        .append(tx.category.replace(";", ",")).append(';')
                        .append('"').append(tx.title.replace(";", ",")).append('"').append(';')
                        .append(amountRaw.toString()).append(';')
                        .append(if (tx.isTransfer) "OUI" else "NON").append(';')
                        .append(if (tx.hasReceipt) "OUI" else "NON").append(';')
                        .append('"').append(tx.note.replace(";", ",").replace("\n", " ")).append('"')
                        .append('\n')
                }

                writer.append("\n# SYNTHESE DE LA PERIODE\n")
                writer.append("TOTAL PRODUITS;").append(totalIncomeCents.toString()).append('\n')
                writer.append("TOTAL CHARGES;").append(totalExpenseCents.toString()).append('\n')
                writer.append("RESULTAT;").append(netBalanceCents.toString()).append('\n')
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

        val chooser = Intent.createChooser(intent, "Partager le bilan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ---------------------------------------------------------------------
    // Aides de dessin
    // ---------------------------------------------------------------------
    private fun textPaint(size: Float, color: Int, bold: Boolean = false): Paint = Paint().apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        isAntiAlias = true
    }

    private fun tableRow(label: String, value: String): String = "$label : $value"
}
