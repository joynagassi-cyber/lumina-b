package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.CategoryBudgetProgress
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Fiche d'un budget : plafond, consommation, reste, et les dépenses qui le
 * composent. La modification et la suppression restent à portée de pouce.
 */
@Composable
fun BudgetDetailScreen(
    item: CategoryBudgetProgress,
    transactions: List<TransactionEntity>,
    monthLabel: String,
    displayMode: AmountDisplayMode,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    val spent = transactions
        .filter { it.category == item.categoryName && it.type == com.example.data.TransactionType.EXPENSE }
        .sortedByDescending { it.timestamp }
    val gaugeColor = when {
        item.isExceeded -> ExpenseRed
        item.isNearLimit -> Color(0xFFB45309)
        else -> IncomeGreen
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("budget_detail_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LuminaTokens.heroGradient(accent))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour aux budgets",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "DÉTAIL DU BUDGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.2.sp
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White)
                    }
                }

                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "DÉPENSÉ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Text(
                            CurrencyFormatter.format(item.spentAmountCents, displayMode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (item.remainingCents < 0) "DÉPASSEMENT" else "RESTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Text(
                            CurrencyFormatter.format(abs(item.remainingCents), displayMode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { item.percentage.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(LuminaTokens.ShapePill),
                    color = gaugeColor,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            InfoGroup(title = "Plafond mensuel") {
                InfoRow("Plafond", CurrencyFormatter.format(item.budgetAmountCents, displayMode))
                InfoRow("Consommé", CurrencyFormatter.format(item.spentAmountCents, displayMode))
                InfoRow("Utilisation", "${(item.percentage * 100).toInt()} %")
                InfoRow(
                    "État",
                    when {
                        item.isExceeded -> "Dépassé"
                        item.isNearLimit -> "Vigilance"
                        else -> "Sous contrôle"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retirer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "DÉPENSES DE CETTE CATÉGORIE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (spent.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuminaTokens.ShapeSubCard,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "Aucune dépense enregistrée dans cette catégorie sur la période.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                spent.take(20).forEach { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = LuminaTokens.ShapeSubCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.cashRegisterName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "-" + CurrencyFormatter.format(abs(tx.amountCents), displayMode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
