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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        memberCount: Int = 0,
        periodStartMillis: Long = 0L,
        periodEndMillis: Long = 0L,
        generatedBy: String = "",
        documentNumber: String = "",
        workspaceId: String = "default"
    ): File {
        val cacheDir = reportDirectory(context, workspaceId)
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

        // Bornes de la période, référence du document et devise
        val reportEnd = when {
            periodEndMillis <= 0L || periodEndMillis == Long.MAX_VALUE -> System.currentTimeMillis()
            else -> periodEndMillis
        }
        val reportStart = when {
            periodStartMillis > 0L -> periodStartMillis
            else -> transactions.minOfOrNull { it.timestamp } ?: reportEnd
        }
        val periodRangeLabel = "${dateFormat.format(Date(reportStart))} → ${dateFormat.format(Date(reportEnd))}"
        val docRef = documentNumber.ifBlank {
            "RG-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.FRANCE).format(Date())
        }
        val currencyCode = profile.currencyCode.ifBlank { "FCFA" }
        val issuedBy = generatedBy.ifBlank { "Non identifié" }

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
            "RAPPORT DE GESTION MENSUEL — SITUATION DE TRÉSORERIE",
            textLeft,
            80f,
            textPaint(8.5f, AndroidColor.WHITE, bold = true)
        )

        // =================================================================
        // 2. Bandeau d'identification du document
        // =================================================================
        var currentY = 110f
        canvas.drawText("RÉFÉRENCES DU DOCUMENT", MARGIN, currentY, sectionPaint)
        currentY += 11f
        canvas.drawText(
            "Document n° $docRef   •   Période du $periodRangeLabel   •   $periodLabel",
            MARGIN,
            currentY,
            regularPaint
        )
        currentY += 10f
        canvas.drawText(
            "Devise : $currencyCode   •   Base comptable : trésorerie " +
                "(encaissements et décaissements réels, hors engagements)",
            MARGIN,
            currentY,
            smallPaint
        )
        currentY += 10f
        val signatories = buildList {
            if (profile.pastorName.isNotBlank()) add("Responsable : ${profile.pastorName}")
            if (profile.treasurerName.isNotBlank()) add("Trésorier : ${profile.treasurerName}")
            add("Édité le ${fullDateTimeFormat.format(Date())}")
            add("Saisie : $issuedBy")
        }.joinToString("   •   ")
        canvas.drawText(signatories.take(150), MARGIN, currentY, smallPaint)
        currentY += 8f
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
        currentY += 14f

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
            Triple(
                "TRÉSORERIE CLÔTURE",
                CurrencyFormatter.format(
                    if (bilan.registerStatements.isEmpty()) bilan.tresorerieTotaleCents
                    else bilan.closingTresorerieCents
                ),
                AndroidColor.rgb(30, 41, 59)
            )
        )
        kpis.forEachIndexed { index, kpi ->
            val x = MARGIN + index * (kpiWidth + 8f)
            canvas.drawRoundRect(x, currentY, x + kpiWidth, currentY + kpiHeight, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(x, currentY, x + kpiWidth, currentY + kpiHeight, 6f, 6f, cardBorderPaint)
            canvas.drawText(kpi.first, x + 8f, currentY + 14f, smallPaint)
            canvas.drawText(kpi.second.take(16), x + 8f, currentY + 32f, textPaint(9.5f, kpi.third, bold = true))
        }
        currentY += kpiHeight + 6f
        val savingsText = if (totalIncomeCents <= 0L) {
            "N/A (aucun produit enregistré sur la période)"
        } else {
            String.format(Locale.FRANCE, "%.1f %%", bilan.tauxEpargnePercent)
        }
        canvas.drawText(
            "Taux d'épargne de la période : $savingsText" +
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
        val statements = bilan.registerStatements
        if (statements.isNotEmpty()) {
            if (currentY + 90f > PAGE_HEIGHT - 90f) {
                newPage("RAPPORT DE GESTION — SUITE")
                currentY = 58f
            }
            canvas.drawText("3. SITUATION DE TRÉSORERIE PAR CAISSE", MARGIN, currentY, sectionPaint)
            currentY += 12f

            val colName = MARGIN + 4f
            val colType = MARGIN + 128f
            val colOpen = MARGIN + 250f
            val colIn = MARGIN + 336f
            val colOut = MARGIN + 421f
            val colClose = PAGE_WIDTH - MARGIN - 4f
            val cellPaint = textPaint(7.3f, AndroidColor.rgb(63, 63, 70))
            val cellBold = textPaint(7.3f, AndroidColor.rgb(9, 9, 11), bold = true)

            fun moneyCell(value: Long): String = CurrencyFormatter.format(value)

            fun headerRow() {
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, tableHeaderPaint)
                val headSmall = textPaint(7f, AndroidColor.WHITE, bold = true)
                canvas.drawText("CAISSE", colName, currentY + 10f, headSmall)
                canvas.drawText("TYPE", colType, currentY + 10f, headSmall)
                fun rightHead(label: String, x: Float) {
                    val rp = Paint(headSmall).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(label, x, currentY + 10f, rp)
                }
                rightHead("OUVERTURE", colOpen)
                rightHead("ENTRÉES", colIn)
                rightHead("SORTIES", colOut)
                rightHead("CLÔTURE", colClose)
                currentY += 15f
            }
            headerRow()

            fun rightCell(value: String, x: Float, paint: Paint) {
                val rp = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(value, x, currentY + 10f, rp)
            }

            statements.forEachIndexed { index, statement ->
                if (currentY + 16f > PAGE_HEIGHT - 60f) {
                    newPage("RAPPORT DE GESTION — SUITE")
                    currentY = 58f
                    headerRow()
                }
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 14f, cardBgPaint)
                }
                canvas.drawText(statement.name.take(24), colName, currentY + 10f, cellPaint)
                canvas.drawText(statement.typeLabel.take(13), colType, currentY + 10f, cellPaint)
                rightCell(moneyCell(statement.openingCents), colOpen, cellPaint)
                rightCell("+${moneyCell(statement.incomeCents)}", colIn, cellPaint)
                rightCell("−${moneyCell(statement.expenseCents)}", colOut, cellPaint)
                rightCell(moneyCell(statement.closingCents), colClose, cellBold)
                currentY += 14f
            }

            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
            currentY += 12f
            canvas.drawText("TOTAL", colName, currentY, boldPaint)
            fun rightTotal(value: String, x: Float) {
                val rp = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(value, x, currentY, rp)
            }
            rightTotal(moneyCell(bilan.openingTresorerieCents), colOpen)
            rightTotal("+${moneyCell(statements.sumOf { it.incomeCents })}", colIn)
            rightTotal("−${moneyCell(statements.sumOf { it.expenseCents })}", colOut)
            rightTotal(moneyCell(bilan.closingTresorerieCents), colClose)
            currentY += 16f

            canvas.drawText(
                "Contrôle : total ouverture + total entrées − total sorties = total clôture " +
                    "(${CurrencyFormatter.format(bilan.closingTresorerieCents)} $currencyCode).",
                MARGIN,
                currentY,
                smallPaint
            )
            currentY += 10f
            canvas.drawText(
                "Les virements internes figurent dans les mouvements des deux caisses concernées " +
                    "mais n'affectent ni les produits, ni les charges, ni le résultat.",
                MARGIN,
                currentY,
                smallPaint
            )
            currentY += 16f
        }

        // =================================================================
        // 6. Graphique d'évolution (6 derniers mois)
        // =================================================================
        val series = bilan.monthlySeries
        if (series.isNotEmpty()) {
            val chartHeight = 96f
            if (currentY + chartHeight + 40f > PAGE_HEIGHT - 60f) {
                newPage("RAPPORT DE GESTION — SUITE")
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
                newPage("RAPPORT DE GESTION — SUITE")
                currentY = 58f
            }
            canvas.drawText("5. RÉPARTITION DES CHARGES", MARGIN, currentY, sectionPaint)
            currentY += 12f

            val amountRight = PAGE_WIDTH - MARGIN
            val percentRight = PAGE_WIDTH - MARGIN - 78f
            bilan.chargesByCategory.take(6).forEach { entry ->
                val ratio = (entry.second.toDouble() / totalCharges.toDouble()).toFloat()
                canvas.drawText(entry.first.take(38), MARGIN, currentY, regularPaint)
                val barLeft = MARGIN + 190f
                val barRight = percentRight - 96f
                canvas.drawRect(barLeft, currentY - 7f, barRight, currentY - 1f, cardBorderPaint)
                canvas.drawRect(
                    barLeft,
                    currentY - 7f,
                    barLeft + (barRight - barLeft) * ratio,
                    currentY - 1f,
                    Paint().apply { color = accent; isAntiAlias = true }
                )
                val percentPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    String.format(Locale.FRANCE, "%.1f", ratio * 100.0).replace(',', '.') + " %",
                    percentRight,
                    currentY,
                    percentPaint
                )
                val amountPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    CurrencyFormatter.format(entry.second),
                    amountRight,
                    currentY,
                    amountPaint
                )
                currentY += 15f
            }
            canvas.drawLine(MARGIN, currentY - 6f, PAGE_WIDTH - MARGIN, currentY - 6f, rulePaint)
            canvas.drawText("TOTAL DES CHARGES", MARGIN, currentY + 4f, boldPaint)
            val totalPercentPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("100,0 %", percentRight, currentY + 4f, totalPercentPaint)
            canvas.drawText(CurrencyFormatter.format(totalCharges), amountRight, currentY + 4f, totalPercentPaint)
            currentY += 18f
        }

        // =================================================================
        // 6 bis. Budget prévu vs réalisé
        // =================================================================
        val budgetLines = bilan.budgetLines
        if (budgetLines.isNotEmpty()) {
            if (currentY + 100f > PAGE_HEIGHT - 80f) {
                newPage("RAPPORT DE GESTION — SUITE")
                currentY = 58f
            }
            canvas.drawText("6. BUDGET PRÉVU / RÉALISÉ", MARGIN, currentY, sectionPaint)
            currentY += 12f

            val colCatB = MARGIN + 4f
            val colPlan = MARGIN + 250f
            val colReal = MARGIN + 340f
            val colGap = MARGIN + 430f
            val colPct = PAGE_WIDTH - MARGIN - 4f
            val linePaint = textPaint(7.5f, AndroidColor.rgb(63, 63, 70))
            val lineBold = textPaint(7.5f, AndroidColor.rgb(9, 9, 11), bold = true)

            fun budgetHeader() {
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, tableHeaderPaint)
                val head = textPaint(7.2f, AndroidColor.WHITE, bold = true)
                canvas.drawText("CATÉGORIE", colCatB, currentY + 10f, head)
                fun rightH(label: String, x: Float) {
                    val p = Paint(head).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(label, x, currentY + 10f, p)
                }
                rightH("PRÉVU", colPlan)
                rightH("RÉALISÉ", colReal)
                rightH("ÉCART", colGap)
                rightH("USAGE", colPct)
                currentY += 15f
            }
            budgetHeader()

            budgetLines.take(14).forEachIndexed { index, line ->
                if (currentY + 16f > PAGE_HEIGHT - 60f) {
                    newPage("RAPPORT DE GESTION — SUITE")
                    currentY = 58f
                    budgetHeader()
                }
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 14f, cardBgPaint)
                }
                val gap = line.plannedCents - line.actualCents
                val usage = if (line.plannedCents > 0L) {
                    (line.actualCents.toDouble() / line.plannedCents.toDouble()) * 100.0
                } else {
                    null
                }
                canvas.drawText(line.categoryName.take(34), colCatB, currentY + 10f, linePaint)
                fun rightV(value: String, x: Float, paint: Paint, color: Int? = null) {
                    val p = Paint(paint).apply {
                        textAlign = Paint.Align.RIGHT
                        if (color != null) this.color = color
                    }
                    canvas.drawText(value, x, currentY + 10f, p)
                }
                rightV(CurrencyFormatter.format(line.plannedCents), colPlan, linePaint)
                rightV(CurrencyFormatter.format(line.actualCents), colReal, linePaint)
                rightV(
                    CurrencyFormatter.format(kotlin.math.abs(gap), withSign = false),
                    colGap,
                    lineBold,
                    if (gap >= 0) AndroidColor.rgb(4, 120, 87) else AndroidColor.rgb(190, 24, 93)
                )
                rightV(
                    usage?.let { String.format(Locale.FRANCE, "%.0f %%", it) } ?: "—",
                    colPct,
                    linePaint
                )
                currentY += 14f
            }
            currentY += 8f
            canvas.drawText(
                "Écart positif = marge disponible ; écart négatif = dépassement du plafond.",
                MARGIN,
                currentY,
                smallPaint
            )
            currentY += 18f
        }

        // =================================================================
        // 7. Fonds affectés (projets)
        // =================================================================
        val projectLines = bilan.projectSummaries
        if (projectLines.isNotEmpty()) {
            if (currentY + 100f > PAGE_HEIGHT - 80f) {
                newPage("RAPPORT DE GESTION — SUITE")
                currentY = 58f
            }
            canvas.drawText("7. FONDS AFFECTÉS (PROJETS)", MARGIN, currentY, sectionPaint)
            currentY += 12f
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, tableHeaderPaint)
            val head = textPaint(7.2f, AndroidColor.WHITE, bold = true)
            fun headRight(label: String, x: Float) {
                val p = Paint(head).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(label, x, currentY + 10f, p)
            }
            canvas.drawText("PROJET", MARGIN + 4f, currentY + 10f, head)
            headRight("OBJECTIF", MARGIN + 300f)
            headRight("COLLECTÉ", MARGIN + 380f)
            headRight("DÉPENSÉ", MARGIN + 455f)
            headRight("DISPONIBLE", PAGE_WIDTH - MARGIN - 4f)
            currentY += 15f

            val projPaint = textPaint(7.5f, AndroidColor.rgb(63, 63, 70))
            val projBold = textPaint(7.5f, AndroidColor.rgb(9, 9, 11), bold = true)
            projectLines.forEachIndexed { index, project ->
                if (currentY + 16f > PAGE_HEIGHT - 60f) {
                    newPage("RAPPORT DE GESTION — SUITE")
                    currentY = 58f
                }
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 14f, cardBgPaint)
                }
                fun rightVal(value: String, x: Float, paint: Paint, color: Int? = null) {
                    val p = Paint(paint).apply {
                        textAlign = Paint.Align.RIGHT
                        if (color != null) this.color = color
                    }
                    canvas.drawText(value, x, currentY + 10f, p)
                }
                canvas.drawText(project.name.take(38), MARGIN + 4f, currentY + 10f, projPaint)
                rightVal(
                    if (project.targetCents > 0L) CurrencyFormatter.format(project.targetCents) else "—",
                    MARGIN + 300f,
                    projPaint
                )
                rightVal(CurrencyFormatter.format(project.collectedCents), MARGIN + 380f, projPaint, AndroidColor.rgb(4, 120, 87))
                rightVal(CurrencyFormatter.format(project.spentCents), MARGIN + 455f, projPaint, AndroidColor.rgb(190, 24, 93))
                rightVal(
                    CurrencyFormatter.format(project.balanceCents),
                    PAGE_WIDTH - MARGIN - 4f,
                    projBold
                )
                currentY += 14f
            }
            currentY += 8f
            canvas.drawText(
                "Les écritures rattachées à un projet constituent les dons affectés : " +
                    "le disponible ne peut être employé que sur ce projet.",
                MARGIN,
                currentY,
                smallPaint
            )
            currentY += 18f
        }

        // =================================================================
        // 8. Détail chronologique des écritures
        // =================================================================
        if (transactions.isNotEmpty()) {
            newPage("DÉTAIL DES ÉCRITURES")
            currentY = 58f

            val colDate = MARGIN
            val colCaisse = MARGIN + 48f
            val colCat = MARGIN + 134f
            val colTitle = MARGIN + 236f
            val colBy = PAGE_WIDTH - MARGIN - 96f
            val colAmount = PAGE_WIDTH - MARGIN

            fun drawTableHeaderRow() {
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 15f, tableHeaderPaint)
                canvas.drawText("DATE", colDate + 4f, currentY + 10f, headerTextPaint)
                canvas.drawText("CAISSE", colCaisse, currentY + 10f, headerTextPaint)
                canvas.drawText("CATÉGORIE", colCat, currentY + 10f, headerTextPaint)
                canvas.drawText("INTITULÉ", colTitle, currentY + 10f, headerTextPaint)
                val smallHead = textPaint(7f, AndroidColor.WHITE, bold = true)
                canvas.drawText("SAISIE PAR", colBy, currentY + 10f, smallHead)
                val right = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("MONTANT", colAmount - 4f, currentY + 10f, right)
                currentY += 15f
            }
            drawTableHeaderRow()

            val rightRegular = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
            transactions.sortedByDescending { it.timestamp }.forEachIndexed { index, tx ->
                if (currentY + 24f > PAGE_HEIGHT - 45f) {
                    newPage("DÉTAIL DES ÉCRITURES (SUITE)")
                    currentY = 58f
                    drawTableHeaderRow()
                }
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, cardBgPaint)
                }
                val title = if (tx.note.isNotBlank()) "${tx.title} — ${tx.note}" else tx.title
                canvas.drawText(dateFormat.format(Date(tx.timestamp)), colDate + 4f, currentY + 10f, regularPaint)
                canvas.drawText(tx.cashRegisterName.take(15), colCaisse, currentY + 10f, regularPaint)
                canvas.drawText(tx.category.take(18), colCat, currentY + 10f, regularPaint)
                canvas.drawText(title.take(32), colTitle, currentY + 10f, regularPaint)
                val attribution = tx.updatedBy.split(" — ", limit = 2)
                canvas.drawText(
                    attribution.firstOrNull().orEmpty().take(18).ifBlank { "—" },
                    colBy,
                    currentY + 9f,
                    textPaint(6.5f, AndroidColor.rgb(100, 116, 139))
                )
                attribution.getOrNull(1)?.let { role ->
                    canvas.drawText(role.take(16), colBy, currentY + 18f, textPaint(6f, AndroidColor.rgb(100, 116, 139)))
                }

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
                currentY += 22f
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
        canvas.drawText("8. VALIDATION", MARGIN, currentY, sectionPaint)
        currentY += 12f
        canvas.drawText(
            "Document établi à partir des écritures saisies dans l'application de gestion de l'église.",
            MARGIN,
            currentY,
            regularPaint
        )
        currentY += 14f
        canvas.drawText(
            "Montants exprimés en $currencyCode, arrêtés au ${dateFormat.format(Date(reportEnd))} " +
                "(base : comptabilité de trésorerie).",
            MARGIN,
            currentY,
            regularPaint
        )
        currentY += 12f
        canvas.drawText(
            "Document $docRef • Période $periodRangeLabel • Saisie : $issuedBy • " +
                "${transactions.size} écriture(s) rapprochée(s) du journal • " +
                "${bilan.cancelledCount} annulée(s) par contre-écriture.",
            MARGIN,
            currentY,
            smallPaint
        )
        currentY += 24f

        val signatureWidth = (PAGE_WIDTH - 2 * MARGIN - 20f) / 3f
        val labels = listOf(
            profile.treasurerName.ifBlank { "………………………" } to "Trésorier (saisie)",
            profile.pastorName.ifBlank { "………………………" } to "Responsable (validation)",
            "………………………" to "Contrôle / Audit (visa)"
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

    /** Rapport PDF autonome, volontairement distinct du bilan financier complet. */
    fun generateTablePdf(
        context: Context,
        profile: ChurchProfileEntity,
        title: String,
        periodLabel: String,
        columns: List<String>,
        rows: List<List<String>>,
        workspaceId: String = "default",
        generatedBy: String = ""
    ): File {
        require(columns.isNotEmpty()) { "Le rapport doit avoir au moins une colonne" }
        val cacheDir = reportDirectory(context, workspaceId)
        val safeTitle = title.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "Rapport" }
        val file = File(cacheDir, "${safeTitle}_${System.currentTimeMillis()}.pdf")
        val accent = runCatching { AndroidColor.parseColor(profile.accentColorHex) }.getOrDefault(ACCENT_DEFAULT)
        val doc = PdfDocument()
        val margin = 34f
        val contentWidth = PAGE_WIDTH - margin * 2
        val rowHeight = 22f
        val pageCapacity = 29
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        val titlePaint = textPaint(17f, AndroidColor.rgb(17, 24, 39), bold = true)
        val subtitlePaint = textPaint(9f, AndroidColor.rgb(82, 89, 102))
        val headerPaint = textPaint(8f, AndroidColor.WHITE, bold = true)
        val cellPaint = textPaint(7.5f, AndroidColor.rgb(31, 41, 55))
        val linePaint = Paint().apply { color = AndroidColor.rgb(220, 224, 232); strokeWidth = 0.7f }
        val bandPaint = Paint().apply { color = accent; isAntiAlias = true }
        val alternatePaint = Paint().apply { color = AndroidColor.rgb(246, 248, 251); isAntiAlias = true }

        fun startPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = doc.startPage(info)
            val canvas = page!!.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, bandPaint)
            canvas.drawText(profile.displayName.take(50), margin, 30f, subtitlePaint)
            canvas.drawText(title.take(60), margin, 57f, titlePaint)
            canvas.drawText("$periodLabel · Édité le ${dateFormat.format(Date())} · ${rows.size} ligne(s)", margin, 74f, subtitlePaint)
            canvas.drawText("Établi par : ${generatedBy.ifBlank { "Non identifié" }}", margin, 88f, subtitlePaint)
            y = 108f
            val colWidth = contentWidth / columns.size
            canvas.drawRoundRect(margin, y, PAGE_WIDTH - margin, y + rowHeight, 4f, 4f, bandPaint)
            columns.forEachIndexed { index, value ->
                canvas.drawText(value.take(18), margin + index * colWidth + 6f, y + 14f, headerPaint)
            }
            y += rowHeight
        }

        fun finishPage() {
            val current = page ?: return
            val canvas = current.canvas
            canvas.drawLine(margin, PAGE_HEIGHT - 35f, PAGE_WIDTH - margin, PAGE_HEIGHT - 35f, linePaint)
            val footerPaint = textPaint(7f, AndroidColor.rgb(115, 122, 135))
            canvas.drawText("${profile.displayName} · Page $pageNumber", margin, PAGE_HEIGHT - 20f, footerPaint)
            doc.finishPage(current)
            page = null
        }

        // Local functions can reference one another; initialize the first page after declarations.
        startPage()
        if (rows.isEmpty()) {
            page!!.canvas.drawText("Aucune donnée pour cette période.", margin, y + 18f, cellPaint)
        } else {
            val colWidth = contentWidth / columns.size
            rows.forEachIndexed { rowIndex, values ->
                if (rowIndex > 0 && rowIndex % pageCapacity == 0) {
                    finishPage()
                    startPage()
                }
                val canvas = page!!.canvas
                if (rowIndex % 2 == 1) canvas.drawRect(margin, y, PAGE_WIDTH - margin, y + rowHeight, alternatePaint)
                repeat(columns.size) { columnIndex ->
                    val value = values.getOrElse(columnIndex) { "" }
                    val maxChars = (colWidth / 4.2f).toInt().coerceAtLeast(8)
                    canvas.drawText(value.replace('\n', ' ').take(maxChars), margin + columnIndex * colWidth + 6f, y + 14f, cellPaint)
                }
                canvas.drawLine(margin, y + rowHeight, PAGE_WIDTH - margin, y + rowHeight, linePaint)
                y += rowHeight
            }
        }
        finishPage()
        try {
            FileOutputStream(file).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return file
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
        profile: ChurchProfileEntity = ChurchProfileEntity(),
        workspaceId: String = "default",
        generatedBy: String = ""
    ): File {
        val cacheDir = reportDirectory(context, workspaceId)
        val csvFile = File(cacheDir, "Releve_${System.currentTimeMillis()}.csv")
        fun csvField(value: String): String {
            val singleLine = value.replace("\r", " ").replace("\n", " ")
            val first = singleLine.trimStart().firstOrNull()
            val protected = if (first == '=' || first == '+' || first == '-' || first == '@') "'$singleLine" else singleLine
            return "\"${protected.replace("\"", "\"\"")}\""
        }

        FileOutputStream(csvFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.append("# ${profile.displayName} — relevé des opérations\n")
                writer.append("# Période: ").append(periodLabel).append("\n")
                writer.append("# Édité le: ").append(fullDateTimeFormat.format(Date())).append("\n")
                writer.append("# Établi par: ").append(generatedBy.ifBlank { "Non identifié" }).append("\n")
                writer.append("# Devise: ").append(profile.currencyCode.ifBlank { "FCFA" }).append("\n\n")

                writer.append("Date;Heure;Caisse;Nature;Catégorie;Intitulé;Montant;Virement interne;Justificatif;Note;Modifié par\n")

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
                    writer.append(
                        listOf(
                            csvField(dateStr),
                            csvField(timeStr),
                            csvField(tx.cashRegisterName),
                            csvField(nature),
                            csvField(tx.category),
                            csvField(tx.title),
                            amountRaw.toString(), // Montant numérique, pas une formule de texte
                            csvField(if (tx.isTransfer) "OUI" else "NON"),
                            csvField(if (tx.hasReceipt) "OUI" else "NON"),
                            csvField(tx.note),
                            csvField(tx.updatedBy.ifBlank { "—" })
                        ).joinToString(";") + "\n"
                    )
                }

                writer.append("\n# SYNTHESE DE LA PERIODE\n")
                writer.append("TOTAL PRODUITS;").append(totalIncomeCents.toString()).append('\n')
                writer.append("TOTAL CHARGES;").append(totalExpenseCents.toString()).append('\n')
                writer.append("RESULTAT;").append(netBalanceCents.toString()).append('\n')
            }
        }

        return csvFile
    }

    /** Génère un classeur OOXML .xlsx avec des cellules texte et numériques typées. */
    fun generateExcelExport(
        context: Context,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        netBalanceCents: Long,
        profile: ChurchProfileEntity = ChurchProfileEntity(),
        workspaceId: String = "default",
        generatedBy: String = ""
    ): File {
        val cacheDir = reportDirectory(context, workspaceId)
        val file = File(cacheDir, "Releve_${System.currentTimeMillis()}.xlsx")
        fun xml(value: String): String = value
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
        fun columnName(index: Int): String {
            var n = index + 1
            val result = StringBuilder()
            while (n > 0) {
                val remainder = (n - 1) % 26
                result.append(('A'.code + remainder).toChar())
                n = (n - 1) / 26
            }
            return result.reverse().toString()
        }
        fun textCell(ref: String, value: String): String =
            "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>"
        fun numberCell(ref: String, value: Long): String = "<c r=\"$ref\"><v>$value</v></c>"
        val rows = mutableListOf<List<String>>()
        rows += listOf(profile.displayName, "Période", periodLabel).mapIndexed { i, value -> textCell("${columnName(i)}1", value) }
        rows += listOf("Établi par", generatedBy.ifBlank { "Non identifié" }).mapIndexed { i, value -> textCell("${columnName(i)}2", value) }
        rows += listOf("Devise", profile.currencyCode.ifBlank { "FCFA" }).mapIndexed { i, value -> textCell("${columnName(i)}3", value) }
        val headers = listOf("Date", "Heure", "Caisse", "Nature", "Catégorie", "Intitulé", "Montant (centimes)", "Virement interne", "Justificatif", "Note", "Modifié par")
        rows += headers.mapIndexed { i, value -> textCell("${columnName(i)}4", value) }
        transactions.sortedBy { it.timestamp }.forEachIndexed { txIndex, tx ->
            val rowNumber = txIndex + 5
            val kind = when {
                tx.isTransfer -> "VIREMENT"
                tx.type == TransactionType.INCOME -> "ENTRÉE"
                else -> "SORTIE"
            }
            val signedAmount = if (tx.type == TransactionType.INCOME) tx.amountCents else -tx.amountCents
            val cells = listOf(
                textCell("A$rowNumber", dateFormat.format(Date(tx.timestamp))),
                textCell("B$rowNumber", timeFormat.format(Date(tx.timestamp))),
                textCell("C$rowNumber", tx.cashRegisterName), textCell("D$rowNumber", kind),
                textCell("E$rowNumber", tx.category), textCell("F$rowNumber", tx.title),
                numberCell("G$rowNumber", signedAmount), textCell("H$rowNumber", if (tx.isTransfer) "Oui" else "Non"),
                textCell("I$rowNumber", if (tx.hasReceipt) "Oui" else "Non"), textCell("J$rowNumber", tx.note),
                textCell("K$rowNumber", tx.updatedBy.ifBlank { "—" })
            )
            rows += cells
        }
        val summaryStart = rows.size + 1
        rows += listOf(textCell("A$summaryStart", "Synthèse"), textCell("B$summaryStart", "Entrées (centimes)"), numberCell("C$summaryStart", totalIncomeCents))
        rows += listOf(textCell("A${summaryStart + 1}", ""), textCell("B${summaryStart + 1}", "Sorties (centimes)"), numberCell("C${summaryStart + 1}", totalExpenseCents))
        rows += listOf(textCell("A${summaryStart + 2}", ""), textCell("B${summaryStart + 2}", "Solde net (centimes)"), numberCell("C${summaryStart + 2}", netBalanceCents))
        val sheetXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            rows.forEachIndexed { i, cells -> append("<row r=\"${i + 1}\">").append(cells.joinToString("")).append("</row>") }
            append("</sheetData></worksheet>")
        }
        val entries = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Opérations" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/worksheets/sheet1.xml" to sheetXml
        )
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
        return file
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

    private fun reportDirectory(context: Context, workspaceId: String): File {
        val root = File(context.cacheDir, "reports")
        if (workspaceId == "default") return root.apply { mkdirs() }
        require(workspaceId.matches(Regex("[A-Za-z0-9_-]+"))) { "Identifiant de dossier invalide" }
        return File(root, workspaceId).apply { mkdirs() }
    }

    private fun tableRow(label: String, value: String): String = "$label : $value"
}
