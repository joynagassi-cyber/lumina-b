package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val cashRegisterDao: CashRegisterDao,
    private val churchProfileDao: ChurchProfileDao,
    private val memberDao: MemberDao
) {
    // ==========================================
    // Identité de l'église (profil local)
    // ==========================================
    val profile: Flow<ChurchProfileEntity?> = churchProfileDao.observeProfile()

    suspend fun getProfile(): ChurchProfileEntity? = churchProfileDao.getProfile()

    suspend fun saveProfile(profile: ChurchProfileEntity) = churchProfileDao.upsert(profile)

    // ==========================================
    // Membres de l'assemblée
    // ==========================================
    val allMembers: Flow<List<MemberEntity>> = memberDao.observeMembers()

    suspend fun insertMember(member: MemberEntity): Long = memberDao.insert(member)

    suspend fun updateMember(member: MemberEntity) = memberDao.update(member)

    suspend fun deleteMember(member: MemberEntity) = memberDao.delete(member)

    suspend fun deleteMemberById(id: Long) = memberDao.deleteById(id)

    suspend fun getMember(id: Long): MemberEntity? = memberDao.getById(id)
    // ==========================================
    // Transactions
    // ==========================================
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(start, end)
    }

    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCategory(category)
    }

    fun getTransactionsByCashRegister(cashRegisterId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCashRegister(cashRegisterId)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getById(id)
    }

    fun getTotalExpenseBetween(start: Long, end: Long): Flow<Long?> {
        return transactionDao.getTotalExpenseBetween(start, end)
    }

    fun getTotalIncomeBetween(start: Long, end: Long): Flow<Long?> {
        return transactionDao.getTotalIncomeBetween(start, end)
    }

    suspend fun insert(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        return transactionDao.insertTransactions(transactions)
    }

    suspend fun update(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteByTransferGroup(groupId: String) {
        transactionDao.deleteByTransferGroup(groupId)
    }

    suspend fun renameCategoryEverywhere(oldName: String, newName: String) {
        transactionDao.renameCategory(oldName, newName)
        budgetDao.renameCategory(oldName, newName)
    }

    // ==========================================
    // Catégories
    // ==========================================
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun getCategoryById(id: Long): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteById(id)
    }

    // ==========================================
    // Budgets & Contraintes Budgétaires
    // ==========================================
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    fun getBudgetsForPeriod(monthYearKey: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForPeriod(monthYearKey)
    }

    fun observeBudgetForCategoryAndPeriod(categoryName: String, monthYearKey: String): Flow<BudgetEntity?> {
        return budgetDao.observeBudgetForCategoryAndPeriod(categoryName, monthYearKey)
    }

    suspend fun getBudgetForCategoryAndPeriod(categoryName: String, monthYearKey: String): BudgetEntity? {
        return budgetDao.getBudgetForCategoryAndPeriod(categoryName, monthYearKey)
    }

    suspend fun saveBudget(categoryName: String, amountCents: Long, monthYearKey: String) {
        saveBudgetConstraint(
            categoryName = categoryName,
            amountCents = amountCents,
            monthYearKey = monthYearKey
        )
    }

    suspend fun saveBudgetConstraint(
        categoryName: String,
        amountCents: Long,
        monthYearKey: String,
        alertThresholdPercent: Int = 80,
        isHardLimit: Boolean = false,
        notes: String = ""
    ) {
        val existing = budgetDao.getBudgetForCategoryAndPeriod(categoryName, monthYearKey)
        if (existing != null) {
            budgetDao.updateBudget(
                existing.copy(
                    amountCents = amountCents,
                    alertThresholdPercent = alertThresholdPercent,
                    isHardLimit = isHardLimit,
                    notes = notes
                )
            )
        } else {
            budgetDao.insertOrUpdateBudget(
                BudgetEntity(
                    categoryName = categoryName,
                    amountCents = amountCents,
                    monthYearKey = monthYearKey,
                    alertThresholdPercent = alertThresholdPercent,
                    isHardLimit = isHardLimit,
                    notes = notes
                )
            )
        }
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteById(id)
    }

    suspend fun deleteBudgetByCategory(categoryName: String) {
        budgetDao.deleteByCategory(categoryName)
    }

    // ==========================================
    // Caisses
    // ==========================================
    val allCashRegisters: Flow<List<CashRegisterEntity>> = cashRegisterDao.getAllCashRegisters()

    suspend fun insertCashRegister(cashRegister: CashRegisterEntity): Long {
        return cashRegisterDao.insertCashRegister(cashRegister)
    }

    suspend fun updateCashRegister(cashRegister: CashRegisterEntity) {
        cashRegisterDao.updateCashRegister(cashRegister)
    }

    suspend fun deleteCashRegister(cashRegister: CashRegisterEntity) {
        cashRegisterDao.deleteCashRegister(cashRegister)
    }

    suspend fun deleteCashRegisterById(id: Long) {
        cashRegisterDao.deleteById(id)
    }

    suspend fun getCashRegisterById(id: Long): CashRegisterEntity? {
        return cashRegisterDao.getById(id)
    }

    suspend fun getCashRegisterCount(): Int = cashRegisterDao.getCount()

    /** Supprime une caisse et ses écritures pour ne jamais laisser d'orphelines. */
    suspend fun deleteCashRegisterAndTransactions(id: Long) {
        transactionDao.deleteByCashRegister(id)
        cashRegisterDao.deleteById(id)
    }
}
