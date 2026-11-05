package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.designsystem.LuminaCard
import com.example.ui.designsystem.LuminaCardElevation
import com.example.ui.designsystem.LuminaTokens
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import java.util.Locale

@Composable
fun FinancialSummaryCard(
    totalIncomeCents: Long,
    totalExpenseCents: Long,
    netBalanceCents: Long,
    savingsRatePercent: Double,
    periodLabel: String,
    amountDisplayMode: AmountDisplayMode = AmountDisplayMode.STANDARD,
    modifier: Modifier = Modifier
) {
    val isPositive = netBalanceCents >= 0
    val netFormatted = CurrencyFormatter.format(
        amount = netBalanceCents,
        mode = amountDisplayMode,
        withSign = true,
        isPositive = isPositive
    )
    val incomeFormatted = CurrencyFormatter.format(totalIncomeCents, amountDisplayMode)
    val expenseFormatted = CurrencyFormatter.format(totalExpenseCents, amountDisplayMode)

    // La carte suit l'accent actif du profil (palette de l'église), comme les autres surfaces héro.
    val accent = MaterialTheme.colorScheme.primary
    val heroGradient = LuminaTokens.heroGradient(accent)

    LuminaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_summary_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = LuminaCardElevation.HERO,
        backgroundBrush = heroGradient,
        spotShadowColor = LuminaTokens.heroSpotShadow(accent),
        ambientShadowColor = Color(0x44000000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Ligne 1 : En-tête Eyebrow + Badge Statut (translucide compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOLDE NET • ${periodLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White.copy(alpha = 0.82f)
                )

                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (isPositive) "Statut excédentaire" else "Statut déficitaire",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPositive) "Excédent" else "Déficit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ligne 2 : Montant principal + Delta en ligne optimisée (format ultra-compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = netFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                // Pastille de variation compacte
                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (totalIncomeCents <= 0L) {
                                "Pas de produit"
                            } else {
                                String.format(Locale.FRANCE, "%.1f %% épargne", savingsRatePercent.coerceAtLeast(0.0))
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ligne 3 : Bandeau compact imbriqué (Entrées / Sorties / Taux condensés sur une seule ligne)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Entrées
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Entrées",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+$incomeFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Séparateur vertical discret
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )

                        // Sorties
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Sorties",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "-$expenseFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 11.sp
                            )
                        }

                        // Séparateur vertical discret
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )

                        // Pourcentage
                        Text(
                            text = if (totalIncomeCents <= 0L) {
                                "N/A"
                            } else {
                                String.format(Locale.FRANCE, "%.0f %% conservé", savingsRatePercent.coerceIn(0.0, 100.0))
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mini jauge ultra-fine
                    LinearProgressIndicator(
                        progress = {
                            if (totalIncomeCents <= 0L) 0f
                            else (savingsRatePercent.coerceIn(0.0, 100.0) / 100.0).toFloat()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(LuminaTokens.ShapePill),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
