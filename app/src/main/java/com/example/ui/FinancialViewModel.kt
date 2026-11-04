package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ChurchProfileEntity
import com.example.data.MemberEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.sync.SyncManager
import com.example.util.PinHasher
import com.example.export.ReportExporter
import com.example.ui.theme.ThemeMode
import com.example.util.AmountDisplayMode
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PeriodFilter(val label: String) {
    CE_MOIS("Ce mois"),
    MOIS_DERNIER("Mois dernier"),
    TROIS_MOIS("3 derniers mois"),
    CETTE_ANNEE("Cette année"),
    TOUT("Tout")
}

enum class SortMode(val label: String) {
    DATE_DESC("Plus récent"),
    DATE_ASC("Plus ancien"),
    MONTANT_DESC("Montant décroissant"),
    MONTANT_ASC("Montant croissant")
}

data class CategoryBudgetProgress(
    val categoryName: String,
    val categoryColorHex: String,
    val budgetId: Long?,
    val budgetAmountCents: Long,
    val spentAmountCents: Long,
    val percentage: Float,
    val remainingCents: Long,
    val isExceeded: Boolean,
    val isNearLimit: Boolean
)

data class GlobalBudgetSummary(
    val totalBudgetCeilingCents: Long = 0,
    val totalSpentCents: Long = 0,
    val totalRemainingCents: Long = 0,
    val percentage: Float = 0f,
    val isExceeded: Boolean = false
)

data class CashRegisterSummary(
    val id: Long,
    val name: String,
    val description: String,
    val initialBalanceCents: Long,
    val iconType: String,
    val isDefault: Boolean,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val currentBalanceCents: Long,
    val transactionCount: Int
)

/** Point mensuel pour les graphiques du bilan (12 derniers mois maximum). */
data class MonthlyPoint(
    val monthKey: String,
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long
) {
    val netCents: Long get() = incomeCents - expenseCents
}

/** Agrégats consolidés d'une période, base du rapport professionnel. */
data class BilanSnapshot(
    val totalProductsCents: Long = 0,
    val totalChargesCents: Long = 0,
    val resultatCents: Long = 0,
    val tauxEpargnePercent: Double = 0.0,
    val tresorerieTotaleCents: Long = 0,
    val totalTransfersCents: Long = 0,
    val productsByCategory: List<Pair<String, Long>> = emptyList(),
    val chargesByCategory: List<Pair<String, Long>> = emptyList(),
    val registerBalances: List<Pair<String, Long>> = emptyList(),
    val monthlySeries: List<MonthlyPoint> = emptyList()
)

data class FinancialUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val periodTransactions: List<TransactionEntity> = emptyList(),
    val totalIncomeCents: Long = 0,
    val totalExpenseCents: Long = 0,
    val netBalanceCents: Long = 0,
    val totalCashBalanceCents: Long = 0,
    val savingsRatePercent: Double = 0.0,
    val categoryExpenses: Map<String, Long> = emptyMap(),
    val categoryIncomes: Map<String, Long> = emptyMap(),
    val selectedPeriod: PeriodFilter = PeriodFilter.CE_MOIS,
    val selectedTypeFilter: TransactionType? = null,
    val selectedCategoryFilter: String? = null,
    val minAmountCents: Long = 0L,
    val maxAmountCents: Long = 0L,
    val sortMode: SortMode = SortMode.DATE_DESC,
    val searchQuery: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val cashRegisters: List<CashRegisterEntity> = emptyList(),
    val selectedCashRegisterId: Long? = null,
    val selectedCashRegisterName: String = "Toutes les caisses",
    val cashRegisterSummaries: List<CashRegisterSummary> = emptyList(),
    val budgetMonthKey: String = "",
    val budgetMonthLabel: String = "",
    val categoryBudgetProgress: List<CategoryBudgetProgress> = emptyList(),
    val globalBudgetSummary: GlobalBudgetSummary = GlobalBudgetSummary(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amountDisplayMode: AmountDisplayMode = AmountDisplayMode.STANDARD,
    val currencySymbol: String = "FCFA",
    val profile: ChurchProfileEntity = ChurchProfileEntity(),
    val members: List<MemberEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val currentUser: UserEntity? = null,
    val bilan: BilanSnapshot = BilanSnapshot(),
    val activeFilterCount: Int = 0,
    val canWrite: Boolean = true,
    val canDelete: Boolean = true,
    val canManageUsers: Boolean = true,
    val isLoading: Boolean = false
)

/** Événement destiné à une notification système locale. */
data class NotificationEvent(val title: String, val body: String)

class FinancialViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.FRANCE)
    private val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.CE_MOIS)
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _minAmountCents = MutableStateFlow(0L)
    private val _maxAmountCents = MutableStateFlow(0L)
    private val _sortMode = MutableStateFlow(SortMode.DATE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _currentBudgetCalendar = MutableStateFlow(Calendar.getInstance())
    private val _themeMode = MutableStateFlow<ThemeMode?>(null)
    private val _amountDisplayMode = MutableStateFlow<AmountDisplayMode?>(null)
    private val _selectedCashRegisterId = MutableStateFlow<Long?>(null)
    private val _currentUser = MutableStateFlow<UserEntity?>(null)

    private val _eventFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private val _notificationFlow = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 8)
    val notificationFlow: SharedFlow<NotificationEvent> = _notificationFlow.asSharedFlow()

    val uiState: StateFlow<FinancialUiState> = combine(
        repository.allTransactions,
        repository.allCategories,
        repository.allBudgets,
        repository.allCashRegisters,
        repository.profile,
        repository.allMembers,
        _selectedPeriod,
        _selectedTypeFilter,
        _selectedCategoryFilter,
        _searchQuery,
        _currentBudgetCalendar,
        _themeMode,
        _amountDisplayMode,
        _selectedCashRegisterId,
        _minAmountCents,
        _maxAmountCents,
        _sortMode,
        repository.allUsers,
        _currentUser
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allTx = args[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val allCategories = args[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allBudgets = args[2] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val allRegisters = args[3] as List<CashRegisterEntity>
        val profile = (args[4] as? ChurchProfileEntity) ?: ChurchProfileEntity()
        @Suppress("UNCHECKED_CAST")
        val allMembers = args[5] as List<MemberEntity>
        val period = args[6] as PeriodFilter
        val typeFilter = args[7] as TransactionType?
        val categoryFilter = args[8] as String?
        val query = args[9] as String
        val budgetCal = args[10] as Calendar
        val themeOverride = args[11] as ThemeMode?
        val displayOverride = args[12] as AmountDisplayMode?
        val selectedRegId = args[13] as Long?
        val minAmount = args[14] as Long
        val maxAmount = args[15] as Long
        val sortMode = args[16] as SortMode
        @Suppress("UNCHECKED_CAST")
        val users = args[17] as List<UserEntity>
        val currentUser = args[18] as? UserEntity

        val themeMode = themeOverride ?: runCatching { ThemeMode.valueOf(profile.themeMode) }
            .getOrDefault(ThemeMode.SYSTEM)
        val displayMode = displayOverride ?: runCatching { AmountDisplayMode.valueOf(profile.amountDisplayMode) }
            .getOrDefault(AmountDisplayMode.STANDARD)

        val budgetMonthKey = monthFormat.format(budgetCal.time)
        val budgetMonthLabel = monthDisplayFormat.format(budgetCal.time)
            .replaceFirstChar { it.uppercase() }

        // ==========================================
        // 1. Soldes réels de chaque caisse (virements inclus : ce sont des
        //    mouvements de trésorerie, jamais des produits ou des charges)
        // ==========================================
        val registerSummaries = allRegisters.map { reg ->
            val regTx = allTx.filter { it.cashRegisterId == reg.id }
            val inc = regTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
            val exp = regTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            CashRegisterSummary(
                id = reg.id,
                name = reg.name,
                description = reg.description,
                initialBalanceCents = reg.initialBalanceCents,
                iconType = reg.iconType,
                isDefault = reg.isDefault,
                totalIncomeCents = inc,
                totalExpenseCents = exp,
                currentBalanceCents = reg.initialBalanceCents + inc - exp,
                transactionCount = regTx.size
            )
        }
        val totalCashBalance = registerSummaries.sumOf { it.currentBalanceCents }

        val activeRegisterName = if (selectedRegId != null) {
            allRegisters.find { it.id == selectedRegId }?.name ?: "Caisse"
        } else {
            "Toutes les caisses"
        }

        // ==========================================
        // 2. Périmètre de la période et de la caisse
        // ==========================================
        val (startTime, endTime) = calculateTimeBounds(period)
        val periodBaseTx = allTx.filter { tx ->
            tx.timestamp in startTime..endTime &&
                (selectedRegId == null || tx.cashRegisterId == selectedRegId)
        }

        // Les virements internes sont exclus des produits et des charges :
        // ils ne font que déplacer de la trésorerie d'une caisse à l'autre.
        val operatingTx = periodBaseTx.filter { !it.isTransfer }
        val transferTx = periodBaseTx.filter { it.isTransfer }

        val totalIncome = operatingTx
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }
        val totalExpense = operatingTx
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountCents }
        val netBalance = totalIncome - totalExpense

        val savingsRate = if (totalIncome > 0) {
            (((totalIncome - totalExpense).toDouble() / totalIncome.toDouble()) * 100.0)
                .coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        val catExpenses = operatingTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        val catIncomes = operatingTx
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        // ==========================================
        // 3. Filtres de l'historique (période, caisse, type, catégorie,
        //    montant, recherche, tri) — tous déterministes et combinés.
        // ==========================================
        val filtered = periodBaseTx.filter { tx ->
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = categoryFilter == null || tx.category == categoryFilter
            val matchesQuery = query.isBlank() ||
                tx.title.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.cashRegisterName.contains(query, ignoreCase = true) ||
                tx.note.contains(query, ignoreCase = true)
            val matchesMin = minAmount <= 0L || tx.amountCents >= minAmount
            val matchesMax = maxAmount <= 0L || tx.amountCents <= maxAmount
            matchesType && matchesCategory && matchesQuery && matchesMin && matchesMax
        }
        val filteredTx = when (sortMode) {
            SortMode.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            SortMode.DATE_ASC -> filtered.sortedBy { it.timestamp }
            SortMode.MONTANT_DESC -> filtered.sortedWith(
                compareByDescending<TransactionEntity> { it.amountCents }.thenByDescending { it.timestamp }
            )
            SortMode.MONTANT_ASC -> filtered.sortedWith(
                compareBy<TransactionEntity> { it.amountCents }.thenByDescending { it.timestamp }
            )
        }

        val activeFilterCount = listOf(
            typeFilter != null,
            categoryFilter != null,
            query.isNotBlank(),
            minAmount > 0L,
            maxAmount > 0L,
            sortMode != SortMode.DATE_DESC,
            period != PeriodFilter.CE_MOIS,
            selectedRegId != null
        ).count { it }

        // ==========================================
        // 4. Budgets du mois affiché
        // ==========================================
        val (bStart, bEnd) = calculateMonthBounds(budgetCal)
        val monthExpenseTx = allTx.filter {
            it.type == TransactionType.EXPENSE && !it.isTransfer &&
                it.timestamp in bStart..bEnd &&
                (selectedRegId == null || it.cashRegisterId == selectedRegId)
        }
        val spentPerCategory = monthExpenseTx
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }

        val periodBudgets = allBudgets.filter { it.monthYearKey == budgetMonthKey }
        val budgetMap = periodBudgets.associateBy { it.categoryName }
        val expenseCategories = allCategories.filter { it.type == TransactionType.EXPENSE }
        val relevantCategories =
            (expenseCategories.map { it.name } + budgetMap.keys + spentPerCategory.keys).distinct()
        val categoryColorMap = allCategories.associate { it.name to it.colorHex }

        val progressList = relevantCategories.mapNotNull { catName ->
            val budget = budgetMap[catName]
            val spent = spentPerCategory[catName] ?: 0L
            val ceiling = budget?.amountCents ?: 0L
            if (ceiling > 0L || spent > 0L) {
                val percentage = if (ceiling > 0L) {
                    (spent.toDouble() / ceiling.toDouble()).toFloat()
                } else {
                    1.0f
                }
                CategoryBudgetProgress(
                    categoryName = catName,
                    categoryColorHex = categoryColorMap[catName] ?: ChurchProfileEntity.DEFAULT_ACCENT,
                    budgetId = budget?.id,
                    budgetAmountCents = ceiling,
                    spentAmountCents = spent,
                    percentage = percentage,
                    remainingCents = ceiling - spent,
                    isExceeded = ceiling > 0L && spent > ceiling,
                    isNearLimit = ceiling > 0L && spent <= ceiling &&
                        (spent.toDouble() / ceiling.toDouble()) >= 0.8
                )
            } else {
                null
            }
        }.sortedWith(
            compareByDescending<CategoryBudgetProgress> { it.isExceeded }
                .thenByDescending { it.percentage }
                .thenByDescending { it.budgetAmountCents }
        )

        val totalCeiling = periodBudgets.sumOf { it.amountCents }
        val totalSpentInBudgetedCategories =
            periodBudgets.sumOf { b -> spentPerCategory[b.categoryName] ?: 0L }
        val globalSummary = GlobalBudgetSummary(
            totalBudgetCeilingCents = totalCeiling,
            totalSpentCents = totalSpentInBudgetedCategories,
            totalRemainingCents = totalCeiling - totalSpentInBudgetedCategories,
            percentage = if (totalCeiling > 0L) {
                (totalSpentInBudgetedCategories.toDouble() / totalCeiling.toDouble()).toFloat()
            } else {
                0f
            },
            isExceeded = totalCeiling > 0L && totalSpentInBudgetedCategories > totalCeiling
        )

        // ==========================================
        // 5. Agrégats du bilan et série mensuelle (graphiques)
        // ==========================================
        val monthlySeries = buildMonthlySeries(allTx, selectedRegId)
        val bilan = BilanSnapshot(
            totalProductsCents = totalIncome,
            totalChargesCents = totalExpense,
            resultatCents = netBalance,
            tauxEpargnePercent = savingsRate,
            tresorerieTotaleCents = totalCashBalance,
            totalTransfersCents = transferTx.sumOf { it.amountCents } / 2,
            productsByCategory = catIncomes.toList(),
            chargesByCategory = catExpenses.toList(),
            registerBalances = registerSummaries.map { it.name to it.currentBalanceCents },
            monthlySeries = monthlySeries
        )

        FinancialUiState(
            transactions = filteredTx,
            periodTransactions = periodBaseTx,
            totalIncomeCents = totalIncome,
            totalExpenseCents = totalExpense,
            netBalanceCents = netBalance,
            totalCashBalanceCents = totalCashBalance,
            savingsRatePercent = savingsRate,
            categoryExpenses = catExpenses,
            categoryIncomes = catIncomes,
            selectedPeriod = period,
            selectedTypeFilter = typeFilter,
            selectedCategoryFilter = categoryFilter,
            minAmountCents = minAmount,
            maxAmountCents = maxAmount,
            sortMode = sortMode,
            searchQuery = query,
            categories = allCategories,
            budgets = allBudgets,
            cashRegisters = allRegisters,
            selectedCashRegisterId = selectedRegId,
            selectedCashRegisterName = activeRegisterName,
            cashRegisterSummaries = registerSummaries,
            budgetMonthKey = budgetMonthKey,
            budgetMonthLabel = budgetMonthLabel,
            categoryBudgetProgress = progressList,
            globalBudgetSummary = globalSummary,
            themeMode = themeMode,
            amountDisplayMode = displayMode,
            currencySymbol = profile.currencyCode.ifBlank { "FCFA" },
            profile = profile,
            members = allMembers,
            bilan = bilan,
            activeFilterCount = activeFilterCount,
            users = users,
            currentUser = currentUser,
            canWrite = currentUser?.roleValue?.canWrite ?: true,
            canDelete = currentUser?.roleValue?.canDelete ?: true,
            canManageUsers = currentUser?.roleValue?.canManageUsers ?: true,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialUiState(isLoading = true)
    )

    // ==========================================
    // Filtres
    // ==========================================
    fun setPeriod(period: PeriodFilter) { _selectedPeriod.value = period }
    fun setTypeFilter(type: TransactionType?) { _selectedTypeFilter.value = type }
    fun setCategoryFilter(category: String?) { _selectedCategoryFilter.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setAmountRange(minCents: Long, maxCents: Long) {
        _minAmountCents.value = if (minCents < 0L) 0L else minCents
        _maxAmountCents.value = if (maxCents < 0L) 0L else maxCents
    }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }
    fun setSelectedCashRegister(id: Long?) { _selectedCashRegisterId.value = id }

    fun clearFilters() {
        _selectedPeriod.value = PeriodFilter.CE_MOIS
        _selectedTypeFilter.value = null
        _selectedCategoryFilter.value = null
        _searchQuery.value = ""
        _minAmountCents.value = 0L
        _maxAmountCents.value = 0L
        _sortMode.value = SortMode.DATE_DESC
        _selectedCashRegisterId.value = null
    }

    /** Contrôle des droits avant toute modification des données. */
    private suspend fun ensurePermission(needDelete: Boolean = false): Boolean {
        val role = _currentUser.value?.roleValue ?: return true
        return when {
            needDelete && !role.canDelete -> {
                _eventFlow.emit("Votre profil ne permet pas de supprimer une écriture")
                false
            }
            !role.canWrite -> {
                _eventFlow.emit("Votre profil est en lecture seule")
                false
            }
            else -> true
        }
    }

    // ==========================================
    // Utilisateurs de l'appareil (rôles & PIN)
    // ==========================================
    fun login(userId: Long, pin: String) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user == null) {
                _eventFlow.emit("Utilisateur introuvable")
                return@launch
            }
            val ok = when {
                !user.hasPin -> pin.isBlank()
                else -> PinHasher.verify(pin, user.pinSalt, user.pinHash)
            }
            if (!ok) {
                _eventFlow.emit("Code incorrect")
                return@launch
            }
            repository.touchUserLogin(user.id)
            _currentUser.value = user.copy(lastLoginAt = System.currentTimeMillis())
            _eventFlow.emit("Bienvenue ${user.displayName}")
        }
    }

    /** Crée le premier accès du téléphone et ouvre directement la session. */
    fun createFirstUser(displayName: String, role: UserRole, pin: String) {
        viewModelScope.launch {
            val name = displayName.trim()
            if (name.isBlank() || pin.length < 4) {
                _eventFlow.emit("Indiquez un nom et un code d'au moins 4 chiffres")
                return@launch
            }
            val salt = PinHasher.newSalt()
            val id = repository.saveUser(
                UserEntity(
                    displayName = name,
                    role = role.name,
                    pinSalt = salt,
                    pinHash = PinHasher.hash(pin, salt)
                )
            )
            _currentUser.value = repository.getUserById(id)
            _eventFlow.emit("Bienvenue $name")
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun addUser(displayName: String, role: UserRole, pin: String) {
        viewModelScope.launch {
            val name = displayName.trim()
            if (name.isBlank()) {
                _eventFlow.emit("Le nom de l'utilisateur est obligatoire")
                return@launch
            }
            val salt = PinHasher.newSalt()
            repository.saveUser(
                UserEntity(
                    displayName = name,
                    role = role.name,
                    pinSalt = salt,
                    pinHash = if (pin.isBlank()) "" else PinHasher.hash(pin, salt)
                )
            )
            _eventFlow.emit("$name ajouté(e) comme ${role.label}")
        }
    }

    fun updateUser(user: UserEntity, displayName: String, role: UserRole, newPin: String) {
        viewModelScope.launch {
            val name = displayName.trim()
            if (name.isBlank()) {
                _eventFlow.emit("Le nom de l'utilisateur est obligatoire")
                return@launch
            }
            val salt = if (newPin.isBlank()) user.pinSalt else PinHasher.newSalt()
            repository.saveUser(
                user.copy(
                    displayName = name,
                    role = role.name,
                    pinSalt = salt,
                    pinHash = when {
                        newPin.isBlank() -> user.pinHash
                        else -> PinHasher.hash(newPin, salt)
                    },
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = repository.getUserById(user.id)
            }
            _eventFlow.emit("$name mis(e) à jour")
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            if (repository.getUsersCount() <= 1) {
                _eventFlow.emit("Il doit rester au moins un utilisateur")
                return@launch
            }
            repository.deleteUser(user.id)
            if (_currentUser.value?.id == user.id) _currentUser.value = null
            _eventFlow.emit("${user.displayName} retiré(e)")
        }
    }

    // ==========================================
    // Synchronisation par fichier chiffré
    // ==========================================
    fun exportSyncFile(context: Context, passphrase: String, onReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            try {
                if (passphrase.length < 6) {
                    _eventFlow.emit("La phrase de passe doit contenir au moins 6 caractères")
                    return@launch
                }
                val state = uiState.value
                val directory = java.io.File(context.cacheDir, "echanges")
                val file = SyncManager.exportFile(
                    context = context,
                    repository = repository,
                    profile = state.profile,
                    passphrase = passphrase,
                    authorName = state.currentUser?.displayName ?: "",
                    directory = directory
                )
                onReady(file)
                _eventFlow.emit("Fichier d'échange prêt (${state.periodTransactions.size} écritures de la période)")
            } catch (e: Exception) {
                _eventFlow.emit("Export impossible : ${e.localizedMessage}")
            }
        }
    }

    fun importSyncFile(context: Context, uri: android.net.Uri, passphrase: String) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            try {
                val report = SyncManager.importFile(context, repository, uri, passphrase)
                val message = "Fusion avec ${report.sourceChurch.ifBlank { "un autre appareil" }} : ${report.summary()}"
                _eventFlow.emit(message)
                _notificationFlow.emit(NotificationEvent("Synchronisation terminée", message))
            } catch (e: Exception) {
                _eventFlow.emit("Import impossible : ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // Profil de l'église & préférences
    // ==========================================
    fun completeOnboarding(
        churchName: String,
        denomination: String,
        city: String,
        address: String,
        phone: String,
        pastorName: String,
        treasurerName: String,
        logoBase64: String,
        accentColorHex: String
    ) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: ChurchProfileEntity()
            repository.saveProfile(
                current.copy(
                    churchName = churchName.trim(),
                    denomination = denomination.trim(),
                    city = city.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    pastorName = pastorName.trim(),
                    treasurerName = treasurerName.trim(),
                    logoBase64 = logoBase64,
                    accentColorHex = accentColorHex,
                    onboardingCompleted = true,
                    themeMode = repository.getProfile()?.themeMode ?: "SYSTEM",
                    amountDisplayMode = repository.getProfile()?.amountDisplayMode ?: "STANDARD",
                    updatedAt = System.currentTimeMillis()
                )
            )
            _eventFlow.emit("Bienvenue à ${churchName.trim()} !")
            _notificationFlow.emit(
                NotificationEvent(
                    title = "Dossier prêt",
                    body = "${churchName.trim()} est configurée. Vous pouvez saisir vos premières opérations."
                )
            )
        }
    }

    /** Met à jour un champ libre du profil (nom, logo, palette, contacts…). */
    fun updateProfile(transform: (ChurchProfileEntity) -> ChurchProfileEntity) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: ChurchProfileEntity()
            repository.saveProfile(transform(current).copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun setAccentColor(hex: String) = updateProfile { it.copy(accentColorHex = hex) }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        updateProfile { it.copy(themeMode = mode.name) }
    }

    fun setAmountDisplayMode(mode: AmountDisplayMode) {
        _amountDisplayMode.value = mode
        updateProfile { it.copy(amountDisplayMode = mode.name) }
    }

    fun toggleDiscreetMode() {
        val next = if (uiState.value.amountDisplayMode == AmountDisplayMode.DISCRET) {
            AmountDisplayMode.STANDARD
        } else {
            AmountDisplayMode.DISCRET
        }
        setAmountDisplayMode(next)
    }

    fun completeOnboardingFromScreen(profile: ChurchProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(
                profile.copy(
                    churchName = profile.churchName.trim(),
                    onboardingCompleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _eventFlow.emit("Bienvenue à ${profile.churchName.trim()} !")
        }
    }

    // ==========================================
    // Membres
    // ==========================================
    fun addMember(member: MemberEntity) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            if (member.firstName.isBlank() || member.lastName.isBlank()) {
                _eventFlow.emit("Le prénom et le nom sont obligatoires")
                return@launch
            }
            repository.insertMember(member)
            _eventFlow.emit("${member.fullName} ajouté(e) au registre")
        }
    }

    fun updateMember(member: MemberEntity) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            if (member.firstName.isBlank() || member.lastName.isBlank()) {
                _eventFlow.emit("Le prénom et le nom sont obligatoires")
                return@launch
            }
            repository.updateMember(member)
            _eventFlow.emit("Fiche de ${member.fullName} mise à jour")
        }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch {
            if (!ensurePermission(needDelete = true)) return@launch
            repository.deleteMember(member)
            _eventFlow.emit("${member.fullName} retiré(e) du registre")
        }
    }

    // ==========================================
    // Budgets
    // ==========================================
    fun changeBudgetMonth(offset: Int) {
        _currentBudgetCalendar.value =
            (_currentBudgetCalendar.value.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
    }

    fun resetBudgetMonthToCurrent() {
        _currentBudgetCalendar.value = Calendar.getInstance()
    }

    fun saveBudget(categoryName: String, amountCents: Long) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            val monthKey = monthFormat.format(_currentBudgetCalendar.value.time)
            repository.saveBudget(categoryName, amountCents, monthKey)
            _eventFlow.emit("Plafond de ${CurrencyFormatter.format(amountCents)} défini pour \"$categoryName\"")
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            if (!ensurePermission(needDelete = true)) return@launch
            repository.deleteBudget(id)
            _eventFlow.emit("Budget supprimé")
        }
    }

    // ==========================================
    // Caisses
    // ==========================================
    fun addCashRegister(name: String, description: String, initialBalanceCents: Long, iconType: String) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            val entity = CashRegisterEntity(
                name = name.trim(),
                description = description.trim(),
                initialBalanceCents = initialBalanceCents,
                iconType = iconType,
                isDefault = false
            )
            repository.insertCashRegister(entity)
            _eventFlow.emit("Caisse \"${entity.name}\" créée")
        }
    }

    fun updateCashRegister(
        id: Long,
        name: String,
        description: String,
        initialBalanceCents: Long,
        iconType: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            val entity = CashRegisterEntity(
                id = id,
                name = name.trim(),
                description = description.trim(),
                initialBalanceCents = initialBalanceCents,
                iconType = iconType,
                isDefault = isDefault
            )
            repository.updateCashRegister(entity)
            _eventFlow.emit("Caisse \"${entity.name}\" mise à jour")
        }
    }

    fun deleteCashRegister(id: Long) {
        viewModelScope.launch {
            if (!ensurePermission(needDelete = true)) return@launch
            val reg = repository.getCashRegisterById(id)
            if (reg != null) {
                if (reg.isDefault) {
                    _eventFlow.emit("Impossible de supprimer la caisse principale")
                    return@launch
                }
                val remaining = repository.getCashRegisterCount()
                repository.deleteCashRegisterAndTransactions(id)
                if (_selectedCashRegisterId.value == id) _selectedCashRegisterId.value = null
                _eventFlow.emit(
                    "Caisse \"${reg.name}\" supprimée avec ses écritures ($remaining caisse(s) restante(s))"
                )
            }
        }
    }

    /**
     * Virement interne déterministe.
     *
     * Deux écritures indissociables partagent le même [TransactionEntity.transferGroupId] :
     * un débit sur la caisse source et un crédit sur la caisse cible, au même
     * horodatage. Elles modifient les soldes des deux caisses mais n'entrent
     * jamais dans les produits ou les charges du bilan.
     */
    fun transferBetweenCaisses(
        sourceId: Long,
        targetId: Long,
        amountCents: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            if (sourceId == targetId) {
                _eventFlow.emit("La caisse de départ et la caisse d'arrivée doivent être différentes")
                return@launch
            }
            if (amountCents <= 0L) {
                _eventFlow.emit("Le montant du virement doit être supérieur à 0")
                return@launch
            }

            val sourceReg = repository.getCashRegisterById(sourceId)
            val targetReg = repository.getCashRegisterById(targetId)
            if (sourceReg == null || targetReg == null) {
                _eventFlow.emit("Caisse introuvable")
                return@launch
            }

            val sourceSummary = uiState.value.cashRegisterSummaries.find { it.id == sourceId }
            val available = sourceSummary?.currentBalanceCents ?: 0L
            if (available < amountCents) {
                _eventFlow.emit(
                    "Solde insuffisant dans ${sourceReg.name} " +
                        "(${CurrencyFormatter.format(available)} disponibles pour " +
                        "${CurrencyFormatter.format(amountCents)} demandés)"
                )
                return@launch
            }

            val groupId = UUID.randomUUID().toString()
            val reference = note.trim().ifBlank { "Virement interne" }

            val debit = TransactionEntity(
                title = "Virement vers ${targetReg.name}",
                type = TransactionType.EXPENSE,
                amountCents = amountCents,
                category = "Virement interne",
                timestamp = timestamp,
                note = reference,
                cashRegisterId = sourceReg.id,
                cashRegisterName = sourceReg.name,
                transferGroupId = groupId
            )
            val credit = TransactionEntity(
                title = "Virement depuis ${sourceReg.name}",
                type = TransactionType.INCOME,
                amountCents = amountCents,
                category = "Virement interne",
                timestamp = timestamp,
                note = reference,
                cashRegisterId = targetReg.id,
                cashRegisterName = targetReg.name,
                transferGroupId = groupId
            )
            repository.insertTransactions(listOf(debit, credit))

            val newSource = available - amountCents
            val newTarget = (uiState.value.cashRegisterSummaries.find { it.id == targetId }
                ?.currentBalanceCents ?: 0L) + amountCents
            val message = buildString {
                append("Virement de ${CurrencyFormatter.format(amountCents)} : ")
                append("${sourceReg.name} ${CurrencyFormatter.format(available)} → ${CurrencyFormatter.format(newSource)}")
                append(" • ${targetReg.name} ${CurrencyFormatter.format(newTarget - amountCents)} → ${CurrencyFormatter.format(newTarget)}")
            }
            _eventFlow.emit(message)
            _notificationFlow.emit(NotificationEvent("Virement enregistré", message))
        }
    }

    /** Supprime un virement complet (les deux écritures) ou une opération simple. */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            if (!ensurePermission(needDelete = true)) return@launch
            val tx = repository.getTransactionById(id)
            if (tx != null && tx.isTransfer) {
                repository.deleteByTransferGroup(tx.transferGroupId)
                _eventFlow.emit("Virement supprimé des deux caisses")
            } else {
                repository.deleteById(id)
                _eventFlow.emit("Opération supprimée")
            }
        }
    }

    // ==========================================
    // Catégories
    // ==========================================
    fun addCategory(name: String, type: TransactionType, colorHex: String) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            repository.insertCategory(
                CategoryEntity(
                    name = trimmedName,
                    type = type,
                    colorHex = colorHex.ifBlank { ChurchProfileEntity.DEFAULT_ACCENT },
                    isDefault = false
                )
            )
            _eventFlow.emit("Catégorie \"$trimmedName\" créée")
        }
    }

    fun updateCategory(id: Long, name: String, type: TransactionType, colorHex: String, isDefault: Boolean) {
        viewModelScope.launch {
            if (!ensurePermission()) return@launch
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val previous = repository.getCategoryById(id)
            repository.updateCategory(
                CategoryEntity(
                    id = id,
                    name = trimmedName,
                    type = type,
                    colorHex = colorHex,
                    isDefault = isDefault
                )
            )
            // Renommage : on propage aux écritures et budgets existants.
            if (previous != null && previous.name != trimmedName) {
                repository.renameCategoryEverywhere(previous.name, trimmedName)
            }
            _eventFlow.emit("Catégorie \"$trimmedName\" modifiée")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!ensurePermission(needDelete = true)) return@launch
            repository.deleteCategory(category)
            repository.deleteBudgetByCategory(category.name)
            _eventFlow.emit("Catégorie \"${category.name}\" supprimée")
        }
    }

    // ==========================================
    // Transactions
    // ==========================================
    fun addTransaction(
        title: String,
        type: TransactionType,
        amountCents: Long,
        category: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        cashRegisterId: Long = 1L,
        cashRegisterName: String = "Caisse principale",
        receiptPhotoUri: String = ""
    ) {
        viewModelScope.launch {
            if (!uiState.value.canWrite) {
                _eventFlow.emit("Votre profil est en lecture seule")
                return@launch
            }
            val entity = TransactionEntity(
                title = title.trim(),
                type = type,
                amountCents = amountCents,
                category = category.trim().ifBlank {
                    if (type == TransactionType.INCOME) "Autres entrées" else "Autres sorties"
                },
                timestamp = timestamp,
                note = note.trim(),
                cashRegisterId = cashRegisterId,
                cashRegisterName = cashRegisterName,
                receiptPhotoUri = receiptPhotoUri,
                updatedBy = uiState.value.currentUser?.displayName ?: ""
            )
            repository.insert(entity)

            val amountLabel = CurrencyFormatter.format(amountCents)
            _eventFlow.emit("Opération \"${entity.title}\" enregistrée sur ${entity.cashRegisterName}")
            _notificationFlow.emit(
                NotificationEvent(
                    title = if (type == TransactionType.INCOME) "Entrée enregistrée" else "Sortie enregistrée",
                    body = "$amountLabel • ${entity.title} (${entity.cashRegisterName})"
                )
            )

            // Alerte budgétaire : même seuil que l'affichage, calculé sur le mois courant
            val current = uiState.value
            val budget = current.budgets.find {
                it.categoryName == entity.category && it.monthYearKey == current.budgetMonthKey
            }
            if (type == TransactionType.EXPENSE && budget != null && budget.amountCents > 0L) {
                val spent = current.categoryBudgetProgress
                    .find { it.categoryName == entity.category }?.spentAmountCents
                    ?: 0L
                val ratio = (spent.toDouble() / budget.amountCents.toDouble()) * 100.0
                val threshold = budget.alertThresholdPercent.toDouble().coerceIn(1.0, 100.0)
                if (ratio >= 100.0) {
                    _notificationFlow.emit(
                        NotificationEvent(
                            "Plafond dépassé : ${entity.category}",
                            "${CurrencyFormatter.format(spent)} dépensés sur ${CurrencyFormatter.format(budget.amountCents)}"
                        )
                    )
                } else if (ratio >= threshold) {
                    _notificationFlow.emit(
                        NotificationEvent(
                            "Seuil d'alerte atteint : ${entity.category}",
                            "${ratio.toInt()} % du plafond mensuel consommé"
                        )
                    )
                }
            }
        }
    }

    // ==========================================
    // Exports
    // ==========================================
    fun exportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val pdfFile = ReportExporter.generatePdfReport(
                    context = context,
                    profile = state.profile,
                    periodLabel = "${state.selectedPeriod.label} (${state.selectedCashRegisterName})",
                    transactions = state.periodTransactions,
                    totalIncomeCents = state.totalIncomeCents,
                    totalExpenseCents = state.totalExpenseCents,
                    netBalanceCents = state.netBalanceCents,
                    bilan = state.bilan,
                    memberCount = state.members.size
                )
                ReportExporter.shareFile(
                    context = context,
                    file = pdfFile,
                    mimeType = "application/pdf",
                    subject = "Bilan financier ${state.profile.displayName} — ${state.selectedPeriod.label}"
                )
                _eventFlow.emit("Rapport PDF généré")
            } catch (e: Exception) {
                _eventFlow.emit("Erreur lors de la génération PDF : ${e.localizedMessage}")
            }
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val csvFile = ReportExporter.generateCsvExport(
                    context = context,
                    periodLabel = "${state.selectedPeriod.label} (${state.selectedCashRegisterName})",
                    transactions = state.periodTransactions,
                    totalIncomeCents = state.totalIncomeCents,
                    totalExpenseCents = state.totalExpenseCents,
                    netBalanceCents = state.netBalanceCents
                )
                ReportExporter.shareFile(
                    context = context,
                    file = csvFile,
                    mimeType = "text/csv",
                    subject = "Relevé ${state.profile.displayName} — ${state.selectedPeriod.label}"
                )
                _eventFlow.emit("Export CSV généré")
            } catch (e: Exception) {
                _eventFlow.emit("Erreur lors de l'export CSV : ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // Aides au calcul
    // ==========================================
    private fun buildMonthlySeries(
        allTx: List<TransactionEntity>,
        selectedRegId: Long?
    ): List<MonthlyPoint> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -5)

        val labelFormat = SimpleDateFormat("MMM yy", Locale.FRANCE)
        val result = mutableListOf<MonthlyPoint>()
        repeat(6) {
            val key = monthFormat.format(cal.time)
            val (start, end) = calculateMonthBounds(cal)
            val monthTx = allTx.filter {
                !it.isTransfer && it.timestamp in start..end &&
                    (selectedRegId == null || it.cashRegisterId == selectedRegId)
            }
            result.add(
                MonthlyPoint(
                    monthKey = key,
                    label = labelFormat.format(cal.time).replaceFirstChar { c -> c.uppercase() },
                    incomeCents = monthTx.filter { t -> t.type == TransactionType.INCOME }
                        .sumOf { t -> t.amountCents },
                    expenseCents = monthTx.filter { t -> t.type == TransactionType.EXPENSE }
                        .sumOf { t -> t.amountCents }
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    private fun calculateMonthBounds(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return Pair(start, cal.timeInMillis)
    }

    private fun calculateTimeBounds(period: PeriodFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        fun startOfMonth() {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return when (period) {
            PeriodFilter.TOUT -> Pair(0L, Long.MAX_VALUE)
            PeriodFilter.CE_MOIS -> {
                startOfMonth()
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                Pair(start, cal.timeInMillis)
            }
            PeriodFilter.MOIS_DERNIER -> {
                cal.add(Calendar.MONTH, -1)
                startOfMonth()
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                Pair(start, cal.timeInMillis)
            }
            PeriodFilter.TROIS_MOIS -> {
                startOfMonth()
                cal.add(Calendar.MONTH, -2)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 3)
                cal.add(Calendar.MILLISECOND, -1)
                Pair(start, cal.timeInMillis)
            }
            PeriodFilter.CETTE_ANNEE -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                Pair(start, cal.timeInMillis)
            }
        }
    }
}

class FinancialViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinancialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
