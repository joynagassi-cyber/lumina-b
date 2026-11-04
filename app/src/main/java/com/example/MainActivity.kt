package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.material.icons.rounded.Groups
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
import com.example.ui.screens.ChurchProfileScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProjectsScreen
import androidx.compose.material.icons.filled.Savings
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TeamSyncScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.ThemeMode
import com.example.util.AmountDisplayMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("Rapport"),
    CAISSES("Caisses"),
    BUDGETS("Budgets"),
    MEMBERS("Membres"),
    PROJECTS("Projets"),
    TIMELINE("Historique"),
    EXPORT("Exports"),
    TEAM("Équipe & partage"),
    SETTINGS("Paramètres")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.notifications.LuminaNotifier.ensureChannel(applicationContext)
        val database = LuminaDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = TransactionRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.cashRegisterDao(),
            database.churchProfileDao(),
            database.memberDao(),
            database.userDao(),
            database.projectDao()
        )

        setContent {
            val viewModel: FinancialViewModel = viewModel(
                factory = FinancialViewModelFactory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LuminaTheme(
                themeMode = uiState.themeMode,
                accentHex = uiState.profile.accentColorHex
            ) {
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
    var showProfileEditor by remember { mutableStateOf(false) }
    var showChurchProfile by remember { mutableStateOf(false) }
    // Incrémenté à chaque appui sur le bouton flottant : chaque page ouvre sa propre création
    var createSignal by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var transactionToEdit by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }
    // Sauvegarde & restauration
    var restorePassword by remember { mutableStateOf("") }
    var pendingRestore by remember {
        mutableStateOf<Pair<android.net.Uri, com.example.export.BackupSnapshot>?>(null)
    }

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

    // Copie de secours silencieuse au démarrage (une fois par jour au plus)
    LaunchedEffect(Unit) {
        viewModel.runAutoBackup(context)
    }
    val lastAutoBackupLabel = remember(uiState.transactions.size, uiState.members.size) {
        val file = com.example.export.BackupService.latestAutoFile(context)
        file?.let {
            android.text.format.DateFormat
                .format("dd/MM/yyyy 'à' HH:mm", it.lastModified())
                .toString()
        } ?: ""
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.inspectBackup(context, uri, restorePassword) { result ->
                result.onSuccess { snapshot -> pendingRestore = uri to snapshot }
                    .onFailure {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                it.message ?: "Sauvegarde illisible"
                            )
                        }
                    }
            }
        }
    }

    // Réception des messages de l'application
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Notifications système locales (confirmations, virements, alertes budget)
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(uiState.profile.onboardingCompleted) {
        if (uiState.profile.onboardingCompleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.notificationFlow.collectLatest { event ->
            com.example.notifications.LuminaNotifier.notify(context, event.title, event.body)
        }
    }

    // Premier lancement : assistant de création du profil de l'église
    if (!uiState.profile.onboardingCompleted) {
        OnboardingScreen(
            initialProfile = uiState.profile,
            onFinish = { profile -> viewModel.completeOnboardingFromScreen(profile) }
        )
        return
    }

    // Ouverture de session locale : chacun choisit son nom et saisit son code
    if (uiState.currentUser == null) {
        LoginScreen(
            churchName = uiState.profile.churchName,
            users = uiState.users,
            onLogin = { id, pin -> viewModel.login(id, pin) },
            onCreateFirstUser = { name, role, pin -> viewModel.createFirstUser(name, role, pin) }
        )
        return
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
                                text = "Gestion financière de l'église",
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
                    label = { Text("Rapport de gestion", fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("Registre des Membres", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    selected = currentScreen == AppScreen.MEMBERS,
                    onClick = {
                        currentScreen = AppScreen.MEMBERS
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_members")
                )

                NavigationDrawerItem(
                    label = { Text("Projets & fonds affectés", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                    selected = currentScreen == AppScreen.PROJECTS,
                    onClick = {
                        currentScreen = AppScreen.PROJECTS
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_projects")
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
                    label = { Text("Équipe & partage", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    selected = currentScreen == AppScreen.TEAM,
                    onClick = {
                        currentScreen = AppScreen.TEAM
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_team")
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
                        text = "Données enregistrées sur votre appareil",
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
                                AppScreen.MEMBERS -> "Membres"
                                AppScreen.PROJECTS -> "Projets"
                                AppScreen.EXPORT -> "Rapports"
                                AppScreen.TEAM -> "Équipe & partage"
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                icon = Icons.Rounded.AccountBalanceWallet,
                                label = "Rapport",
                                testTag = "nav_item_dashboard",
                                modifier = Modifier.weight(1f)
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.CAISSES,
                                onClick = { currentScreen = AppScreen.CAISSES },
                                icon = Icons.Rounded.Payments,
                                label = "Caisses",
                                testTag = "nav_item_caisses",
                                modifier = Modifier.weight(1f)
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.BUDGETS,
                                onClick = { currentScreen = AppScreen.BUDGETS },
                                icon = Icons.Rounded.PieChart,
                                label = "Budgets",
                                testTag = "nav_item_budgets",
                                modifier = Modifier.weight(1f)
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.TIMELINE,
                                onClick = { currentScreen = AppScreen.TIMELINE },
                                icon = Icons.Rounded.ReceiptLong,
                                label = "Historique",
                                testTag = "nav_item_timeline",
                                modifier = Modifier.weight(1f)
                            )
                            FloatingNavPillItem(
                                selected = currentScreen == AppScreen.MEMBERS,
                                onClick = { currentScreen = AppScreen.MEMBERS },
                                icon = Icons.Rounded.Groups,
                                label = "Membres",
                                testTag = "nav_item_members",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Lecture seule : aucune création possible.
                // Le bouton crée ce que la page affiche : caisse, budget, membre ou opération.
                val fabLabel = when (currentScreen) {
                    AppScreen.CAISSES -> "Caisse"
                    AppScreen.BUDGETS -> "Budget"
                    AppScreen.MEMBERS -> "Membre"
                    AppScreen.PROJECTS -> "Projet"
                    else -> "Opération"
                }
                if (uiState.canWrite) FloatingActionButton(
                    onClick = {
                        when (currentScreen) {
                            AppScreen.CAISSES, AppScreen.BUDGETS, AppScreen.MEMBERS, AppScreen.PROJECTS -> createSignal++
                            else -> showAddDialog = true
                        }
                    },
                    shape = com.example.ui.designsystem.LuminaTokens.ShapePill,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Créer : $fabLabel",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(fabLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        onTransferBetweenCaisses = { src, tgt, amt, note -> viewModel.transferBetweenCaisses(src, tgt, amt, note) },
                        createSignal = createSignal
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
                    AppScreen.MEMBERS -> MembersScreen(
                        members = uiState.members,
                        onAddMember = { viewModel.addMember(it) },
                        onUpdateMember = { viewModel.updateMember(it) },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        createSignal = createSignal
                    )
                    AppScreen.PROJECTS -> ProjectsScreen(
                        projects = uiState.projectSummaries,
                        createSignal = createSignal,
                        onAddProject = { name, description, target ->
                            viewModel.addProject(name, description, target)
                        },
                        onUpdateProject = { project, name, description, target, closed ->
                            viewModel.updateProject(project, name, description, target, closed)
                        },
                        onDeleteProject = { project -> viewModel.deleteProject(project) }
                    )
                    AppScreen.TIMELINE -> TimelineScreen(
                        uiState = uiState,
                        onSelectCaisseFilter = { viewModel.setSelectedCashRegister(it) },
                        onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                        onAmountRangeChange = { min, max -> viewModel.setAmountRange(min, max) },
                        onSortModeSelected = { viewModel.setSortMode(it) },
                        onResetFilters = { viewModel.clearFilters() },
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onTypeFilterSelected = { viewModel.setTypeFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onEditTransaction = { transactionToEdit = it },
                        onCancelTransaction = { tx -> viewModel.cancelTransaction(tx, tx.note) }
                    )
                    AppScreen.EXPORT -> ExportScreen(
                        uiState = uiState,
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onExportPdf = { viewModel.exportPdf(context) },
                        onExportCsv = { viewModel.exportCsv(context) }
                    )
                    AppScreen.TEAM -> TeamSyncScreen(
                        uiState = uiState,
                        onAddUser = { name, role, pin -> viewModel.addUser(name, role, pin) },
                        onUpdateUser = { user, name, role, pin -> viewModel.updateUser(user, name, role, pin) },
                        onDeleteUser = { user -> viewModel.deleteUser(user) },
                        onExport = { ctx, phrase, onReady -> viewModel.exportSyncFile(ctx, phrase, onReady) },
                        onImport = { ctx, uri, phrase -> viewModel.importSyncFile(ctx, uri, phrase) },
                        onShareFile = { ctx, file ->
                            com.example.export.ReportExporter.shareFile(
                                ctx,
                                file,
                                "application/octet-stream",
                                "Échange de données Lumina"
                            )
                        },
                        onLogout = { viewModel.logout() }
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onEditChurchProfile = { showChurchProfile = true },
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onAmountDisplayModeChange = { viewModel.setAmountDisplayMode(it) },
                        onNavigateToCaisses = { currentScreen = AppScreen.CAISSES },
                        onNavigateToCategories = { showCategoryManager = true },
                        onNavigateToExport = { currentScreen = AppScreen.EXPORT },
                        onWipeData = { fullReset -> viewModel.wipeData(context, fullReset) },
                        onClosePreviousMonth = { viewModel.closePreviousMonth() },
                        onReopenBooks = { viewModel.reopenBooks() },
                        onCreateBackup = { password ->
                            viewModel.createBackup(context, password) { result ->
                                result.onSuccess { export ->
                                    com.example.export.ReportExporter.shareFile(
                                        context,
                                        export.file,
                                        "application/octet-stream",
                                        "Sauvegarde du dossier — ${uiState.profile.churchName}"
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Sauvegarde prête : ${export.snapshot.transactionCount} écritures, " +
                                                "${export.snapshot.memberCount} membres" +
                                                if (export.encrypted) " (protégée par mot de passe)" else ""
                                        )
                                    }
                                }.onFailure {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            it.message ?: "Sauvegarde impossible"
                                        )
                                    }
                                }
                            }
                        },
                        onRequestRestore = { password ->
                            restorePassword = password
                            restorePicker.launch(arrayOf("*/*"))
                        },
                        onRestoreAuto = {
                            viewModel.restoreLatestAutoBackup(context) { result ->
                                result.onFailure {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            it.message ?: "Aucune copie automatique"
                                        )
                                    }
                                }
                            }
                        },
                        lastAutoBackupLabel = lastAutoBackupLabel
                    )
                }
            }
        }
    }

    if (showChurchProfile) {
        ChurchProfileScreen(
            profile = uiState.profile,
            memberCount = uiState.members.size,
            onBack = { showChurchProfile = false },
            onEdit = {
                showChurchProfile = false
                showProfileEditor = true
            }
        )
    }

    if (showProfileEditor) {
        OnboardingScreen(
            initialProfile = uiState.profile,
            startAtStep = 1,
            finishLabel = "Enregistrer",
            onFinish = { profile ->
                viewModel.completeOnboardingFromScreen(profile)
                showProfileEditor = false
            }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = uiState.categories,
            cashRegisters = uiState.cashRegisters,
            projects = uiState.projects,
            defaultCashRegisterId = uiState.selectedCashRegisterId,
            onDismissRequest = { showAddDialog = false },
            onOpenCategoryManager = {
                showAddDialog = false
                showCategoryManager = true
            },
            onSaveTransaction = { title, type, amountCents, category, timestamp, note, regId, regName, receiptPath, projectId ->
                viewModel.addTransaction(
                    title = title,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    cashRegisterId = regId,
                    cashRegisterName = regName,
                    receiptPhotoUri = receiptPath,
                    projectId = projectId
                )
            }
        )
    }

    transactionToEdit?.let { tx ->
        AddTransactionDialog(
            categories = uiState.categories,
            cashRegisters = uiState.cashRegisters,
            projects = uiState.projects,
            defaultCashRegisterId = tx.cashRegisterId,
            initial = tx,
            onDismissRequest = { transactionToEdit = null },
            onOpenCategoryManager = {
                transactionToEdit = null
                showCategoryManager = true
            },
            onSaveTransaction = { title, type, amountCents, category, timestamp, note, regId, regName, receiptPath, projectId ->
                viewModel.updateTransaction(
                    id = tx.id,
                    title = title,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    cashRegisterId = regId,
                    cashRegisterName = regName,
                    receiptPhotoUri = receiptPath,
                    projectId = projectId
                )
                transactionToEdit = null
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

    // Confirmation avant remplacement du dossier par une sauvegarde
    pendingRestore?.let { (uri, snapshot) ->
        val sauvegardeLe = java.text.SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", java.util.Locale.FRANCE)
            .format(java.util.Date(snapshot.createdAt))
        val entete = "Sauvegarde du $sauvegardeLe" +
            if (snapshot.churchName.isNotBlank()) " — ${snapshot.churchName}" else ""
        val contenu = entete + "\n\n" +
            "${snapshot.transactionCount} écritures, ${snapshot.memberCount} membres, " +
            "${snapshot.registers.size} caisses et ${snapshot.projects.size} projets.\n\n" +
            "Les données actuellement dans l'application seront remplacées."
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { androidx.compose.material3.Text("Remplacer le dossier actuel ?") },
            text = {
                androidx.compose.material3.Text(contenu)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.restoreBackup(context, uri, restorePassword) { result ->
                            result.onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        it.message ?: "Restauration impossible"
                                    )
                                }
                            }
                        }
                        pendingRestore = null
                    }
                ) {
                    androidx.compose.material3.Text("Restaurer")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingRestore = null }) {
                    androidx.compose.material3.Text("Annuler")
                }
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
    testTag: String,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(com.example.ui.designsystem.LuminaTokens.ShapeSubCard)
            .background(background)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
