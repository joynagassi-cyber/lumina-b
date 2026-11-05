package com.example.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Niveaux d'élévation par superposition tonale et ombrage de profondeur pour LuminaCard.
 */
enum class LuminaCardElevation(
    val shadowElevation: Dp,
    val tonalElevation: Dp
) {
    FLAT(shadowElevation = 0.dp, tonalElevation = 0.dp),
    LOW(shadowElevation = 2.dp, tonalElevation = 1.dp),
    MEDIUM(shadowElevation = 4.dp, tonalElevation = 2.dp),
    HIGH(shadowElevation = 8.dp, tonalElevation = 4.dp),
    HERO(shadowElevation = 10.dp, tonalElevation = 6.dp)
}

/**
 * Composant réutilisable 'LuminaCard' respectant le design system :
 * - Coins arrondis stricts à 24px (24dp)
 * - Gestion fluide de l'élévation par superposition tonale et ombrage de profondeur
 * - Support direct des dégradés (Brush) et couleurs personnalisées
 * - Support optionnel du clic avec retour tactile (ripple)
 */
@Composable
fun LuminaCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: LuminaCardElevation = LuminaCardElevation.LOW,
    containerColor: Color? = null,
    backgroundBrush: Brush? = null,
    contentColor: Color? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    spotShadowColor: Color = Color.Black.copy(alpha = 0.45f),
    ambientShadowColor: Color = Color.Black.copy(alpha = 0.25f),
    content: @Composable ColumnScope.() -> Unit
) {
    val isGlass = MaterialTheme.colorScheme.background.alpha == 0f
    val defaultContainerColor = containerColor ?: MaterialTheme.colorScheme.surface
    val glassBorder = BorderStroke(
        width = LuminaTokens.BorderWidthThin,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.48f)
            )
        )
    )
    val defaultBorder = border ?: when {
        isGlass -> glassBorder
        backgroundBrush == null -> BorderStroke(
            width = LuminaTokens.BorderWidthThin,
            color = MaterialTheme.colorScheme.outline
        )
        else -> null
    }
    val resolvedSpotShadow = if (isGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else spotShadowColor
    val resolvedAmbientShadow = if (isGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else ambientShadowColor
    val glassSheen = if (isGlass) {
        Modifier.drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
                    startY = 0f,
                    endY = 34.dp.toPx()
                )
            )
        }
    } else Modifier

    // Base surface avec ombrage de profondeur configurable
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation.shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = resolvedAmbientShadow,
                spotColor = resolvedSpotShadow
            ),
        shape = shape,
        color = if (backgroundBrush != null) Color.Transparent else defaultContainerColor,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
        tonalElevation = elevation.tonalElevation,
        border = defaultBorder
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else Modifier

        val backgroundModifier = if (backgroundBrush != null) {
            Modifier.background(backgroundBrush)
        } else Modifier

        Column(
            modifier = Modifier
                .clip(shape)
                .then(backgroundModifier)
                .then(clickModifier)
                .then(glassSheen),
            content = content
        )
    }
}
