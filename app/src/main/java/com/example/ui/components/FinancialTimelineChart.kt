package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Mode d'affichage du graphique interactif de timeline
 */
enum class TimelineChartMode {
    CUMULATIVE_LINE, // Courbe d'évolution continue de trésorerie
    DAILY_FLOW_BARS  // Barres de flux quotidiens (Entrées vs Sorties)
}

/**
 * Point de données agrégé par jour pour la visualisation
 */
data class DailyTimelinePoint(
    val timestamp: Long,
    val dateLabel: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netDayCents: Long,
    val cumulativeBalanceCents: Long
)

/**
 * Composant de visualisation de données interactif pour la timeline financière.
 * Prend en charge l'inspection tactile au glisser/toucher, le basculement courbe / barres de flux,
 * les repères d'échelle déterministes et les styles fluides Material 3.
 */
@Composable
fun FinancialTimelineChart(
    transactions: List<TransactionEntity>,
    title: String = "Évolution & Dynamique des Flux",
    subtitle: String = "Inspection interactive des mouvements de trésorerie",
    displayMode: AmountDisplayMode = AmountDisplayMode.STANDARD,
    modifier: Modifier = Modifier,
    testTag: String = "financial_timeline_chart"
) {
    var chartMode by remember { mutableStateOf(TimelineChartMode.CUMULATIVE_LINE) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val dayFormat = remember { SimpleDateFormat("dd MMM", Locale.FRANCE) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE) }

    // Préparer les points de données chronologiques triés
    val timelinePoints = remember(transactions) {
        if (transactions.isEmpty()) {
            emptyList()
        } else {
            val sorted = transactions.sortedBy { it.timestamp }
            val cal = Calendar.getInstance()

            // Agréger par jour (clé "yyyyMMdd")
            val dayGroups = sorted.groupBy { tx ->
                val d = Date(tx.timestamp)
                SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(d)
            }

            var runningBalance = 0L
            val result = mutableListOf<DailyTimelinePoint>()

            dayGroups.forEach { (_, txs) ->
                val firstTime = txs.first().timestamp
                val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
                val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
                val dayNet = dayIncome - dayExpense
                runningBalance += dayNet

                result.add(
                    DailyTimelinePoint(
                        timestamp = firstTime,
                        dateLabel = dayFormat.format(Date(firstTime)),
                        incomeCents = dayIncome,
                        expenseCents = dayExpense,
                        netDayCents = dayNet,
                        cumulativeBalanceCents = runningBalance
                    )
                )
            }
            result
        }
    }

    // Réinitialiser le point sélectionné quand les données changent
    LaunchedEffect(timelinePoints.size) {
        if (timelinePoints.isNotEmpty()) {
            selectedIndex = timelinePoints.lastIndex
        } else {
            selectedIndex = -1
        }
    }

    // Animation d'entrée
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(timelinePoints.size, chartMode) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = LuminaTokens.BorderWidthThin,
                color = MaterialTheme.colorScheme.outline,
                shape = LuminaTokens.ShapeCard
            )
            .testTag(testTag),
        shape = LuminaTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LuminaTokens.CardElevationNone)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // En-tête avec titre et sélecteur de mode (Courbe / Barres)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bascule de mode Courbe / Barres
                Row(
                    modifier = Modifier
                        .clip(LuminaTokens.ShapePill)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { chartMode = TimelineChartMode.CUMULATIVE_LINE },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (chartMode == TimelineChartMode.CUMULATIVE_LINE)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else
                                    Color.Transparent
                            )
                            .testTag("chart_mode_line")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Mode Courbe",
                            modifier = Modifier.size(16.dp),
                            tint = if (chartMode == TimelineChartMode.CUMULATIVE_LINE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { chartMode = TimelineChartMode.DAILY_FLOW_BARS },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (chartMode == TimelineChartMode.DAILY_FLOW_BARS)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else
                                    Color.Transparent
                            )
                            .testTag("chart_mode_bars")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Mode Barres",
                            modifier = Modifier.size(16.dp),
                            tint = if (chartMode == TimelineChartMode.DAILY_FLOW_BARS)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (timelinePoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune opération sur cette période pour tracer le graphique",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val activePoint = if (selectedIndex in timelinePoints.indices) {
                    timelinePoints[selectedIndex]
                } else {
                    timelinePoints.last()
                }

                // Bandeau d'inspection interactif (détail du point sélectionné)
                Surface(
                    shape = LuminaTokens.ShapeSubCard,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = fullDateFormat.format(Date(activePoint.timestamp)).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (chartMode == TimelineChartMode.CUMULATIVE_LINE) {
                                    "Trésorerie : ${CurrencyFormatter.format(activePoint.cumulativeBalanceCents, displayMode, withSign = true, isPositive = activePoint.cumulativeBalanceCents >= 0)}"
                                } else {
                                    "Flux net : ${CurrencyFormatter.format(activePoint.netDayCents, displayMode, withSign = true, isPositive = activePoint.netDayCents >= 0)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activePoint.cumulativeBalanceCents >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Entrée du jour
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Entrées", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(
                                    CurrencyFormatter.format(activePoint.incomeCents, displayMode),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            // Sortie du jour
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Sorties", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(
                                    CurrencyFormatter.format(activePoint.expenseCents, displayMode),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zone de dessin Canvas interactive
                // Sens de la courbe : vert quand la trésorerie monte, rouge quand elle descend.
                val cumulatives = timelinePoints.map { it.cumulativeBalanceCents }
                val isRising = (cumulatives.lastOrNull() ?: 0L) >= (cumulatives.firstOrNull() ?: 0L)
                val lineColor = when {
                    chartMode != TimelineChartMode.CUMULATIVE_LINE -> MaterialTheme.colorScheme.primary
                    isRising -> IncomeGreen
                    else -> ExpenseRed
                }
                val outlineColor = MaterialTheme.colorScheme.outline
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(timelinePoints) {
                                detectTapGestures { offset ->
                                    val count = timelinePoints.size
                                    if (count > 0) {
                                        val stepX = size.width / (count.coerceAtLeast(1))
                                        val idx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                }
                            }
                            .pointerInput(timelinePoints) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val count = timelinePoints.size
                                    if (count > 0) {
                                        val stepX = size.width / (count.coerceAtLeast(1))
                                        val idx = (change.position.x / stepX).toInt().coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val count = timelinePoints.size

                        if (count == 0) return@Canvas

                        // Dessiner 3 repères horizontaux fins (haut, milieu, bas)
                        val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(0f, height * 0.15f),
                            end = Offset(width, height * 0.15f),
                            strokeWidth = 1f,
                            pathEffect = gridPathEffect
                        )
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(0f, height * 0.5f),
                            end = Offset(width, height * 0.5f),
                            strokeWidth = 1f,
                            pathEffect = gridPathEffect
                        )
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(0f, height * 0.85f),
                            end = Offset(width, height * 0.85f),
                            strokeWidth = 1f,
                            pathEffect = gridPathEffect
                        )

                        if (chartMode == TimelineChartMode.CUMULATIVE_LINE) {
                            // MODE 1 : COURBE CUMULÉE
                            val values = timelinePoints.map { it.cumulativeBalanceCents.toFloat() }
                            val minVal = values.minOrNull() ?: 0f
                            val maxVal = values.maxOrNull() ?: 1f
                            val range = if (maxVal == minVal) 1f else (maxVal - minVal)

                            val stepX = if (count > 1) width / (count - 1) else width
                            val path = Path()
                            val fillPath = Path()

                            val points = values.mapIndexed { idx, v ->
                                val x = if (count > 1) idx * stepX else width / 2
                                val norm = (v - minVal) / range
                                val animatedNorm = norm * animatedProgress.value
                                val y = height * 0.85f - (animatedNorm * (height * 0.70f))
                                Offset(x, y)
                            }

                            if (points.isNotEmpty()) {
                                path.moveTo(points.first().x, points.first().y)
                                fillPath.moveTo(points.first().x, height)
                                fillPath.lineTo(points.first().x, points.first().y)

                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val controlX = (p0.x + p1.x) / 2f
                                    path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                                }

                                fillPath.lineTo(points.last().x, height)
                                fillPath.close()

                                // Remplissage en dégradé sous la courbe
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            lineColor.copy(alpha = 0.28f * animatedProgress.value),
                                            lineColor.copy(alpha = 0.02f)
                                        ),
                                        startY = 0f,
                                        endY = height
                                    )
                                )

                                // Tracé de la courbe
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Marquer le point sélectionné avec halo
                                val selIdx = if (selectedIndex in points.indices) selectedIndex else points.lastIndex
                                val activePointOffset = points[selIdx]

                                // Ligne verticale guide
                                drawLine(
                                    color = lineColor.copy(alpha = 0.45f),
                                    start = Offset(activePointOffset.x, 0f),
                                    end = Offset(activePointOffset.x, height),
                                    strokeWidth = 1.5f,
                                    pathEffect = gridPathEffect
                                )

                                // Halo externe
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.25f),
                                    radius = 11.dp.toPx(),
                                    center = activePointOffset
                                )
                                // Cercle principal
                                drawCircle(
                                    color = lineColor,
                                    radius = 5.dp.toPx(),
                                    center = activePointOffset
                                )
                                // Point central blanc
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = activePointOffset
                                )
                            }
                        } else {
                            // MODE 2 : BARRES DE FLUX (ENTRÉES VS SORTIES)
                            val maxFlow = timelinePoints.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents).toFloat() } ?: 1f
                            val safeMax = if (maxFlow <= 0f) 1f else maxFlow

                            val totalSlots = count
                            val slotWidth = width / totalSlots
                            val barWidth = (slotWidth * 0.32f).coerceAtLeast(3f)

                            timelinePoints.forEachIndexed { idx, point ->
                                val slotCenter = (idx * slotWidth) + (slotWidth / 2f)
                                val incNorm = (point.incomeCents / safeMax) * animatedProgress.value
                                val expNorm = (point.expenseCents / safeMax) * animatedProgress.value

                                val incHeight = incNorm * (height * 0.70f)
                                val expHeight = expNorm * (height * 0.70f)

                                val isSel = (idx == selectedIndex)

                                // Barre Entrée (Verte)
                                if (point.incomeCents > 0) {
                                    val leftX = slotCenter - barWidth - 1.dp.toPx()
                                    val topY = height * 0.85f - incHeight
                                    drawRoundRect(
                                        color = if (isSel) IncomeGreen else IncomeGreen.copy(alpha = 0.75f),
                                        topLeft = Offset(leftX, topY),
                                        size = Size(barWidth, incHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                                    )
                                }

                                // Barre Sortie (Rouge)
                                if (point.expenseCents > 0) {
                                    val leftX = slotCenter + 1.dp.toPx()
                                    val topY = height * 0.85f - expHeight
                                    drawRoundRect(
                                        color = if (isSel) ExpenseRed else ExpenseRed.copy(alpha = 0.75f),
                                        topLeft = Offset(leftX, topY),
                                        size = Size(barWidth, expHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                                    )
                                }

                                // Indicateur de sélection
                                if (isSel) {
                                    drawLine(
                                        color = lineColor.copy(alpha = 0.40f),
                                        start = Offset(slotCenter, 0f),
                                        end = Offset(slotCenter, height),
                                        strokeWidth = 1f,
                                        pathEffect = gridPathEffect
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dates clés en bas du graphique
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = timelinePoints.first().dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    if (timelinePoints.size > 2) {
                        Text(
                            text = timelinePoints[timelinePoints.size / 2].dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = timelinePoints.last().dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
