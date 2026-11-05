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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryBudgetProgress
import com.example.ui.FinancialUiState
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.components.SetBudgetDialog
import com.example.ui.components.parseHexColor
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.PillButton
import com.example.ui.designsystem.SectionHeader
import com.example.ui.designsystem.StatTrio
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LuminaGold
import java.util.Locale

@Composable
fun BudgetScreen(
    uiState: FinancialUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSaveBudget: (categoryName: String, amountCents: Long) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onAddCategory: (name: String, type: com.example.data.TransactionType, colorHex: String) -> Unit,
    onUpdateCategory: (id: Long, name: String, type: com.example.data.TransactionType, colorHex: String, isDefault: Boolean) -> Unit,
    onDeleteCategory: (com.example.data.CategoryEntity) -> Unit,
    createSignal: Int = 0,
    onCreateConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<CategoryBudgetProgress?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf<CategoryBudgetProgress?>(null) }
    var showBudgetSearch by remember { mutableStateOf(false) }
    var budgetSearchQuery by remember { mutableStateOf("") }
    var showBudgetFilterMenu by remember { mutableStateOf(false) }
    var budgetStatusFilter by remember { mutableStateOf("all") }
    val visibleBudgets = remember(uiState.categoryBudgetProgress, budgetSearchQuery, budgetStatusFilter) {
        uiState.categoryBudgetProgress.filter { item ->
            val matchesName = item.categoryName.contains(budgetSearchQuery.trim(), ignoreCase = true)
            val matchesStatus = when (budgetStatusFilter) {
                "exceeded" -> item.isExceeded
                "near" -> item.isNearLimit && !item.isExceeded
                "under" -> !item.isExceeded && !item.isNearLimit
                else -> true
            }
            matchesName && matchesStatus
        }
    }

    // Le bouton flottant de l'application ouvre la création d'un budget
    LaunchedEffect(createSignal) {
        if (createSignal > 0) {
            budgetToEdit = null
            showSetBudgetDialog = true
            onCreateConsumed()
        }
    }

    val detail = selectedBudget
    if (detail != null) {
        BudgetDetailScreen(
            item = uiState.categoryBudgetProgress.firstOrNull { it.categoryName == detail.categoryName } ?: detail,
            transactions = uiState.periodTransactions,
            monthLabel = uiState.budgetMonthLabel,
            displayMode = uiState.amountDisplayMode,
            onBack = { selectedBudget = null },
            onEdit = { budgetToEdit = detail; showSetBudgetDialog = true },
            onDelete = {
                detail.budgetId?.let { onDeleteBudget(it) }
                selectedBudget = null
            }
        )
    } else {

    // Une seule page, un seul plan : la synthèse défile avec les catégories.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("budget_screen")
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budgets & Suivi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Maîtrisez vos plafonds de dépenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showBudgetSearch = !showBudgetSearch }) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher une catégorie")
                }
                Box {
                    IconButton(onClick = { showBudgetFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrer les budgets")
                    }
                    DropdownMenu(expanded = showBudgetFilterMenu, onDismissRequest = { showBudgetFilterMenu = false }) {
                        listOf("all" to "Toutes les catégories", "exceeded" to "Dépassement", "near" to "Proche du plafond", "under" to "Sous le seuil").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { budgetStatusFilter = value; showBudgetFilterMenu = false }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showCategoryManager = true },
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier.testTag("open_category_manager_button")
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catégories", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showBudgetSearch) {
            OutlinedTextField(
                value = budgetSearchQuery,
                onValueChange = { budgetSearchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("budget_search_field"),
                singleLine = true,
                label = { Text("Rechercher une catégorie") },
                trailingIcon = { TextButton(onClick = { budgetSearchQuery = "" }) { Text("Effacer") } }
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Sélecteur de Mois
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = LuminaTokens.ShapePill,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Mois précédent")
                }

                Text(
                    text = uiState.budgetMonthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Mois suivant")
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Synthèse globale du budget (HeroCard)
        val global = uiState.globalBudgetSummary
        HeroCard(
            eyebrow = "Budget Mensuel Global • ${uiState.budgetMonthLabel}",
            primaryAmount = com.example.util.CurrencyFormatter.format(global.totalBudgetCeilingCents, uiState.amountDisplayMode),
            deltaText = when {
                global.totalBudgetCeilingCents == 0L -> "Non défini"
                global.isExceeded -> "Dépassement !"
                global.percentage >= 0.8f -> "Alerte 80%"
                else -> String.format(Locale.FRANCE, "%.1f %% consommé", global.percentage * 100f)
            },
            isDeltaPositive = !global.isExceeded,
            deltaDescription = if (global.isExceeded) "Plafond mensuel dépassé" else "Dépenses sous contrôle",
            testTag = "global_budget_card",
            trailingBadge = {
                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.20f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (global.isExceeded) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                global.totalBudgetCeilingCents == 0L -> "Libre"
                                global.isExceeded -> "Alerte"
                                global.percentage >= 0.8f -> "Vigilance"
                                else -> "Équilibré"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            nestedContent = {
                // StatTrio (Dépensé / Plafond / Reste)
                StatTrio(
                    item1Label = "Dépensé",
                    item1Value = com.example.util.CurrencyFormatter.format(global.totalSpentCents, uiState.amountDisplayMode),
                    item1Color = Color.White,
                    item2Label = "Plafond",
                    item2Value = com.example.util.CurrencyFormatter.format(global.totalBudgetCeilingCents, uiState.amountDisplayMode),
                    item2Color = Color.White,
                    item3Label = if (global.totalRemainingCents < 0) "Dépassement" else "Reste",
                    item3Value = com.example.util.CurrencyFormatter.format(
                        amount = Math.abs(global.totalRemainingCents),
                        mode = uiState.amountDisplayMode,
                        withSign = true,
                        isPositive = global.totalRemainingCents >= 0
                    ),
                    item3Color = if (global.totalRemainingCents < 0) Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                    containerColor = Color.White.copy(alpha = 0.14f),
                    labelColor = Color.White.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Jauge globale en pilule
                val progressValue = (global.percentage).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(LuminaTokens.ShapePill),
                    color = when {
                        global.isExceeded -> Color(0xFFFF6B6B)
                        global.percentage >= 0.8f -> Color(0xFFFBBF24)
                        else -> Color(0xFF4ADE80)
                    },
                    trackColor = Color.White.copy(alpha = 0.20f)
                )
            }
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // Titre de section & bouton d'ajout de budget
        SectionHeader(
            title = "Progression par Catégorie",
            actionLabel = "+ Définir",
            onActionClick = {
                budgetToEdit = null
                showSetBudgetDialog = true
            }
        )

        if (uiState.categoryBudgetProgress.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = LuminaTokens.ShapeCard,
                border = androidx.compose.foundation.BorderStroke(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aucun budget défini pour ${uiState.budgetMonthLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cliquez sur 'Définir' pour fixer vos plafonds de dépenses mensuels par catégorie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (visibleBudgets.isEmpty()) {
                    Text(
                        "Aucun budget ne correspond à la recherche ou au filtre.",
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                visibleBudgets.forEach { item ->
                    CategoryBudgetCard(
                        item = item,
                        displayMode = uiState.amountDisplayMode,
                        onOpenClick = { selectedBudget = item },
                        onEditClick = {
                            budgetToEdit = item
                            showSetBudgetDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
    }

    // Dialogue Définir / Modifier Budget
    if (showSetBudgetDialog) {
        SetBudgetDialog(
            monthLabel = uiState.budgetMonthLabel,
            categories = uiState.categories,
            initialCategory = budgetToEdit?.categoryName,
            initialAmountCents = budgetToEdit?.budgetAmountCents,
            existingBudgetId = budgetToEdit?.budgetId,
            onDismissRequest = {
                showSetBudgetDialog = false
                budgetToEdit = null
            },
            onSaveBudget = { cat, amount ->
                onSaveBudget(cat, amount)
            },
            onDeleteBudget = if (budgetToEdit?.budgetId != null) {
                { id -> onDeleteBudget(id) }
            } else null
        )
    }

    // Dialogue Gestion Catégories
    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = uiState.categories,
            onDismissRequest = { showCategoryManager = false },
            onAddCategory = onAddCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory
        )
    }
}

@Composable
fun CategoryBudgetCard(
    item: CategoryBudgetProgress,
    displayMode: com.example.util.AmountDisplayMode = com.example.util.AmountDisplayMode.STANDARD,
    onOpenClick: () -> Unit = {},
    onEditClick: () -> Unit
) {
    val categoryColor = parseHexColor(item.categoryColorHex)
    val spentFormatted = com.example.util.CurrencyFormatter.format(item.spentAmountCents, displayMode)
    val budgetFormatted = com.example.util.CurrencyFormatter.format(item.budgetAmountCents, displayMode)
    val remainingFormatted = com.example.util.CurrencyFormatter.format(Math.abs(item.remainingCents), displayMode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenClick() }
            .border(
                width = LuminaTokens.BorderWidthThin,
                color = MaterialTheme.colorScheme.outline,
                shape = LuminaTokens.ShapeCard
            )
            .testTag("budget_item_${item.categoryName}"),
        shape = LuminaTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LuminaTokens.CardElevationNone)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LuminaTokens.SpacingDefault)
        ) {
            // En-tête de la carte
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
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge d'alerte / statut
                    Surface(
                        shape = LuminaTokens.ShapeBadge,
                        color = when {
                            item.budgetAmountCents == 0L -> MaterialTheme.colorScheme.surfaceVariant
                            item.isExceeded -> MaterialTheme.colorScheme.errorContainer
                            item.isNearLimit -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = when {
                                item.budgetAmountCents == 0L -> "Sans plafond"
                                item.isExceeded -> "Dépassement !"
                                item.isNearLimit -> "Alerte 80%"
                                else -> "Normal"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                item.budgetAmountCents == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                                item.isExceeded -> MaterialTheme.colorScheme.onErrorContainer
                                item.isNearLimit -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_budget_${item.categoryName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier ce budget",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // StatTrio pour la catégorie : Dépensé / Plafond / Reste
            StatTrio(
                item1Label = "Dépensé",
                item1Value = spentFormatted,
                item2Label = "Plafond",
                item2Value = if (item.budgetAmountCents > 0L) budgetFormatted else "Libre",
                item3Label = if (item.isExceeded) "Dépassement" else "Reste",
                item3Value = if (item.budgetAmountCents > 0L) (if (item.isExceeded) "+$remainingFormatted" else remainingFormatted) else "Illimité",
                item3Color = if (item.isExceeded) ExpenseRed else IncomeGreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Barre de progression
            val progressVal = if (item.budgetAmountCents > 0L) item.percentage.coerceIn(0f, 1f) else 1f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(LuminaTokens.ShapePill),
                color = when {
                    item.isExceeded -> ExpenseRed
                    item.isNearLimit -> LuminaGold
                    else -> categoryColor
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.budgetAmountCents > 0L) {
                        String.format(Locale.FRANCE, "%.1f %% utilisé", item.percentage * 100f)
                    } else {
                        "Dépenses non plafonnées"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
