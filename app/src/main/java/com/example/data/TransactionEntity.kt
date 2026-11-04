package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.util.CurrencyFormatter

enum class TransactionType {
    INCOME,   // Entrée
    EXPENSE   // Sortie
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["cashRegisterId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val amountCents: Long, // Montant déterministe en FCFA
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val cashRegisterId: Long = 1L, // Identifiant de la caisse
    val cashRegisterName: String = "Caisse Espèces" // Nom de la caisse pour traçabilité
) {
    val amountFormatted: String
        get() = CurrencyFormatter.format(amountCents)
}

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }
}
