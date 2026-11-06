package com.example.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.ProjectEntity
import com.example.data.ProjectActionItem
import com.example.data.decodeProjectActionItems
import com.example.data.encodeProjectActionItems
import com.example.data.moveProjectActionItem
import com.example.ui.components.LuminaDateRangePickerPanel
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
import java.util.Calendar
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
    onAddProject: (String, String, Long, String, Long?, Long?, String) -> Unit,
    onUpdateProject: (ProjectEntity, String, String, Long, Boolean, String, Long?, Long?, String) -> Unit,
    onUpdateProjectActions: (ProjectEntity, String) -> Unit = { _, _ -> },
    onDeleteProject: (ProjectEntity) -> Unit,
    onCreateConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectSummary?>(null) }
    var selectedProject by remember { mutableStateOf<ProjectSummary?>(null) }

    LaunchedEffect(createSignal) {
        if (createSignal > 0) {
            creating = true
            onCreateConsumed()
        }
    }

    if (selectedProject != null) {
        val project = requireNotNull(selectedProject)
        ProjectDetailPanel(
            project = project,
            transactions = transactions.filter {
                it.projectId == project.id && !it.isDeleted && !it.isNeutralised
            }.sortedByDescending { it.timestamp },
            onBack = { selectedProject = null },
            onUpdateActions = { actionItemsJson -> onUpdateProjectActions(project.toEntity(), actionItemsJson) },
            onEdit = { editing = project.toEntity() }
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
                GroupedCard(title = "Progression des projets et budgets", testTag = "project_progress_chart") {
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
                                if (project.kind == "EVENT") "Dépensé ${CurrencyFormatter.format(project.spentCents)} sur le budget ${CurrencyFormatter.format(project.targetCents)}"
                                else "Collecté ${CurrencyFormatter.format(project.collectedCents)} sur ${CurrencyFormatter.format(project.targetCents)}",
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
                    onClick = { selectedProject = project },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
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
                            AmountBlock(if (project.kind == "EVENT") "Entrées" else "Collecté", project.collectedCents, IncomeGreen)
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
                                text = if (project.kind == "EVENT") {
                                    "Budget : ${CurrencyFormatter.format(project.targetCents)} (${(project.progress * 100).toInt()} % dépensé)"
                                } else {
                                    "Objectif : ${CurrencyFormatter.format(project.targetCents)} (${(project.progress * 100).toInt()} %)"
                                },
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
            onConfirm = { name, description, target, _, kind, start, end, actions ->
                onAddProject(name, description, target, kind, start, end, actions)
                creating = false
            }
        )
    }

    editing?.let { project ->
        ProjectDialog(
            title = "Modifier le projet",
            initial = project,
            onDismiss = { editing = null },
            onConfirm = { name, description, target, closed, kind, start, end, actions ->
                onUpdateProject(project, name, description, target, closed, kind, start, end, actions)
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
                        onDeleteProject(project.toEntity())
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
    onUpdateActions: (String) -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRANCE) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(remember(project.id) { ScrollState(0) })
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

        if (project.kind == "EVENT") {
            ProjectEventWorkspaceCard(project = project, onActionsChanged = onUpdateActions)
        } else {
            ProjectIdentityCard(project)
            Spacer(Modifier.height(12.dp))
            ProjectFinanceCard(project, event = false)
        }

        Spacer(Modifier.height(20.dp))
        Text("Historique des mouvements affectés", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${transactions.size} mouvement(s) · entrées et sorties rattachées à ce dossier",
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
                    "Aucune entrée ni sortie n'est encore affectée à ce dossier. À la saisie d'une opération, sélectionnez cet événement dans « Projet / fonds affecté ».",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val incomes = transactions.filter { it.type == TransactionType.INCOME }.sortedBy { it.timestamp }
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sortedBy { it.timestamp }
            if (incomes.isNotEmpty()) {
                Text("ENTRÉES AFFECTÉES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                incomes.forEachIndexed { index, tx ->
                    ProjectMovementRow(tx, dateFormat)
                    if (index < incomes.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (expenses.isNotEmpty()) {
                if (incomes.isNotEmpty()) Spacer(Modifier.height(12.dp))
                Text("SORTIES AFFECTÉES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                expenses.forEachIndexed { index, tx ->
                    ProjectMovementRow(tx, dateFormat)
                    if (index < expenses.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ProjectIdentityCard(project: ProjectSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LuminaTokens.ShapeHero,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        ProjectIdentityContent(project, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun ProjectEventWorkspaceCard(
    project: ProjectSummary,
    onActionsChanged: (String) -> Unit
) {
    GroupedCard(title = "Fiche complète de l'événement", testTag = "event_management_card") {
        ProjectIdentityContent(project, modifier = Modifier.padding(horizontal = 2.dp))
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 620.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    ProjectFinanceCard(project, event = true, modifier = Modifier.weight(0.92f))
                    ProjectEventCalendar(
                        startAtMillis = project.startAtMillis,
                        endAtMillis = project.endAtMillis,
                        modifier = Modifier.weight(1.08f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProjectFinanceCard(project, event = true)
                    ProjectEventCalendar(project.startAtMillis, project.endAtMillis)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        EventActionsCard(
            projectId = project.id,
            actionItemsJson = project.actionItemsJson,
            onActionsChanged = onActionsChanged
        )
    }
}

@Composable
private fun ProjectIdentityContent(project: ProjectSummary, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            if (project.isClosed) "DOSSIER CLÔTURÉ" else projectKindLabel(project.kind).uppercase(Locale.FRANCE),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(project.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        if (project.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (project.kind == "EVENT") {
            val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.FRANCE) }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (project.startAtMillis != null && project.endAtMillis != null) {
                        "${dateFormat.format(Date(project.startAtMillis))}  –  ${dateFormat.format(Date(project.endAtMillis))}"
                    } else "Période à définir · Modifier pour choisir les dates",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProjectFinanceCard(project: ProjectSummary, event: Boolean, modifier: Modifier = Modifier) {
    GroupedCard(
        modifier = modifier,
        title = if (event) "Finances de l'événement" else "Situation financière",
        testTag = if (event) "event_finance_card" else "project_finance_card"
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AmountBlock(if (event) "Entrées" else "Dons collectés", project.collectedCents, IncomeGreen)
            AmountBlock(if (event) "Sorties" else "Dépenses", project.spentCents, ExpenseRed)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Disponible : ${CurrencyFormatter.format(project.balanceCents)}",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
        )
        if (project.targetCents > 0L) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = {
                    if (event) (project.spentCents.toDouble() / project.targetCents.toDouble()).toFloat().coerceIn(0f, 1f)
                    else project.progress
                },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(LuminaTokens.ShapePill),
                color = if (event) ExpenseRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(5.dp))
            if (event) {
                val remaining = (project.targetCents - project.spentCents).coerceAtLeast(0L)
                Text("Budget défini : ${CurrencyFormatter.format(project.targetCents)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Dépensé ${CurrencyFormatter.format(project.spentCents)} · reste ${CurrencyFormatter.format(remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Objectif ${CurrencyFormatter.format(project.targetCents)} · ${(project.progress * 100).toInt()} % collecté",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (event) {
            Text("Aucun budget défini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectMovementRow(transaction: TransactionEntity, dateFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape)
                .background(if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(if (transaction.type == TransactionType.INCOME) "+" else "−", fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                "${dateFormat.format(Date(transaction.timestamp))} · ${transaction.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transaction.cashRegisterName.isNotBlank()) {
                Text(transaction.cashRegisterName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            CurrencyFormatter.format(transaction.amountCents, withSign = true, isPositive = transaction.type == TransactionType.INCOME),
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
        )
    }
}

@Composable
private fun ProjectEventCalendar(startAtMillis: Long?, endAtMillis: Long?, modifier: Modifier = Modifier) {
    val initialMonth = remember(startAtMillis, endAtMillis) {
        Calendar.getInstance().apply {
            timeInMillis = startAtMillis ?: System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var displayedMonth by remember(startAtMillis, endAtMillis) { mutableStateOf(initialMonth) }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.FRANCE) }
    val shortDateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.FRANCE) }
    val accessibleDateFormatter = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE) }
    val rangeLabel = if (startAtMillis != null && endAtMillis != null) {
        "${shortDateFormatter.format(Date(startAtMillis))}  –  ${shortDateFormatter.format(Date(endAtMillis))}"
    } else "Dates non définies — modifiez l'événement pour choisir sa période."
    val monthCalendar = Calendar.getInstance().apply { timeInMillis = displayedMonth.timeInMillis }
    val year = monthCalendar.get(Calendar.YEAR)
    val month = monthCalendar.get(Calendar.MONTH)
    val firstDayCalendar = Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 12, 0, 0)
    }
    val firstOffset = (firstDayCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday first.
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startKey = startAtMillis?.let(::projectDateKey)
    val endKey = endAtMillis?.let(::projectDateKey)

    GroupedCard(modifier = modifier, title = "Calendrier de l'événement", testTag = "project_event_calendar") {
        Text(rangeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                displayedMonth = Calendar.getInstance().apply {
                    timeInMillis = displayedMonth.timeInMillis
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Mois précédent")
            }
            Text(
                monthFormatter.format(firstDayCalendar.time).replaceFirstChar { it.uppercase(Locale.FRANCE) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                displayedMonth = Calendar.getInstance().apply {
                    timeInMillis = displayedMonth.timeInMillis
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Mois suivant")
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f).padding(vertical = 3.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        repeat(6) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val dayNumber = week * 7 + weekday - firstOffset + 1
                    val dateMillis = if (dayNumber in 1..daysInMonth) {
                        Calendar.getInstance().apply {
                            clear()
                            set(year, month, dayNumber, 12, 0, 0)
                        }.timeInMillis
                    } else null
                    val dateKey = dateMillis?.let(::projectDateKey)
                    val inRange = dateKey != null && startKey != null && endKey != null && dateKey in startKey..endKey
                    val isEndpoint = dateMillis != null && (
                        (startAtMillis != null && sameProjectDate(dateMillis, startAtMillis)) ||
                            (endAtMillis != null && sameProjectDate(dateMillis, endAtMillis))
                        )
                    val cellColor = when {
                        isEndpoint -> MaterialTheme.colorScheme.primary
                        inRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                        else -> Color.Transparent
                    }
                    val numberColor = when {
                        isEndpoint -> MaterialTheme.colorScheme.onPrimary
                        inRange -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(cellColor)
                            .semantics {
                                if (dateMillis != null) contentDescription = accessibleDateFormatter.format(Date(dateMillis))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (dateMillis != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    dayNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = numberColor,
                                    fontWeight = if (isEndpoint) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                        if (inRange) {
                                    Box(
                                        Modifier.size(6.dp).clip(CircleShape)
                                            .background(if (isEndpoint) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    )
                                } else Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(7.dp))
            Text(
                "Un point marque chaque jour inclus; les pastilles pleines indiquent le début et la fin.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventActionsCard(
    projectId: Long,
    actionItemsJson: String,
    onActionsChanged: (String) -> Unit
) {
    var newActionTitle by remember(projectId) { mutableStateOf("") }
    var newSubtaskTitle by remember(projectId) { mutableStateOf("") }
    var subtaskParentId by remember(projectId) { mutableStateOf<String?>(null) }
    var actions by remember(actionItemsJson) { mutableStateOf(decodeProjectActionItems(actionItemsJson)) }
    val allItems = actions.flatMap { listOf(it) + it.subTasks }
    val pendingCount = allItems.count { !it.isDone }
    val completedCount = allItems.size - pendingCount

    fun saveActions(updated: List<ProjectActionItem>) {
        actions = updated
        onActionsChanged(encodeProjectActionItems(updated))
    }

    GroupedCard(title = "Tâches et sous-tâches", testTag = "project_action_items") {
        Text(
            if (actions.isEmpty()) "Ajoutez les tâches de préparation, puis cochez-les au fur et à mesure."
            else "$completedCount terminée(s) · $pendingCount restante(s) · ${allItems.size} au total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (allItems.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (allItems.isEmpty()) 0f else completedCount.toFloat() / allItems.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(LuminaTokens.ShapePill)
            )
            Text(
                "Maintenez la poignée ⋮⋮ et faites glisser une tâche ou sous-tâche pour changer son ordre.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 4.dp)
            )
        }
        actions.forEachIndexed { actionIndex, action ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("project_action_${action.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = action.isDone,
                    onCheckedChange = { checked ->
                        saveActions(updateProjectActionItems(actions, action.id) { it.copy(isDone = checked) })
                    }
                )
                Text(
                    action.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (action.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (action.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                DragReorderHandle(
                    itemKey = action.id,
                    canMoveUp = actionIndex > 0,
                    canMoveDown = actionIndex < actions.lastIndex,
                    onMoveUp = { saveActions(moveProjectActionItem(actions, actionIndex, -1)) },
                    onMoveDown = { saveActions(moveProjectActionItem(actions, actionIndex, 1)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${action.subTasks.size} sous-tâche(s)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    subtaskParentId = if (subtaskParentId == action.id) null else action.id
                    newSubtaskTitle = ""
                }) { Text(if (subtaskParentId == action.id) "Annuler" else "+ sous-tâche") }
            }
            action.subTasks.forEachIndexed { subtaskIndex, subtask ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp).testTag("project_subtask_${subtask.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = subtask.isDone,
                        onCheckedChange = { checked ->
                            saveActions(updateProjectActionItems(actions, subtask.id) { it.copy(isDone = checked) })
                        }
                    )
                    Text(
                        subtask.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (subtask.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (subtask.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    DragReorderHandle(
                        itemKey = subtask.id,
                        canMoveUp = subtaskIndex > 0,
                        canMoveDown = subtaskIndex < action.subTasks.lastIndex,
                        onMoveUp = {
                            val moved = moveProjectActionItem(action.subTasks, subtaskIndex, -1)
                            saveActions(actions.map { if (it.id == action.id) it.copy(subTasks = moved) else it })
                        },
                        onMoveDown = {
                            val moved = moveProjectActionItem(action.subTasks, subtaskIndex, 1)
                            saveActions(actions.map { if (it.id == action.id) it.copy(subTasks = moved) else it })
                        }
                    )
                }
            }
            if (subtaskParentId == action.id) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        label = { Text("Nouvelle sous-tâche") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_project_subtask_field")
                    )
                    IconButton(
                        onClick = {
                            val cleanTitle = newSubtaskTitle.trim()
                            if (cleanTitle.isNotEmpty()) {
                                saveActions(actions.map { item ->
                                    if (item.id == action.id) item.copy(subTasks = item.subTasks + ProjectActionItem(title = cleanTitle)) else item
                                })
                                newSubtaskTitle = ""
                                subtaskParentId = null
                            }
                        },
                        enabled = newSubtaskTitle.isNotBlank(),
                        modifier = Modifier.testTag("add_project_subtask")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter la sous-tâche", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newActionTitle,
                onValueChange = { newActionTitle = it },
                label = { Text("Ajouter une tâche") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("new_project_action_field")
            )
            IconButton(
                onClick = {
                    val cleanTitle = newActionTitle.trim()
                    if (cleanTitle.isNotEmpty()) {
                        saveActions(actions + ProjectActionItem(title = cleanTitle))
                        newActionTitle = ""
                    }
                },
                enabled = newActionTitle.isNotBlank(),
                modifier = Modifier.testTag("add_project_action")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter la tâche", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DragReorderHandle(
    itemKey: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var accumulatedDrag by remember(itemKey) { mutableStateOf(0f) }
    val thresholdPx = with(LocalDensity.current) { 38.dp.toPx() }
    val canMoveUpState by rememberUpdatedState(canMoveUp)
    val canMoveDownState by rememberUpdatedState(canMoveDown)
    val moveUpState by rememberUpdatedState(onMoveUp)
    val moveDownState by rememberUpdatedState(onMoveDown)
    val dragState = rememberDraggableState { delta ->
        accumulatedDrag += delta
        if (accumulatedDrag <= -thresholdPx && canMoveUpState) {
            moveUpState()
            accumulatedDrag += thresholdPx
        } else if (accumulatedDrag >= thresholdPx && canMoveDownState) {
            moveDownState()
            accumulatedDrag -= thresholdPx
        }
    }
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = if (canMoveUp || canMoveDown) "Maintenir et glisser pour réorganiser" else "Ordre fixe",
        tint = if (canMoveUp || canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.size(40.dp).draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            enabled = canMoveUp || canMoveDown,
            onDragStarted = { accumulatedDrag = 0f },
            onDragStopped = { accumulatedDrag = 0f }
        )
    )
}

private fun updateProjectActionItems(
    items: List<ProjectActionItem>,
    targetId: String,
    update: (ProjectActionItem) -> ProjectActionItem
): List<ProjectActionItem> = items.map { item ->
    if (item.id == targetId) update(item)
    else item.copy(subTasks = updateProjectActionItems(item.subTasks, targetId, update))
}

private fun projectDateKey(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }
    .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }

private fun sameProjectDate(firstMillis: Long, secondMillis: Long): Boolean =
    projectDateKey(firstMillis) == projectDateKey(secondMillis)

private fun ProjectSummary.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    uid = uid,
    name = name,
    description = description,
    targetCents = targetCents,
    isClosed = isClosed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    kind = kind,
    startAtMillis = startAtMillis,
    endAtMillis = endAtMillis,
    actionItemsJson = actionItemsJson
)

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
    onConfirm: (String, String, Long, Boolean, String, Long?, Long?, String) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var targetText by remember(initial?.id) {
        mutableStateOf(initial?.targetCents?.takeIf { it > 0L }?.toString() ?: "")
    }
    var isClosed by remember(initial?.id) { mutableStateOf(initial?.isClosed ?: false) }
    var kind by remember(initial?.id) { mutableStateOf(initial?.kind ?: "PROJECT") }
    var kindMenuExpanded by remember { mutableStateOf(false) }
    var startAtMillis by remember(initial?.id) { mutableStateOf(initial?.startAtMillis) }
    var endAtMillis by remember(initial?.id) { mutableStateOf(initial?.endAtMillis) }
    var showDateRangePicker by remember(initial?.id) { mutableStateOf(false) }
    var actionText by remember(initial?.id) {
        mutableStateOf(decodeProjectActionItems(initial?.actionItemsJson ?: "[]").joinToString("\n") { it.title })
    }
    val projectDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRANCE) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = if (showDateRangePicker) {
                Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.94f)
            } else {
                Modifier.fillMaxWidth(0.95f).heightIn(max = 760.dp)
            },
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            if (showDateRangePicker) {
                // The Material date picker contains its own vertical scrolling calendar.
                // Do not put it under the form's verticalScroll: Compose can measure that
                // nested scrollable with infinite height and crash the Activity.
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Période de l'événement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showDateRangePicker = false }) { Text("Retour") }
                    }
                    LuminaDateRangePickerPanel(
                        initialStartLocalMillis = startAtMillis,
                        initialEndLocalMillis = endAtMillis,
                        onCancel = { showDateRangePicker = false },
                        onRangeSelected = { start, end ->
                            startAtMillis = start
                            endAtMillis = end
                            showDateRangePicker = false
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onDismiss) { Text("Fermer") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du projet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("project_name_field")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
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
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { input -> targetText = input.filter { it.isDigit() } },
                        label = { Text(if (kind == "EVENT") "Budget prévu (FCFA, optionnel)" else "Objectif de collecte (FCFA, optionnel)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (kind == "EVENT") {
                        Spacer(Modifier.height(10.dp))
                        Text("DATES DE L'ÉVÉNEMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showDateRangePicker = true }
                                .testTag("project_date_range_button"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    "Début   ${startAtMillis?.let { projectDateFormat.format(Date(it)) } ?: "À définir"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Fin       ${endAtMillis?.let { projectDateFormat.format(Date(it)) } ?: "À définir"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (initial == null) {
                            OutlinedTextField(
                                value = actionText,
                                onValueChange = { actionText = it },
                                label = { Text("Premières tâches (une par ligne)") },
                                placeholder = { Text("Ex. Réserver la salle\nPréparer le programme") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("project_actions_field")
                            )
                        } else {
                            Text(
                                "Les tâches et sous-tâches se gèrent dans la fiche de l'événement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (initial != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = isClosed,
                                onCheckedChange = { isClosed = it }
                            )
                            Text("Projet clôturé", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Annuler") }
                        TextButton(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val actionItemsJson = when {
                                        kind != "EVENT" -> "[]"
                                        initial?.kind == "EVENT" -> encodeProjectActionItems(decodeProjectActionItems(initial.actionItemsJson))
                                        else -> encodeProjectActionItems(actionText.lines().mapNotNull { line ->
                                            line.trim().takeIf(String::isNotBlank)?.let { ProjectActionItem(title = it) }
                                        })
                                    }
                                    onConfirm(
                                        name,
                                        description,
                                        targetText.toLongOrNull() ?: 0L,
                                        isClosed,
                                        kind,
                                        if (kind == "EVENT") startAtMillis else null,
                                        if (kind == "EVENT") endAtMillis else null,
                                        actionItemsJson
                                    )
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.testTag("save_project_button")
                        ) { Text("Enregistrer") }
                    }
                }
            }
        }
    }
}
