package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_registers")
data class CashRegisterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val initialBalanceCents: Long = 0L, // Solde initial en FCFA
    val iconType: String = "cash", // "cash", "mobile", "bank", "vault"
    val isDefault: Boolean = false
)
