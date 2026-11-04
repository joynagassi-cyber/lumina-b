package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorHex: String = "#0F766E",
    val isDefault: Boolean = false,
    val iconName: String = ""
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryName", "monthYearKey"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val amountCents: Long, // Plafond maximal de la contrainte budgétaire
    val monthYearKey: String, // ex: "2026-11" ou "GLOBAL"
    val alertThresholdPercent: Int = 80, // Seuil d'alerte en pourcentage (ex: 80% avant dépassement)
    val isHardLimit: Boolean = false, // Contrainte stricte / blocage d'excès
    val notes: String = ""
)
