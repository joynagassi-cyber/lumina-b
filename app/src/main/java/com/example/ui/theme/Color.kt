package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Accent Unique : Bleu Sombre Indigo
val IndigoPrimaryLight = Color(0xFF3730A3) // Indigo profond
val IndigoPrimaryDark = Color(0xFF6366F1)  // Indigo éclatant pour contraste sur OLED
val IndigoContainerLight = Color(0xFFEEF2FF)
val IndigoContainerDark = Color(0xFF1E1B4B)
val IndigoOnContainerLight = Color(0xFF1E1B4B)
val IndigoOnContainerDark = Color(0xFFC7D2FE)

// Thème Monochrome OLED Pure (Noir absolu #000000)
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0C0C0E)
val OledSurfaceVariant = Color(0xFF18181B)
val OledBorder = Color(0xFF27272A)
val OledOnSurface = Color(0xFFFAFAFA)
val OledOnSurfaceVariant = Color(0xFFA1A1AA)

// Thème clair demandé : blanc pur et net, avec des nuances très légères
// uniquement pour distinguer les cartes, champs et séparateurs.
val PureLightBackground = Color(0xFFFFFFFF)
val PureLightSurface = Color(0xFFFFFFFF)
val PureLightSurfaceVariant = Color(0xFFF4F6FA)
val PureLightBorder = Color(0xFFE5E9F0)
val PureLightOnSurface = Color(0xFF17191F)
val PureLightOnSurfaceVariant = Color(0xFF626875)

// Couleurs fonctionnelles minimalistes monochromes / indigo
val IncomeIndigo = Color(0xFF4338CA)
val IncomeIndigoDark = Color(0xFF818CF8)
val ExpenseMuted = Color(0xFF71717A)
val ExpenseMutedDark = Color(0xFFA1A1AA)

// Couleurs sémantiques : le vert signale une entrée / une hausse,
// le rouge une sortie / une baisse. Elles ne dépendent pas de l'accent choisi.
val IncomeGreen = Color(0xFF15803D)      // Vert forêt (texte et traits)
val IncomeGreenLight = Color(0xFF22C55E) // Vert clair (barres, dégradés)
val ExpenseRed = Color(0xFFB91C1C)       // Rouge profond (texte et traits)
val ExpenseRedLight = Color(0xFFEF4444)  // Rouge clair (barres, dégradés)
val LuminaGold = Color(0xFFB45309)       // Ambre (alertes douces)
val ExpenseBgLight = Color(0xFFFEF2F2)
val ExpenseBgDark = Color(0xFF2A1215)
val IncomeBgLight = Color(0xFFF0FDF4)
val IncomeBgDark = Color(0xFF0F2417)
