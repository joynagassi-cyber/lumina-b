package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemberEntity
import com.example.ui.designsystem.LuminaTokens

/**
 * Registre des membres de l'église.
 *
 * Seuls le prénom et le nom sont requis ; tous les autres champs servent à
 * joindre la personne et à tenir un registre complet de l'assemblée.
 */
@Composable
fun MembersScreen(
    members: List<MemberEntity>,
    onAddMember: (MemberEntity) -> Unit,
    onUpdateMember: (MemberEntity) -> Unit,
    onDeleteMember: (MemberEntity) -> Unit,
    createSignal: Int = 0,
    onCreateConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<MemberEntity?>(null) }
    var editing by remember { mutableStateOf<MemberEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MemberEntity?>(null) }

    val filtered = remember(members, query) { members.filter { it.matches(query) } }

    // Le bouton flottant de l'application ouvre directement la création
    LaunchedEffect(createSignal) {
        if (createSignal > 0) {
            editing = null
            showForm = true
            onCreateConsumed()
        }
    }

    val detail = selectedMember
    if (detail != null) {
        MemberDetailScreen(
            member = members.firstOrNull { it.id == detail.id } ?: detail,
            onBack = { selectedMember = null },
            onEdit = { editing = detail; showForm = true },
            onDelete = { pendingDelete = detail }
        )
    } else {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LuminaTokens.SpacingDefault)
            .testTag("members_screen")
    ) {
        Text(
            text = "Membres",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "${members.size} fiche(s) au registre de l'assemblée",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Nom, téléphone, profession…") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = LuminaTokens.ShapeSubCard,
                modifier = Modifier.weight(1f).testTag("members_search")
            )
            Button(
                onClick = { editing = null; showForm = true },
                shape = LuminaTokens.ShapeSubCard,
                modifier = Modifier.testTag("members_add_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingMedium))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (members.isEmpty()) "Aucun membre enregistré" else "Aucun résultat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (members.isEmpty()) {
                            "Ajoutez les personnes de votre assemblée pour les joindre facilement."
                        } else {
                            "Modifiez votre recherche pour retrouver une fiche."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
            ) {
                items(filtered, key = { it.id }) { member ->
                    MemberRow(
                        member = member,
                        onOpen = { selectedMember = member },
                        onEdit = { editing = member; showForm = true },
                        onDelete = { pendingDelete = member }
                    )
                }
            }
        }
    }
    }

    if (showForm) {
        MemberFormDialog(
            member = editing,
            onDismiss = { showForm = false; editing = null },
            onSave = { saved ->
                if (saved.id == 0L) onAddMember(saved) else onUpdateMember(saved)
                showForm = false
                editing = null
            }
        )
    }

    pendingDelete?.let { member ->
        DeleteMemberConfirm(
            member = member,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteMember(member)
                if (selectedMember?.id == member.id) selectedMember = null
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun DeleteMemberConfirm(
    member: MemberEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirer ce membre ?") },
        text = { Text("La fiche de ${member.fullName} sera supprimée du registre.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Supprimer", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun MemberRow(
    member: MemberEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("member_card_${member.id}"),
        shape = LuminaTokens.ShapeSubCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.initials,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                val subtitle = listOfNotNull(
                    member.profession.takeIf { it.isNotBlank() },
                    member.residence.takeIf { it.isNotBlank() },
                    member.phone.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (member.maritalStatus.isNotBlank()) {
                    Text(
                        text = if (member.spouseName.isNotBlank()) {
                            "${member.maritalStatus} • conjoint : ${member.spouseName}"
                        } else {
                            member.maritalStatus
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberFormDialog(
    member: MemberEntity?,
    onDismiss: () -> Unit,
    onSave: (MemberEntity) -> Unit
) {
    var firstName by remember { mutableStateOf(member?.firstName ?: "") }
    var lastName by remember { mutableStateOf(member?.lastName ?: "") }
    var maritalStatus by remember { mutableStateOf(member?.maritalStatus ?: "") }
    var spouseName by remember { mutableStateOf(member?.spouseName ?: "") }
    var phone by remember { mutableStateOf(member?.phone ?: "") }
    var address by remember { mutableStateOf(member?.address ?: "") }
    var residence by remember { mutableStateOf(member?.residence ?: "") }
    var profession by remember { mutableStateOf(member?.profession ?: "") }
    var notes by remember { mutableStateOf(member?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "Nouveau membre" else "Modifier la fiche") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it; error = null },
                        label = { Text("Prénom *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it; error = null },
                        label = { Text("Nom *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = maritalStatus,
                    onValueChange = { maritalStatus = it },
                    label = { Text("Situation matrimoniale") },
                    placeholder = { Text("Célibataire, marié(e), veuf(ve)…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = spouseName,
                    onValueChange = { spouseName = it },
                    label = { Text("Conjoint(e)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact téléphonique") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_phone_field")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = residence,
                    onValueChange = { residence = it },
                    label = { Text("Lieu de résidence") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text("Profession") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = "* Champs obligatoires. Toutes les autres informations sont optionnelles.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (firstName.isBlank() || lastName.isBlank()) {
                    error = "Le prénom et le nom sont obligatoires."
                } else {
                    onSave(
                        (member ?: MemberEntity(firstName = firstName, lastName = lastName)).copy(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            maritalStatus = maritalStatus.trim(),
                            spouseName = spouseName.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            residence = residence.trim(),
                            profession = profession.trim(),
                            notes = notes.trim()
                        )
                    )
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
