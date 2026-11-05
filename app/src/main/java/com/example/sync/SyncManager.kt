package com.example.sync

import android.content.Context
import android.net.Uri
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ChurchProfileEntity
import com.example.data.MemberEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.data.UserRole
import java.io.File
import java.util.UUID

/**
 * Échange de données entre appareils, sans réseau ni compte.
 *
 * Le bundle chiffré transporte les données structurées et justificatifs. Les
 * révisions sont rapprochées par identité stable; une version strictement plus
 * récente gagne. Une égalité divergente est mise en attente dans un journal
 * persistant, puis arbitrable depuis l'écran d'équipe. Les comptes et PIN restent locaux.
 */
object SyncManager {

    private const val MAX_SYNC_FILE_BYTES = 150 * 1024 * 1024

    data class MergeReport(
        val sourceChurch: String = "",
        val exportedBy: String = "",
        val exportedByRole: String = "",
        val sourceDeviceId: String = "",
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
        val updatedBudgets: Int = 0,
        val addedProjects: Int = 0,
        val updatedProjects: Int = 0,
        val conflicts: Int = 0,
        val conflictDetails: List<SyncConflictItem> = emptyList(),
        val auditSaved: Boolean = true,
        val ignored: Int = 0
    ) {
        val exportedByLabel: String
            get() = exportedBy.trim().let { name ->
                val roleLabel = runCatching { UserRole.valueOf(exportedByRole).label }.getOrNull()
                listOf(name, roleLabel).filterNotNull().filter { it.isNotBlank() }.joinToString(" — ")
            }

        val totalChanges: Int
            get() = addedTransactions + updatedTransactions + addedMembers +
                updatedMembers + addedRegisters + updatedRegisters + addedCategories +
                updatedCategories + addedBudgets + updatedBudgets +
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
            if (updatedBudgets > 0) append(", $updatedBudgets budget(s) mis à jour")
            if (addedProjects > 0) append(", $addedProjects projet(s)")
            if (updatedProjects > 0) append(", $updatedProjects projet(s) mis à jour")
            if (conflicts > 0) append(" — $conflicts conflit(s) à examiner; la copie locale est conservée jusqu'à arbitrage")
            if (!auditSaved) append(" — ATTENTION : le journal des conflits n'a pas pu être enregistré")
            if (ignored > 0) append(" — $ignored élément(s) inchangé(s) ou plus ancien(s)")
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
        authorRole: String,
        directory: File
    ): File {
        val bundle = SyncBundle(
            churchName = profile.displayName,
            churchUid = profile.churchUid,
            deviceId = DeviceIdentity.deviceId(context),
            author = authorName,
            authorRole = authorRole,
            exportedAt = System.currentTimeMillis(),
            registers = repository.registersForSync(),
            categories = repository.categoriesForSync(),
            members = repository.membersForSync(),
            budgets = repository.budgetsForSync(),
            projects = repository.projectsForSync(),
            transactions = repository.transactionsForSync()
        ).let { bundle ->
            val receipts = com.example.export.BackupService.collectReceiptFilesForSync(
                context,
                repository.workspaceId,
                bundle.transactions
            )
            require(bundle.transactions.filter { it.hasReceipt }.all { it.uid in receipts }) {
                "Un justificatif est inaccessible; l'export a été arrêté pour éviter une perte de pièce jointe."
            }
            bundle.copy(receiptFilesByTransactionUid = receipts)
        }
        val plain = SyncBundleCodec.encode(bundle)
        val encrypted = SyncCrypto.encrypt(plain, passphrase)
        require(encrypted.size <= MAX_SYNC_FILE_BYTES) { "Le fichier d'échange dépasserait la taille maximale autorisée." }

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
        val bytes = readLimited(context, uri)

        val plain = SyncCrypto.decrypt(bytes, passphrase)
        val bundle = SyncBundleCodec.decode(plain)
        verifySource(context, repository, bundle, rejectSameDevice = true)

        val report = mergeBundle(context, repository, bundle)
        if (report.conflictDetails.isNotEmpty()) {
            val persisted = runCatching {
                SyncConflictJournal.recordDetected(context, repository.workspaceId, bytes, report.conflictDetails)
            }.getOrNull()
            return if (persisted != null) report.copy(conflictDetails = persisted)
            else report.copy(auditSaved = false)
        }
        return report
    }

