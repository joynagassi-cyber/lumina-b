package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.PeriodFilter
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.PillButton
import com.example.ui.designsystem.SectionHeader
import com.example.ui.designsystem.StatTrio
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(
    uiState: FinancialUiState,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onExportExcel: () -> Unit = {},
    onExportMembersPdf: () -> Unit = {},
    onExportBudgetPdf: () -> Unit = {},
    onExportHistoryPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var periodMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("export_screen")
    ) {
        // En-tête
        Text(
            text = "Rapports & Exports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Exports haute fidélité pour votre comptabilité et bilans",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Choix de la période de rapport
        Text(
            text = "Période du rapport :",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box {
            Button(
                onClick = { periodMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = LuminaTokens.ShapePill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiState.selectedPeriod.label, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir une période")
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
        if (uiState.selectedPeriod == PeriodFilter.PERSONNALISEE && uiState.bilan.periodStartMillis > 0L) {
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
            Text(
                "Du ${dateFormat.format(Date(uiState.bilan.periodStartMillis))} au ${dateFormat.format(Date(uiState.bilan.periodEndMillis))}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Aperçu du Bilan Actuel (HeroCard)
        HeroCard(
            eyebrow = "Synthèse à Exporter • ${uiState.selectedPeriod.label.uppercase()}",
            primaryAmount = CurrencyFormatter.format(uiState.netBalanceCents, uiState.amountDisplayMode),
            deltaText = "${uiState.periodTransactions.size} opérations",
            isDeltaPositive = uiState.netBalanceCents >= 0,
            deltaDescription = uiState.selectedCashRegisterName,
            testTag = "export_summary_card",
            trailingBadge = {
                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.20f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Certifié",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            nestedContent = {
                StatTrio(
                    item1Label = "Entrées",
                    item1Value = CurrencyFormatter.format(uiState.totalIncomeCents, uiState.amountDisplayMode),
                    item1Color = androidx.compose.ui.graphics.Color(0xFF4ADE80),
                    item2Label = "Sorties",
                    item2Value = CurrencyFormatter.format(uiState.totalExpenseCents, uiState.amountDisplayMode),
                    item2Color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                    item3Label = "Solde Net",
                    item3Value = CurrencyFormatter.format(uiState.netBalanceCents, uiState.amountDisplayMode),
                    item3Color = if (uiState.netBalanceCents >= 0) androidx.compose.ui.graphics.Color(0xFF4ADE80) else androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f),
                    labelColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)
                )
            }
        )


        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // Carte Export 1 : PDF Stylisé
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline, LuminaTokens.ShapeCard),
            shape = LuminaTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = LuminaTokens.CardElevationNone)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LuminaTokens.SpacingDefault)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            icon = Icons.Default.Description,
                            contentDescription = null,
                            size = 42.dp,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Rapport PDF Officiel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mise en page éditoriale stylée",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = LuminaTokens.ShapeBadge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "PDF A4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Génère un document PDF complet avec logo, tableau des opérations chronologiques, KPIs de trésorerie et solde net. Idéal pour l'impression, l'archivage ou l'envoi par email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                PillButton(
                    text = "Générer & Partager le PDF",
                    icon = Icons.Default.Share,
                    onClick = onExportPdf,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "export_pdf_button"
                )
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = LuminaTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
        ) {
            Column(Modifier.fillMaxWidth().padding(LuminaTokens.SpacingDefault)) {
                Text("Rapports dédiés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Documents ciblés, séparés du bilan financier complet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    text = "PDF membres", onClick = onExportMembersPdf,
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Groups,
                    isPrimary = false, testTag = "export_members_pdf_button"
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    text = "PDF du budget", onClick = onExportBudgetPdf,
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.PieChart,
                    isPrimary = false, testTag = "export_budget_pdf_button"
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    text = "PDF de l'historique", onClick = onExportHistoryPdf,
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.History,
                    isPrimary = false, testTag = "export_history_pdf_button"
                )
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Carte Export 2 : Excel / CSV Universel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(LuminaTokens.BorderWidthThin, MaterialTheme.colorScheme.outline, LuminaTokens.ShapeCard),
            shape = LuminaTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = LuminaTokens.CardElevationNone)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LuminaTokens.SpacingDefault)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            icon = Icons.Default.TableChart,
                            contentDescription = null,
                            size = 42.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconTint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Excel et CSV séparés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Classeur Excel ou texte CSV",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = LuminaTokens.ShapeBadge,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "XLS / CSV",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Excel produit un classeur ouvrable avec colonnes typées et montants numériques. CSV produit un fichier texte UTF-8 séparé par des points-virgules, pour l'import dans d'autres outils.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                PillButton(
                    text = "Générer le classeur Excel (.xlsx)",
                    icon = Icons.Default.FileDownload,
                    onClick = onExportExcel,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "export_excel_button"
                )
                Spacer(modifier = Modifier.height(8.dp))
                PillButton(
                    text = "Générer le fichier CSV (.csv)",
                    icon = Icons.Default.FileDownload,
                    onClick = onExportCsv,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "export_csv_button"
                )
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Badge de garantie déterministe et sécurité locale
        Surface(
            shape = LuminaTokens.ShapeCard,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.Security,
                    contentDescription = null,
                    size = 32.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Vos chiffres restent sur votre téléphone, sans aucun serveur externe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
