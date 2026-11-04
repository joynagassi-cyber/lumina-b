package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.data.TransactionType
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

enum class PeriodFilter(val label: String) {
    CE_MOIS("Ce mois"),
    MOIS_DERNIER("Mois dernier"),
    CETTE_ANNEE("Cette année"),
    TOUT("Tout")
}

data class CategoryBudgetProgress(
    val categoryName: String,
    val categoryColorHex: String,
    val budgetId: Long?,
    val budgetAmountCents: Long,
    val spentAmountCents: Long,
    val percentage: Float, // 0.0f .. 1.0f+
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

data class FinancialUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalIncomeCents: Long = 0,
    val totalExpenseCents: Long = 0,
    val netBalanceCents: Long = 0,
    val totalCashBalanceCents: Long = 0, // Somme de toutes les caisses
    val savingsRatePercent: Double = 0.0,
    val categoryExpenses: Map<String, Long> = emptyMap(),
    val categoryIncomes: Map<String, Long> = emptyMap(),
    val selectedPeriod: PeriodFilter = PeriodFilter.CE_MOIS,
    val selectedTypeFilter: TransactionType? = null,
    val searchQuery: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val cashRegisters: List<CashRegisterEntity> = emptyList(),
    val selectedCashRegisterId: Long? = null, // null = Toutes les caisses
    val selectedCashRegisterName: String = "Toutes les caisses",
    val cashRegisterSummaries: List<CashRegisterSummary> = emptyList(),
    val budgetMonthKey: String = "",
    val budgetMonthLabel: String = "",
    val categoryBudgetProgress: List<CategoryBudgetProgress> = emptyList(),
    val globalBudgetSummary: GlobalBudgetSummary = GlobalBudgetSummary(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amountDisplayMode: AmountDisplayMode = AmountDisplayMode.STANDARD,
    val currencySymbol: String = "FCFA",
    val isLoading: Boolean = false
)

class FinancialViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.FRANCE)
    private val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.CE_MOIS)
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _currentBudgetCalendar = MutableStateFlow(Calendar.getInstance())
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _amountDisplayMode = MutableStateFlow(AmountDisplayMode.STANDARD)
    private val _selectedCashRegisterId = MutableStateFlow<Long?>(null)

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    val uiState: StateFlow<FinancialUiState> = combine(
        repository.allTransactions,
        repository.allCategories,
        repository.allBudgets,
        repository.allCashRegisters,
        _selectedPeriod,
        _selectedTypeFilter,
        _searchQuery,
        _currentBudgetCalendar,
        _themeMode,
        _amountDisplayMode,
        _selectedCashRegisterId
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allTx = args[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val allCategories = args[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allBudgets = args[2] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val allRegisters = args[3] as List<CashRegisterEntity>
        val period = args[4] as PeriodFilter
        val typeFilter = args[5] as TransactionType?
        val query = args[6] as String
        val budgetCal = args[7] as Calendar
        val themeMode = args[8] as ThemeMode
        val displayMode = args[9] as AmountDisplayMode
        val selectedRegId = args[10] as Long?

        val budgetMonthKey = monthFormat.format(budgetCal.time)
        val budgetMonthLabel = monthDisplayFormat.format(budgetCal.time).replaceFirstChar { it.uppercase() }

        // ==========================================
        // 1. Calcul des soldes complets de chaque Caisse
        // ==========================================
        val registerSummaries = allRegisters.map { reg ->
            val regTx = allTx.filter { it.cashRegisterId == reg.id }
            val inc = regTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
            val exp = regTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            val currentBal = reg.initialBalanceCents + inc - exp
            CashRegisterSummary(
                id = reg.id,
                name = reg.name,
                description = reg.description,
                initialBalanceCents = reg.initialBalanceCents,
                iconType = reg.iconType,
                isDefault = reg.isDefault,
                totalIncomeCents = inc,
                totalExpenseCents = exp,
                currentBalanceCents = currentBal,
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
        // 2. Filtrage des transactions pour la période & Caisse
        // ==========================================
        val (startTime, endTime) = calculateTimeBounds(period)
        val periodBaseTx = allTx.filter { tx ->
            val inTime = tx.timestamp in startTime..endTime
            val inRegister = selectedRegId == null || tx.cashRegisterId == selectedRegId
            inTime && inRegister
        }

        val totalIncome = periodBaseTx
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }

        val totalExpense = periodBaseTx
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountCents }

        val netBalance = totalIncome - totalExpense

        val savingsRate = if (totalIncome > 0) {
            val rate = ((totalIncome - totalExpense).toDouble() / totalIncome.toDouble()) * 100.0
            rate.coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        val catExpenses = periodBaseTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        val catIncomes = periodBaseTx
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        // Filtrage spécifique pour la liste Timeline
        val filteredTx = periodBaseTx.filter { tx ->
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.cashRegisterName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true)
            matchesType && matchesQuery
        }

        // ==========================================
        // 3. Budgets & Suivi pour budgetMonthKey
        // ==========================================
        val (bStart, bEnd) = calculateMonthBounds(budgetCal)
        val monthExpenseTx = allTx.filter {
            it.type == TransactionType.EXPENSE &&
            it.timestamp in bStart..bEnd &&
            (selectedRegId == null || it.cashRegisterId == selectedRegId)
        }

        val spentPerCategory = monthExpenseTx
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }

        val periodBudgets = allBudgets.filter { it.monthYearKey == budgetMonthKey }
        val budgetMap = periodBudgets.associateBy { it.categoryName }

        val expenseCategories = allCategories.filter { it.type == TransactionType.EXPENSE }
        val relevantCategories = (expenseCategories.map { it.name } + budgetMap.keys + spentPerCategory.keys).distinct()
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
                val remaining = ceiling - spent
                CategoryBudgetProgress(
                    categoryName = catName,
                    categoryColorHex = categoryColorMap[catName] ?: "#4338CA",
                    budgetId = budget?.id,
                    budgetAmountCents = ceiling,
                    spentAmountCents = spent,
                    percentage = percentage,
                    remainingCents = remaining,
                    isExceeded = ceiling > 0L && spent > ceiling,
                    isNearLimit = ceiling > 0L && spent <= ceiling && (spent.toDouble() / ceiling.toDouble()) >= 0.8
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
        val totalSpentInBudgetedCategories = periodBudgets.sumOf { b ->
            spentPerCategory[b.categoryName] ?: 0L
        }
        val globalRemaining = totalCeiling - totalSpentInBudgetedCategories
        val globalPercentage = if (totalCeiling > 0L) {
            (totalSpentInBudgetedCategories.toDouble() / totalCeiling.toDouble()).toFloat()
        } else {
            0f
        }

        val globalSummary = GlobalBudgetSummary(
            totalBudgetCeilingCents = totalCeiling,
            totalSpentCents = totalSpentInBudgetedCategories,
            totalRemainingCents = globalRemaining,
            percentage = globalPercentage,
            isExceeded = totalCeiling > 0L && totalSpentInBudgetedCategories > totalCeiling
        )

        FinancialUiState(
            transactions = filteredTx,
            totalIncomeCents = totalIncome,
            totalExpenseCents = totalExpense,
            netBalanceCents = netBalance,
            totalCashBalanceCents = totalCashBalance,
            savingsRatePercent = savingsRate,
            categoryExpenses = catExpenses,
            categoryIncomes = catIncomes,
            selectedPeriod = period,
            selectedTypeFilter = typeFilter,
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
            currencySymbol = CurrencyFormatter.CURRENCY_CODE,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialUiState(isLoading = true)
    )

    fun setPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setAmountDisplayMode(mode: AmountDisplayMode) {
        _amountDisplayMode.value = mode
    }

    fun toggleDiscreetMode() {
        _amountDisplayMode.value = if (_amountDisplayMode.value == AmountDisplayMode.DISCRET) {
            AmountDisplayMode.STANDARD
        } else {
            AmountDisplayMode.DISCRET
        }
    }

    // Gestion du filtre de Caisse
    fun setSelectedCashRegister(id: Long?) {
        _selectedCashRegisterId.value = id
    }

    // Gestion des Budgets
    fun changeBudgetMonth(offset: Int) {
        val newCal = (_currentBudgetCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, offset)
        }
        _currentBudgetCalendar.value = newCal
    }

    fun resetBudgetMonthToCurrent() {
        _currentBudgetCalendar.value = Calendar.getInstance()
    }

    fun saveBudget(categoryName: String, amountCents: Long) {
        viewModelScope.launch {
            val monthKey = monthFormat.format(_currentBudgetCalendar.value.time)
            repository.saveBudget(categoryName, amountCents, monthKey)
            _eventFlow.emit("Budget de ${CurrencyFormatter.format(amountCents)} défini pour \"$categoryName\"")
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _eventFlow.emit("Budget supprimé")
        }
    }

    // ==========================================
    // Gestion du Système de Caisses
    // ==========================================
    fun addCashRegister(name: String, description: String, initialBalanceCents: Long, iconType: String) {
        viewModelScope.launch {
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

    fun updateCashRegister(id: Long, name: String, description: String, initialBalanceCents: Long, iconType: String, isDefault: Boolean) {
        viewModelScope.launch {
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
            val reg = repository.getCashRegisterById(id)
            if (reg != null) {
                if (reg.isDefault) {
                    _eventFlow.emit("Impossible de supprimer la caisse principale par défaut")
                    return@launch
                }
                repository.deleteCashRegisterById(id)
                if (_selectedCashRegisterId.value == id) {
                    _selectedCashRegisterId.value = null
                }
                _eventFlow.emit("Caisse \"${reg.name}\" supprimée")
            }
        }
    }

    // Transfert inter-caisses transparent (Virement interne)
    fun transferBetweenCaisses(sourceId: Long, targetId: Long, amountCents: Long, note: String) {
        viewModelScope.launch {
            if (sourceId == targetId) {
                _eventFlow.emit("La caisse de départ et la caisse d'arrivée doivent être différentes")
                return@launch
            }
            if (amountCents <= 0L) {
                _eventFlow.emit("Le montant du transfert doit être supérieur à 0")
                return@launch
            }

            val sourceReg = repository.getCashRegisterById(sourceId)
            val targetReg = repository.getCashRegisterById(targetId)

            if (sourceReg == null || targetReg == null) {
                _eventFlow.emit("Caisse introuvable")
                return@launch
            }

            val now = System.currentTimeMillis()

            // 1. Sortie de la caisse source
            val debitTx = TransactionEntity(
                title = "Transfert vers ${targetReg.name}",
                type = TransactionType.EXPENSE,
                amountCents = amountCents,
                category = "Transfert / Virement",
                timestamp = now,
                note = note.ifBlank { "Virement interne" },
                cashRegisterId = sourceReg.id,
                cashRegisterName = sourceReg.name
            )
            repository.insert(debitTx)

            // 2. Entrée dans la caisse cible
            val creditTx = TransactionEntity(
                title = "Transfert depuis ${sourceReg.name}",
                type = TransactionType.INCOME,
                amountCents = amountCents,
                category = "Transfert / Virement",
                timestamp = now + 1,
                note = note.ifBlank { "Virement interne" },
                cashRegisterId = targetReg.id,
                cashRegisterName = targetReg.name
            )
            repository.insert(creditTx)

            _eventFlow.emit("Transfert de ${CurrencyFormatter.format(amountCents)} effectué de ${sourceReg.name} vers ${targetReg.name}")
        }
    }

    // ==========================================
    // Catégories
    // ==========================================
    fun addCategory(name: String, type: TransactionType, colorHex: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val entity = CategoryEntity(
                name = trimmedName,
                type = type,
                colorHex = colorHex.ifBlank { "#4338CA" },
                isDefault = false
            )
            repository.insertCategory(entity)
            _eventFlow.emit("Catégorie \"$trimmedName\" créée")
        }
    }

    fun updateCategory(id: Long, name: String, type: TransactionType, colorHex: String, isDefault: Boolean) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val entity = CategoryEntity(
                id = id,
                name = trimmedName,
                type = type,
                colorHex = colorHex,
                isDefault = isDefault
            )
            repository.updateCategory(entity)
            _eventFlow.emit("Catégorie \"$trimmedName\" modifiée")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
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
        cashRegisterName: String = "Caisse Espèces"
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                type = type,
                amountCents = amountCents,
                category = category.trim().ifBlank { if (type == TransactionType.INCOME) "Autre revenu" else "Autre dépense" },
                timestamp = timestamp,
                note = note.trim(),
                cashRegisterId = cashRegisterId,
                cashRegisterName = cashRegisterName
            )
            repository.insert(entity)
            _eventFlow.emit("Opération \"${entity.title}\" enregistrée sur ${entity.cashRegisterName}")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _eventFlow.emit("Opération supprimée")
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val pdfFile = ReportExporter.generatePdfReport(
                    context = context,
                    periodLabel = "${state.selectedPeriod.label} (${state.selectedCashRegisterName})",
                    transactions = state.transactions,
                    totalIncomeCents = state.totalIncomeCents,
                    totalExpenseCents = state.totalExpenseCents,
                    netBalanceCents = state.netBalanceCents
                )
                ReportExporter.shareFile(
                    context = context,
                    file = pdfFile,
                    mimeType = "application/pdf",
                    subject = "Bilan Financier Lumina-beta - ${state.selectedPeriod.label}"
                )
                _eventFlow.emit("Rapport PDF généré avec succès")
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
                    transactions = state.transactions,
                    totalIncomeCents = state.totalIncomeCents,
                    totalExpenseCents = state.totalExpenseCents,
                    netBalanceCents = state.netBalanceCents
                )
                ReportExporter.shareFile(
                    context = context,
                    file = csvFile,
                    mimeType = "text/csv",
                    subject = "Export Relevé Lumina-beta - ${state.selectedPeriod.label}"
                )
                _eventFlow.emit("Fichier Excel/CSV généré avec succès")
            } catch (e: Exception) {
                _eventFlow.emit("Erreur lors de l'export CSV : ${e.localizedMessage}")
            }
        }
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
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private fun calculateTimeBounds(period: PeriodFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (period) {
            PeriodFilter.TOUT -> Pair(0L, Long.MAX_VALUE)
            PeriodFilter.CE_MOIS -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            PeriodFilter.MOIS_DERNIER -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                val end = cal.timeInMillis
                Pair(start, end)
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
                val end = cal.timeInMillis
                Pair(start, end)
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
