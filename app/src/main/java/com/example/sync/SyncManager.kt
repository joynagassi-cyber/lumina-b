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
 * Le bundle chiffré transporte les écritures, membres, caisses, catégories,
 * budgets et projets. Les identités stables servent de clé entre appareils;
 * une révision horodatée ne remplace une révision plus ancienne que lorsque
 * l'ordre est clair. Les divergences à horodatage égal sont signalées et la
 * copie locale est conservée. Les comptes utilisateurs et les PIN restent locaux.
 *
 * Les reçus/fichiers joints, l'identité visuelle et les historiques de fusion
 * ne sont pas encore inclus dans cet échange incrémental; l'archive Lumina
 * complète est le format prévu pour le transfert initial du dossier.
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
        val updatedRegisters: Int = 0,
        val addedCategories: Int = 0,
        val updatedCategories: Int = 0,
        val addedBudgets: Int = 0,
        val addedProjects: Int = 0,
        val updatedProjects: Int = 0,
        val conflicts: Int = 0,
        val ignored: Int = 0
    ) {
        val totalChanges: Int
            get() = addedTransactions + updatedTransactions + addedMembers +
                updatedMembers + addedRegisters + updatedRegisters + addedCategories +
                updatedCategories + addedBudgets +
                addedProjects + updatedProjects

        fun summary(): String = buildString {
            append("$addedTransactions nouvelle(s) écriture(s)")
            if (updatedTransactions > 0) append(", $updatedTransactions mise(s) à jour")
            if (addedMembers > 0) append(", $addedMembers membre(s)")
            if (addedRegisters > 0) append(", $addedRegisters caisse(s)")
            if (updatedRegisters > 0) append(", $updatedRegisters caisse(s) mise(s) à jour")
            if (addedCategories > 0) append(", $addedCategories catégorie(s)")
            if (updatedCategories > 0) append(", $updatedCategories catégorie(s) mise(s) à jour")
            if (addedBudgets > 0) append(", $addedBudgets budget(s)")
            if (addedProjects > 0) append(", $addedProjects projet(s)")
            if (updatedProjects > 0) append(", $updatedProjects projet(s) mis à jour")
            if (conflicts > 0) append(" — $conflicts conflit(s) à examiner; les données locales ambiguës ont été conservées")
            if (ignored > 0) append(" — $ignored élément(s) inchangé(s) ou plus ancien(s)")
        }
    }

    class SameFileAsLocal : Exception("Ce fichier provient de cet appareil : aucune donnée à importer")
    class DifferentChurch : Exception("Ce fichier appartient à une autre église. Ouvrez le dossier correspondant avant l'import.")
    class UnsupportedSyncAttachments : Exception(
        "Cet échange contient des justificatifs mais ne transporte pas encore leurs fichiers. Aucun élément n'a été importé; utilisez l'archive Lumina complète (ZIP) pour conserver toutes les pièces jointes."
    )

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
            budgets = repository.budgetsForSync(),
            projects = repository.projectsForSync(),
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
        if (bundle.transactionUidsWithReceipts.isNotEmpty()) throw UnsupportedSyncAttachments()
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
        var updatedRegisters = 0
        var addedCategories = 0
        var updatedCategories = 0
        var addedBudgets = 0
        var addedProjects = 0
        var updatedProjects = 0
        var conflicts = 0
        var ignored = 0

        repository.inDatabaseTransaction {
            // 1. D'abord les identités des caisses et projets, nécessaires aux écritures.
            val localIdByRegisterUid = mutableMapOf<String, Long>()
            val existingRegisters = repository.registersForSync()
            for (remote in bundle.registers) {
                if (remote.uid.isBlank()) { ignored++; continue }
                val local = repository.registerByUid(remote.uid)
                if (local == null) {
                    val newId = repository.insertCashRegister(remote.copy(id = 0))
                    localIdByRegisterUid[remote.uid] = newId
                    if (!remote.isDeleted) addedRegisters++
                } else {
                    localIdByRegisterUid[remote.uid] = local.id
                    when {
                        remote.updatedAt > local.updatedAt -> {
                            repository.insertRegistersRaw(listOf(remote.copy(id = local.id)))
                            updatedRegisters++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> conflicts++
                        else -> ignored++
                    }
                }
            }
            existingRegisters.forEach { localIdByRegisterUid.putIfAbsent(it.uid, it.id) }
            val defaultRegisterId = repository.getCashRegistersOnce().firstOrNull()?.id ?: 1L
            val registerUidByName = SyncBundleCodec.registerUidsByName(bundle)

            val localIdByProjectUid = mutableMapOf<String, Long>()
            repository.projectsForSync().forEach { localIdByProjectUid[it.uid] = it.id }
            for (remote in bundle.projects) {
                if (remote.uid.isBlank()) { ignored++; continue }
                val local = repository.projectByUid(remote.uid)
                if (local == null) {
                    val id = repository.insertProjectRaw(remote.copy(id = 0))
                    localIdByProjectUid[remote.uid] = id
                    if (!remote.isDeleted) addedProjects++
                } else {
                    localIdByProjectUid[remote.uid] = local.id
                    when {
                        remote.updatedAt > local.updatedAt -> {
                            repository.insertProjectRaw(remote.copy(id = local.id))
                            updatedProjects++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> conflicts++
                        else -> ignored++
                    }
                }
            }

            // 2. Catégories : les identités stables et horodatages distinguent les edits.
            for (remote in bundle.categories) {
                if (remote.uid.isBlank()) { ignored++; continue }
                val local = repository.categoryByUid(remote.uid)
                    ?: repository.getCategoryByName(remote.name)
                if (local == null) {
                    repository.insertCategoriesRaw(listOf(remote.copy(id = 0)))
                    if (!remote.isDeleted) addedCategories++
                } else {
                    when {
                        remote.updatedAt > local.updatedAt -> {
                            repository.insertCategoriesRaw(listOf(remote.copy(id = local.id)))
                            updatedCategories++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> conflicts++
                        else -> ignored++
                    }
                }
            }

            // 3. Membres
            for (remote in bundle.members) {
                if (remote.uid.isBlank()) { ignored++; continue }
                val local = repository.memberByUid(remote.uid)
                when {
                    local == null -> {
                        repository.insertMembersRaw(listOf(remote.copy(id = 0)))
                        if (!remote.isDeleted) addedMembers++
                    }
                    remote.updatedAt > local.updatedAt -> {
                        repository.insertMembersRaw(listOf(remote.copy(id = local.id)))
                        updatedMembers++
                    }
                    remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> conflicts++
                    else -> ignored++
                }
            }

            // 4. Budgets sans horodatage individuel : nouveau créés, identiques ignorés,
            //    divergents signalés et jamais écrasés silencieusement.
            for (remote in bundle.budgets) {
                val local = repository.getBudgetForCategoryAndPeriod(remote.categoryName, remote.monthYearKey)
                when {
                    local == null -> {
                        repository.insertBudgetRaw(remote.copy(id = 0))
                        addedBudgets++
                    }
                    remote.copy(id = local.id) == local -> ignored++
                    else -> conflicts++
                }
            }

            // 5. Écritures : UID caisse/projet en priorité, repli compatible v1 ensuite.
            for (remote in bundle.transactions) {
                if (remote.uid.isBlank()) { ignored++; continue }
                val local = repository.transactionByUid(remote.uid)
                val registerUid = bundle.registerUidByTransactionUid[remote.uid]
                    ?: registerUidByName[remote.cashRegisterName]
                val localRegisterId = registerUid?.let { localIdByRegisterUid[it] } ?: defaultRegisterId
                val projectUid = bundle.projectUidByTransactionUid[remote.uid]
                val mappedProjectId = projectUid?.let { localIdByProjectUid[it] } ?: 0L
                when {
                    local == null -> {
                        repository.insertTransactionsRaw(
                            listOf(remote.copy(id = 0, cashRegisterId = localRegisterId, projectId = mappedProjectId))
                        )
                        if (!remote.isDeleted) addedTx++
                    }
                    remote.updatedAt > local.updatedAt -> {
                        repository.insertTransactionsRaw(
                            listOf(remote.copy(
                                id = local.id,
                                cashRegisterId = localRegisterId,
                                projectId = if (projectUid == null) local.projectId else mappedProjectId,
                                receiptPhotoUri = local.receiptPhotoUri
                            ))
                        )
                        updatedTx++
                    }
                    remote.updatedAt == local.updatedAt -> {
                        val normalized = remote.copy(
                            id = local.id,
                            cashRegisterId = localRegisterId,
                            projectId = if (projectUid == null) local.projectId else mappedProjectId,
                            receiptPhotoUri = local.receiptPhotoUri
                        )
                        if (normalized != local) conflicts++ else ignored++
                    }
                    else -> ignored++
                }
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
            updatedRegisters = updatedRegisters,
            addedCategories = addedCategories,
            updatedCategories = updatedCategories,
            addedBudgets = addedBudgets,
            addedProjects = addedProjects,
            updatedProjects = updatedProjects,
            conflicts = conflicts,
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