    fun pendingConflicts(context: Context, repository: TransactionRepository): List<SyncConflictItem> =
        SyncConflictJournal.pending(context, repository.workspaceId)

    fun conflictHistory(context: Context, repository: TransactionRepository): List<SyncConflictItem> =
        SyncConflictJournal.history(context, repository.workspaceId)

    suspend fun resolveConflict(
        context: Context,
        repository: TransactionRepository,
        conflict: SyncConflictItem,
        useImported: Boolean,
        passphrase: String
    ): MergeReport {
        val report = if (useImported) {
            val encrypted = SyncConflictJournal.readPendingBundle(context, repository.workspaceId, conflict)
            val bundle = SyncBundleCodec.decode(SyncCrypto.decrypt(encrypted, passphrase))
            verifySource(context, repository, bundle, rejectSameDevice = false)
            mergeBundle(context, repository, bundle, forcedIncomingKeys = setOf(conflict.key)).also {
                require(it.conflictDetails.none { detail -> detail.key == conflict.key }) {
                    "La révision importée n'a pas pu être appliquée. Le conflit reste en attente."
                }
            }
        } else {
            MergeReport(sourceChurch = repository.getProfile()?.churchName.orEmpty())
        }
        SyncConflictJournal.markResolved(
            context,
            repository.workspaceId,
            conflict,
            if (useImported) "USE_IMPORTED" else "KEEP_LOCAL"
        )
        return report
    }

    private suspend fun verifySource(
        context: Context,
        repository: TransactionRepository,
        bundle: SyncBundle,
        rejectSameDevice: Boolean
    ) {
        val localProfile = repository.getProfile()
            ?: throw IllegalStateException("Configurez d'abord le profil de cette église.")
        val sameChurch = if (bundle.churchUid.isNotBlank() && localProfile.churchUid.isNotBlank()) {
            bundle.churchUid == localProfile.churchUid
        } else {
            repository.workspaceId == "default" &&
                bundle.churchName.trim().equals(localProfile.churchName.trim(), ignoreCase = true)
        }
        if (!sameChurch) throw DifferentChurch()
        if (rejectSameDevice && bundle.deviceId.isNotBlank() &&
            bundle.deviceId == DeviceIdentity.deviceId(context)
        ) throw SameFileAsLocal()
    }

