package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.sync.SyncConflictItem
import com.example.ui.FinancialUiState
import com.example.ui.designsystem.LuminaTokens

/**
 * Écran « Équipe & échange » : gestion des personnes autorisées sur l'appareil
 * et transfert des données par fichier chiffré.
 */
@Composable
fun TeamSyncScreen(
    uiState: FinancialUiState,
    onAddUser: (String, UserRole, String) -> Unit,
    onUpdateUser: (UserEntity, String, UserRole, String) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onExport: (Context, String, (java.io.File) -> Unit) -> Unit,
    onCreateInvitation: (Context, String, (java.io.File) -> Unit) -> Unit,
    onImport: (Context, android.net.Uri, String) -> Unit,
    onRefreshConflicts: (Context) -> Unit,
    onResolveConflict: (Context, SyncConflictItem, Boolean, String) -> Unit,
    onShareFile: (Context, java.io.File) -> Unit,
    onCreateCloudInvite: (Context, (Result<String>) -> Unit) -> Unit = { _, result -> result(Result.failure(IllegalStateException("Cloud indisponible"))) },
    onAcceptCloudInvite: (Context, String, (Result<String>) -> Unit) -> Unit = { _, _, result -> result(Result.failure(IllegalStateException("Cloud indisponible"))) },
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(uiState.workspaceId, uiState.isDemoMode) {
        if (!uiState.isDemoMode) onRefreshConflicts(context)
    }
    var editing by remember { mutableStateOf<UserEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<UserEntity?>(null) }
    var passphrase by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var cloudInviteCode by remember { mutableStateOf("") }
    var cloudInviteInput by remember { mutableStateOf("") }
    var cloudInviteMessage by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("team_sync_screen")
    ) {
        // -------------------------------------------------
        // Section 1 : l'équipe
        // -------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Équipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "${uiState.users.size} personne(s) autorisée(s) sur cet appareil",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.canManageUsers) {
                Button(
                    onClick = { creating = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_user_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        uiState.users.forEach { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = LuminaTokens.ShapeSubCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.initials,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.displayName, fontWeight = FontWeight.Bold)
                            if (uiState.currentUser?.id == user.id) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("vous", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                append(user.roleValue.label)
                                if (!user.hasPin) append(" • sans code")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.canManageUsers) {
                        IconButton(onClick = { editing = user }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier ${user.displayName}", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { deleting = user }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Retirer ${user.displayName}",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        if (uiState.syncContributors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sync_contributors"),
                shape = LuminaTokens.ShapeSubCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contributeurs des échanges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Les personnes ci-dessous ont partagé des données avec ce dossier. Ce sont des attributions d'échange, pas des comptes locaux ni des PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.syncContributors.forEach { contributor ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    contributor.displayName.trim().split(" ").filter { it.isNotBlank() }
                                        .take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(contributor.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    contributor.role.takeIf { it.isNotBlank() }?.let { contributor.roleValue.label }
                                        ?: "Rôle non communiqué",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Trésorier : tout gérer.  Comptable : saisir et modifier sans supprimer.  " +
                "Assistant : saisir sans gérer l'équipe.  Lecture : consulter uniquement.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // -------------------------------------------------
        // Section 2 : échange de données
        // -------------------------------------------------
        Text("Échange entre appareils", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Un fichier chiffré est produit ici, puis ouvert sur l'autre téléphone. " +
                "Les ajouts sont rapprochés par identifiant stable. Une version clairement plus récente remplace l'ancienne; " +
                "en cas de divergence ambiguë, la copie locale est conservée et le conflit est signalé.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.syncConflicts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sync_conflict_journal"),
                shape = LuminaTokens.ShapeSubCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Conflits à arbitrer (${uiState.syncConflicts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "La copie locale reste active jusqu'à votre choix. Les décisions sont conservées dans le journal de ce dossier.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    uiState.syncConflicts.forEach { conflict ->
                        HorizontalDivider()
                        Text("${conflict.kind} : ${conflict.label}", fontWeight = FontWeight.SemiBold)
                        Text("Sur cet appareil : ${conflict.localValue}", style = MaterialTheme.typography.bodySmall)
                        Text("Reçu : ${conflict.incomingValue}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onResolveConflict(context, conflict, false, passphrase) },
                                enabled = uiState.canWrite,
                                modifier = Modifier.weight(1f)
                            ) { Text("Garder ici") }
                            Button(
                                onClick = { onResolveConflict(context, conflict, true, passphrase) },
                                enabled = uiState.canWrite && passphrase.length >= 6,
                                modifier = Modifier.weight(1f)
                            ) { Text("Prendre la version reçue") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (uiState.syncConflictHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sync_conflict_history"),
                shape = LuminaTokens.ShapeSubCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Journal des arbitrages", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    uiState.syncConflictHistory.forEach { entry ->
                        Text("${entry.kind} : ${entry.label}", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (entry.resolution == "USE_IMPORTED") "Décision : version reçue appliquée"
                            else "Décision : version locale conservée",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Local : ${entry.localValue}  ·  Reçu : ${entry.incomingValue}", style = MaterialTheme.typography.labelSmall)
                        HorizontalDivider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Phrase de passe commune (6 caractères minimum)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("sync_passphrase")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    onExport(context, passphrase) { file -> onShareFile(context, file) }
                },
                enabled = passphrase.length >= 6,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("sync_export")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exporter")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("sync_import")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importer")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "L'échange chiffré inclut l'église, caisses, catégories, budgets, projets, écritures, membres et justificatifs. " +
                "Le nom et le rôle de l'expéditeur accompagnent le fichier et apparaissent comme contributeur à sa réception. " +
                "Aucun compte local ni PIN n'est exporté. Si un justificatif est inaccessible, l'export s'arrête sans fichier incomplet.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("church_invitation_card"),
            shape = LuminaTokens.ShapeSubCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Invitation légère pour un nouveau membre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                        "Ce petit fichier contient le nom, l'identité visuelle et l'identifiant de l'église, sans finances, écritures, membres ni comptes/PIN. " +
                        "Il est chiffré avec la phrase ci-dessus : transmettez-la séparément. La personne l'importe pendant l'accueil, choisit son rôle, crée son profil et son PIN, puis importe séparément un fichier d'échange si elle doit recevoir les données.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { onCreateInvitation(context, passphrase) { file -> onShareFile(context, file) } },
                    enabled = uiState.canWrite && passphrase.length >= 6,
                    modifier = Modifier.fillMaxWidth().testTag("create_church_invitation")
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer et partager l'invitation")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("cloud_backup_invitation_card"),
            shape = LuminaTokens.ShapeSubCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sauvegarde partagée de l'église", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Activez d'abord la sauvegarde cloud dans Paramètres. Puis créez un code à usage unique et transmettez-le en privé. Un appareil neuf doit d'abord importer l'invitation légère de l'église; ensuite, il accepte ce code et restaure la version partagée avant toute publication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        onCreateCloudInvite(context) { result ->
                            result.onSuccess {
                                cloudInviteCode = it
                                cloudInviteMessage = "Code valable 7 jours. Transmettez-le uniquement au membre concerné."
                            }.onFailure { cloudInviteMessage = it.message ?: "Création du code impossible" }
                        }
                    },
                    enabled = uiState.canWrite && !uiState.isDemoMode,
                    modifier = Modifier.fillMaxWidth().testTag("create_cloud_backup_invite")
                ) { Text("Créer un code d'invitation cloud") }
                if (cloudInviteCode.isNotBlank()) {
                    OutlinedTextField(
                        value = cloudInviteCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Code à transmettre en privé") },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_invite_code")
                    )
                }
                OutlinedTextField(
                    value = cloudInviteInput,
                    onValueChange = { cloudInviteInput = it.trim() },
                    label = { Text("Code reçu de l'église") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cloud_invite_input")
                )
                Button(
                    onClick = {
                        onAcceptCloudInvite(context, cloudInviteInput) { result ->
                            result.onSuccess {
                                cloudInviteMessage = "Appareil associé. Restaurez la sauvegarde cloud avant de publier vos données locales."
                                cloudInviteInput = ""
                            }.onFailure { cloudInviteMessage = it.message ?: "Invitation impossible à accepter" }
                        }
                    },
                    enabled = cloudInviteInput.isNotBlank() && !uiState.isDemoMode,
                    modifier = Modifier.fillMaxWidth().testTag("accept_cloud_backup_invite")
                ) { Text("Associer cet appareil") }
                if (cloudInviteMessage.isNotBlank()) {
                    Text(cloudInviteMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Fermer ma session")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // ------- Dialogue création -------
    if (creating) {
        UserDialog(
            title = "Nouvelle personne",
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { name, role, pin ->
                onAddUser(name, role, pin)
                creating = false
            }
        )
    }

    // ------- Dialogue édition -------
    editing?.let { user ->
        UserDialog(
            title = "Modifier ${user.displayName}",
            initial = user,
            onDismiss = { editing = null },
            onConfirm = { name, role, pin ->
                onUpdateUser(user, name, role, pin)
                editing = null
            }
        )
    }

    // ------- Confirmation suppression -------
    deleting?.let { user ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Retirer ${user.displayName} ?") },
            text = { Text("La personne ne pourra plus ouvrir l'application sur cet appareil. Les écritures déjà saisies restent intactes.") },
            confirmButton = {
                TextButton(onClick = { onDeleteUser(user); deleting = null }) { Text("Retirer") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annuler") } }
        )
    }

    // ------- Saisie de la phrase de passe pour importer -------
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Ouvrir le fichier d'échange") },
            text = {
                Column {
                    Text("Saisissez la phrase de passe utilisée lors de l'export.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Phrase de passe") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImport(context, uri, passphrase)
                        pendingImport = null
                    }
                ) { Text("Fusionner") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun UserDialog(
    title: String,
    initial: UserEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, UserRole, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.displayName ?: "") }
    var role by remember { mutableStateOf(initial?.roleValue ?: UserRole.TRESORIER) }
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom et prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("user_name_field")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Rôle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.entries.forEach { candidate ->
                        FilterChip(
                            selected = role == candidate,
                            onClick = { role = candidate },
                            label = { Text(candidate.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(if (initial == null) "Code PIN (optionnel)" else "Nouveau code (laisser vide pour conserver)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, role, pin) },
                enabled = name.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
