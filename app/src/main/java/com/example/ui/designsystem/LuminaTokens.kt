package com.example.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de design centralisés pour l'application Lumina.
 * Basés sur la hiérarchie des surfaces, grands rayons de courbure (20-28dp),
 * espacements généreux et zones tactiles de 48dp minimum.
 */
object LuminaTokens {
    // ==========================================
    // Rayons de courbure (Corners)
    // ==========================================
    val CornerHero = 24.dp
    val CornerCard = 22.dp
    val CornerSubCard = 16.dp
    val CornerTile = 14.dp
    val CornerChip = 10.dp
    val CornerBadge = 8.dp
    val CornerPill = 999.dp

    val ShapeHero = RoundedCornerShape(CornerHero)
    val ShapeCard = RoundedCornerShape(CornerCard)
    val ShapeSubCard = RoundedCornerShape(CornerSubCard)
    val ShapeTile = RoundedCornerShape(CornerTile)
    val ShapeChip = RoundedCornerShape(CornerChip)
    val ShapeBadge = RoundedCornerShape(CornerBadge)
    val ShapePill = RoundedCornerShape(CornerPill)

    // ==========================================
    // Espacements (Spacing & Padding)
    // ==========================================
    val SpacingMicro = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingDefault = 16.dp
    val SpacingLarge = 20.dp
    val SpacingXLarge = 24.dp
    val SpacingSection = 28.dp

    // ==========================================
    // Éléments d'interaction & Touch Targets
    // ==========================================
    val MinTouchTarget = 48.dp
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 20.dp
    val IconSizeLarge = 24.dp
    val IconTileSize = 42.dp

    // ==========================================
    // Bordures subtiles & Élévations
    // ==========================================
    val BorderWidthThin = 1.dp
    val BorderWidthActive = 1.5.dp
    val CardElevationNone = 0.dp
    val CardElevationSubtle = 1.dp
    val CardElevationRaised = 3.dp

    // ==========================================
    // Typographie des Nombres & Montants Clés
    // ==========================================
    val FontHeroAmount = 32.sp
    val FontHeroSubAmount = 20.sp
    val FontSectionTitle = 18.sp
    val LetterSpacingCompressed = (-0.5).sp
    val LetterSpacingEyebrow = 1.2.sp

    // ==========================================
    // Dégradés & Ombres Héro (Bleu Indigo Électrique)
    // ==========================================
    val HeroGradientIndigo = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF3730A3), // Indigo intense
            androidx.compose.ui.graphics.Color(0xFF4338CA), // Indigo électrique
            androidx.compose.ui.graphics.Color(0xFF1E1B4B)  // Bleu nuit profond
        )
    )
    val HeroSpotShadowIndigo = androidx.compose.ui.graphics.Color(0x663730A3)
    val HeroAmbientShadow = androidx.compose.ui.graphics.Color(0x44000000)
}

