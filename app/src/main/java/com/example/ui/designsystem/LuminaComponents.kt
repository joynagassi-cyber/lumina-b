package com.example.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

/**
 * Carte Héro en tête d'écran : met en avant le chiffre clé de la page
 * avec variation/delta, sous-cartes imbriquées et esthétique soignée.
 * Repose sur LuminaCard avec coins 24px, dégradé bleu indigo électrique et ombre portée.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    eyebrow: String,
    primaryAmount: String,
    deltaText: String? = null,
    isDeltaPositive: Boolean = true,
    deltaDescription: String? = null,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = LuminaTokens.HeroGradientIndigo,
    spotShadowColor: Color = LuminaTokens.HeroSpotShadowIndigo,
    ambientShadowColor: Color = LuminaTokens.HeroAmbientShadow,
    trailingBadge: @Composable (() -> Unit)? = null,
    nestedContent: @Composable (ColumnScope.() -> Unit)? = null,
    testTag: String = "hero_card"
) {
    val isGradient = backgroundBrush != null

    LuminaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = LuminaTokens.ShapeHero,
        elevation = LuminaCardElevation.HERO,
        backgroundBrush = backgroundBrush,
        spotShadowColor = spotShadowColor,
        ambientShadowColor = ambientShadowColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Ligne d'en-tête (Eyebrow + TrailingBadge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isGradient) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                trailingBadge?.invoke()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Montant principal en typographie forte et compacte
            Text(
                text = primaryAmount,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = LuminaTokens.LetterSpacingCompressed
                ),
                color = if (isGradient) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // Delta / Tendance
            if (deltaText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGradient) {
                        Surface(
                            shape = LuminaTokens.ShapePill,
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDeltaPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = deltaText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        DeltaBadge(
                            text = deltaText,
                            isPositive = isDeltaPositive
                        )
                    }

                    if (deltaDescription != null) {
                        Spacer(modifier = Modifier.width(LuminaTokens.SpacingSmall))
                        Text(
                            text = deltaDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGradient) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Contenu imbriqué (sous-cartes, métriques, jauges)
            if (nestedContent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                nestedContent()
            }
        }
    }
}


/**
 * Tuile d'icône carrée à coins arrondis (IconTile).
 * Inspirée des listes de paramètres et écrans d'actifs.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = LuminaTokens.IconTileSize,
    shape: RoundedCornerShape = LuminaTokens.ShapeTile
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(LuminaTokens.IconSizeMedium)
        )
    }
}

/**
 * Pastille de variation / Delta (DeltaBadge) avec flèche et couleur sémantique.
 */
@Composable
fun DeltaBadge(
    text: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPositive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPositive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = LuminaTokens.ShapeBadge,
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Trio de statistiques alignées en colonnes (StatTrio).
 * Équivalent du trio "Mensuel / Cible / Rendement" ou "Entrées / Sorties / Solde".
 */
@Composable
fun StatTrio(
    modifier: Modifier = Modifier,
    item1Label: String,
    item1Value: String,
    item1Color: Color = MaterialTheme.colorScheme.onSurface,
    item2Label: String,
    item2Value: String,
    item2Color: Color = MaterialTheme.colorScheme.onSurface,
    item3Label: String,
    item3Value: String,
    item3Color: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    labelColor: Color? = null
) {
    val defaultBg = containerColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val defaultLabel = labelColor ?: MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LuminaTokens.ShapeSubCard,
        colors = CardDefaults.cardColors(containerColor = defaultBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Col 1
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item1Label,
                    style = MaterialTheme.typography.labelSmall,
                    color = defaultLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item1Value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = item1Color,
                    maxLines = 1
                )
            }

            // Col 2
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item2Label,
                    style = MaterialTheme.typography.labelSmall,
                    color = defaultLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item2Value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = item2Color,
                    maxLines = 1
                )
            }

            // Col 3
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item3Label,
                    style = MaterialTheme.typography.labelSmall,
                    color = defaultLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item3Value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = item3Color,
                    maxLines = 1
                )
            }
        }
    }
}


/**
 * Carte groupée pour organiser des listes de lignes (modèle One UI Settings).
 */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = LuminaTokens.LetterSpacingEyebrow,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = LuminaTokens.BorderWidthThin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = LuminaTokens.ShapeCard
                ).then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = LuminaTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = LuminaTokens.CardElevationNone)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LuminaTokens.SpacingDefault),
                content = content
            )
        }
    }
}

/**
 * Ligne de liste groupée (ListRow).
 * Modèle de l'écran Paramètres & Watchlist :
 * [IconTile] + [Titre & Sous-titre] + [Valeur / Chevron / Radio] + Inset divider optionnel.
 */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTileColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = LuminaTokens.SpacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                IconTile(
                    icon = icon,
                    contentDescription = null,
                    containerColor = iconTileColor,
                    iconTint = iconTint,
                    size = 38.dp,
                    shape = LuminaTokens.ShapeTile
                )
                Spacer(modifier = Modifier.width(LuminaTokens.SpacingMedium))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(LuminaTokens.SpacingSmall))
                trailingContent()
            }
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingSmall))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = if (icon != null) 50.dp else 0.dp)
            )
        }
    }
}

/**
 * Contrôle segmenté en pilule (SegmentedControl).
 * Idéal pour les sélecteurs de périodes (1J, 1S, 1M...) ou de types de flux.
 */
@Composable
fun <T> SegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = LuminaTokens.ShapePill,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val targetBg = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(150), label = "segment_bg")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(LuminaTokens.ShapePill)
                        .background(animatedBg)
                        .clickable { onItemSelected(item) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Bouton en pilule pleine largeur ou personnalisé (PillButton).
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    testTag: String? = null
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            shape = LuminaTokens.ShapePill,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = modifier
                .height(48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = LuminaTokens.ShapePill,
            modifier = modifier
                .height(48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/**
 * En-tête de section discret avec action secondaire alignée à droite.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LuminaTokens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = LuminaTokens.FontSectionTitle,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(LuminaTokens.ShapePill)
                    .clickable { onActionClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Mini-graphique sparkline vectoriel Canvas pour les cartes et lignes d'activité.
 */
@Composable
fun SparklineMiniChart(
    points: List<Float> = listOf(10f, 15f, 12f, 22f, 18f, 28f, 25f, 35f),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier.size(width = 56.dp, height = 24.dp)
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val maxVal = points.maxOrNull() ?: 1f
        val minVal = points.minOrNull() ?: 0f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        val path = Path()
        val stepX = size.width / (points.size - 1)

        points.forEachIndexed { index, value ->
            val normY = (value - minVal) / range
            val x = index * stepX
            val y = size.height - (normY * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
