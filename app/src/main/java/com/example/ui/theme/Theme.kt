package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.data.ChurchProfileEntity

enum class ThemeMode(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair"),
    DARK("Sombre OLED")
}

/**
 * Palettes d'accent proposées à la création du profil de l'église.
 * La première est la palette par défaut (bleu indigo).
 */
data class AccentPalette(
    val key: String,
    val label: String,
    val hex: String
)

val AccentPalettes: List<AccentPalette> = listOf(
    AccentPalette("indigo", "Bleu indigo (défaut)", "#3730A3"),
    AccentPalette("ocean", "Bleu océan", "#0E7490"),
    AccentPalette("emeraude", "Vert émeraude", "#047857"),
    AccentPalette("pourpre", "Violet pourpre", "#6D28D9"),
    AccentPalette("bordeaux", "Bordeaux", "#9F1239"),
    AccentPalette("ambre", "Ambre doré", "#B45309"),
    AccentPalette("ardoise", "Ardoise neutre", "#334155")
)

fun parseHexColor(hex: String, fallback: Color = IndigoPrimaryLight): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

private fun Color.onColor(): Color {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return if (luminance > 0.6f) Color(0xFF111827) else Color.White
}

/** Schéma clair construit autour de la couleur d'accent choisie. */
private fun lightSchemeWith(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = accent.onColor(),
    primaryContainer = lerp(Color.White, accent, 0.14f),
    onPrimaryContainer = lerp(Color.Black, accent, 0.65f),
    secondary = accent,
    onSecondary = accent.onColor(),
    secondaryContainer = lerp(Color.White, accent, 0.10f),
    onSecondaryContainer = lerp(Color.Black, accent, 0.6f),
    tertiary = Color(0xFF71717A),
    background = PureLightBackground,
    onBackground = PureLightOnSurface,
    surface = PureLightSurface,
    onSurface = PureLightOnSurface,
    surfaceVariant = PureLightSurfaceVariant,
    onSurfaceVariant = PureLightOnSurfaceVariant,
    outline = PureLightBorder,
    outlineVariant = Color(0xFFE4E4E7)
)

/** Schéma sombre OLED construit autour de la couleur d'accent choisie. */
private fun darkSchemeWith(accent: Color) = darkColorScheme(
    primary = lerp(accent, Color.White, 0.35f),
    onPrimary = Color(0xFF0B0B0F),
    primaryContainer = lerp(OledSurface, accent, 0.42f),
    onPrimaryContainer = lerp(Color.White, accent, 0.25f),
    secondary = lerp(accent, Color.White, 0.35f),
    onSecondary = Color(0xFF0B0B0F),
    secondaryContainer = lerp(OledSurface, accent, 0.35f),
    onSecondaryContainer = lerp(Color.White, accent, 0.3f),
    tertiary = Color(0xFFA1A1AA),
    background = OledBackground,
    onBackground = OledOnSurface,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledBorder,
    outlineVariant = Color(0xFF1F1F23)
)

@Composable
fun LuminaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentHex: String = ChurchProfileEntity.DEFAULT_ACCENT,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accent = parseHexColor(accentHex)
    val colorScheme = if (isDark) darkSchemeWith(accent) else lightSchemeWith(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
