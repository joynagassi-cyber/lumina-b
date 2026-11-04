package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import java.util.Locale

// Palettes sémantiques : rouge pour les sorties, vert pour les entrées.
private val ExpensePalette = listOf(
    Color(0xFFB91C1C), // Rouge profond
    Color(0xFFDC2626), // Rouge
    Color(0xFFEF4444), // Rouge clair
    Color(0xFFF87171), // Rouge pastel
    Color(0xFF7F1D1D), // Bordeaux sombre
    Color(0xFFFDA4AF), // Rose
    Color(0xFF991B1B), // Rouge brun
    Color(0xFFFCA5A5)  // Rouge très clair
)

private val IncomePalette = listOf(
    Color(0xFF15803D), // Vert profond
    Color(0xFF16A34A), // Vert
    Color(0xFF22C55E), // Vert clair
    Color(0xFF4ADE80), // Vert pastel
    Color(0xFF065F46), // Émeraude sombre
    Color(0xFF86EFAC), // Vert très clair
    Color(0xFF047857), // Émeraude
    Color(0xFFA7F3D0)  // Menthe
)

private fun paletteFor(isExpense: Boolean) = if (isExpense) ExpensePalette else IncomePalette

@Composable
fun CategoryDistributionCard(
    categoryTotals: Map<String, Long>,
    title: String,
    displayMode: AmountDisplayMode = AmountDisplayMode.STANDARD,
    isExpense: Boolean = true,
    modifier: Modifier = Modifier
) {
    val totalCents = categoryTotals.values.sum()
    val palette = paletteFor(isExpense)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = com.example.ui.designsystem.LuminaTokens.BorderWidthThin,
                color = MaterialTheme.colorScheme.outline,
                shape = com.example.ui.designsystem.LuminaTokens.ShapeCard
            )
            .testTag("category_distribution_card"),
        shape = com.example.ui.designsystem.LuminaTokens.ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = com.example.ui.designsystem.LuminaTokens.CardElevationNone)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryTotals.isEmpty() || totalCents == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune opération sur cette période",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryDonutChart(
                            categories = categoryTotals,
                            totalCents = totalCents,
                            palette = palette
                        )
                        Text(
                            text = "${categoryTotals.size} cat.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Liste des principales catégories
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryTotals.entries.take(4).forEachIndexed { index, entry ->
                            val color = palette[index % palette.size]
                            val percent = (entry.value.toDouble() / totalCents.toDouble()) * 100.0
                            val amountStr = CurrencyFormatter.format(entry.value, displayMode)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.key,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = amountStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { (percent / 100.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categories: Map<String, Long>,
    totalCents: Long,
    palette: List<Color> = ExpensePalette,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Canvas(modifier = modifier.size(90.dp)) {
        val strokeWidth = 14.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        categories.entries.forEachIndexed { index, entry ->
            val slicePercent = (entry.value.toDouble() / totalCents.toDouble()).toFloat()
            val sweepAngle = slicePercent * 360f * animatedProgress.value
            val color = palette[index % palette.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // léger espacement entre parts
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            startAngle += sweepAngle
        }
    }
}
