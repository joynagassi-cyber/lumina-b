package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // --- Lectures applicatives : les écritures supprimées (tombes) sont masquées
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND category = :category AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByCategoryAndPeriod(category: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND cashRegisterId = :cashRegisterId ORDER BY timestamp DESC")
    fun getTransactionsByCashRegister(cashRegisterId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT SUM(amountCents) FROM transactions WHERE isDeleted = 0 AND type = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT SUM(amountCents) FROM transactions WHERE isDeleted = 0 AND type = 'INCOME' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT SUM(amountCents) FROM transactions WHERE isDeleted = 0 AND category = :category AND type = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalExpenseForCategoryBetween(category: String, startTime: Long, endTime: Long): Flow<Long?>

    // --- Écritures
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /** Suppression logique : la tombale est conservée pour la synchronisation. */
    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp, updatedBy = :author WHERE id = :id")
    suspend fun softDeleteById(id: Long, timestamp: Long, author: String)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp, updatedBy = :author WHERE transferGroupId = :groupId AND transferGroupId != ''")
    suspend fun softDeleteByTransferGroup(groupId: String, timestamp: Long, author: String)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp, updatedBy = :author WHERE cashRegisterId = :cashRegisterId")
    suspend fun softDeleteByCashRegister(cashRegisterId: Long, timestamp: Long, author: String)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)

    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId AND isDeleted = 0")
    suspend fun getByTransferGroup(groupId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0")
    suspend fun getCount(): Int

    // --- Synchronisation (journal complet, tombes incluses)
    @Query("SELECT * FROM transactions")
    suspend fun getAllForSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): TransactionEntity?
}