    private suspend fun mergeBundle(
        context: Context,
        repository: TransactionRepository,
        bundle: SyncBundle,
        forcedIncomingKeys: Set<String> = emptySet()
    ): MergeReport {
        var addedTx = 0
        var updatedTx = 0
        var addedMembers = 0
        var updatedMembers = 0
        var addedRegisters = 0
        var updatedRegisters = 0
        var addedCategories = 0
        var updatedCategories = 0
        var addedBudgets = 0
        var updatedBudgets = 0
        var addedProjects = 0
        var updatedProjects = 0
        var conflicts = 0
        val conflictDetails = mutableListOf<SyncConflictItem>()
        var ignored = 0
        val createdReceiptPaths = mutableListOf<String>()
        val obsoleteReceiptPaths = mutableListOf<String>()

        try {
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
                    val key = "register:${remote.uid}"
                    when {
                        remote.updatedAt > local.updatedAt || key in forcedIncomingKeys -> {
                            repository.insertRegistersRaw(listOf(remote.copy(id = local.id)))
                            updatedRegisters++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> {
                            conflicts++
                            conflictDetails += conflictItem(
                                key, "Caisse", remote.uid, remote.name,
                                "${local.name} · solde initial ${local.initialBalanceCents} · supprimée=${local.isDeleted}",
                                "${remote.name} · solde initial ${remote.initialBalanceCents} · supprimée=${remote.isDeleted}",
                                local.updatedAt, remote.updatedAt
                            )
                        }
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
                    val key = "project:${remote.uid}"
                    when {
                        remote.updatedAt > local.updatedAt || key in forcedIncomingKeys -> {
                            repository.insertProjectRaw(remote.copy(id = local.id))
                            updatedProjects++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> {
                            conflicts++
                            conflictDetails += conflictItem(
                                key, "Projet", remote.uid, remote.name,
                                "${local.name} · objectif ${local.targetCents} · clos=${local.isClosed}",
                                "${remote.name} · objectif ${remote.targetCents} · clos=${remote.isClosed}",
                                local.updatedAt, remote.updatedAt
                            )
                        }
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
                    val key = "category:${remote.uid}"
                    when {
                        remote.updatedAt > local.updatedAt || key in forcedIncomingKeys -> {
                            repository.insertCategoriesRaw(listOf(remote.copy(id = local.id)))
                            updatedCategories++
                        }
                        remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> {
                            conflicts++
                            conflictDetails += conflictItem(
                                key, "Catégorie", remote.uid, remote.name,
                                "${local.name} · ${local.type} · ${local.colorHex} · supprimée=${local.isDeleted}",
                                "${remote.name} · ${remote.type} · ${remote.colorHex} · supprimée=${remote.isDeleted}",
                                local.updatedAt, remote.updatedAt
                            )
                        }
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
                    remote.updatedAt > local.updatedAt || "member:${remote.uid}" in forcedIncomingKeys -> {
                        repository.insertMembersRaw(listOf(remote.copy(id = local.id)))
                        updatedMembers++
                    }
                    remote.updatedAt == local.updatedAt && remote.copy(id = local.id) != local -> {
                        conflicts++
                        conflictDetails += conflictItem(
                            "member:${remote.uid}", "Membre", remote.uid, remote.fullName,
                            "${local.fullName} · ${local.phone} · actif=${local.isActive} · supprimé=${local.isDeleted}",
                            "${remote.fullName} · ${remote.phone} · actif=${remote.isActive} · supprimé=${remote.isDeleted}",
                            local.updatedAt, remote.updatedAt
                        )
                    }
                    else -> ignored++
                }
            }

            // 4. Budgets sans horodatage individuel : nouveau créés, identiques ignorés,
            //    divergents signalés et jamais écrasés silencieusement.
            for (remote in bundle.budgets) {
                val local = repository.getBudgetForCategoryAndPeriod(remote.categoryName, remote.monthYearKey)
                val key = "budget:${remote.categoryName}|${remote.monthYearKey}"
                when {
                    local == null -> {
                        repository.insertBudgetRaw(remote.copy(id = 0))
                        addedBudgets++
                    }
                    remote.copy(id = local.id) == local -> ignored++
                    key in forcedIncomingKeys -> {
                        repository.insertBudgetRaw(remote.copy(id = local.id))
                        updatedBudgets++
                    }
                    else -> {
                        conflicts++
                        conflictDetails += conflictItem(
                            key, "Budget", key, remote.categoryName,
                            "${local.monthYearKey} · plafond ${local.amountCents} · seuil ${local.alertThresholdPercent}% · strict=${local.isHardLimit}",
                            "${remote.monthYearKey} · plafond ${remote.amountCents} · seuil ${remote.alertThresholdPercent}% · strict=${remote.isHardLimit}",
                            0L, 0L
                        )
                    }
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
                val remoteHasReceipt = remote.uid in bundle.receiptFilesByTransactionUid
                when {
                    local == null -> {
                        val receiptPath = if (remoteHasReceipt) {
                            val path = com.example.export.BackupService.writeReceiptFilesForSync(
                                context,
                                repository.workspaceId,
                                mapOf(remote.uid to bundle.receiptFilesByTransactionUid.getValue(remote.uid))
                            ).getValue(remote.uid)
                            createdReceiptPaths += path
                            path
                        } else ""
                        repository.insertTransactionsRaw(
                            listOf(remote.copy(
                                id = 0,
                                cashRegisterId = localRegisterId,
                                projectId = mappedProjectId,
                                receiptPhotoUri = receiptPath
                            ))
                        )
                        if (!remote.isDeleted) addedTx++
                    }
                    remote.updatedAt > local.updatedAt || "transaction:${remote.uid}" in forcedIncomingKeys -> {
                        val receiptPath = if (remoteHasReceipt) {
                            val path = com.example.export.BackupService.writeReceiptFilesForSync(
                                context,
                                repository.workspaceId,
                                mapOf(remote.uid to bundle.receiptFilesByTransactionUid.getValue(remote.uid))
                            ).getValue(remote.uid)
                            createdReceiptPaths += path
                            path
                        } else ""
                        if (local.receiptPhotoUri.isNotBlank() && local.receiptPhotoUri != receiptPath) {
                            obsoleteReceiptPaths += local.receiptPhotoUri
                        }
                        repository.insertTransactionsRaw(
                            listOf(remote.copy(
                                id = local.id,
                                cashRegisterId = localRegisterId,
                                projectId = if (projectUid == null) local.projectId else mappedProjectId,
                                receiptPhotoUri = receiptPath
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
                        val receiptMatches = if (remoteHasReceipt) {
                            com.example.export.BackupService.collectReceiptFilesForSync(
                                context,
                                repository.workspaceId,
                                listOf(local)
                            )[remote.uid]?.contentEquals(bundle.receiptFilesByTransactionUid.getValue(remote.uid)) == true
                        } else !local.hasReceipt
                        if (normalized != local || !receiptMatches) {
                            conflicts++
                            conflictDetails += conflictItem(
                                "transaction:${remote.uid}", "Écriture", remote.uid, remote.title,
                                "${local.title} · ${local.amountCents} · ${local.category} · justificatif=${local.hasReceipt}",
                                "${remote.title} · ${remote.amountCents} · ${remote.category} · justificatif=$remoteHasReceipt",
                                local.updatedAt, remote.updatedAt
                            )
                        } else ignored++
                    }
                    else -> ignored++
                }
            }
            }
        } catch (error: Throwable) {
            com.example.export.BackupService.deleteReceiptFilesForSync(
                context,
                repository.workspaceId,
                createdReceiptPaths
            )
            throw error
        }
        com.example.export.BackupService.deleteReceiptFilesForSync(
            context,
            repository.workspaceId,
            obsoleteReceiptPaths
        )

        return MergeReport(
            sourceChurch = bundle.churchName,
            exportedBy = bundle.author,
            exportedByRole = bundle.authorRole,
            sourceDeviceId = bundle.deviceId,
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
            updatedBudgets = updatedBudgets,
            addedProjects = addedProjects,
            updatedProjects = updatedProjects,
            conflicts = conflicts,
            conflictDetails = conflictDetails,
            ignored = ignored
        )
    }

    private fun conflictItem(
        key: String,
        kind: String,
        uid: String,
        label: String,
        localValue: String,
        incomingValue: String,
        localUpdatedAt: Long,
        incomingUpdatedAt: Long
    ) = SyncConflictItem(
        key = key,
        kind = kind,
        uid = uid,
        label = label,
        localValue = localValue,
        incomingValue = incomingValue,
        localUpdatedAt = localUpdatedAt,
        incomingUpdatedAt = incomingUpdatedAt
    )

    private fun readLimited(context: Context, uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Fichier illisible")
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        input.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_SYNC_FILE_BYTES) { "Le fichier dépasse la taille maximale autorisée." }
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
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
