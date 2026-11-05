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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinancialUiState
import com.example.ui.designsystem.GroupedCard
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.ListRow
import com.example.ui.designsystem.LuminaCard
import com.example.ui.designsystem.LuminaBrandMark
import com.example.ui.designsystem.LuminaCardElevation
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.StatTrio
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.AccentPalettes
import com.example.ui.theme.parseHexColor
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import androidx.compose.ui.graphics.Color


private enum class SettingsPage(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    IDENTITY("Identité de l'église", "Nom, logo, coordonnées et couleur des rapports.", Icons.Default.Church),
    APPEARANCE("Apparence et thème", "Choisissez le mode clair ou sombre et la couleur de Lumina.", Icons.Default.LightMode),
    CURRENCY("Devise", "Sélectionnez la devise utilisée dans les écritures et rapports.", Icons.Default.Payments),
    AMOUNTS("Affichage des montants", "Adaptez la précision des chiffres à votre façon de lire les comptes.", Icons.Default.Description),
    TOOLS("Outils de gestion", "Accédez aux catégories, aux exports et aux opérations courantes.", Icons.Default.Category),
    SECURITY("Sécurité et confidentialité", "Protégez les accès et vérifiez le comportement des données locales.", Icons.Default.Security),
    DEMO("Données de démonstration", "Essayez Lumina sans toucher aux données réelles de l'église.", Icons.Default.Visibility),
    CLOSURE("Clôture des comptes", "Verrouillez ou rouvrez les périodes comptables avec précaution.", Icons.Default.Lock),
    BACKUP("Fichier Lumina et sauvegardes", "Créez, partagez ou restaurez le dossier complet de l'église.", Icons.Default.Description)
}

