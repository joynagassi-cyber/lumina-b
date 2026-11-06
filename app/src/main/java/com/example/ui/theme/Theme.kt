package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.data.ChurchProfileEntity

enum class ThemeMode(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair épuré"),
    DARK("Sombre OLED"),
    GLASS_LIGHT("Glass clair"),
    GLASS_DARK("Glass sombre")
}

private val ThemeMode.isGlass: Boolean
    get() = this == ThemeMode.GLASS_LIGHT || this == ThemeMode.GLASS_DARK

private val ThemeMode.isDarkAppearance: Boolean
    get() = this == ThemeMode.DARK || this == ThemeMode.GLASS_DARK

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
    AccentPalette("ardoise", "Ardoise neutre", "#334155"),
    AccentPalette("corail", "Corail terracotta", "#B9382E"),
    AccentPalette("lagon", "Turquoise lagon", "#0F766E"),
    AccentPalette("rose", "Rose profond", "#BE185D"),
    AccentPalette("foret", "Vert forêt", "#166534"),
    AccentPalette("cobalt", "Cobalt", "#1D4ED8"),
    AccentPalette("prune", "Prune", "#7E22CE")
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

/** Palette Glass : panneaux translucides, traits lumineux et contraste doux. */
private fun glassLightSchemeWith(accent: Color) = lightColorScheme(
    primary = accent.copy(alpha = 0.88f),
    onPrimary = accent.onColor(),
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = lerp(Color(0xFF111827), accent, 0.55f),
    secondary = lerp(accent, Color(0xFF286C83), 0.35f),
    onSecondary = Color.White,
    secondaryContainer = Color(0x887E9DB7),
    onSecondaryContainer = Color(0xFF18283A),
    tertiary = Color(0xFF7658B5),
    background = Color.Transparent,
    onBackground = Color(0xFF182235),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF182235),
    surfaceVariant = Color(0xA6FFFFFF),
    onSurfaceVariant = Color(0xFF4A5A70),
    outline = Color(0x66788AA1),
    outlineVariant = Color(0x4D788AA1),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xE6FFFFFF),
    surfaceContainerLow = Color(0xCCFFFFFF),
    surfaceContainer = Color(0xB8FFFFFF),
    surfaceContainerHigh = Color(0x99FFFFFF),
    surfaceContainerHighest = Color(0x887F96AF)
)

private fun glassDarkSchemeWith(accent: Color) = darkColorScheme(
    primary = lerp(accent, Color.White, 0.35f).copy(alpha = 0.92f),
    onPrimary = Color(0xFF10131B),
    primaryContainer = accent.copy(alpha = 0.26f),
    onPrimaryContainer = Color(0xFFF3F6FF),
    secondary = Color(0xFF9ABBE0),
    onSecondary = Color(0xFF111622),
    secondaryContainer = Color(0x704B6383),
    onSecondaryContainer = Color(0xFFE2ECFA),
    tertiary = Color(0xFFC3A6FF),
    background = Color.Transparent,
    onBackground = Color(0xFFF5F7FC),
    surface = Color(0xCC111722),
    onSurface = Color(0xFFF5F7FC),
    surfaceVariant = Color(0xA01B2636),
    onSurfaceVariant = Color(0xFFD0D8E5),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x40FFFFFF),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xD910151F),
    surfaceContainerLow = Color(0xCC141C29),
    surfaceContainer = Color(0xB81B2636),
    surfaceContainerHigh = Color(0xA6273447),
    surfaceContainerHighest = Color(0x90344056)
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
        ThemeMode.LIGHT, ThemeMode.GLASS_LIGHT -> false
        ThemeMode.DARK, ThemeMode.GLASS_DARK -> true
    }
    val accent = parseHexColor(accentHex)
    val colorScheme = when (themeMode) {
        ThemeMode.GLASS_LIGHT -> glassLightSchemeWith(accent)
        ThemeMode.GLASS_DARK -> glassDarkSchemeWith(accent)
        else -> if (isDark) darkSchemeWith(accent) else lightSchemeWith(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        if (themeMode.isGlass) {
            GlassBackdrop(isDark = themeMode.isDarkAppearance, accent = colorScheme.primary, content = content)
        } else {
            content()
        }
    }
}

/**
 * Fonds chromatiques sous les panneaux translucides. Les halos sont des dégradés
 * radiaux plutôt qu'un blur de couche : le Modifier.blur standard floute le contenu
 * du composable, pas son arrière-plan, et n'est pas pris en charge avant Android 12.
 */
@Composable
private fun GlassBackdrop(
    isDark: Boolean,
    accent: Color,
    content: @Composable () -> Unit
) {
    val baseColors = if (isDark) {
        listOf(Color(0xFF080B12), Color(0xFF111928), Color(0xFF171225))
    } else {
        listOf(Color(0xFFDDEBFA), Color(0xFFF0EAFE), Color(0xFFE2F2EF))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(baseColors))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 112.dp, y = (-58).dp)
                .size(310.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isDark) 0.40f else 0.28f),
                            accent.copy(alpha = if (isDark) 0.12f else 0.08f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-92).dp, y = 64.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isDark) Color(0xFF267F96) else Color(0xFF64B7C4)).copy(alpha = 0.34f),
                            (if (isDark) Color(0xFF267F96) else Color(0xFF64B7C4)).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        content()
    }
}
