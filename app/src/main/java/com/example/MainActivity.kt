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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.ChurchWorkspace
import com.example.data.ChurchWorkspaceCatalog
import com.example.ui.FinancialUiState
import com.example.ui.FinancialViewModel
import com.example.ui.FinancialViewModelFactory
import com.example.ui.designsystem.LuminaBranding
import com.example.ui.designsystem.LuminaBrandMark
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CaissesScreen
import com.example.ui.screens.ChurchProfileScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProjectsScreen
import androidx.compose.material.icons.filled.Savings
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TeamSyncScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.WorkspacePickerDialog
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.parseHexColor
import com.example.util.AmountDisplayMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("Tableau de bord"),
    CAISSES("Caisses"),
    BUDGETS("Budgets"),
    MEMBERS("Membres"),
    PROJECTS("Projets"),
    TIMELINE("Historique"),
    NOTIFICATIONS("Notifications"),
    EXPORT("Exports"),
    TEAM("Équipe & partage"),
    SETTINGS("Paramètres")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.notifications.LuminaNotifier.ensureChannel(applicationContext)
        val workspaceCatalog = ChurchWorkspaceCatalog(applicationContext)
        val activeWorkspace = workspaceCatalog.ensureInitialWorkspace()
        val database = LuminaDatabase.getDatabaseNamed(
            applicationContext,
            activeWorkspace.databaseName,
            lifecycleScope
        )
        val repository = TransactionRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.cashRegisterDao(),
            database.churchProfileDao(),
            database.memberDao(),
            database.userDao(),
            database.projectDao(),
            workspaceId = activeWorkspace.id,
            database = database,
            syncContributorDao = database.syncContributorDao()
        )

        setContent {
            val viewModel: FinancialViewModel = viewModel(
                factory = FinancialViewModelFactory(repository, applicationContext)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showBrandSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(700L)
                showBrandSplash = false
            }
            LaunchedEffect(activeWorkspace.id, uiState.profile.churchName, uiState.profile.onboardingCompleted) {
                if (uiState.profile.onboardingCompleted) {
                    workspaceCatalog.updateDisplayName(activeWorkspace.id, uiState.profile.displayName)
                }
            }

            LuminaTheme(
                themeMode = uiState.themeMode,
                accentHex = uiState.profile.accentColorHex
            ) {
                if (showBrandSplash) {
                    com.example.ui.components.LuminaBrandSplashScreen(
                        isDark = when (uiState.themeMode) {
                            ThemeMode.DARK, ThemeMode.GLASS_DARK -> true
                            ThemeMode.LIGHT, ThemeMode.GLASS_LIGHT -> false
                            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                        }
                    )
                } else LuminaApp(
                    viewModel = viewModel,
                    uiState = uiState,
                    workspaces = workspaceCatalog.getWorkspaces(),
                    activeWorkspace = activeWorkspace,
                    onSelectWorkspace = { id ->
                        workspaceCatalog.setActiveWorkspace(id)
                        viewModelStore.clear()
                        recreate()
                    },
                    onCreateWorkspace = {
                        workspaceCatalog.createWorkspace()
                        viewModelStore.clear()
                        recreate()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaApp(
    viewModel: FinancialViewModel,
    uiState: FinancialUiState,
    workspaces: List<ChurchWorkspace>,
    activeWorkspace: ChurchWorkspace,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCustomPeriodPicker by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showChurchProfile by remember { mutableStateOf(false) }
    var showWorkspacePicker by remember { mutableStateOf(false) }
    // Requête de création à usage unique, ciblée sur l'écran courant.
    var createSignal by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var createTarget by remember { mutableStateOf<AppScreen?>(null) }
    var transactionToEdit by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }
    // Sauvegarde & restauration
    var restorePassword by remember { mutableStateOf("") }
    var pendingRestore by remember {
        mutableStateOf<Pair<android.net.Uri, com.example.export.BackupSnapshot>?>(null)
    }
    var cloudBackupStatus by remember(activeWorkspace.id) {
        mutableStateOf(com.example.cloud.CloudBackupLocalStore.status(context, activeWorkspace.id))
    }
    val cloudProgressFlow = remember(activeWorkspace.id) {
        com.example.cloud.CloudBackupLocalStore.progressFlow(activeWorkspace.id)
    }
    val cloudProgress by cloudProgressFlow.collectAsStateWithLifecycle(initialValue = null)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val selectPeriod: (com.example.ui.PeriodFilter) -> Unit = { period ->
        if (period != com.example.ui.PeriodFilter.PERSONNALISEE) {
            viewModel.setPeriod(period)
        } else {
            showCustomPeriodPicker = true
        }
    }

    if (showCustomPeriodPicker) {
        com.example.ui.components.LuminaDateRangePickerDialog(
            initialStartLocalMillis = null,
            initialEndLocalMillis = null,
            onDismissRequest = { showCustomPeriodPicker = false },
            onRangeSelected = { start, end ->
                viewModel.setCustomPeriod(
                    com.example.util.localStartOfDay(start),
                    com.example.util.localEndOfDay(end)
                )
                showCustomPeriodPicker = false
            }
        )
    }

    // Gestion du retour système
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.DASHBOARD) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    // La sauvegarde automatique ne s'exécute qu'après connexion à un dossier réel,
    // jamais sur l'écran de connexion ni pendant la démonstration.
    LaunchedEffect(uiState.isDemoMode, uiState.currentUser?.id) {
        if (!uiState.isDemoMode && uiState.currentUser != null) {
            viewModel.runAutoBackup(context)
        }
    }
    val lastAutoBackupLabel = remember(activeWorkspace.id, uiState.transactions.size, uiState.members.size) {
        val file = com.example.export.BackupService.latestAutoFile(context, activeWorkspace.id)
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
            cloudBackupStatus = com.example.cloud.CloudBackupLocalStore.status(context, activeWorkspace.id)
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

    if (showWorkspacePicker) {
        WorkspacePickerDialog(
            workspaces = workspaces,
            activeWorkspaceId = activeWorkspace.id,
            onDismiss = { showWorkspacePicker = false },
            onSelectWorkspace = { id ->
                showWorkspacePicker = false
                if (id != activeWorkspace.id) onSelectWorkspace(id)
            },
            onCreateWorkspace = {
                showWorkspacePicker = false
                onCreateWorkspace()
            }
        )
    }

    // Premier lancement : assistant de création du profil de l'église
    if (!uiState.profile.onboardingCompleted) {
        OnboardingScreen(
            initialProfile = uiState.profile,
            onFinish = { profile -> viewModel.completeOnboardingFromScreen(profile) },
            onOpenExistingProfiles = if (workspaces.any { it.id != activeWorkspace.id }) {
                { showWorkspacePicker = true }
            } else null,
            onJoinFromFile = { uri, password, onResult ->
                viewModel.importOrganizationBackupForNewMember(context, uri, password, onResult)
            }
        )
        return
    }

    // Ouverture de session locale : chacun choisit son nom et saisit son code
    if (uiState.currentUser == null) {
        LoginScreen(
            churchName = uiState.profile.churchName,
            brandBackgroundColor = parseHexColor(uiState.profile.accentColorHex),
            users = uiState.users,
            onLogin = { id, pin -> viewModel.login(id, pin) },
            onCreateFirstUser = { name, role, pin -> viewModel.createFirstUser(name, role, pin) },
            isInvitedFirstUser = uiState.firstUserIsInvitee,
            onStartDemo = { viewModel.startDemoMode(context) },
            onChooseWorkspace = { showWorkspacePicker = true }
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .testTag("drawer_scroll_content")
                    ) {
                    // Header du tiroir
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LuminaBrandMark(
                                modifier = Modifier.size(42.dp),
                                backgroundColor = parseHexColor(uiState.profile.accentColorHex)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Lumina",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Dossier : ${uiState.profile.displayName}",
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
                                    listOf(ThemeMode.GLASS_LIGHT, ThemeMode.GLASS_DARK)
                                ).forEach { rowModes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowModes.forEach { mode ->
                                            val isSelected = uiState.themeMode == mode
                                            val label = when (mode) {
                                                ThemeMode.SYSTEM -> "Auto"
                                                ThemeMode.LIGHT -> "Clair"
                                                ThemeMode.DARK -> "OLED"
                                                ThemeMode.GLASS_LIGHT -> "Glass clair"
                                                ThemeMode.GLASS_DARK -> "Glass sombre"
                                            }
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
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
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

                    if (!uiState.isDemoMode) {
                        NavigationDrawerItem(
                            label = { Text("Dossiers d'église") },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                            selected = false,
                            onClick = {
                                showWorkspacePicker = true
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("open_workspace_picker_drawer")
                        )

                    }

                    if (!uiState.isDemoMode) {
                        NavigationDrawerItem(
                            label = { Text("Explorer la démo (essais temporaires)") },
                            icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.startDemoMode(context)
                                currentScreen = AppScreen.DASHBOARD
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("start_read_only_demo_drawer")
                        )
                    }
                    if (uiState.isDemoMode) {
                        NavigationDrawerItem(
                            label = { Text("Quitter la démo et revenir à mon profil") },
                            icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.exitDemoMode(context)
                                currentScreen = AppScreen.DASHBOARD
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("exit_read_only_demo")
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Éléments de navigation du tiroir
                    NavigationDrawerItem(
                        label = { Text("Tableau de bord", fontWeight = FontWeight.SemiBold) },
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
                        label = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.NotificationsNone, contentDescription = null) },
                        selected = currentScreen == AppScreen.NOTIFICATIONS,
                        onClick = {
                            currentScreen = AppScreen.NOTIFICATIONS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_item_notifications")
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

                    }

                    // Paramètres reste accessible même lorsque la liste est longue.
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

                    // Footer de version dans le tiroir
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Lumina • ${LuminaBranding.SLOGAN}",
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
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                AppScreen.DASHBOARD -> "Tableau de bord"
                                AppScreen.CAISSES -> "Caisses"
                                AppScreen.BUDGETS -> "Budgets"
                                AppScreen.TIMELINE -> "Historique"
                                AppScreen.NOTIFICATIONS -> "Notifications"
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

                        // Bascule clair/sombre ; conserve le style Glass lorsqu'il est actif.
                        IconButton(
                            onClick = {
                                val nextMode = when (uiState.themeMode) {
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.GLASS_LIGHT -> ThemeMode.GLASS_DARK
                                    ThemeMode.GLASS_DARK -> ThemeMode.GLASS_LIGHT
                                    ThemeMode.SYSTEM, ThemeMode.LIGHT -> ThemeMode.DARK
                                }
                                viewModel.setThemeMode(nextMode)
                            },
                            modifier = Modifier.testTag("quick_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.themeMode == ThemeMode.DARK || uiState.themeMode == ThemeMode.GLASS_DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Basculer le mode clair ou sombre",
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
                if (!uiState.isDemoMode) cloudProgress?.let { progress ->
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.percent / 100f,
                        animationSpec = tween(durationMillis = 450),
                        label = "cloud-backup-progress"
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("cloud_backup_progress"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (progress.isRunning) "Sauvegarde cloud en cours" else "Sauvegarde terminée",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${progress.percent}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                            Text(
                                text = if (progress.isRunning) progress.message else "La copie a été confirmée dans le cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val glassNav = MaterialTheme.colorScheme.background.alpha == 0f
                    val navBorder = if (glassNav) {
                        androidx.compose.foundation.BorderStroke(
                            width = com.example.ui.designsystem.LuminaTokens.BorderWidthThin,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                    Color.White.copy(alpha = 0.24f)
                                )
                            )
                        )
                    } else {
                        androidx.compose.foundation.BorderStroke(
                            width = com.example.ui.designsystem.LuminaTokens.BorderWidthThin,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Surface(
                        shape = com.example.ui.designsystem.LuminaTokens.ShapePill,
                        color = if (glassNav) MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                        else MaterialTheme.colorScheme.surface,
                        border = navBorder,
                        shadowElevation = if (glassNav) 12.dp else 6.dp,
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
                                label = "Finance",
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
                // En démo, seul l'essai d'une écriture temporaire est proposé ;
                // aucun bouton ne peut modifier la base réelle.
                val temporaryDemoAction = uiState.isDemoMode &&
                    currentScreen in setOf(AppScreen.DASHBOARD, AppScreen.TIMELINE)
                val fabLabel = if (uiState.isDemoMode) "Essayer" else when (currentScreen) {
                    AppScreen.CAISSES -> "Caisse"
                    AppScreen.BUDGETS -> "Budget"
                    AppScreen.MEMBERS -> "Membre"
                    AppScreen.PROJECTS -> "Projet"
                    else -> "Opération"
                }
                if (uiState.canWrite || temporaryDemoAction) FloatingActionButton(
                    onClick = {
                        if (uiState.isDemoMode) {
                            showAddDialog = true
                        } else when (currentScreen) {
                            AppScreen.CAISSES, AppScreen.BUDGETS, AppScreen.MEMBERS, AppScreen.PROJECTS -> {
                                createTarget = currentScreen
                                createSignal++
                            }
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
                            contentDescription = if (uiState.isDemoMode) "Essayer une écriture temporaire" else "Créer : $fabLabel",
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
                        onPeriodSelected = { selectPeriod(it) },
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
                        createSignal = if (createTarget == AppScreen.CAISSES) createSignal else 0,
                        onCreateConsumed = { if (createTarget == AppScreen.CAISSES) createTarget = null }
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
                        onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                        createSignal = if (createTarget == AppScreen.BUDGETS) createSignal else 0,
                        onCreateConsumed = { if (createTarget == AppScreen.BUDGETS) createTarget = null }
                    )
                    AppScreen.NOTIFICATIONS -> NotificationsScreen()
                    AppScreen.MEMBERS -> MembersScreen(
                        members = uiState.members,
                        onAddMember = { viewModel.addMember(it) },
                        onUpdateMember = { viewModel.updateMember(it) },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        createSignal = if (createTarget == AppScreen.MEMBERS) createSignal else 0,
                        onCreateConsumed = { if (createTarget == AppScreen.MEMBERS) createTarget = null }
                    )
                    AppScreen.PROJECTS -> ProjectsScreen(
                        projects = uiState.projectSummaries,
                        transactions = uiState.allTransactions,
                        createSignal = if (createTarget == AppScreen.PROJECTS) createSignal else 0,
                        onAddProject = { name, description, target, kind, start, end, actions ->
                            viewModel.addProject(name, description, target, kind, start, end, actions)
                        },
                        onUpdateProject = { project, name, description, target, closed, kind, start, end, actions ->
                            viewModel.updateProject(project, name, description, target, closed, kind, start, end, actions)
                        },
                        onUpdateProjectActions = { project, actions -> viewModel.updateProjectActions(project, actions) },
                        onDeleteProject = { project -> viewModel.deleteProject(project) },
                        onCreateConsumed = { if (createTarget == AppScreen.PROJECTS) createTarget = null }
                    )
                    AppScreen.TIMELINE -> TimelineScreen(
                        uiState = uiState,
                        onSelectCaisseFilter = { viewModel.setSelectedCashRegister(it) },
                        onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                        onAmountRangeChange = { min, max -> viewModel.setAmountRange(min, max) },
                        onSortModeSelected = { viewModel.setSortMode(it) },
                        onResetFilters = { viewModel.clearFilters() },
                        onPeriodSelected = { selectPeriod(it) },
                        onTypeFilterSelected = { viewModel.setTypeFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onEditTransaction = { transactionToEdit = it },
                        onCancelTransaction = { tx -> viewModel.cancelTransaction(tx, tx.note) }
                    )
                    AppScreen.EXPORT -> ExportScreen(
                        uiState = uiState,
                        onPeriodSelected = { selectPeriod(it) },
                        onExportPdf = { viewModel.exportPdf(context) },
                        onExportCsv = { viewModel.exportCsv(context) },
                        onExportExcel = { viewModel.exportExcel(context) },
                        onExportMembersPdf = { viewModel.exportMembersPdf(context) },
                        onExportBudgetPdf = { viewModel.exportBudgetPdf(context) },
                        onExportHistoryPdf = { viewModel.exportHistoryPdf(context) }
                    )
                    AppScreen.TEAM -> TeamSyncScreen(
                        uiState = uiState,
                        onAddUser = { name, role, pin -> viewModel.addUser(name, role, pin) },
                        onUpdateUser = { user, name, role, pin -> viewModel.updateUser(user, name, role, pin) },
                        onDeleteUser = { user -> viewModel.deleteUser(user) },
                        onExport = { ctx, phrase, onReady -> viewModel.exportSyncFile(ctx, phrase, onReady) },
                        onCreateInvitation = { ctx, phrase, onReady -> viewModel.createChurchInvitation(ctx, phrase, onReady) },
                        onImport = { ctx, uri, phrase -> viewModel.importSyncFile(ctx, uri, phrase) },
                        onRefreshConflicts = { ctx -> viewModel.refreshSyncConflicts(ctx) },
                        onResolveConflict = { ctx, conflict, useImported, phrase ->
                            viewModel.resolveSyncConflict(ctx, conflict, useImported, phrase)
                        },
                        onShareFile = { ctx, file ->
                            com.example.export.ReportExporter.shareFile(
                                ctx,
                                file,
                                "application/octet-stream",
                                if (file.name.endsWith(".lumina-invite")) "Invitation légère Lumina" else "Échange de données Lumina"
                            )
                        },
                        onCreateCloudInvite = { ctx, onResult -> viewModel.createCloudInvite(ctx, onResult) },
                        onAcceptCloudInvite = { ctx, code, onResult ->
                            viewModel.acceptCloudInvite(ctx, code) { result ->
                                if (result.isSuccess) {
                                    com.example.cloud.CloudBackupScheduler.schedule(
                                        ctx,
                                        activeWorkspace.id,
                                        activeWorkspace.databaseName
                                    )
                                }
                                onResult(result)
                            }
                        },
                        onLogout = { viewModel.logout(context) }
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onEditChurchProfile = { showChurchProfile = true },
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onAccentColorChange = { viewModel.setAccentColor(it) },
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
                                            "Fichier Lumina prêt : ${export.snapshot.transactionCount} écritures, " +
                                                "${export.snapshot.memberCount} membres" +
                                                if (export.missingReceiptCount > 0) {
                                                    " — ${export.missingReceiptCount} justificatif(s) absent(s) du stockage local"
                                                } else if (export.encrypted) " (ZIP protégé par mot de passe)" else " (ZIP non chiffré)"
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
                        onEnableCloudBackup = {
                            viewModel.enableCloudBackup(context) { result ->
                                result.onSuccess { message ->
                                    com.example.cloud.CloudBackupScheduler.schedule(
                                        context,
                                        activeWorkspace.id,
                                        activeWorkspace.databaseName
                                    )
                                    cloudBackupStatus = message
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }.onFailure { error ->
                                    cloudBackupStatus = error.message ?: "Sauvegarde cloud indisponible"
                                    scope.launch { snackbarHostState.showSnackbar(cloudBackupStatus) }
                                }
                            }
                        },
                        onCheckCloudBackup = {
                            viewModel.checkCloudBackup(context) { result ->
                                result.onSuccess { message ->
                                    cloudBackupStatus = message
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }.onFailure { error ->
                                    cloudBackupStatus = error.message ?: "Vérification cloud impossible"
                                    scope.launch { snackbarHostState.showSnackbar(cloudBackupStatus) }
                                }
                            }
                        },
                        onRestoreCloudBackup = {
                            viewModel.restoreCloudBackup(context) { result ->
                                result.onSuccess { snapshot ->
                                    cloudBackupStatus = "Restauration cloud terminée : ${snapshot.transactionCount} écritures, ${snapshot.memberCount} membres."
                                    scope.launch { snackbarHostState.showSnackbar(cloudBackupStatus) }
                                }.onFailure { error ->
                                    cloudBackupStatus = error.message ?: "Restauration cloud impossible"
                                    scope.launch { snackbarHostState.showSnackbar(cloudBackupStatus) }
                                }
                            }
                        },
                        cloudBackupStatus = cloudBackupStatus,
                        lastAutoBackupLabel = if (uiState.isDemoMode) "" else lastAutoBackupLabel
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
            workspaceId = if (uiState.isDemoMode) "demo" else uiState.workspaceId,
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
            workspaceId = if (uiState.isDemoMode) "demo" else uiState.workspaceId,
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
