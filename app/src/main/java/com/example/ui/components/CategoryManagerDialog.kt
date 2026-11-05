package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CategoryEntity
import com.example.data.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.ExpenseBgDark
import com.example.ui.theme.ExpenseBgLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.IncomeBgDark
import com.example.ui.theme.IncomeBgLight

@Composable
private fun CategoryTypeTab(
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
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) selectedSurface else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) semanticColor else MaterialTheme.colorScheme.outline
                ),
                shape
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (type == TransactionType.EXPENSE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = if (selected) semanticColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) semanticColor else MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sélectionné",
                tint = semanticColor,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

private val PresetColors = listOf(
    "#0F766E", // Teal Lumina
    "#10B981", // Émeraude
    "#3B82F6", // Bleu
    "#F59E0B", // Ambre
    "#EF4444", // Rouge
    "#EC4899", // Rose
    "#8B5CF6", // Violet
    "#64748B", // Ardoise
    "#06B6D4", // Cyan
    "#84CC16"  // Lime
)

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0F766E)
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismissRequest: () -> Unit,
    onAddCategory: (name: String, type: TransactionType, colorHex: String) -> Unit,
    onUpdateCategory: (id: Long, name: String, type: TransactionType, colorHex: String, isDefault: Boolean) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var selectedTabType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    // Formulaire d'édition / création
    var formName by remember { mutableStateOf("") }
    var formType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var formColor by remember { mutableStateOf(PresetColors.first()) }
    var formError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .heightIn(max = 700.dp)
                .testTag("category_manager_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestion des Catégories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personnalisez vos postes financiers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_category_manager_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sélecteur Dépenses / Entrées : contraste renforcé, notamment en thème clair.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryTypeTab(
                        type = TransactionType.EXPENSE,
                        selected = selectedTabType == TransactionType.EXPENSE,
                        label = "Sorties / Dépenses",
                        modifier = Modifier.weight(1f).testTag("category_tab_expense"),
                        onClick = {
                            selectedTabType = TransactionType.EXPENSE
                            if (isCreatingNew) formType = TransactionType.EXPENSE
                        }
                    )
                    CategoryTypeTab(
                        type = TransactionType.INCOME,
                        selected = selectedTabType == TransactionType.INCOME,
                        label = "Entrées / Revenus",
                        modifier = Modifier.weight(1f).testTag("category_tab_income"),
                        onClick = {
                            selectedTabType = TransactionType.INCOME
                            if (isCreatingNew) formType = TransactionType.INCOME
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bouton Nouvelle Catégorie
                if (!isCreatingNew && editingCategory == null) {
                    Button(
                        onClick = {
                            isCreatingNew = true
                            formName = ""
                            formType = selectedTabType
                            formColor = PresetColors.first()
                            formError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_new_category_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Créer une nouvelle catégorie", fontWeight = FontWeight.Bold)
                    }
                }

                // Formulaire d'ajout / modification inline
                if (isCreatingNew || editingCategory != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isCreatingNew) "Nouvelle catégorie" else "Modifier \"${editingCategory?.name}\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Type de catégorie",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CategoryTypeTab(
                                    type = TransactionType.EXPENSE,
                                    selected = formType == TransactionType.EXPENSE,
                                    label = "Sortie / Dépense",
                                    modifier = Modifier.weight(1f).testTag("category_form_expense"),
                                    onClick = {
                                        formType = TransactionType.EXPENSE
                                        selectedTabType = TransactionType.EXPENSE
                                    }
                                )
                                CategoryTypeTab(
                                    type = TransactionType.INCOME,
                                    selected = formType == TransactionType.INCOME,
                                    label = "Entrée / Revenu",
                                    modifier = Modifier.weight(1f).testTag("category_form_income"),
                                    onClick = {
                                        formType = TransactionType.INCOME
                                        selectedTabType = TransactionType.INCOME
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = formName,
                                onValueChange = {
                                    formName = it
                                    formError = null
                                },
                                label = { Text("Nom de la catégorie") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_name_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Couleur d'identification :",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresetColors.take(6).forEach { hex ->
                                    val color = parseHexColor(hex)
                                    val isSelected = formColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { formColor = hex }
                                    )
                                }
                            }

                            if (formError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        isCreatingNew = false
                                        editingCategory = null
                                    }
                                ) {
                                    Text("Annuler")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (formName.isBlank()) {
                                            formError = "Le nom ne peut pas être vide"
                                            return@Button
                                        }

                                        if (isCreatingNew) {
                                            onAddCategory(formName, formType, formColor)
                                            isCreatingNew = false
                                        } else if (editingCategory != null) {
                                            onUpdateCategory(
                                                editingCategory!!.id,
                                                formName,
                                                formType,
                                                formColor,
                                                editingCategory!!.isDefault
                                            )
                                            selectedTabType = formType
                                            editingCategory = null
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("save_category_button")
                                ) {
                                    Text(if (isCreatingNew) "Créer" else "Enregistrer")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Liste des catégories existantes
                val filteredCategories = categories.filter { it.type == selectedTabType }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("category_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCategories.isEmpty()) {
                        item(key = "empty-category-list") {
                            Text(
                                text = if (selectedTabType == TransactionType.INCOME) "Aucune catégorie d'entrée pour le moment." else "Aucune catégorie de sortie pour le moment.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    items(filteredCategories, key = { it.id }) { category ->
                        val catColor = parseHexColor(category.colorHex)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_card_${category.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category.name,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (category.isDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "Défaut",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingCategory = category
                                            isCreatingNew = false
                                            formName = category.name
                                            formType = category.type
                                            formColor = category.colorHex
                                            formError = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Modifier",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier.size(36.dp)
                                    ) {
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
                    }
                }
            }
        }
    }

    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Supprimer la catégorie ?") },
            text = { Text("Voulez-vous supprimer la catégorie \"${cat.name}\" ? Les budgets associés seront également retirés.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
