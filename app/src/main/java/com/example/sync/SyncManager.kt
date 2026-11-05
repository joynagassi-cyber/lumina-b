package com.example.sync

import android.content.Context
import android.net.Uri
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ChurchProfileEntity
import com.example.data.MemberEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import java.io.File
import java.util.UUID

/**
 * Échange de données entre appareils, sans réseau ni compte.
 *
 * Un appareil exporte un fichier chiffré (toutes ses écritures, membres,
 * caisses et catégories), l'autre l'importe : les enregistrements sont fusionnés
 * par identité stable (`uid`) et la version la plus récente l'emporte. Comme les
 * écritures ne sont jamais modifiées mais seulement ajoutées ou supprimées
 * (tombes), la fusion est déterministe et ne peut pas produire de solde faux.
 *
 * Les comptes utilisateurs et les codes PIN ne quittent jamais l'appareil.
 */
object SyncManager {

    data class MergeReport(
        val sourceChurch: String = "",
        val exportedBy: String = "",
        val exportedAt: Long = 0L,
        val addedTransactions: Int = 0,
        val updatedTransactions: Int = 0,
        val addedMembers: Int = 0,
        val updatedMembers: Int = 0,
        val addedRegisters: Int = 0,
        val addedCategories: Int = 0,
        val ignored: Int = 0
    ) {
        val totalChanges: Int
            get() = addedTransactions + updatedTransactions + addedMembers +
                updatedMembers + addedRegisters + addedCategories

        fun summary(): String = buildString {
            append("$addedTransactions nouvelle(s) écriture(s)")
            if (updatedTransactions > 0) append(", $updatedTransactions mise(s) à jour")
            if (addedMembers > 0) append(", $addedMembers membre(s)")
            if (addedRegisters > 0) append(", $addedRegisters caisse(s)")
            if (addedCategories > 0) append(", $addedCategories catégorie(s)")
            if (ignored > 0) append(" — $ignored déjà à jour")
        }
    }

    class SameFileAsLocal : Exception("Ce fichier provient de cet appareil : aucune donnée à importer")
    class DifferentChurch : Exception("Ce fichier appartient à une autre église. Ouvrez le dossier correspondant avant l'import.")

