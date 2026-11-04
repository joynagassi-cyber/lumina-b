package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY id DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE monthYearKey = :monthYearKey ORDER BY categoryName ASC")
    fun getBudgetsForPeriod(monthYearKey: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryName = :categoryName AND monthYearKey = :monthYearKey LIMIT 1")
    suspend fun getBudgetForCategoryAndPeriod(categoryName: String, monthYearKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryName = :categoryName AND monthYearKey = :monthYearKey LIMIT 1")
    fun observeBudgetForCategoryAndPeriod(categoryName: String, monthYearKey: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>): List<Long>

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE budgets SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)

    @Query("DELETE FROM budgets WHERE categoryName = :categoryName")
    suspend fun deleteByCategory(categoryName: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getBudgetCount(): Int
}
