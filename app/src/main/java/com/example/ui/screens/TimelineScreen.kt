package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.FinancialUiState
import com.example.ui.PeriodFilter
import com.example.ui.designsystem.GroupedCard
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.StatTrio
import com.example.util.CurrencyFormatter
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    uiState: FinancialUiState,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onTypeFilterSelected: (TransactionType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onSelectCaisseFilter: (Long?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    val caisseScrollState = rememberScrollState()

    // Regrouper par date
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("timeline_screen")
    ) {
        // En-tête
        Text(
            text = "Historique & Relevés",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Chronologie exacte relevée par caisse • ${uiState.selectedCashRegisterName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Carte Héro Historique & Flux Périodique
        HeroCard(
            eyebrow = "Flux Périodique • ${uiState.selectedPeriod.label.uppercase()}",
            primaryAmount = CurrencyFormatter.format(
                amount = uiState.netBalanceCents,
                mode = uiState.amountDisplayMode,
                withSign = true,
                isPositive = uiState.netBalanceCents >= 0
            ),
            deltaText = "${uiState.transactions.size} écritures",
            isDeltaPositive = uiState.netBalanceCents >= 0,
            deltaDescription = uiState.selectedCashRegisterName,
            testTag = "timeline_hero_card",
            trailingBadge = {
                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Traçabilité",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            nestedContent = {
                StatTrio(
                    item1Label = "Entrées",
                    item1Value = CurrencyFormatter.format(uiState.totalIncomeCents, uiState.amountDisplayMode),
                    item1Color = Color(0xFF4ADE80),
                    item2Label = "Sorties",
                    item2Value = CurrencyFormatter.format(uiState.totalExpenseCents, uiState.amountDisplayMode),
                    item2Color = Color(0xFFFF6B6B),
                    item3Label = "Solde Période",
                    item3Value = CurrencyFormatter.format(
                        amount = uiState.netBalanceCents,
                        mode = uiState.amountDisplayMode,
                        withSign = true,
                        isPositive = uiState.netBalanceCents >= 0
                    ),
                    item3Color = if (uiState.netBalanceCents >= 0) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                    containerColor = Color.White.copy(alpha = 0.14f),
                    labelColor = Color.White.copy(alpha = 0.75f)
                )
            }
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Champ de recherche épuré
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher par titre, catégorie ou note...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Recherche",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            shape = LuminaTokens.ShapeSubCard,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timeline_search_input")
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingSmall))

        // Filtre par Caisse défilant horizontalement
        if (uiState.cashRegisters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(caisseScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCashRegisterId == null,
                    onClick = { onSelectCaisseFilter(null) },
                    label = { Text("Toutes les caisses", fontSize = 11.sp, fontWeight = if (uiState.selectedCashRegisterId == null) FontWeight.Bold else FontWeight.Normal) },
                    shape = LuminaTokens.ShapePill,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                uiState.cashRegisters.forEach { reg ->
                    val isSelected = uiState.selectedCashRegisterId == reg.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCaisseFilter(reg.id) },
                        label = { Text(reg.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = LuminaTokens.ShapePill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Filtres Type (Tous, Entrées, Sorties)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.selectedTypeFilter == null,
                onClick = { onTypeFilterSelected(null) },
                label = { Text("Tous les flux", fontSize = 11.sp, fontWeight = if (uiState.selectedTypeFilter == null) FontWeight.Bold else FontWeight.Normal) },
                shape = LuminaTokens.ShapePill,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
            FilterChip(
                selected = uiState.selectedTypeFilter == TransactionType.INCOME,
                onClick = { onTypeFilterSelected(TransactionType.INCOME) },
                label = { Text("Entrées (+)", fontSize = 11.sp, fontWeight = if (uiState.selectedTypeFilter == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal) },
                shape = LuminaTokens.ShapePill,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = uiState.selectedTypeFilter == TransactionType.EXPENSE,
                onClick = { onTypeFilterSelected(TransactionType.EXPENSE) },
                label = { Text("Sorties (-)", fontSize = 11.sp) },
                shape = LuminaTokens.ShapePill,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        if (uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune opération trouvée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ajustez vos filtres ou enregistrez une transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedTransactions.forEach { (dateGroup, list) ->
                    item(key = "header_$dateGroup") {
                        GroupedCard(title = dateGroup) {
                            list.forEachIndexed { index, tx ->
                                TimelineTransactionRow(
                                    transaction = tx,
                                    displayMode = uiState.amountDisplayMode,
                                    timeFormatted = timeFormat.format(Date(tx.timestamp)),
                                    onDeleteClick = { transactionToDelete = tx },
                                    showDivider = index < list.size - 1
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Dialogue de confirmation de suppression
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Supprimer l'opération ?") },
            text = { Text("Voulez-vous supprimer définitivement \"${tx.title}\" (${com.example.util.CurrencyFormatter.format(tx.amountCents)}) de la ${tx.cashRegisterName} ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(tx.id)
                        transactionToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun TimelineTransactionRow(
    transaction: TransactionEntity,
    displayMode: com.example.util.AmountDisplayMode,
    timeFormatted: String,
    onDeleteClick: () -> Unit,
    showDivider: Boolean
) {
    val isIncome = transaction.type == TransactionType.INCOME

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_item_${transaction.id}")
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tuile icône
            IconTile(
                icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (isIncome) "Entrée" else "Sortie",
                size = 38.dp,
                containerColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconTint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Titre & Sous-titre
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
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
                        text = "${transaction.category} • $timeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Montant & Corbeille
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = com.example.util.CurrencyFormatter.format(
                        amount = transaction.amountCents,
                        mode = displayMode,
                        withSign = true,
                        isPositive = isIncome
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

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

        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}
