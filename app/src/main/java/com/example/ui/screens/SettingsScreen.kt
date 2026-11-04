package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinancialUiState
import com.example.ui.designsystem.GroupedCard
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.ListRow
import com.example.ui.designsystem.LuminaCard
import com.example.ui.designsystem.LuminaCardElevation
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.StatTrio
import com.example.ui.theme.ThemeMode
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(
    uiState: FinancialUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmountDisplayModeChange: (AmountDisplayMode) -> Unit,
    onNavigateToCaisses: () -> Unit = {},
    onNavigateToCategories: () -> Unit,
    onNavigateToExport: () -> Unit,
    onEditChurchProfile: () -> Unit = {},
    onWipeData: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // null = pas de confirmation en cours ; false = vider le dossier ; true = tout effacer
    var confirmWipe: Boolean? by remember { mutableStateOf(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("settings_screen")
    ) {
        // En-tête
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Personnalisation et confidentialité",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Carte Héro Moteur & Système (LuminaCard avec dégradé indigo électrique et ombrage)
        LuminaCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_system_card"),
            shape = LuminaTokens.ShapeHero,
            elevation = LuminaCardElevation.HERO,
            backgroundBrush = LuminaTokens.HeroGradientIndigo,
            spotShadowColor = LuminaTokens.HeroSpotShadowIndigo,
            ambientShadowColor = LuminaTokens.HeroAmbientShadow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(LuminaTokens.ShapeTile)
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(LuminaTokens.SpacingDefault))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lumina Comptabilité",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fonctionne entièrement hors-ligne",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }

                    Surface(
                        shape = LuminaTokens.ShapePill,
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "XOF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trio métriques système
                StatTrio(
                    item1Label = "Écritures",
                    item1Value = "${uiState.periodTransactions.size}",
                    item1Color = Color.White,
                    item2Label = "Caisses",
                    item2Value = "${uiState.cashRegisters.size}",
                    item2Color = Color.White,
                    item3Label = "Catégories",
                    item3Value = "${uiState.categories.size}",
                    item3Color = Color.White,
                    containerColor = Color.White.copy(alpha = 0.14f),
                    labelColor = Color.White.copy(alpha = 0.75f)
                )
            }
        }


        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 0 : IDENTITÉ DE L'ÉGLISE
        GroupedCard(title = "Identité de l'église") {
            ListRow(
                title = uiState.profile.displayName,
                subtitle = listOfNotNull(
                    uiState.profile.denomination.takeIf { it.isNotBlank() },
                    uiState.profile.city.takeIf { it.isNotBlank() }
                ).joinToString(" • ").ifBlank { "Nom, logo et couleur d'accent des rapports" },
                icon = Icons.Default.Church,
                iconTileColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onEditChurchProfile,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 1 : APPARENCE & THÈME (GroupedCard avec IconTiles et Inset dividers)
        GroupedCard(title = "Apparence & Thème") {
            // Option Système
            ListRow(
                title = "Automatique (Système)",
                subtitle = "Suit les préférences d'affichage Android",
                icon = Icons.Default.PhoneAndroid,
                iconTileColor = if (uiState.themeMode == ThemeMode.SYSTEM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (uiState.themeMode == ThemeMode.SYSTEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                },
                showDivider = true
            )

            // Option Sombre OLED
            ListRow(
                title = "Sombre OLED Pur (#000000)",
                subtitle = "Noir absolu économe en batterie avec accent indigo",
                icon = Icons.Default.DarkMode,
                iconTileColor = if (uiState.themeMode == ThemeMode.DARK) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (uiState.themeMode == ThemeMode.DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                },
                showDivider = true
            )

            // Option Clair
            ListRow(
                title = "Clair Épuré (Monochrome)",
                subtitle = "Fond blanc immaculé et surfaces minimalistes",
                icon = Icons.Default.LightMode,
                iconTileColor = if (uiState.themeMode == ThemeMode.LIGHT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (uiState.themeMode == ThemeMode.LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 2 : DEVISE UNIQUE DÉTERMINISTE
        GroupedCard(title = "Devise Comptable") {
            ListRow(
                title = "Franc CFA (FCFA / XOF)",
                subtitle = "Devise unique de l'Afrique de l'Ouest, montants exacts au franc près",
                icon = Icons.Default.Payments,
                iconTileColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                trailingContent = {
                    Surface(
                        shape = LuminaTokens.ShapeBadge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Actif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 3 : MODES D'AFFICHAGE DE L'ARGENT
        GroupedCard(title = "Affichage des Chiffres") {
            AmountDisplayMode.values().forEachIndexed { index, mode ->
                val isSelected = uiState.amountDisplayMode == mode
                val example = CurrencyFormatter.format(150000L, mode)

                ListRow(
                    title = mode.label,
                    subtitle = "${mode.description} • Aperçu : $example",
                    icon = when (mode) {
                        AmountDisplayMode.STANDARD -> Icons.Default.Payments
                        AmountDisplayMode.COMPACT -> Icons.Default.Payments
                        AmountDisplayMode.COMPTABLE -> Icons.Default.Description
                        AmountDisplayMode.DISCRET -> Icons.Default.VisibilityOff
                    },
                    iconTileColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onAmountDisplayModeChange(mode) },
                    trailingContent = {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onAmountDisplayModeChange(mode) }
                        )
                    },
                    showDivider = index < AmountDisplayMode.values().lastIndex
                )
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 4 : OUTILS & GESTION
        GroupedCard(title = "Outils & Trésorerie") {
            ListRow(
                title = "Système de Caisses & Liquidités",
                subtitle = "${uiState.cashRegisters.size} caisses configurées (Espèces, Wave, BOA...)",
                icon = Icons.Default.Payments,
                onClick = onNavigateToCaisses,
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                showDivider = true
            )

            ListRow(
                title = "Gestionnaire des Catégories",
                subtitle = "${uiState.categories.size} catégories de flux personnalisées",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategories,
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                showDivider = true
            )

            ListRow(
                title = "Centre d'Exports (PDF & Excel)",
                subtitle = "Bilans officiels et relevés complets certifiés",
                icon = Icons.Default.Description,
                onClick = onNavigateToExport,
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 5 : SÉCURITÉ & DÉTERMINISME
        GroupedCard(title = "Sécurité & Confidentialité") {
            ListRow(
                title = "100 % hors-ligne et privé",
                subtitle = "Vos écritures ne transitent par aucun serveur externe : tout est enregistré sur votre appareil.",
                icon = Icons.Default.Security,
                iconTileColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // SECTION 6 : DONNÉES D'ESSAI
        GroupedCard(title = "Données d'essai") {
            ListRow(
                title = "Effacer les écritures d'essai",
                subtitle = "Supprime toutes les opérations, les membres, les budgets et les caisses. " +
                    "Le nom de l'église, le logo et l'équipe sont conservés.",
                icon = Icons.Default.DeleteSweep,
                iconTileColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { confirmWipe = false },
                showDivider = true,
                modifier = Modifier.testTag("wipe_operational_row")
            )

            ListRow(
                title = "Tout effacer et repartir de zéro",
                subtitle = "Efface aussi le profil de l'église, le logo et l'équipe. " +
                    "L'assistant de premier lancement réapparaît.",
                icon = Icons.Default.RestartAlt,
                iconTileColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { confirmWipe = true },
                showDivider = false,
                modifier = Modifier.testTag("wipe_all_row")
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    confirmWipe?.let { full ->
        AlertDialog(
            onDismissRequest = { confirmWipe = null },
            title = {
                Text(if (full) "Tout effacer ?" else "Effacer les écritures d'essai ?")
            },
            text = {
                Text(
                    if (full) {
                        "Le profil de l'église, l'équipe, les écritures, les membres et les budgets " +
                            "seront définitivement supprimés de cet appareil. Cette action est irréversible."
                    } else {
                        "Les écritures, les membres, les budgets et les caisses seront définitivement " +
                            "supprimés. Le profil de l'église et l'équipe restent en place. " +
                            "Cette action est irréversible."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWipeData(full)
                        confirmWipe = null
                    }
                ) {
                    Text(if (full) "Tout effacer" else "Effacer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = null }) { Text("Annuler") }
            }
        )
    }
}
