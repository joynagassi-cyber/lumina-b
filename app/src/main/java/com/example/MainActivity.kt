package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LuminaDatabase
import com.example.data.TransactionRepository
import com.example.ui.FinancialUiState
import com.example.ui.FinancialViewModel
import com.example.ui.FinancialViewModelFactory
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CaissesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.ThemeMode
import com.example.util.AmountDisplayMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("Bilan"),
    CAISSES("Caisses"),
    BUDGETS("Budgets"),
    TIMELINE("Historique"),
    EXPORT("Exports"),
    SETTINGS("Paramètres")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = LuminaDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = TransactionRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.cashRegisterDao()
        )

        setContent {
            val viewModel: FinancialViewModel = viewModel(
                factory = FinancialViewModelFactory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LuminaTheme(themeMode = uiState.themeMode) {
                LuminaApp(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaApp(
    viewModel: FinancialViewModel,
    uiState: FinancialUiState
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Gestion du retour système
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.DASHBOARD) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    // Réception des notifications
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Modal Navigation Drawer (Menu Hamburger)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(300.dp)
                    .testTag("hamburger_drawer_sheet")
            ) {
                // Header du tiroir
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lumina-beta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comptabilité Déterministe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sélecteur rapide de thème dans le menu hamburger
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val isSelected = uiState.themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.SYSTEM -> "Auto"
                                            ThemeMode.LIGHT -> "Clair"
                                            ThemeMode.DARK -> "OLED"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Éléments de navigation du tiroir
                NavigationDrawerItem(
                    label = { Text("Bilan Financier", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = {
                        currentScreen = AppScreen.DASHBOARD
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_dashboard")
                )

                NavigationDrawerItem(
                    label = { Text("Système de Caisses", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    selected = currentScreen == AppScreen.CAISSES,
                    onClick = {
                        currentScreen = AppScreen.CAISSES
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_caisses")
                )

                NavigationDrawerItem(
                    label = { Text("Budgets & Suivi", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    selected = currentScreen == AppScreen.BUDGETS,
                    onClick = {
                        currentScreen = AppScreen.BUDGETS
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_budgets")
                )

                NavigationDrawerItem(
                    label = { Text("Historique des Opérations", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = currentScreen == AppScreen.TIMELINE,
                    onClick = {
                        currentScreen = AppScreen.TIMELINE
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_timeline")
                )

                NavigationDrawerItem(
                    label = { Text("Rapports & Exports", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    selected = currentScreen == AppScreen.EXPORT,
                    onClick = {
                        currentScreen = AppScreen.EXPORT
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_export")
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                NavigationDrawerItem(
                    label = { Text("Gestion des Catégories", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    selected = false,
                    onClick = {
                        showCategoryManager = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_categories")
                )

                NavigationDrawerItem(
                    label = { Text("Paramètres", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = {
                        currentScreen = AppScreen.SETTINGS
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_settings")
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer de version dans le tiroir
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Lumina-beta • Pure OLED & Indigo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Room SQLite • Arithmétique entière",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                AppScreen.DASHBOARD -> "Lumina-beta"
                                AppScreen.CAISSES -> "Caisses"
                                AppScreen.BUDGETS -> "Budgets"
                                AppScreen.TIMELINE -> "Historique"
                                AppScreen.EXPORT -> "Rapports"
                                AppScreen.SETTINGS -> "Paramètres"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        // Bouton Hamburger Menu
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Ouvrir le menu hamburger",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        // Bascule rapide Mode Discret / Affichage Standard
                        IconButton(
                            onClick = { viewModel.toggleDiscreetMode() },
                            modifier = Modifier.testTag("quick_discreet_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.amountDisplayMode == AmountDisplayMode.DISCRET) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mode discret",
                                tint = if (uiState.amountDisplayMode == AmountDisplayMode.DISCRET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Bascule rapide Thème Clair / OLED
                        IconButton(
                            onClick = {
                                val nextMode = if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                viewModel.setThemeMode(nextMode)
                            },
                            modifier = Modifier.testTag("quick_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Basculer thème",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Raccourci Paramètres
                        IconButton(
                            onClick = { currentScreen = AppScreen.SETTINGS },
                            modifier = Modifier.testTag("topbar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Paramètres",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = com.example.ui.designsystem.LuminaTokens.ShapePill,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = com.example.ui.designsystem.LuminaTokens.BorderWidthThin,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        shadowElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                icon = Icons.Rounded.AccountBalanceWallet,
                                label = "Bilan",
                                testTag = "nav_item_dashboard"
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.CAISSES,
                                onClick = { currentScreen = AppScreen.CAISSES },
                                icon = Icons.Rounded.Payments,
                                label = "Caisses",
                                testTag = "nav_item_caisses"
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.BUDGETS,
                                onClick = { currentScreen = AppScreen.BUDGETS },
                                icon = Icons.Rounded.PieChart,
                                label = "Budgets",
                                testTag = "nav_item_budgets"
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.TIMELINE,
                                onClick = { currentScreen = AppScreen.TIMELINE },
                                icon = Icons.Rounded.ReceiptLong,
                                label = "Historique",
                                testTag = "nav_item_timeline"
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.EXPORT,
                                onClick = { currentScreen = AppScreen.EXPORT },
                                icon = Icons.Rounded.Share,
                                label = "Exports",
                                testTag = "nav_item_export"
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    shape = com.example.ui.designsystem.LuminaTokens.ShapePill,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter une transaction",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opération", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition",
                modifier = Modifier.padding(innerPadding)
            ) { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        uiState = uiState,
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onNavigateToTimeline = { currentScreen = AppScreen.TIMELINE },
                        onNavigateToBudgets = { currentScreen = AppScreen.BUDGETS },
                        onNavigateToCaisses = { currentScreen = AppScreen.CAISSES },
                        onSelectCashRegister = { viewModel.setSelectedCashRegister(it) },
                        onExportPdf = { viewModel.exportPdf(context) },
                        onExportCsv = { viewModel.exportCsv(context) }
                    )
                    AppScreen.CAISSES -> CaissesScreen(
                        uiState = uiState,
                        onSelectCaisseFilter = {
                            viewModel.setSelectedCashRegister(it)
                            currentScreen = AppScreen.DASHBOARD
                        },
                        onAddCaisse = { name, desc, bal, icon -> viewModel.addCashRegister(name, desc, bal, icon) },
                        onUpdateCaisse = { id, name, desc, bal, icon, isDef -> viewModel.updateCashRegister(id, name, desc, bal, icon, isDef) },
                        onDeleteCaisse = { id -> viewModel.deleteCashRegister(id) },
                        onTransferBetweenCaisses = { src, tgt, amt, note -> viewModel.transferBetweenCaisses(src, tgt, amt, note) }
                    )
                    AppScreen.BUDGETS -> BudgetScreen(
                        uiState = uiState,
                        onPreviousMonth = { viewModel.changeBudgetMonth(-1) },
                        onNextMonth = { viewModel.changeBudgetMonth(1) },
                        onResetCurrentMonth = { viewModel.resetBudgetMonthToCurrent() },
                        onSaveBudget = { category, amount -> viewModel.saveBudget(category, amount) },
                        onDeleteBudget = { id -> viewModel.deleteBudget(id) },
                        onAddCategory = { name, type, color -> viewModel.addCategory(name, type, color) },
                        onUpdateCategory = { id, name, type, color, isDefault ->
                            viewModel.updateCategory(id, name, type, color, isDefault)
                        },
                        onDeleteCategory = { cat -> viewModel.deleteCategory(cat) }
                    )
                    AppScreen.TIMELINE -> TimelineScreen(
                        uiState = uiState,
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onTypeFilterSelected = { viewModel.setTypeFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                    AppScreen.EXPORT -> ExportScreen(
                        uiState = uiState,
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onExportPdf = { viewModel.exportPdf(context) },
                        onExportCsv = { viewModel.exportCsv(context) }
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onAmountDisplayModeChange = { viewModel.setAmountDisplayMode(it) },
                        onNavigateToCaisses = { currentScreen = AppScreen.CAISSES },
                        onNavigateToCategories = { showCategoryManager = true },
                        onNavigateToExport = { currentScreen = AppScreen.EXPORT }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = uiState.categories,
            cashRegisters = uiState.cashRegisters,
            defaultCashRegisterId = uiState.selectedCashRegisterId,
            onDismissRequest = { showAddDialog = false },
            onOpenCategoryManager = {
                showAddDialog = false
                showCategoryManager = true
            },
            onSaveTransaction = { title, type, amountCents, category, timestamp, note, regId, regName ->
                viewModel.addTransaction(
                    title = title,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    cashRegisterId = regId,
                    cashRegisterName = regName
                )
            }
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = uiState.categories,
            onDismissRequest = { showCategoryManager = false },
            onAddCategory = { name, type, color ->
                viewModel.addCategory(name, type, color)
            },
            onUpdateCategory = { id, name, type, color, isDefault ->
                viewModel.updateCategory(id, name, type, color, isDefault)
            },
            onDeleteCategory = { cat ->
                viewModel.deleteCategory(cat)
            }
        )
    }
}

@Composable
private fun FloatingNavPillItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String
) {
    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val inactiveBg = androidx.compose.ui.graphics.Color.Transparent
    val activeBorder = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .clip(com.example.ui.designsystem.LuminaTokens.ShapePill)
            .background(if (selected) activeBg else inactiveBg)
            .then(if (selected) Modifier.border(activeBorder, com.example.ui.designsystem.LuminaTokens.ShapePill) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = if (selected) 12.dp else 10.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(if (selected) 23.dp else 22.dp)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
            }
        }
    }
}


