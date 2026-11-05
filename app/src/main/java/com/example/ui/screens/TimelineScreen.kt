package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.FinancialUiState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.ExpenseBgDark
import com.example.ui.theme.ExpenseBgLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.IncomeBgDark
import com.example.ui.theme.IncomeBgLight
import com.example.ui.isTransactionLocked
import com.example.ui.PeriodFilter
import com.example.ui.SortMode
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ui.components.CollapsibleFilters
import com.example.ui.components.FinancialTimelineChart
import com.example.ui.designsystem.GroupedCard
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.StatTrio
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Historique des opérations.
 *
 * La page entière défile (l'en-tête ne mange plus l'écran) et les filtres sont
 * rangés derrière deux icônes : le panneau ne s'ouvre qu'à la demande.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TimelineScreen(
    uiState: FinancialUiState,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onTypeFilterSelected: (TransactionType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onCancelTransaction: (TransactionEntity) -> Unit = {},
    onSelectCaisseFilter: (Long?) -> Unit = {},
    onCategoryFilterSelected: (String?) -> Unit = {},
    onAmountRangeChange: (Long, Long) -> Unit = { _, _ -> },
    onSortModeSelected: (SortMode) -> Unit = {},
    onResetFilters: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var minAmountInput by remember { mutableStateOf(if (uiState.minAmountCents > 0) uiState.minAmountCents.toString() else "") }
    var maxAmountInput by remember { mutableStateOf(if (uiState.maxAmountCents > 0) uiState.maxAmountCents.toString() else "") }

    val dayHeaderFormat = remember { SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    val caisseScrollState = rememberScrollState()

    val groupedTransactions = remember(uiState.transactions) {
        val cal = Calendar.getInstance()
        val today = SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(Date())
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(cal.time)

        uiState.transactions.groupBy { tx ->
            val key = SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(Date(tx.timestamp))
            when (key) {
                today -> "Aujourd'hui"
                yesterday -> "Hier"
                else -> dayHeaderFormat.format(Date(tx.timestamp)).replaceFirstChar { it.uppercase() }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LuminaTokens.SpacingDefault)
            .testTag("timeline_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = LuminaTokens.SpacingMedium,
            bottom = 96.dp
        )
    ) {
        item(key = "header") {
            var openPanel by remember { mutableStateOf<Int?>(null) }

            // Deux filtres, deux icônes rattachées à la page (pas de boutons flottants).
            val countPeriod = listOf(
                uiState.selectedPeriod != PeriodFilter.CE_MOIS,
                uiState.selectedTypeFilter != null,
                uiState.sortMode != SortMode.DATE_DESC
            ).count { it }
            val countScope = listOf(
                uiState.selectedCashRegisterId != null,
                uiState.selectedCategoryFilter != null,
                uiState.minAmountCents > 0L || uiState.maxAmountCents > 0L
            ).count { it }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Historique",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${uiState.transactions.size} opération(s) • ${uiState.selectedCashRegisterName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        FilterPageIcon(
                            count = countPeriod,
                            icon = Icons.Default.Tune,
                            label = "Période, nature et tri",
                            testTag = "filter_icon_period",
                            onClick = { openPanel = if (openPanel == 1) null else 1 }
                        )
                        if (openPanel == 1) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, 56),
                                onDismissRequest = { openPanel = null },
                                properties = PopupProperties(focusable = true)
                            ) {
                                FloatingFilterCard(
                                    title = "Période, nature, tri",
                                    onClose = { openPanel = null }
                                ) {
                                    Text("Période", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PeriodFilter.values().forEach { period ->
                                            FilterChip(
                                                selected = uiState.selectedPeriod == period,
                                                onClick = { onPeriodSelected(period) },
                                                label = { Text(period.label, fontSize = 11.sp) },
                                                shape = LuminaTokens.ShapePill
                                            )
                                        }
                                    }

                                    Text("Nature", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = uiState.selectedTypeFilter == null,
                                            onClick = { onTypeFilterSelected(null) },
                                            label = { Text("Tout", fontSize = 11.sp) },
                                            shape = LuminaTokens.ShapePill
                                        )
                                        TimelineTypeFilterChip(
                                            type = TransactionType.INCOME,
                                            selected = uiState.selectedTypeFilter == TransactionType.INCOME,
                                            label = "Entrées",
                                            onClick = { onTypeFilterSelected(TransactionType.INCOME) }
                                        )
                                        TimelineTypeFilterChip(
                                            type = TransactionType.EXPENSE,
                                            selected = uiState.selectedTypeFilter == TransactionType.EXPENSE,
                                            label = "Sorties",
                                            onClick = { onTypeFilterSelected(TransactionType.EXPENSE) }
                                        )
                                    }

                                    Text("Tri", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SortMode.values().forEach { mode ->
                                            FilterChip(
                                                selected = uiState.sortMode == mode,
                                                onClick = { onSortModeSelected(mode) },
                                                label = { Text(mode.label, fontSize = 11.sp) },
                                                shape = LuminaTokens.ShapePill
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        FilterPageIcon(
                            count = countScope,
                            icon = Icons.Default.FilterAlt,
                            label = "Caisse, catégorie et montant",
                            testTag = "filter_icon_scope",
                            onClick = { openPanel = if (openPanel == 2) null else 2 }
                        )
                        if (openPanel == 2) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, 56),
                                onDismissRequest = { openPanel = null },
                                properties = PopupProperties(focusable = true)
                            ) {
                                FloatingFilterCard(
                                    title = "Caisse, catégorie, montant",
                                    onClose = { openPanel = null }
                                ) {
                                    if (uiState.cashRegisters.isNotEmpty()) {
                                        Text("Caisse", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = uiState.selectedCashRegisterId == null,
                                                onClick = { onSelectCaisseFilter(null) },
                                                label = { Text("Toutes", fontSize = 11.sp) },
                                                shape = LuminaTokens.ShapePill
                                            )
                                            uiState.cashRegisters.forEach { reg ->
                                                FilterChip(
                                                    selected = uiState.selectedCashRegisterId == reg.id,
                                                    onClick = { onSelectCaisseFilter(reg.id) },
                                                    label = { Text(reg.name, fontSize = 11.sp) },
                                                    shape = LuminaTokens.ShapePill
                                                )
                                            }
                                        }
                                    }

                                    Text("Catégorie", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = uiState.selectedCategoryFilter == null,
                                            onClick = { onCategoryFilterSelected(null) },
                                            label = { Text("Toutes", fontSize = 11.sp) },
                                            shape = LuminaTokens.ShapePill
                                        )
                                        uiState.categories.forEach { category ->
                                            FilterChip(
                                                selected = uiState.selectedCategoryFilter == category.name,
                                                onClick = { onCategoryFilterSelected(category.name) },
                                                label = { Text(category.name, fontSize = 11.sp) },
                                                shape = LuminaTokens.ShapePill
                                            )
                                        }
                                    }

                                    Text("Montant (FCFA)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = minAmountInput,
                                            onValueChange = { value ->
                                                minAmountInput = value.filter { it.isDigit() }
                                                onAmountRangeChange(
                                                    minAmountInput.toLongOrNull() ?: 0L,
                                                    maxAmountInput.toLongOrNull() ?: 0L
                                                )
                                            },
                                            label = { Text("Min") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = maxAmountInput,
                                            onValueChange = { value ->
                                                maxAmountInput = value.filter { it.isDigit() }
                                                onAmountRangeChange(
                                                    minAmountInput.toLongOrNull() ?: 0L,
                                                    maxAmountInput.toLongOrNull() ?: 0L
                                                )
                                            },
                                            label = { Text("Max") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (uiState.activeFilterCount > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                minAmountInput = ""
                                                maxAmountInput = ""
                                                onResetFilters()
                                                openPanel = null
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Réinitialiser les filtres")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Rechercher une opération…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = LuminaTokens.ShapeSubCard,
                    modifier = Modifier.fillMaxWidth().testTag("timeline_search_input")
                )
            }
        }

        if (uiState.transactions.isNotEmpty()) {
            item(key = "chart") {
                FinancialTimelineChart(
                    transactions = uiState.transactions,
                    title = "Courbe des flux • ${uiState.selectedPeriod.label}",
                    subtitle = "Évolution sur la période filtrée",
                    displayMode = uiState.amountDisplayMode,
                    testTag = "timeline_screen_chart"
                )
            }
        }

        if (uiState.transactions.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune opération trouvée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ajustez les filtres ou enregistrez une opération.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            groupedTransactions.forEach { (dateGroup, list) ->
                item(key = "header_$dateGroup") {
                    GroupedCard(title = dateGroup) {
                        list.forEachIndexed { index, tx ->
                            val isClosedByPeriod = isTransactionLocked(
                                tx.timestamp,
                                uiState.profile.booksClosedUntilMillis
                            )
                            TimelineTransactionRow(
                                transaction = tx,
                                displayMode = uiState.amountDisplayMode,
                                timeFormatted = timeFormat.format(Date(tx.timestamp)),
                                isLocked = isClosedByPeriod,
                                onOpenClick = {
                                    if (!tx.isTransfer && !isClosedByPeriod) onEditTransaction(tx)
                                },
                                onDeleteClick = {
                                    if (!tx.isNeutralised && !isClosedByPeriod) transactionToDelete = tx
                                },
                                showDivider = index < list.size - 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        var reason by remember(tx.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(if (tx.isTransfer) "Annuler le virement ?" else "Annuler l'opération ?")
            },
            text = {
                Column {
                    Text(
                        if (tx.isTransfer) {
                            "Une contre-écriture sera créée dans les deux caisses. Les deux lignes d'origine restent au journal, marquées annulées : la piste d'audit est conservée."
                        } else {
                            "\"${tx.title}\" (${CurrencyFormatter.format(tx.amountCents)}) sera neutralisée par une contre-écriture. L'écriture d'origine reste au journal."
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motif de l'annulation (optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelTransaction(tx.copy(note = reason))
                        transactionToDelete = null
                    }
                ) {
                    Text("Annuler l'écriture", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Retour") }
            }
        )
    }
}

@Composable
fun TimelineTransactionRow(
    transaction: TransactionEntity,
    displayMode: com.example.util.AmountDisplayMode,
    timeFormatted: String,
    isLocked: Boolean = false,
    onOpenClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    showDivider: Boolean
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.isTransfer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onOpenClick() }
            .testTag("timeline_item_${transaction.id}")
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = when {
                    isTransfer -> Icons.Default.SwapHoriz
                    isIncome -> Icons.Default.ArrowUpward
                    else -> Icons.Default.ArrowDownward
                },
                contentDescription = when {
                    isTransfer -> "Virement interne"
                    isIncome -> "Entrée"
                    else -> "Sortie"
                },
                size = 38.dp,
                containerColor = when {
                    isTransfer -> MaterialTheme.colorScheme.secondaryContainer
                    isIncome -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                iconTint = when {
                    isTransfer -> MaterialTheme.colorScheme.onSecondaryContainer
                    isIncome -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isNeutralised) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.isNeutralised) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = LuminaTokens.ShapeBadge,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (transaction.isReversal) "contre-écriture" else "annulée",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = LuminaTokens.ShapeBadge,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = transaction.cashRegisterName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTransfer) {
                            "Virement interne • $timeFormatted"
                        } else {
                            "${transaction.category} • $timeFormatted"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (transaction.hasReceipt) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Justificatif joint",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.format(
                        amount = transaction.amountCents,
                        mode = displayMode,
                        withSign = !isTransfer,
                        isPositive = isIncome
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
                        isIncome -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Écriture verrouillée par la clôture des comptes",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                            .testTag("locked_transaction_${transaction.id}")
                    )
                } else {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

/** Icône de filtre rattachée à l'en-tête de la page, avec le nombre de filtres actifs. */
@Composable
private fun FilterPageIcon(
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0) {
            Surface(
                shape = LuminaTokens.ShapePill,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/** Liste flottante ouverte depuis une icône de l'en-tête. */
@Composable
private fun FloatingFilterCard(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = LuminaTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .width(300.dp)
            .heightIn(max = 460.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            content()
        }
    }
}


@Composable
private fun TimelineTypeFilterChip(
    type: TransactionType,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val semanticColor = when (type) {
        TransactionType.INCOME -> if (darkSurface) IncomeGreenLight else IncomeGreen
        TransactionType.EXPENSE -> if (darkSurface) ExpenseRedLight else ExpenseRed
    }
    val selectedSurface = when (type) {
        TransactionType.INCOME -> if (darkSurface) IncomeBgDark else IncomeBgLight
        TransactionType.EXPENSE -> if (darkSurface) ExpenseBgDark else ExpenseBgLight
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        leadingIcon = {
            Icon(
                imageVector = if (type == TransactionType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        shape = LuminaTokens.ShapePill,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedSurface,
            selectedLabelColor = semanticColor,
            selectedLeadingIconColor = semanticColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = semanticColor,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}
