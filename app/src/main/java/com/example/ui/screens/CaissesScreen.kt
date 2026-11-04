package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.window.Dialog
import com.example.ui.CashRegisterSummary
import com.example.ui.FinancialUiState
import com.example.ui.designsystem.HeroCard
import com.example.ui.designsystem.IconTile
import com.example.ui.designsystem.LuminaTokens
import com.example.ui.designsystem.PillButton
import com.example.ui.designsystem.SectionHeader
import com.example.ui.designsystem.StatTrio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import com.example.util.CurrencyFormatter
import androidx.compose.ui.graphics.Color

@Composable
fun CaissesScreen(
    uiState: FinancialUiState,
    onSelectCaisseFilter: (Long?) -> Unit,
    onAddCaisse: (name: String, description: String, initialBalance: Long, iconType: String) -> Unit,
    onUpdateCaisse: (id: Long, name: String, description: String, initialBalance: Long, iconType: String, isDefault: Boolean) -> Unit,
    onDeleteCaisse: (Long) -> Unit,
    onTransferBetweenCaisses: (sourceId: Long, targetId: Long, amountCents: Long, note: String) -> Unit,
    createSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    var showAddCaisseDialog by remember { mutableStateOf(false) }
    var caisseToEdit by remember { mutableStateOf<CashRegisterSummary?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var caisseToDelete by remember { mutableStateOf<CashRegisterSummary?>(null) }
    var selectedCaisse by remember { mutableStateOf<CashRegisterSummary?>(null) }
    var transferSourceId by remember { mutableStateOf<Long?>(null) }

    // Le bouton flottant de l'application ouvre la création d'une caisse
    LaunchedEffect(createSignal) {
        if (createSignal > 0) {
            caisseToEdit = null
            showAddCaisseDialog = true
        }
    }

    val detail = selectedCaisse
    if (detail != null) {
        CaisseDetailScreen(
            caisse = uiState.cashRegisterSummaries.firstOrNull { it.id == detail.id } ?: detail,
            transactions = uiState.transactions,
            displayMode = uiState.amountDisplayMode,
            onBack = { selectedCaisse = null },
            onEdit = { caisseToEdit = detail; showAddCaisseDialog = true },
            onDelete = { caisseToDelete = detail },
            onTransfer = { transferSourceId = detail.id; showTransferDialog = true }
        )
    } else {
    // Une seule page, un seul plan : l'en-tête défile avec le reste.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LuminaTokens.SpacingDefault, vertical = LuminaTokens.SpacingMedium)
            .testTag("caisses_screen")
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Système de Caisses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Traçabilité intégrale de chaque flux par caisse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Bouton Virement
                IconButton(
                    onClick = { showTransferDialog = true },
                    modifier = Modifier.testTag("transfer_caisse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Virement inter-caisses",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Bouton Nouvelle Caisse
                IconButton(
                    onClick = {
                        caisseToEdit = null
                        showAddCaisseDialog = true
                    },
                    modifier = Modifier.testTag("new_caisse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouvelle Caisse",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Carte Héro Solde Total Consolidé (Toutes les Caisses)
        HeroCard(
            eyebrow = "Trésorerie Globale",
            primaryAmount = CurrencyFormatter.format(uiState.totalCashBalanceCents, uiState.amountDisplayMode),
            deltaText = "${uiState.cashRegisterSummaries.size} caisses actives",
            isDeltaPositive = true,
            deltaDescription = "Liquidités & comptes",
            testTag = "total_cash_card",
            trailingBadge = {
                Surface(
                    shape = LuminaTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Multi-Caisses",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            nestedContent = {
                // Filtre actif de caisse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LuminaTokens.ShapeSubCard)
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtre actif : ${uiState.selectedCashRegisterName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    if (uiState.selectedCashRegisterId != null) {
                        Surface(
                            shape = LuminaTokens.ShapePill,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.clickable { onSelectCaisseFilter(null) }
                        ) {
                            Text(
                                text = "Réinitialiser",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingDefault))

        // Actions en duo (Pill Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillButton(
                text = "Virement interne",
                icon = Icons.Default.SwapHoriz,
                onClick = { showTransferDialog = true },
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Nouvelle Caisse",
                icon = Icons.Default.Add,
                onClick = {
                    caisseToEdit = null
                    showAddCaisseDialog = true
                },
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(LuminaTokens.SpacingLarge))

        // Section Titre
        SectionHeader(
            title = "Détail des Caisses & Relevés"
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.cashRegisterSummaries.forEach { caisse ->
                val isSelected = uiState.selectedCashRegisterId == caisse.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCaisse = caisse }
                        .border(
                            width = if (isSelected) LuminaTokens.BorderWidthActive else LuminaTokens.BorderWidthThin,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = LuminaTokens.ShapeCard
                        )
                        .testTag("caisse_item_${caisse.id}"),
                    shape = LuminaTokens.ShapeCard,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(LuminaTokens.SpacingDefault)) {
                        // En-tête de la caisse
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconTile(
                                    icon = when (caisse.iconType) {
                                        "mobile" -> Icons.Default.PhoneAndroid
                                        "bank" -> Icons.Default.AccountBalance
                                        "vault" -> Icons.Default.Savings
                                        else -> Icons.Default.Payments
                                    },
                                    contentDescription = null,
                                    size = 40.dp,
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = caisse.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (caisse.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = LuminaTokens.ShapeBadge,
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "Par défaut",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (caisse.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = caisse.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        caisseToEdit = caisse
                                        showAddCaisseDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!caisse.isDefault) {
                                    IconButton(
                                        onClick = { caisseToDelete = caisse },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trio de statistiques (StatTrio)
                        StatTrio(
                            item1Label = "Solde initial",
                            item1Value = CurrencyFormatter.format(caisse.initialBalanceCents, uiState.amountDisplayMode),
                            item2Label = "Entrées (+)",
                            item2Value = "+${CurrencyFormatter.format(caisse.totalIncomeCents, uiState.amountDisplayMode)}",
                            item2Color = MaterialTheme.colorScheme.primary,
                            item3Label = "Sorties (-)",
                            item3Value = "-${CurrencyFormatter.format(caisse.totalExpenseCents, uiState.amountDisplayMode)}",
                            item3Color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Solde net disponible & bouton d'isolation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Solde disponible :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(caisse.currentBalanceCents, uiState.amountDisplayMode),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = {
                                    if (isSelected) onSelectCaisseFilter(null) else onSelectCaisseFilter(caisse.id)
                                },
                                shape = LuminaTokens.ShapePill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(if (isSelected) "Filtré ✓" else "Isoler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
    }

    // Dialogue Créer / Modifier Caisse
    if (showAddCaisseDialog) {
        AddEditCaisseDialog(
            initialCaisse = caisseToEdit,
            onDismissRequest = {
                showAddCaisseDialog = false
                caisseToEdit = null
            },
            onSave = { name, desc, initialBal, icon ->
                if (caisseToEdit != null) {
                    onUpdateCaisse(caisseToEdit!!.id, name, desc, initialBal, icon, caisseToEdit!!.isDefault)
                } else {
                    onAddCaisse(name, desc, initialBal, icon)
                }
            }
        )
    }

    // Dialogue Virement Inter-Caisses
    if (showTransferDialog) {
        TransferCaisseDialog(
            caisses = uiState.cashRegisterSummaries,
            initialSourceId = transferSourceId,
            onDismissRequest = { showTransferDialog = false; transferSourceId = null },
            onTransfer = { sourceId, targetId, amount, note ->
                onTransferBetweenCaisses(sourceId, targetId, amount, note)
            }
        )
    }

    // Confirmation de suppression
    if (caisseToDelete != null) {
        val reg = caisseToDelete!!
        AlertDialog(
            onDismissRequest = { caisseToDelete = null },
            title = { Text("Supprimer la caisse ?") },
            text = { Text("Voulez-vous supprimer la caisse \"${reg.name}\" (${CurrencyFormatter.format(reg.currentBalanceCents)}) ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCaisse(reg.id)
                        if (selectedCaisse?.id == reg.id) selectedCaisse = null
                        caisseToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { caisseToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AddEditCaisseDialog(
    initialCaisse: CashRegisterSummary?,
    onDismissRequest: () -> Unit,
    onSave: (name: String, description: String, initialBalance: Long, iconType: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCaisse?.name ?: "") }
    var description by remember { mutableStateOf(initialCaisse?.description ?: "") }
    var initialBalanceText by remember {
        mutableStateOf(if (initialCaisse != null && initialCaisse.initialBalanceCents > 0) initialCaisse.initialBalanceCents.toString() else "")
    }
    var selectedIcon by remember { mutableStateOf(initialCaisse?.iconType ?: "cash") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = LuminaTokens.ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(LuminaTokens.SpacingLarge)) {
                Text(
                    text = if (initialCaisse != null) "Modifier la Caisse" else "Nouvelle Caisse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nom de la caisse") },
                    placeholder = { Text("ex: Caisse Espèces, Wave, BOA...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caisse_name_input"),
                    shape = LuminaTokens.ShapeSubCard
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            initialBalanceText = input
                            error = null
                        }
                    },
                    label = { Text("Solde initial (FCFA)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caisse_initial_balance_input"),
                    shape = LuminaTokens.ShapeSubCard
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Usage") },
                    placeholder = { Text("ex: Pour dépenses quotidiennes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuminaTokens.ShapeSubCard
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Icône :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash" to "Espèces", "mobile" to "Mobile", "bank" to "Banque", "vault" to "Coffre").forEach { (type, label) ->
                        FilterChip(
                            selected = selectedIcon == type,
                            onClick = { selectedIcon = type },
                            label = { Text(label) },
                            shape = LuminaTokens.ShapePill
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                error = "Veuillez renseigner un nom pour la caisse"
                                return@Button
                            }
                            val initialBal = initialBalanceText.toLongOrNull() ?: 0L
                            onSave(name, description, initialBal, selectedIcon)
                            onDismissRequest()
                        },
                        shape = LuminaTokens.ShapePill,
                        modifier = Modifier.testTag("save_caisse_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun TransferCaisseDialog(
    caisses: List<CashRegisterSummary>,
    initialSourceId: Long? = null,
    onDismissRequest: () -> Unit,
    onTransfer: (sourceId: Long, targetId: Long, amountCents: Long, note: String) -> Unit
) {
    var sourceId by remember {
        mutableStateOf(initialSourceId ?: caisses.firstOrNull()?.id ?: 1L)
    }
    var targetId by remember {
        mutableStateOf(
            caisses.firstOrNull { it.id != (initialSourceId ?: caisses.firstOrNull()?.id) }?.id
                ?: caisses.getOrNull(1)?.id
                ?: caisses.firstOrNull()?.id
                ?: 1L
        )
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val source = caisses.firstOrNull { it.id == sourceId }
    val target = caisses.firstOrNull { it.id == targetId }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = LuminaTokens.ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .testTag("transfer_dialog")
            ) {
                Text(
                    text = "Virement entre caisses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Le montant quitte une caisse et arrive dans l'autre, en une seule opération traçable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                CaisseSelector(
                    label = "Depuis la caisse",
                    selected = source,
                    options = caisses,
                    onSelect = { picked ->
                        sourceId = picked.id
                        if (targetId == picked.id) {
                            targetId = caisses.firstOrNull { it.id != picked.id }?.id ?: targetId
                        }
                        error = null
                    },
                    testTag = "transfer_source_selector"
                )

                // Bouton d'inversion
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val oldSource = sourceId
                            sourceId = targetId
                            targetId = oldSource
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Inverser les caisses",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                CaisseSelector(
                    label = "Vers la caisse",
                    selected = target,
                    options = caisses,
                    onSelect = { picked ->
                        targetId = picked.id
                        if (sourceId == picked.id) {
                            sourceId = caisses.firstOrNull { it.id != picked.id }?.id ?: sourceId
                        }
                        error = null
                    },
                    testTag = "transfer_target_selector"
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            amountText = input
                            error = null
                        }
                    },
                    label = { Text("Montant (FCFA)") },
                    placeholder = { Text("ex : 50 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    shape = LuminaTokens.ShapeSubCard
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Motif (optionnel)") },
                    placeholder = { Text("Approvisionnement, dépôt…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuminaTokens.ShapeSubCard
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        when {
                            source == null || target == null -> error = "Choisissez les deux caisses"
                            source.id == target.id -> error = "Les deux caisses doivent être différentes"
                            amount <= 0L -> error = "Saisissez un montant valide"
                            source.currentBalanceCents < amount ->
                                error = "Solde insuffisant dans « ${source.name} »"
                            else -> {
                                onTransfer(source.id, target.id, amount, note)
                                onDismissRequest()
                            }
                        }
                    },
                    shape = LuminaTokens.ShapePill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_transfer_button")
                ) {
                    Text("Effectuer le virement", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler")
                }
            }
        }
    }
}

/** Menu déroulant de sélection d'une caisse : nom, solde, et coche sur la sélection. */
@Composable
private fun CaisseSelector(
    label: String,
    selected: CashRegisterSummary?,
    options: List<CashRegisterSummary>,
    onSelect: (CashRegisterSummary) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = LuminaTokens.ShapeSubCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected?.name ?: "Choisir une caisse",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selected != null) {
                        Text(
                            text = "Solde : ${CurrencyFormatter.format(selected.currentBalanceCents)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.86f)
            ) {
                options.forEach { caisse ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = caisse.name,
                                    fontWeight = if (caisse.id == selected?.id) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Solde : ${CurrencyFormatter.format(caisse.currentBalanceCents)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            if (caisse.id == selected?.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelect(caisse)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
