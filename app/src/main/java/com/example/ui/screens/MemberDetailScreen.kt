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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemberEntity
import com.example.ui.designsystem.LuminaTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fiche d'un membre : toutes les informations recueillies, alignées et lisibles.
 * Les champs facultatifs vides ne sont pas affichés comme des trous, mais comme
 * « — » pour garder une grille régulière.
 */
@Composable
fun MemberDetailScreen(
    member: MemberEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val joinFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.FRANCE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("member_detail_screen")
    ) {
        // Bandeau d'identité
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour au registre",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "FICHE DU MEMBRE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.2.sp
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initials,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = member.profession.ifBlank { "Membre du registre" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = LuminaTokens.ShapePill,
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = member.maritalStatus.ifBlank { "Situation non renseignée" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            InfoGroup(title = "Identité") {
                InfoRow("Prénom", member.firstName)
                InfoRow("Nom", member.lastName)
                InfoRow("Profession", member.profession)
            }

            InfoGroup(title = "Situation familiale") {
                InfoRow("Situation", member.maritalStatus)
                InfoRow("Conjoint(e)", member.spouseName)
            }

            InfoGroup(title = "Contact & résidence") {
                InfoRow("Téléphone", member.phone)
                InfoRow("Adresse", member.address)
                InfoRow("Résidence", member.residence)
            }

            InfoGroup(title = "Vie d'église") {
                InfoRow("Inscrit le", joinFormat.format(Date(member.joinedAt)))
                InfoRow("Notes", member.notes)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_member_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modifier cette fiche", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_member_button")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retirer du registre", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
