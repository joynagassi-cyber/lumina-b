package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionType
import com.example.ui.FinancialUiState
import com.example.ui.PeriodFilter
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.FinancialTimelineChart
import com.example.ui.designsystem.GroupedCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.SectionHeader
import com.example.ui.designsystem.SegmentedControl
import com.example.ui.designsystem.SparklineMiniChart
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LuminaGold
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: FinancialUiState,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToCaisses: () -> Unit = {},
    onSelectCashRegister: (Long?) -> Unit = {},
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val horizontalCaissesScroll = rememberScrollState()
    var periodMenuExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("dashboard_screen")
    ) {
        if (uiState.isDemoMode) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("read_only_demo_banner"),
                shape = LuminaTokens.ShapeSubCard,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Démonstration — vos données réelles sont protégées", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Les écritures d'essai sont fictives et effacées à la sortie.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
        // En-tête Bilan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Finances",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Suivi clair et traçable de vos finances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Période groupée dans un menu pour éviter d'éparpiller les options.
        Box {
            Button(
                onClick = { periodMenuExpanded = true },
                modifier = Modifier.fillMaxWidth().testTag("dashboard_period_selector"),
                shape = LuminaTokens.ShapePill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Période : ${uiState.selectedPeriod.label}", fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir la période")
                }
            }
            DropdownMenu(expanded = periodMenuExpanded, onDismissRequest = { periodMenuExpanded = false }) {
                PeriodFilter.values().forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.label) },
                        onClick = {
                            periodMenuExpanded = false
                            onPeriodSelected(period)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Carte Héro Bilan Synthétique
        FinancialSummaryCard(
            totalIncomeCents = uiState.totalIncomeCents,
            totalExpenseCents = uiState.totalExpenseCents,
            netBalanceCents = uiState.netBalanceCents,
            savingsRatePercent = uiState.savingsRatePercent,
            periodLabel = uiState.selectedPeriod.label,
            amountDisplayMode = uiState.amountDisplayMode
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Visualisation interactive de la timeline financière dans la section principale
        FinancialTimelineChart(
            transactions = uiState.periodTransactions,
            title = "Dynamique des Flux • ${uiState.selectedPeriod.label}",
            subtitle = "Inspection tactile de l'évolution financière",
            displayMode = uiState.amountDisplayMode,
            testTag = "dashboard_timeline_chart"
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // ==========================================
        // Carrousel Horizontal des Caisses & Liquidités (Référence Findex / Winvest)
        // ==========================================
        SectionHeader(
            title = "Mes Caisses & Liquidités",
            actionLabel = "Gérer",
            onActionClick = onNavigateToCaisses
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalCaissesScroll)
                .testTag("dashboard_treasury_card"),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Carte Totale consolidée
            val isAllSelected = uiState.selectedCashRegisterId == null
            Card(
                modifier = Modifier
                    .width(180.dp)
                    .clickable { onSelectCashRegister(null) }
                    .border(
                        width = if (isAllSelected) LuminaTokens.BorderWidthActive else LuminaTokens.BorderWidthThin,
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = LuminaTokens.ShapeCard
                    ),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(
                    containerColor = if (isAllSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(
                            icon = Icons.Default.Payments,
                            contentDescription = null,
                            size = 32.dp,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                        if (isAllSelected) {
                            Surface(
                                shape = LuminaTokens.ShapeBadge,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Actif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Total Trésorerie",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(uiState.totalCashBalanceCents, uiState.amountDisplayMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Cartes individuelles de caisses
            uiState.cashRegisterSummaries.forEach { reg ->
                val isSelected = uiState.selectedCashRegisterId == reg.id
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable { onSelectCashRegister(reg.id) }
                        .border(
                            width = if (isSelected) LuminaTokens.BorderWidthActive else LuminaTokens.BorderWidthThin,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = LuminaTokens.ShapeCard
                        ),
                    shape = LuminaTokens.ShapeCard,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconTile(
                                icon = when (reg.iconType) {
                                    "mobile" -> Icons.Default.PhoneAndroid
                                    "bank" -> Icons.Default.AccountBalance
                                    "vault" -> Icons.Default.Savings
                                    else -> Icons.Default.Payments
                                },
                                contentDescription = null,
                                size = 32.dp,
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Surface(
                                    shape = LuminaTokens.ShapeBadge,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "Isolé",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = reg.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(reg.currentBalanceCents, uiState.amountDisplayMode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // ==========================================
        // Suivi Budgétaire (Carte à jauge style Reference 4)
        // ==========================================
        val globalBudget = uiState.globalBudgetSummary
        if (globalBudget.totalBudgetCeilingCents > 0L) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBudgets() }
                    .border(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline, LuminaTokens.ShapeCard)
                    .testTag("dashboard_budget_widget"),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(
                                icon = Icons.Default.PieChart,
                                contentDescription = null,
                                size = 32.dp,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Budget Mensuel",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.budgetMonthLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = LuminaTokens.ShapeBadge,
                            color = if (globalBudget.isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (globalBudget.isExceeded) "Dépassement !" else String.format(Locale.FRANCE, "%.1f %%", globalBudget.percentage * 100f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (globalBudget.isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${CurrencyFormatter.format(globalBudget.totalSpentCents, uiState.amountDisplayMode)} consommés",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Plafond : ${CurrencyFormatter.format(globalBudget.totalBudgetCeilingCents, uiState.amountDisplayMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { globalBudget.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(LuminaTokens.ShapePill),
                        color = if (globalBudget.isExceeded) ExpenseRed else if (globalBudget.percentage >= 0.8f) LuminaGold else IncomeGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
        }

        // ==========================================
        // Actions Rapides (Tuiles arrondies avec IconTile)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bouton PDF
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExportPdf() }
                    .border(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline, LuminaTokens.ShapeCard)
                    .testTag("quick_export_pdf_button"),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(
                        icon = Icons.Default.Description,
                        contentDescription = "PDF",
                        size = 36.dp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Rapport PDF",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Comptabilité de trésorerie",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bouton Excel/CSV
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExportCsv() }
                    .border(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline, LuminaTokens.ShapeCard)
                    .testTag("quick_export_csv_button"),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(
                        icon = Icons.Default.TableChart,
                        contentDescription = "Excel",
                        size = 36.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Export Excel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tableur UTF-8",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Répartition des dépenses
        CategoryDistributionCard(
            categoryTotals = uiState.categoryExpenses,
            title = "Répartition des Sorties",
            displayMode = uiState.amountDisplayMode
        )

        if (uiState.categoryIncomes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))
            CategoryDistributionCard(
                categoryTotals = uiState.categoryIncomes,
                title = "Origine des Entrées",
                displayMode = uiState.amountDisplayMode,
                isExpense = false
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // ==========================================
        // Dernières Opérations (Watchlist Layout - Modèle Findex / Winvest)
        // ==========================================
        SectionHeader(
            title = "Dernières opérations",
            actionLabel = "Voir tout",
            onActionClick = onNavigateToTimeline
        )

        if (uiState.periodTransactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = LuminaTokens.ShapeCard,
                border = androidx.compose.foundation.BorderStroke(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                            text = "Aucune opération enregistrée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // GroupedCard avec séparateurs fins insets
            GroupedCard {
                uiState.periodTransactions.take(4).forEachIndexed { index, tx ->
                    val isIncome = tx.type == TransactionType.INCOME
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTimeline() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tuile icône
                        IconTile(
                            icon = if (isIncome) Icons.Default.Payments else Icons.Default.Payments,
                            contentDescription = null,
                            size = 38.dp,
                            containerColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            iconTint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Titre & Sous-titre
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.cashRegisterName} • ${tx.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        // Sparkline mini-graphique discret
                        SparklineMiniChart(
                            points = if (isIncome) listOf(5f, 10f, 8f, 15f, 22f, 30f) else listOf(30f, 24f, 26f, 18f, 12f, 6f),
                            lineColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(width = 44.dp, height = 20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Montant
                        Text(
                            text = CurrencyFormatter.format(
                                amount = tx.amountCents,
                                mode = uiState.amountDisplayMode,
                                withSign = true,
                                isPositive = isIncome
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (index < 3 && index < uiState.periodTransactions.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
