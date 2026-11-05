package com.example.data

import com.example.export.BackupSnapshot
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val cashRegisterDao: CashRegisterDao,
    private val churchProfileDao: ChurchProfileDao,
    private val memberDao: MemberDao,
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    val workspaceId: String = "default"
) {
    // ==========================================
    // Utilisateurs de l'appareil (rôles & PIN)
    // ==========================================
    val allUsers: Flow<List<UserEntity>> = userDao.observeUsers()

    suspend fun getUsers(): List<UserEntity> = userDao.getUsers()

    suspend fun getUserById(id: Long): UserEntity? = userDao.getById(id)

    suspend fun getUsersCount(): Int = userDao.count()

    suspend fun saveUser(user: UserEntity): Long = userDao.upsert(user)

    suspend fun deleteUser(id: Long) = userDao.softDelete(id, System.currentTimeMillis())

    suspend fun touchUserLogin(id: Long) = userDao.touchLastLogin(id, System.currentTimeMillis())
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

    suspend fun deleteMember(member: MemberEntity) = memberDao.softDeleteById(member.id, System.currentTimeMillis())

    suspend fun deleteMemberById(id: Long) = memberDao.softDeleteById(id, System.currentTimeMillis())

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

    suspend fun deleteById(id: Long, author: String = "") {
        transactionDao.softDeleteById(id, System.currentTimeMillis(), author)
    }

    suspend fun getTransferGroup(groupId: String): List<TransactionEntity> =
        transactionDao.getByTransferGroup(groupId)

    suspend fun deleteByTransferGroup(groupId: String, author: String = "") {
        transactionDao.softDeleteByTransferGroup(groupId, System.currentTimeMillis(), author)
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

    suspend fun getCashRegistersOnce(): List<CashRegisterEntity> = cashRegisterDao.getAllOnce()

    /** Supprime une caisse et ses écritures pour ne jamais laisser d'orphelines. */
    suspend fun deleteCashRegisterAndTransactions(id: Long) {
        val now = System.currentTimeMillis()
        transactionDao.softDeleteByCashRegister(id, now, "")
        cashRegisterDao.softDeleteById(id, now)
    }

    // ==========================================
    // Projets / fonds affectés
    // ==========================================
    val allProjects: Flow<List<ProjectEntity>> = projectDao.observeProjects()

    suspend fun getProjectsOnce(): List<ProjectEntity> = projectDao.getAllOnce()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getById(id)

    suspend fun saveProject(project: ProjectEntity): Long = projectDao.upsert(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.update(project)

    suspend fun deleteProject(id: Long) = projectDao.softDelete(id, System.currentTimeMillis())

    suspend fun deleteAllProjects() = projectDao.getAllOnce().forEach {
        projectDao.softDelete(it.id, System.currentTimeMillis())
    }

    // ==========================================
    // Remise à zéro des données d'essai
    // ==========================================

    /**
     * Efface les données de travail : écritures, membres, budgets et caisses.
     * Le profil de l'église, les catégories et l'équipe sont conservés.
     * Une caisse principale vide est recréée pour que la saisie reste possible.
     */
    suspend fun wipeOperationalData() {
        transactionDao.deleteAllTransactions()
        memberDao.deleteAllMembers()
        budgetDao.deleteAllBudgets()
        cashRegisterDao.deleteAllCashRegisters()
        cashRegisterDao.insertCashRegister(
            CashRegisterEntity(
                name = "Caisse principale",
                description = "Caisse physique de l'église",
                initialBalanceCents = 0L,
                iconType = "cash",
                isDefault = true
            )
        )
    }

    /**
     * Remise à zéro complète : même chose, plus les catégories, l'équipe et
     * l'identité de l'église. L'application repart de l'assistant de départ.
     */
    suspend fun wipeEverything() {
        transactionDao.deleteAllTransactions()
        memberDao.deleteAllMembers()
        budgetDao.deleteAllBudgets()
        cashRegisterDao.deleteAllCashRegisters()
        categoryDao.deleteAllCategories()
        userDao.deleteAllUsers()
        churchProfileDao.deleteAllProfiles()
        cashRegisterDao.insertCashRegister(
            CashRegisterEntity(
                name = "Caisse principale",
                description = "Caisse physique de l'église",
                initialBalanceCents = 0L,
                iconType = "cash",
                isDefault = true
            )
        )
        ensureDefaultCategories()
    }

    /** Rétablit le plan de comptes de départ si le dossier n'en a plus. */
    suspend fun ensureDefaultCategories() {
        if (categoryDao.getAllCategoriesOnce().isNotEmpty()) return
        DefaultChartOfAccounts.expenses.forEach { (name, color, icon) ->
            categoryDao.insertCategory(
                CategoryEntity(
                    name = name,
                    type = TransactionType.EXPENSE,
                    colorHex = color,
                    isDefault = true,
                    iconName = icon
                )
            )
        }
        DefaultChartOfAccounts.incomes.forEach { (name, color, icon) ->
            categoryDao.insertCategory(
                CategoryEntity(
                    name = name,
                    type = TransactionType.INCOME,
                    colorHex = color,
                    isDefault = true,
                    iconName = icon
                )
            )
        }
    }

    // ==========================================
    // Synchronisation par fichier
    // ==========================================
    suspend fun transactionsForSync(): List<TransactionEntity> = transactionDao.getAllForSync()

    suspend fun membersForSync(): List<MemberEntity> = memberDao.getAllForSync()

    suspend fun registersForSync(): List<CashRegisterEntity> = cashRegisterDao.getAllForSync()

    suspend fun categoriesForSync(): List<CategoryEntity> = categoryDao.getAllForSync()

    suspend fun transactionByUid(uid: String): TransactionEntity? = transactionDao.getByUid(uid)

    suspend fun memberByUid(uid: String): MemberEntity? = memberDao.getByUid(uid)

    suspend fun registerByUid(uid: String): CashRegisterEntity? = cashRegisterDao.getByUid(uid)

    suspend fun categoryByUid(uid: String): CategoryEntity? = categoryDao.getByUid(uid)

    suspend fun insertTransactionsRaw(items: List<TransactionEntity>) {
        transactionDao.insertTransactions(items)
    }

    suspend fun insertMembersRaw(items: List<MemberEntity>) {
        items.forEach { memberDao.insert(it) }
    }

    suspend fun insertRegistersRaw(items: List<CashRegisterEntity>) {
        items.forEach { cashRegisterDao.insertCashRegister(it) }
    }

    suspend fun insertCategoriesRaw(items: List<CategoryEntity>) {
        items.forEach { categoryDao.insertCategory(it) }
    }

    // ==========================================
    // Sauvegarde et restauration du dossier
    // ==========================================

    /** Rassemble tout le dossier dans une sauvegarde unique. */
    suspend fun snapshotAll(): BackupSnapshot = BackupSnapshot(
        createdAt = System.currentTimeMillis(),
        churchName = churchProfileDao.getProfile()?.churchName ?: "",
        users = userDao.getUsers(),
        profile = churchProfileDao.getProfile(),
        members = memberDao.getAllForSync(),
        categories = categoryDao.getAllForSync(),
        budgets = budgetDao.getAllOnce(),
        registers = cashRegisterDao.getAllForSync(),
        projects = projectDao.getAllOnce(),
        transactions = transactionDao.getAllForSync()
    )

    /**
     * Remet le dossier dans l'état de la sauvegarde : les données actuelles
     * sont d'abord effacées, puis les écritures sont réinsérées telles quelles
     * (identifiants conservés pour que les rattachements restent valables).
     */
    suspend fun restoreAll(snapshot: BackupSnapshot) {
        transactionDao.deleteAllTransactions()
        memberDao.deleteAllMembers()
        budgetDao.deleteAllBudgets()
        cashRegisterDao.deleteAllCashRegisters()
        categoryDao.deleteAllCategories()
        projectDao.deleteAllProjects()

        snapshot.categories.forEach { categoryDao.insertCategory(it) }
        snapshot.budgets.forEach { budgetDao.insertOrUpdateBudget(it) }
        snapshot.projects.forEach { projectDao.upsert(it) }
        snapshot.members.forEach { memberDao.insert(it) }
        snapshot.registers.forEach { cashRegisterDao.insertCashRegister(it) }
        snapshot.transactions.forEach { transactionDao.insertTransaction(it) }

        if (snapshot.users.isNotEmpty()) {
            userDao.deleteAllUsers()
            snapshot.users.forEach { userDao.upsert(it) }
        }
        snapshot.profile?.let { importedProfile ->
            val existingProfile = churchProfileDao.getProfile()
            val stableUid = importedProfile.churchUid.ifBlank {
                existingProfile?.churchUid?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            }
            churchProfileDao.upsert(importedProfile.copy(churchUid = stableUid))
        }
        ensureDefaultCategories()
        if (cashRegisterDao.getCount() == 0) {
            cashRegisterDao.insertCashRegister(
                CashRegisterEntity(
                    name = "Caisse principale",
                    description = "Caisse physique de l'église",
                    initialBalanceCents = 0L,
                    iconType = "cash",
                    isDefault = true
                )
            )
        }
    }
}