@Composable
fun SettingsScreen(
    uiState: FinancialUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (String) -> Unit = {},
    onAmountDisplayModeChange: (AmountDisplayMode) -> Unit,
    onNavigateToCaisses: () -> Unit = {},
    onNavigateToCategories: () -> Unit,
    onNavigateToExport: () -> Unit,
    onEditChurchProfile: () -> Unit = {},
    onWipeData: (Boolean) -> Unit = {},
    onClosePreviousMonth: () -> Unit = {},
    onReopenBooks: () -> Unit = {},
    onCreateBackup: (String) -> Unit = {},
    onRequestRestore: (String) -> Unit = {},
    onRestoreAuto: () -> Unit = {},
    onEnableCloudBackup: () -> Unit = {},
    onCheckCloudBackup: () -> Unit = {},
    onRestoreCloudBackup: () -> Unit = {},
    cloudBackupStatus: String = "",
    lastAutoBackupLabel: String = "",
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // null = pas de confirmation en cours ; false = vider le dossier ; true = tout effacer
    var confirmWipe: Boolean? by remember { mutableStateOf(null) }
    var showBackupDialog: Boolean by remember { mutableStateOf(false) }
    var showRestoreDialog: Boolean by remember { mutableStateOf(false) }
    var showCloudRestoreConfirm: Boolean by remember { mutableStateOf(false) }
    var showFeatureGuide: Boolean by remember { mutableStateOf(false) }
    var guideStep by remember { mutableIntStateOf(0) }
    var password: String by remember { mutableStateOf("") }
    var selectedPage by remember { mutableStateOf<SettingsPage?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("settings_screen")
    ) {
        if (selectedPage == null) {
            // Hub de réglages : chaque thème ouvre sa propre page.
            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Choisissez un réglage. Chaque page explique clairement ses options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
            OutlinedButton(
                onClick = { guideStep = 0; showFeatureGuide = true },
                modifier = Modifier.fillMaxWidth().testTag("open_feature_guide")
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Guide pas à pas", fontWeight = FontWeight.Bold)
                    Text("Explications simples, page par page", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
            LuminaCard(
                modifier = Modifier.fillMaxWidth().testTag("settings_system_card"),
                shape = LuminaTokens.ShapeHero,
                elevation = LuminaCardElevation.HERO,
                backgroundBrush = LuminaTokens.heroGradient(MaterialTheme.colorScheme.primary),
                spotShadowColor = LuminaTokens.heroSpotShadow(MaterialTheme.colorScheme.primary),
                ambientShadowColor = LuminaTokens.HeroAmbientShadow
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LuminaBrandMark(
                            modifier = Modifier.size(46.dp),
                            backgroundColor = parseHexColor(uiState.profile.accentColorHex)
                        )
                        Spacer(modifier = Modifier.width(LuminaTokens.SpacingDefault))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lumina Comptabilité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Fonctionne entièrement hors-ligne", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.80f))
                        }
                        Surface(shape = LuminaTokens.ShapePill, color = Color.White.copy(alpha = 0.22f)) {
                            Text(
                                text = uiState.profile.currencyCode.ifBlank { "XOF" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    StatTrio(
                        item1Label = "Écritures", item1Value = "${uiState.periodTransactions.size}", item1Color = Color.White,
                        item2Label = "Caisses", item2Value = "${uiState.cashRegisters.size}", item2Color = Color.White,
                        item3Label = "Catégories", item3Value = "${uiState.categories.size}", item3Color = Color.White,
                        containerColor = Color.White.copy(alpha = 0.14f), labelColor = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
            GroupedCard(title = "Choisissez une page") {
                SettingsPage.values().forEachIndexed { index, page ->
                    ListRow(
                        title = page.title,
                        subtitle = page.description,
                        icon = page.icon,
                        iconTileColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { selectedPage = page },
                        showDivider = index < SettingsPage.values().lastIndex,
                        modifier = Modifier.testTag("settings_page_${page.name.lowercase()}")
                    )
                }
            }
        } else {
            TextButton(onClick = { selectedPage = null }, modifier = Modifier.testTag("settings_back_to_pages")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tous les paramètres")
            }
            Text(
                selectedPage!!.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(selectedPage!!.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        if (selectedPage == SettingsPage.IDENTITY) {
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

        }

        if (selectedPage == SettingsPage.APPEARANCE) {
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
                showDivider = true
            )

            // Variantes Glass distinctes des thèmes classiques
            ListRow(
                title = "Glass clair",
                subtitle = "Surfaces translucides, reflets doux et fond lumineux",
                icon = Icons.Default.LightMode,
                iconTileColor = if (uiState.themeMode == ThemeMode.GLASS_LIGHT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (uiState.themeMode == ThemeMode.GLASS_LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onThemeModeChange(ThemeMode.GLASS_LIGHT) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.themeMode == ThemeMode.GLASS_LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.GLASS_LIGHT) }
                    )
                },
                showDivider = true
            )

            ListRow(
                title = "Glass sombre",
                subtitle = "Verre fumé, halos colorés et contours lumineux",
                icon = Icons.Default.DarkMode,
                iconTileColor = if (uiState.themeMode == ThemeMode.GLASS_DARK) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (uiState.themeMode == ThemeMode.GLASS_DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onThemeModeChange(ThemeMode.GLASS_DARK) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.themeMode == ThemeMode.GLASS_DARK,
                        onClick = { onThemeModeChange(ThemeMode.GLASS_DARK) }
                    )
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        GroupedCard(title = "Couleur de l'application") {
            Text(
                text = "La couleur choisie s'applique immédiatement aux cartes, graphiques et rapports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentPalettes.forEach { palette ->
                    val selected = uiState.profile.accentColorHex.equals(palette.hex, ignoreCase = true)
                    val swatch = runCatching { Color(android.graphics.Color.parseColor(palette.hex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onAccentColorChange(palette.hex) }
                            .testTag("accent_palette_${palette.key}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Default.Check, contentDescription = "Sélectionnée", tint = Color.White)
                        }
                        Text(
                            palette.label.substringBefore(" ("),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        }

        if (selectedPage == SettingsPage.CURRENCY) {
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

        }

        if (selectedPage == SettingsPage.AMOUNTS) {
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

        }

        if (selectedPage == SettingsPage.TOOLS) {
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
                subtitle = "Rapports de gestion mensuels et relevés complets",
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

        }

        if (selectedPage == SettingsPage.SECURITY) {
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

        }

        if (selectedPage == SettingsPage.DEMO) {
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


        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        }

        if (selectedPage == SettingsPage.CLOSURE) {
        // SECTION 7 : CLÔTURE DES COMPTES
        GroupedCard(title = "Clôture des comptes") {
            ListRow(
                title = if (uiState.profile.booksClosedUntilMillis > 0L) {
                    "Comptes clôturés jusqu'au ${
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
                            .format(java.util.Date(uiState.profile.booksClosedUntilMillis))
                    }"
                } else {
                    "Aucune période clôturée"
                },
                subtitle = "Une période clôturée n'accepte plus de saisie, de modification ni d'annulation : " +
                    "les comptes restent figés pour le contrôle.",
                icon = Icons.Default.Lock,
                iconTileColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                showDivider = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClosePreviousMonth,
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("close_books_button")
                ) {
                    Text("Clôturer le mois dernier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReopenBooks,
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reopen_books_button")
                ) {
                    Text("Réouvrir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        }

        if (selectedPage == SettingsPage.BACKUP) {
        // SECTION 8 : SAUVEGARDE & RESTAURATION
        GroupedCard(title = "Sauvegarde & restauration") {
            ListRow(
                title = "Copie automatique",
                subtitle = if (lastAutoBackupLabel.isBlank()) {
                    "Une copie de secours est conservée chaque jour dans l'application."
                } else {
                    "Dernière copie : $lastAutoBackupLabel"
                },
                icon = Icons.Default.Security,
                iconTileColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                showDivider = true
            )
            ListRow(
                title = "Emporter une sauvegarde",
                subtitle = "Enregistrez le dossier complet sur une clé, un compte de messagerie " +
                    "ou le stockage du téléphone. Vous pourrez le réinstaller même après un changement d'appareil.",
                icon = Icons.Default.Description,
                iconTileColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    password = ""
                    showBackupDialog = true
                },
                showDivider = true,
                modifier = Modifier.testTag("backup_export_row")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        password = ""
                        showBackupDialog = true
                    },
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup_create_button")
                ) {
                    Text("Créer une sauvegarde", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        password = ""
                        showRestoreDialog = true
                    },
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup_restore_button")
                ) {
                    Text("Restaurer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Copie cloud partagée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                cloudBackupStatus.ifBlank {
                    "Activez la copie partagée une seule fois. Lumina tentera ensuite une sauvegarde quotidienne (au moins trois jours distincts par semaine) quand Internet est disponible."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "L'activation programme les envois; « Vérifier » télécharge et contrôle la dernière copie sans restaurer ni modifier les données. Les envois peuvent être retardés par Android ou l'absence de réseau. Le fournisseur chiffre les données au repos, mais peut techniquement accéder au contenu.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEnableCloudBackup,
                    enabled = uiState.canWrite && !uiState.isDemoMode,
                    modifier = Modifier.weight(1f).testTag("enable_cloud_backup")
                ) { Text("Activer la sauvegarde") }
                OutlinedButton(
                    onClick = onCheckCloudBackup,
                    enabled = !uiState.isDemoMode,
                    modifier = Modifier.weight(1f).testTag("check_cloud_backup")
                ) { Text("Vérifier la copie") }
            }
            OutlinedButton(
                onClick = { showCloudRestoreConfirm = true },
                enabled = !uiState.isDemoMode,
                modifier = Modifier.fillMaxWidth().testTag("restore_cloud_backup")
            ) { Text("Restaurer le cloud") }
        }

        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showFeatureGuide) {
        val guideTopics = listOf(
            "1. Se repérer dans Lumina" to "Le bouton ☰ ouvre le menu. Appuyez sur une rubrique pour changer de page. La flèche Retour de votre téléphone revient à l'écran précédent. Les réglages sont séparés par thèmes.",
            "2. Lire le tableau de bord" to "Choisissez d'abord une période en haut de la page. Les totaux et graphiques se recalculent pour cette période. Touchez une caisse pour voir son solde, ou choisissez « Toutes » pour revenir au total.",
            "3. Ajouter une écriture" to "Touchez le bouton +. Choisissez une entrée d'argent ou une dépense, puis indiquez le montant, la date, la catégorie et la caisse. Vérifiez le résumé avant d'enregistrer. Vous pouvez joindre une photo du reçu.",
            "4. Gérer les caisses" to "Ouvrez « Caisses » pour créer une caisse, noter son solde de départ et consulter les mouvements. Un transfert entre deux caisses ne doit pas être saisi comme un revenu ou une dépense.",
            "5. Préparer un budget" to "Dans « Budgets », choisissez un mois et une catégorie, puis saisissez le montant prévu. Le suivi compare ensuite les dépenses déjà enregistrées à ce montant.",
            "6. Retrouver une opération" to "Dans l'historique, utilisez les filtres de date, catégorie, caisse ou type. Appuyez sur « Effacer les filtres » pour retrouver la liste entière. Les corrections et annulations restent traçables.",
            "7. Gérer les membres" to "Ouvrez « Membres », appuyez sur Ajouter et remplissez le prénom et le nom. Les coordonnées et autres renseignements sont facultatifs. Vérifiez la fiche avant de la valider.",
            "8. Suivre un projet" to "Créez un projet, une collecte ou un événement avec son objectif. Lors de la saisie d'une opération liée, choisissez ce projet pour que son avancement soit correctement calculé.",
            "9. Créer les accès de l'équipe" to "Chaque personne doit avoir son propre nom et son code PIN de 4 à 6 chiffres sur son appareil. Ne partagez pas votre PIN. Les comptes locaux et les PIN ne sont jamais inclus dans un fichier Lumina partagé.",
            "10. Créer et partager un fichier Lumina" to "Dans Paramètres → Fichier Lumina, créez le ZIP complet. Il contient le profil de l'église, le logo, les réglages, les écritures, budgets, caisses, projets, membres et justificatifs disponibles. Un mot de passe est facultatif, mais recommandé pour l'envoi.",
            "11. Rejoindre avec un fichier" to "Sur l'appareil neuf, choisissez « J'ai un fichier Lumina ». Sélectionnez le ZIP et saisissez son mot de passe si demandé. L'import est refusé si le dossier contient déjà des données. Ensuite, créez votre propre prénom, nom et PIN.",
            "12. Essayer sans risque" to "Le mode démonstration contient des exemples temporaires en lecture seule. Il ne modifie pas le vrai dossier. Quittez la démonstration pour revenir à l'église."
        )
        val safeStep = guideStep.coerceIn(0, guideTopics.lastIndex)
        val (topicTitle, topicBody) = guideTopics[safeStep]
        AlertDialog(
            onDismissRequest = { showFeatureGuide = false },
            title = { Text("Guide de Lumina") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Étape ${safeStep + 1} sur ${guideTopics.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(guideTopics.size) { index ->
                            Box(
                                Modifier.weight(1f).height(4.dp).clip(LuminaTokens.ShapePill)
                                    .background(if (index <= safeStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    Text(topicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        topicBody,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(enabled = safeStep > 0, onClick = { guideStep = (safeStep - 1).coerceAtLeast(0) }) {
                            Text("Précédent")
                        }
                        TextButton(
                            onClick = {
                                if (safeStep < guideTopics.lastIndex) guideStep = safeStep + 1
                                else showFeatureGuide = false
                            }
                        ) { Text(if (safeStep == guideTopics.lastIndex) "Terminer" else "Suivant") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFeatureGuide = false }) { Text("Fermer le guide") }
            }
        )
    }

    if (showCloudRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirm = false },
            title = { Text("Restaurer la sauvegarde cloud ?") },
            text = {
                Text("Cette action remplacera les données du dossier par la dernière version partagée de l'église. Une copie locale de sécurité sera créée avant la restauration.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showCloudRestoreConfirm = false
                    onRestoreCloudBackup()
                }) { Text("Restaurer") }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirm = false }) { Text("Annuler") }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Créer une sauvegarde") },
            text = {
                Column {
                    Text(
                        "Une protection par mot de passe est recommandée si la sauvegarde " +
                            "circule par messagerie. Sans mot de passe, le fichier reste lisible " +
                            "par quiconque le détient.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe (facultatif)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_password_field")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateBackup(password)
                        showBackupDialog = false
                    },
                    modifier = Modifier.testTag("backup_confirm_button")
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restaurer un dossier") },
            text = {
                Column {
                    Text(
                        "La restauration remplace le dossier actuel par celui de la sauvegarde. " +
                            "Une copie de sécurité est conservée par l'application.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe de la sauvegarde") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_password_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onRestoreAuto()
                            showRestoreDialog = false
                        },
                        modifier = Modifier.testTag("restore_auto_button")
                    ) {
                        Text("Rétablir la copie automatique du jour", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRequestRestore(password)
                        showRestoreDialog = false
                    },
                    modifier = Modifier.testTag("restore_confirm_button")
                ) {
                    Text("Choisir un fichier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Annuler") }
            }
        )
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
