package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CashRegisterDao {
    @Query("SELECT * FROM cash_registers WHERE isDeleted = 0 ORDER BY isDefault DESC, name ASC")
    fun getAllCashRegisters(): Flow<List<CashRegisterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashRegister(cashRegister: CashRegisterEntity): Long

    @Update
    suspend fun updateCashRegister(cashRegister: CashRegisterEntity)

    @Delete
    suspend fun deleteCashRegister(cashRegister: CashRegisterEntity)

    @Query("DELETE FROM cash_registers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM cash_registers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CashRegisterEntity?

    @Query("SELECT * FROM cash_registers WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): CashRegisterEntity?

    @Query("SELECT * FROM cash_registers")
    suspend fun getAllForSync(): List<CashRegisterEntity>

    @Query("SELECT * FROM cash_registers WHERE isDeleted = 0 ORDER BY isDefault DESC, name ASC")
    suspend fun getAllOnce(): List<CashRegisterEntity>

    @Query("UPDATE cash_registers SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteById(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM cash_registers WHERE isDeleted = 0")
    suspend fun getCount(): Int

    @Query("DELETE FROM cash_registers")
    suspend fun deleteAllCashRegisters()
}
