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
    val cashRegisterName: String = "Caisse Espèces", // Nom de la caisse pour traçabilité
    /**
     * Identifiant commun aux deux écritures d'un virement interne
     * (débit de la caisse source + crédit de la caisse cible).
     * Vide pour une opération ordinaire.
     */
    val transferGroupId: String = "",
    /** Photo ou scan du justificatif (facture, reçu), optionnel. */
    val receiptPhotoUri: String = "",
    /** Identité stable entre appareils (fusion lors des échanges de fichiers). */
    val uid: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Auteur de la saisie (ou de la dernière modification). */
    val updatedBy: String = "",
    /** Suppression logique : l'écriture reste dans le journal pour la fusion. */
    val isDeleted: Boolean = false,
    /**
     * Annulation par contre-écriture : identité de l'écriture annulée.
     * Une contre-écriture porte l'identité de l'écriture qu'elle neutralise.
     */
    val reversalOfUid: String = "",
    /** L'écriture a été annulée par une contre-écriture : elle reste au journal. */
    val isCancelled: Boolean = false,
    /** Projet auquel l'écriture est affectée (0 = non affectée). */
    val projectId: Long = 0L
) {
    val amountFormatted: String
        get() = CurrencyFormatter.format(amountCents)

    /** Un virement interne ne doit jamais être compté comme revenu ou dépense. */
    val isTransfer: Boolean
        get() = transferGroupId.isNotBlank()

    val hasReceipt: Boolean
        get() = receiptPhotoUri.isNotBlank()

    /** Écriture neutralisée : soit annulée, soit contre-écriture d'annulation. */
    val isNeutralised: Boolean
        get() = isCancelled || reversalOfUid.isNotBlank()

    /** Contre-écriture générée par une annulation. */
    val isReversal: Boolean
        get() = reversalOfUid.isNotBlank()
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
