package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.localDateAtOffset
import com.example.util.sameLocalDate
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.ExpenseBgDark
import com.example.ui.theme.ExpenseBgLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.IncomeBgDark
import com.example.ui.theme.IncomeBgLight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity> = emptyList(),
    cashRegisters: List<CashRegisterEntity> = emptyList(),
    defaultCashRegisterId: Long? = null,
    /** Opération existante : le formulaire s'ouvre pré-rempli pour modification. */
    initial: TransactionEntity? = null,
    /** Projets ouverts à l'affectation (fonds fléchés). */
    projects: List<ProjectEntity> = emptyList(),
    workspaceId: String = "default",
    onDismissRequest: () -> Unit,
    onOpenCategoryManager: () -> Unit = {},
    onSaveTransaction: (
        title: String,
        type: TransactionType,
        amountCents: Long,
        category: String,
        timestamp: Long,
        note: String,
        cashRegisterId: Long,
        cashRegisterName: String,
        receiptPhotoUri: String,
        projectId: Long
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryMenuMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.42f)
        .coerceAtMost(320.dp)
        .coerceAtLeast(140.dp)
    var receiptPath by remember { mutableStateOf(initial?.receiptPhotoUri ?: "") }
    val receiptPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            com.example.util.ImageStore.uriToReceiptFile(context, uri, workspaceId)?.let { receiptPath = it }
        }
    }
    var amountText by remember { mutableStateOf(initial?.amountCents?.toString() ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategory by remember {
        mutableStateOf(
            initial?.category
                ?: filteredCategories.firstOrNull()?.name
                ?: if (selectedType == TransactionType.INCOME) "Dîmes" else "Aides & secours"
        )
    }

    LaunchedEffect(selectedType, filteredCategories) {
        if (filteredCategories.none { it.name == selectedCategory }) {
            selectedCategory = filteredCategories.firstOrNull()?.name ?: if (selectedType == TransactionType.INCOME) "Salaire" else "Alimentation"
        }
    }

    // Sélection de la caisse
    var selectedRegisterId by remember {
        val initialId = initial?.cashRegisterId
            ?: defaultCashRegisterId
            ?: cashRegisters.firstOrNull()?.id
            ?: 1L
        mutableLongStateOf(initialId)
    }

    var selectedProjectId by remember { mutableLongStateOf(initial?.projectId ?: 0L) }
    var registerMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isCustomCategorySelected by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var selectedTimestamp by remember {
        mutableLongStateOf(initial?.timestamp ?: System.currentTimeMillis())
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) "Nouvelle Opération" else "Modifier l'opération",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("cancel_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer le dialogue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sélecteur type avec une sélection contrastée, explicitement visible en thème clair.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionTypeOption(
                        type = TransactionType.EXPENSE,
                        selected = selectedType == TransactionType.EXPENSE,
                        label = "Sortie (-)",
                        modifier = Modifier.weight(1f).testTag("type_expense_button"),
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            isCustomCategorySelected = false
                        }
                    )
                    TransactionTypeOption(
                        type = TransactionType.INCOME,
                        selected = selectedType == TransactionType.INCOME,
                        label = "Entrée (+)",
                        modifier = Modifier.weight(1f).testTag("type_income_button"),
                        onClick = {
                            selectedType = TransactionType.INCOME
                            isCustomCategorySelected = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sélecteur de Caisse associée (Transparence par caisse)
                Text(
                    text = "Caisse de destination / prélèvement :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedRegisterName = cashRegisters.firstOrNull { it.id == selectedRegisterId }?.name
                        ?: cashRegisters.firstOrNull()?.name
                        ?: "Aucune caisse disponible"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .clickable(enabled = cashRegisters.isNotEmpty()) { registerMenuExpanded = true }
                            .testTag("transaction_register_dropdown"),
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Caisse sélectionnée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(selectedRegisterName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir une caisse", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(
                        expanded = registerMenuExpanded,
                        onDismissRequest = { registerMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 340.dp)
                    ) {
                        cashRegisters.forEach { register ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        register.name,
                                        fontWeight = if (selectedRegisterId == register.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedRegisterId = register.id
                                    registerMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.testTag("register_option_${register.id}")
                            )
                        }
                    }
                }

                val openProjects = projects.filter { !it.isClosed || it.id == selectedProjectId }
                if (openProjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PROJET / FONDS AFFECTÉ (OPTIONNEL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val selectedProject = openProjects.firstOrNull { it.id == selectedProjectId }
                    Box {
                        Button(
                            onClick = { projectMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                                Text(
                                    selectedProject?.let { project ->
                                        val kindLabel = when (project.kind) {
                                            "CAMPAIGN" -> "Campagne"
                                            "EVENT" -> "Événement"
                                            "DONATION" -> "Collecte"
                                            else -> "Projet"
                                        }
                                        "$kindLabel · ${project.name}"
                                    } ?: "Aucun dossier affecté",
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir un dossier")
                            }
                        }
                        DropdownMenu(
                            expanded = projectMenuExpanded,
                            onDismissRequest = { projectMenuExpanded = false },
                            modifier = Modifier.heightIn(max = categoryMenuMaxHeight)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Aucun dossier affecté") },
                                onClick = { selectedProjectId = 0L; projectMenuExpanded = false }
                            )
                            openProjects.forEach { project ->
                                val kindLabel = when (project.kind) {
                                    "CAMPAIGN" -> "Campagne"
                                    "EVENT" -> "Événement"
                                    "DONATION" -> "Collecte"
                                    else -> "Projet"
                                }
                                DropdownMenuItem(
                                    text = { Text("$kindLabel · ${project.name}") },
                                    onClick = { selectedProjectId = project.id; projectMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Champ Montant en FCFA
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.replace(" ", "").replace(',', '.')
                        if (filtered.count { it == '.' } <= 1 && filtered.all { it.isDigit() || it == '.' }) {
                            amountText = filtered
                            errorMessage = null
                        }
                    },
                    label = { Text("Montant (FCFA)") },
                    placeholder = { Text("ex: 25000") },
                    prefix = {
                        Text(
                            text = if (selectedType == TransactionType.INCOME) "+ " else "- ",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpenseRed
                        )
                    },
                    suffix = { Text("FCFA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))


                // Intitulé
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Intitulé / Motif") },
                    placeholder = { Text(if (selectedType == TransactionType.INCOME) "ex: Salaire, Prestation, Virement..." else "ex: Carburant, Vivres marché, SODECI...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section Catégories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catégorie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = onOpenCategoryManager) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gérer", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { categoryMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isCustomCategorySelected) {
                                    Box(
                                        Modifier.size(10.dp).clip(CircleShape)
                                            .background(parseHexColor(filteredCategories.firstOrNull { it.name == selectedCategory }?.colorHex ?: "#64748B"))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    if (isCustomCategorySelected) customCategory.ifBlank { "Catégorie personnalisée" } else selectedCategory,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir une catégorie")
                        }
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.heightIn(max = categoryMenuMaxHeight)
                    ) {
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(cat.colorHex)))
                                        Spacer(Modifier.width(10.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat.name
                                    isCustomCategorySelected = false
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Créer une catégorie ponctuelle") },
                            onClick = {
                                isCustomCategorySelected = true
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }

                if (isCustomCategorySelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Nom de la catégorie") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calendrier de date : toutes les dates passées restent accessibles.
                Text(
                    text = "Date de l'opération",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { showDatePicker = true }
                        .testTag("transaction_date_picker_button"),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            dateFormat.format(Date(selectedTimestamp)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Ouvrir le calendrier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val now = System.currentTimeMillis()
                    val yesterday = localDateAtOffset(now, -1)
                    TextButton(
                        onClick = { selectedTimestamp = now },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (sameLocalDate(selectedTimestamp, now)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) { Text("Aujourd'hui", fontSize = 12.sp) }
                    TextButton(
                        onClick = { selectedTimestamp = yesterday },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (sameLocalDate(selectedTimestamp, yesterday)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) { Text("Hier", fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Note / Mémo
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note facultative") },
                    placeholder = { Text("Détails supplémentaires...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Justificatif : photo ou scan de la facture (facultatif)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Justificatif (facultatif)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (receiptPath.isBlank()) {
                                "Photo ou scan de la facture, du reçu ou du bordereau"
                            } else {
                                "Pièce jointe à l'opération"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (receiptPath.isBlank()) {
                        androidx.compose.material3.TextButton(
                            onClick = { receiptPicker.launch("image/*") },
                            modifier = Modifier.testTag("attach_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Joindre")
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = { receiptPath = "" }) {
                            Text("Retirer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Message d'erreur
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bouton Enregistrer
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            errorMessage = "Veuillez saisir un montant valide en FCFA supérieur à 0."
                            return@Button
                        }
                        if (title.isBlank()) {
                            errorMessage = "Veuillez saisir un intitulé pour l'opération."
                            return@Button
                        }

                        val amountLong = Math.round(parsedAmount)
                        val finalCategory = if (isCustomCategorySelected && customCategory.isNotBlank()) {
                            customCategory.trim()
                        } else {
                            selectedCategory
                        }

                        val chosenRegister = cashRegisters.find { it.id == selectedRegisterId }
                        val regName = chosenRegister?.name ?: "Caisse Espèces"

                        onSaveTransaction(
                            title,
                            selectedType,
                            amountLong,
                            finalCategory,
                            selectedTimestamp,
                            note,
                            selectedRegisterId,
                            regName,
                            receiptPath,
                            selectedProjectId
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = com.example.ui.designsystem.LuminaTokens.ShapePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (selectedType == TransactionType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedType == TransactionType.INCOME) "Enregistrer l'Entrée (+)" else "Enregistrer la Sortie (-)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        LuminaDatePickerDialog(
            initialLocalDateMillis = selectedTimestamp,
            title = "Date de l'opération",
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedTimestamp = it; showDatePicker = false }
        )
    }
}

@Composable
private fun TransactionTypeOption(
    type: TransactionType,
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val semanticColor = when (type) {
        TransactionType.EXPENSE -> if (darkSurface) ExpenseRedLight else ExpenseRed
        TransactionType.INCOME -> if (darkSurface) IncomeGreenLight else IncomeGreen
    }
    val selectedSurface = when (type) {
        TransactionType.EXPENSE -> if (darkSurface) ExpenseBgDark else ExpenseBgLight
        TransactionType.INCOME -> if (darkSurface) IncomeBgDark else IncomeBgLight
    }
    val shape = RoundedCornerShape(11.dp)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) selectedSurface else Color.Transparent)
            .border(
                BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) semanticColor else MaterialTheme.colorScheme.outlineVariant
                ),
                shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (type == TransactionType.EXPENSE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = if (selected) semanticColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) semanticColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Type sélectionné",
                tint = semanticColor,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
