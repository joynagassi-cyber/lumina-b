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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.ChurchProfileEntity
import com.example.ui.designsystem.LuminaTokens

/**
 * Fiche de l'église : présentation soignée des informations enregistrées.
 * La modification reste accessible, mais n'est plus le passage obligé.
 */
@Composable
fun ChurchProfileScreen(
    profile: ChurchProfileEntity,
    memberCount: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(profile.accentColorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("church_profile_screen")
    ) {
        // Bandeau d'identité
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LuminaTokens.heroGradient(accent))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "FICHE DE L'ÉGLISE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.2.sp
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (profile.denomination.isNotBlank()) {
                            Text(
                                text = profile.denomination,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${memberCount} membre(s) inscrit(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            InfoGroup(title = "Localisation") {
                InfoRow("Ville", profile.city)
                InfoRow("Adresse", profile.address)
            }

            InfoGroup(title = "Contacts") {
                InfoRow("Téléphone", profile.phone)
                InfoRow("Courriel", profile.email)
            }

            InfoGroup(title = "Responsables") {
                InfoRow("Pasteur / responsable", profile.pastorName)
                InfoRow("Trésorier", profile.treasurerName)
            }

            InfoGroup(title = "Identité visuelle") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Couleur d'accent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profile.accentColorHex.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                InfoRow("Logo", if (profile.hasLogo) "Ajouté" else "Aucun logo")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_church_profile")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modifier les informations", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
