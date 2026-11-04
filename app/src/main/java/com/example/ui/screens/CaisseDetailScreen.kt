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
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.data.TransactionType
import com.example.ui.CashRegisterSummary
import com.example.ui.designsystem.LuminaTokens
import com.example.util.CurrencyFormatter
import com.example.util.AmountDisplayMode
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fiche d'une caisse : solde, mouvements, et toutes les actions possibles au
 * même endroit (virement, modification, isolation, suppression).
 */
@Composable
fun CaisseDetailScreen(
    caisse: CashRegisterSummary,
    transactions: List<TransactionEntity>,
    displayMode: AmountDisplayMode,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    val movements = transactions.filter { it.cashRegisterId == caisse.id }.sortedByDescending { it.timestamp }
    val ratio = when {
        caisse.initialBalanceCents + caisse.totalIncomeCents <= 0L -> 0f
        else -> (caisse.currentBalanceCents.toFloat() /
            (caisse.initialBalanceCents + caisse.totalIncomeCents).toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("caisse_detail_screen")
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
                            contentDescription = "Retour aux caisses",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "DÉTAIL DE LA CAISSE",
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
                    text = caisse.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (caisse.description.isNotBlank()) {
                    Text(
                        text = caisse.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SOLDE DISPONIBLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = CurrencyFormatter.format(caisse.currentBalanceCents, displayMode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(LuminaTokens.ShapePill),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            InfoGroup(title = "Situation") {
                InfoRow("Solde initial", CurrencyFormatter.format(caisse.initialBalanceCents, displayMode))
                InfoRow("Entrées cumulées", "+${CurrencyFormatter.format(caisse.totalIncomeCents, displayMode)}")
                InfoRow("Sorties cumulées", "-${CurrencyFormatter.format(caisse.totalExpenseCents, displayMode)}")
                InfoRow("Mouvements", "${caisse.transactionCount}")
                if (caisse.isDefault) InfoRow("Statut", "Caisse principale")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTransfer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Virement", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (!caisse.isDefault) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Supprimer cette caisse",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "DERNIERS MOUVEMENTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (movements.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuminaTokens.ShapeSubCard,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "Aucun mouvement enregistré sur la période affichée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                movements.take(20).forEach { tx ->
                    val isIncome = tx.type == TransactionType.INCOME
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
                                    text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (isIncome) "+" else "-") +
                                    CurrencyFormatter.format(if (tx.amountCents < 0) -tx.amountCents else tx.amountCents, displayMode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