    /** Construit et chiffre le fichier d'export ; renvoie le fichier prêt à partager. */
    suspend fun exportFile(
        context: Context,
        repository: TransactionRepository,
        profile: ChurchProfileEntity,
        passphrase: String,
        authorName: String,
        directory: File
    ): File {
        val bundle = SyncBundle(
            churchName = profile.displayName,
            churchUid = profile.churchUid,
            deviceId = DeviceIdentity.deviceId(context),
            author = authorName,
            exportedAt = System.currentTimeMillis(),
            registers = repository.registersForSync(),
            categories = repository.categoriesForSync(),
            members = repository.membersForSync(),
            transactions = repository.transactionsForSync()
        )
        val plain = SyncBundleCodec.encode(bundle)
        val encrypted = SyncCrypto.encrypt(plain, passphrase)

        directory.mkdirs()
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.FRANCE)
            .format(java.util.Date())
        val safeName = profile.displayName
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim()
            .replace(" ", "_")
            .ifBlank { "Eglise" }
        val file = File(directory, "Lumina_${safeName}_$stamp.lumina")
        file.writeBytes(encrypted)
        return file
    }

    /** Lit, déchiffre et fusionne un fichier d'échange. */
    suspend fun importFile(
        context: Context,
        repository: TransactionRepository,
        uri: Uri,
        passphrase: String
    ): MergeReport {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Fichier illisible")

        val plain = SyncCrypto.decrypt(bytes, passphrase)
        val bundle = SyncBundleCodec.decode(plain)
        val localProfile = repository.getProfile()
            ?: throw IllegalStateException("Configurez d'abord le profil de cette église.")
        val sameChurch = if (bundle.churchUid.isNotBlank() && localProfile.churchUid.isNotBlank()) {
            bundle.churchUid == localProfile.churchUid
        } else {
            repository.workspaceId == "default" &&
                bundle.churchName.trim().equals(localProfile.churchName.trim(), ignoreCase = true)
        }
        if (!sameChurch) throw DifferentChurch()

        if (bundle.deviceId.isNotBlank() && bundle.deviceId == DeviceIdentity.deviceId(context)) {
            throw SameFileAsLocal()
        }

        var addedTx = 0
        var updatedTx = 0
        var addedMembers = 0
        var updatedMembers = 0
        var addedRegisters = 0
        var addedCategories = 0
        var ignored = 0

        // 1. Caisses : la fusion doit précéder les écritures qui s'y rattachent
        val localIdByRegisterUid = mutableMapOf<String, Long>()
        val existingRegisters = repository.registersForSync()
        for (remote in bundle.registers) {
            if (remote.uid.isBlank()) continue
            val local = repository.registerByUid(remote.uid)
            if (local == null) {
                val newId = repository.insertCashRegister(remote.copy(id = 0))
                localIdByRegisterUid[remote.uid] = newId
                if (!remote.isDeleted) addedRegisters++
            } else {
                localIdByRegisterUid[remote.uid] = local.id
                if (remote.updatedAt > local.updatedAt) {
                    repository.insertRegistersRaw(listOf(remote.copy(id = local.id)))
                }
            }
        }
        existingRegisters.forEach { localIdByRegisterUid.putIfAbsent(it.uid, it.id) }

        val defaultRegisterId = repository.getCashRegistersOnce().firstOrNull()?.id ?: 1L
        val registerUidByName = SyncBundleCodec.registerUidsByName(bundle)

        // 2. Catégories (identité stable, puis repli sur le nom)
        for (remote in bundle.categories) {
            if (remote.uid.isBlank()) continue
            val local = repository.categoryByUid(remote.uid)
                ?: repository.getCategoryByName(remote.name)
            if (local == null) {
                repository.insertCategoriesRaw(listOf(remote.copy(id = 0)))
                if (!remote.isDeleted) addedCategories++
            } else if (remote.updatedAt > local.updatedAt) {
                repository.insertCategoriesRaw(listOf(remote.copy(id = local.id)))
            }
        }

        // 3. Membres
        for (remote in bundle.members) {
            if (remote.uid.isBlank()) continue
            val local = repository.memberByUid(remote.uid)
            when {
                local == null -> {
                    repository.insertMembersRaw(listOf(remote.copy(id = 0)))
                    if (!remote.isDeleted) addedMembers++
                }
                remote.updatedAt > local.updatedAt -> {
                    repository.insertMembersRaw(listOf(remote.copy(id = local.id)))
                    if (!remote.isDeleted) updatedMembers++
                }
                else -> ignored++
            }
        }

        // 4. Écritures
        for (remote in bundle.transactions) {
            if (remote.uid.isBlank()) continue
            val local = repository.transactionByUid(remote.uid)
            val localRegisterId = registerUidByName[remote.cashRegisterName]
                ?.let { localIdByRegisterUid[it] }
                ?: defaultRegisterId

            when {
                local == null -> {
                    repository.insertTransactionsRaw(
                        listOf(remote.copy(id = 0, cashRegisterId = localRegisterId))
                    )
                    if (!remote.isDeleted) addedTx++
                }
                remote.updatedAt > local.updatedAt -> {
                    repository.insertTransactionsRaw(
                        listOf(remote.copy(id = local.id, cashRegisterId = localRegisterId))
                    )
                    if (!remote.isDeleted) updatedTx++
                }
                else -> ignored++
            }
        }

        return MergeReport(
            sourceChurch = bundle.churchName,
            exportedBy = bundle.author,
            exportedAt = bundle.exportedAt,
            addedTransactions = addedTx,
            updatedTransactions = updatedTx,
            addedMembers = addedMembers,
            updatedMembers = updatedMembers,
            addedRegisters = addedRegisters,
            addedCategories = addedCategories,
            ignored = ignored
        )
    }

    fun newTransferGroupId(): String = UUID.randomUUID().toString()
}

/** Identité de l'appareil, stable tant que l'application est installée. */
object DeviceIdentity {
    private const val PREFS = "lumina_device"
    private const val KEY_DEVICE_ID = "device_id"

    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }
}
