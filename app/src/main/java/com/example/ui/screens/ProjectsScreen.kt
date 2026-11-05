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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.ProjectSummary
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.GroupedCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Projets et fonds affectés : pour chaque projet, ce qui a été collecté,
 * dépensé, et ce qui reste disponible.
 */
@Composable
fun ProjectsScreen(
    projects: List<ProjectSummary>,
    transactions: List<TransactionEntity> = emptyList(),
    createSignal: Int = 0,
    onAddProject: (String, String, Long, String) -> Unit,
    onUpdateProject: (ProjectEntity, String, String, Long, Boolean, String) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit,
    onCreateConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectSummary?>(null) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }

    LaunchedEffect(createSignal) {
        if (createSignal > 0) {
            creating = true
            onCreateConsumed()
        }
    }

    if (selectedProject != null) {
        ProjectDetailPanel(
            project = selectedProject,
            transactions = transactions.filter {
                it.projectId == selectedProject.id && !it.isDeleted && !it.isNeutralised
            }.sortedByDescending { it.timestamp },
            onBack = { selectedProjectId = null },
            onEdit = {
                editing = ProjectEntity(
                    id = selectedProject.id,
                    uid = selectedProject.uid,
                    name = selectedProject.name,
                    description = selectedProject.description,
                    targetCents = selectedProject.targetCents,
                    isClosed = selectedProject.isClosed,
                    kind = selectedProject.kind
                )
            }
        )
    } else {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("projects_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Projets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Fonds affectés : collecté, dépensé, disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { creating = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nouveau", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aucun projet pour l'instant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Créez un projet (construction, mission, équipement…) puis rattachez-y " +
                            "les dons et les dépenses correspondants.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            val targetProjects = projects.filter { it.targetCents > 0L }.sortedByDescending { it.progress }.take(5)
            if (targetProjects.isNotEmpty()) {
                GroupedCard(title = "Avancement des objectifs", testTag = "project_progress_chart") {
                    targetProjects.forEachIndexed { index, project ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(project.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${(project.progress * 100).toInt()} %", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { project.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(LuminaTokens.ShapePill),
                                color = if (project.isClosed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                "Collecté ${CurrencyFormatter.format(project.collectedCents)} sur ${CurrencyFormatter.format(project.targetCents)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < targetProjects.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            projects.forEach { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { selectedProjectId = project.id }
                        .testTag("project_card_${project.id}"),
                    shape = LuminaTokens.ShapeCard,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (project.description.isNotBlank()) {
                                    Text(
                                        text = project.description,
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
                                    text = projectKindLabel(project.kind),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            if (project.isClosed) {
                                Surface(
                                    shape = LuminaTokens.ShapeBadge,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "clôturé",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    deleting = project
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AmountBlock("Collecté", project.collectedCents, IncomeGreen)
                            AmountBlock("Dépensé", project.spentCents, ExpenseRed)
                            AmountBlock("Disponible", project.balanceCents, MaterialTheme.colorScheme.onSurface)
                        }

                        if (project.targetCents > 0L) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { project.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(LuminaTokens.ShapePill),
                                color = IncomeGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Objectif : ${CurrencyFormatter.format(project.targetCents)} " +
                                    "(${(project.progress * 100).toInt()} %)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    }

    if (creating) {
        ProjectDialog(
            title = "Nouveau projet",
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { name, description, target, _, kind ->
                onAddProject(name, description, target, kind)
                creating = false
            }
        )
    }

    editing?.let { project ->
        ProjectDialog(
            title = "Modifier le projet",
            initial = project,
            onDismiss = { editing = null },
            onConfirm = { name, description, target, closed, kind ->
                onUpdateProject(project, name, description, target, closed, kind)
                editing = null
            }
        )
    }

    deleting?.let { project ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Supprimer ce projet ?") },
            text = {
                Text(
                    "Le projet « ${project.name} » sera retiré. Les écritures déjà rattachées " +
                        "sont conservées, sans affectation."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(
                            ProjectEntity(
                                id = project.id,
                                uid = project.uid,
                                name = project.name,
                                description = project.description,
                                targetCents = project.targetCents,
                                isClosed = project.isClosed,
                                kind = project.kind
                            )
                        )
                        deleting = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun ProjectDetailPanel(
    project: ProjectSummary,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRANCE) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("project_detail_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour aux projets")
            }
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Modifier")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = LuminaTokens.ShapeHero,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (project.isClosed) "DOSSIER CLÔTURÉ" else projectKindLabel(project.kind).uppercase(Locale.FRANCE),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(project.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (project.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AmountBlock("Dons collectés", project.collectedCents, IncomeGreen)
                    AmountBlock("Dépenses", project.spentCents, ExpenseRed)
                }
                Spacer(Modifier.height(12.dp))
                Text("Disponible : ${CurrencyFormatter.format(project.balanceCents)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                if (project.targetCents > 0L) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { project.progress },
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(LuminaTokens.ShapePill),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Objectif ${CurrencyFormatter.format(project.targetCents)} · ${(project.progress * 100).toInt()} % atteint",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Mouvements rattachés", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${transactions.size} écriture(s) · dons et dépenses affectés à ce dossier",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = LuminaTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Text(
                    "Aucune écriture rattachée pour le moment. Lors d'une nouvelle saisie, choisissez ce projet dans le champ d'affectation.",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            transactions.forEachIndexed { index, tx ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(if (tx.type == TransactionType.INCOME) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (tx.type == TransactionType.INCOME) "+" else "−", fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            "${tx.category} · ${dateFormat.format(Date(tx.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        CurrencyFormatter.format(tx.amountCents, withSign = true, isPositive = tx.type == TransactionType.INCOME),
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                    )
                }
                if (index < transactions.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

private fun projectKindLabel(kind: String): String = when (kind) {
    "CAMPAIGN" -> "Campagne"
    "EVENT" -> "Événement"
    "DONATION" -> "Collecte"
    else -> "Projet"
}

@Composable
private fun AmountBlock(label: String, value: Long, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = CurrencyFormatter.format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ProjectDialog(
    title: String,
    initial: ProjectEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var targetText by remember {
        mutableStateOf(if ((initial?.targetCents ?: 0L) > 0L) initial!!.targetCents.toString() else "")
    }
    var isClosed by remember { mutableStateOf(initial?.isClosed ?: false) }
    var kind by remember { mutableStateOf(initial?.kind ?: "PROJECT") }
    var kindMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du projet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("project_name_field")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    TextButton(onClick = { kindMenuExpanded = true }) {
                        Text("Type : ${projectKindLabel(kind)}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir le type")
                    }
                    DropdownMenu(
                        expanded = kindMenuExpanded,
                        onDismissRequest = { kindMenuExpanded = false }
                    ) {
                        listOf(
                            "PROJECT" to "Projet",
                            "CAMPAIGN" to "Campagne",
                            "EVENT" to "Événement",
                            "DONATION" to "Collecte / don dédié"
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { kind = value; kindMenuExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter { it.isDigit() } },
                    label = { Text("Objectif de collecte (FCFA, optionnel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = isClosed,
                            onCheckedChange = { isClosed = it }
                        )
                        Text("Projet clôturé", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, targetText.toLongOrNull() ?: 0L, isClosed, kind)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
