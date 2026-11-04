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
    /**
     * Dégradé des cartes d'en-tête, calculé à partir de la couleur d'accent
     * choisie par l'église : éclairci en haut à gauche, couleur pleine au
     * centre, assombri en bas à droite. Chaque accent obtient ainsi un dégradé
     * cohérent et toujours lisible (texte blanc).
     */
    fun heroGradient(accent: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Brush =
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                androidx.compose.ui.graphics.lerp(accent, androidx.compose.ui.graphics.Color.White, 0.16f),
                accent,
                androidx.compose.ui.graphics.lerp(accent, androidx.compose.ui.graphics.Color.Black, 0.58f)
            )
        )

    fun heroSpotShadow(accent: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
        accent.copy(alpha = 0.42f)

    val HeroAmbientShadow = androidx.compose.ui.graphics.Color(0x44000000)
}

