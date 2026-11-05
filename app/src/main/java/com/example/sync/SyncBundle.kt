package com.example.sync

import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.MemberEntity
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.FinancialAmount
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contenu d'un fichier d'échange, avant chiffrement.
 *
 * Le JSON est volontairement explicite et versionné : un fichier produit par
 * une version ultérieure est refusé plutôt que mal interprété.
 *
 * Les caisses sont diffusées avec leur identité stable (`uid`) car les
 * identifiants locaux ne veulent rien dire d'un appareil à l'autre ; les
 * écritures y font référence par `registerUid`.
 */
data class SyncBundle(
    val churchName: String = "",
    val churchUid: String = "",
    val deviceId: String = "",
    val author: String = "",
    val authorRole: String = "",
    val exportedAt: Long = System.currentTimeMillis(),
    val registers: List<CashRegisterEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    /** projectUid stable, keyed par UID d'écriture; les IDs Room ne traversent pas les appareils. */
    val projectUidByTransactionUid: Map<String, String> = emptyMap(),
    /** registerUid stable, keyed par UID d'écriture; supporte les changements de nom. */
    val registerUidByTransactionUid: Map<String, String> = emptyMap(),
    /** Justificatifs binaires indexés par l'UID de l'écriture. */
    val receiptFilesByTransactionUid: Map<String, ByteArray> = emptyMap()
) {
    val transactionCount: Int get() = transactions.size
}

object SyncBundleCodec {

    const val FORMAT = "lumina-sync"
    const val VERSION = 3

    class UnsupportedFormat(message: String) : Exception(message)

    private const val MAX_RECEIPTS = 10_000
    private const val MAX_RECEIPT_BYTES = 32 * 1024 * 1024
    private const val MAX_TOTAL_RECEIPT_BYTES = 100 * 1024 * 1024

    fun encode(bundle: SyncBundle): ByteArray {
        require(bundle.transactions.all { FinancialAmount.isValidTransactionAmount(it.amountCents) }) {
            "Le dossier contient un montant d'opération invalide ou hors limite."
        }
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("churchName", bundle.churchName)
        root.put("churchUid", bundle.churchUid)
        root.put("deviceId", bundle.deviceId)
        root.put("author", bundle.author)
        root.put("authorRole", bundle.authorRole)
        root.put("exportedAt", bundle.exportedAt)

        val registers = JSONArray()
        bundle.registers.forEach { reg ->
            registers.put(
                JSONObject().apply {
                    put("uid", reg.uid)
                    put("name", reg.name)
                    put("description", reg.description)
                    put("initialBalanceCents", reg.initialBalanceCents)
                    put("iconType", reg.iconType)
                    put("isDefault", reg.isDefault)
                    put("updatedAt", reg.updatedAt)
                    put("isDeleted", reg.isDeleted)
                }
            )
        }
        root.put("registers", registers)

        val categories = JSONArray()
        bundle.categories.forEach { category ->
            categories.put(
                JSONObject().apply {
                    put("uid", category.uid)
                    put("name", category.name)
                    put("type", category.type.name)
                    put("colorHex", category.colorHex)
                    put("isDefault", category.isDefault)
                    put("iconName", category.iconName)
                    put("updatedAt", category.updatedAt)
                    put("isDeleted", category.isDeleted)
                }
            )
        }
        root.put("categories", categories)

        val members = JSONArray()
        bundle.members.forEach { member ->
            members.put(
                JSONObject().apply {
                    put("uid", member.uid)
                    put("firstName", member.firstName)
                    put("lastName", member.lastName)
                    put("maritalStatus", member.maritalStatus)
                    put("spouseName", member.spouseName)
                    put("phone", member.phone)
                    put("address", member.address)
                    put("residence", member.residence)
                    put("profession", member.profession)
                    put("notes", member.notes)
                    put("joinedAt", member.joinedAt)
                    put("isActive", member.isActive)
                    put("updatedAt", member.updatedAt)
                    put("isDeleted", member.isDeleted)
                }
            )
        }
        root.put("members", members)

        val budgets = JSONArray()
        bundle.budgets.forEach { budget ->
            budgets.put(JSONObject().apply {
                put("categoryName", budget.categoryName)
                put("amountCents", budget.amountCents)
                put("monthYearKey", budget.monthYearKey)
                put("alertThresholdPercent", budget.alertThresholdPercent)
                put("isHardLimit", budget.isHardLimit)
                put("notes", budget.notes)
            })
        }
        root.put("budgets", budgets)

        val projects = JSONArray()
        bundle.projects.forEach { project ->
            projects.put(JSONObject().apply {
                put("uid", project.uid)
                put("name", project.name)
                put("description", project.description)
                put("targetCents", project.targetCents)
                put("isClosed", project.isClosed)
                put("createdAt", project.createdAt)
                put("updatedAt", project.updatedAt)
                put("isDeleted", project.isDeleted)
                put("kind", project.kind)
            })
        }
        root.put("projects", projects)

        val registerUidById = bundle.registers.associate { it.id to it.uid }
        val projectUidById = bundle.projects.associate { it.id to it.uid }
        val transactions = JSONArray()
        bundle.transactions.forEach { tx ->
            transactions.put(
                JSONObject().apply {
                    put("uid", tx.uid)
                    put("title", tx.title)
                    put("type", tx.type.name)
                    put("amountCents", tx.amountCents)
                    put("category", tx.category)
                    put("timestamp", tx.timestamp)
                    put("note", tx.note)
                    put("registerUid", registerUidById[tx.cashRegisterId] ?: "")
                    put("cashRegisterName", tx.cashRegisterName)
                    put("transferGroupId", tx.transferGroupId)
                    put("hasReceipt", tx.hasReceipt)
                    put("updatedAt", tx.updatedAt)
                    put("updatedBy", tx.updatedBy)
                    put("isDeleted", tx.isDeleted)
                    put("reversalOfUid", tx.reversalOfUid)
                    put("isCancelled", tx.isCancelled)
                    put("projectUid", projectUidById[tx.projectId] ?: "")
                }
            )
        }
        root.put("transactions", transactions)

        val transactionUidsWithReceipts = bundle.transactions.filter { it.hasReceipt }.map { it.uid }.toSet()
        require(bundle.receiptFilesByTransactionUid.size <= MAX_RECEIPTS) { "Trop de justificatifs dans le fichier d'échange" }
        var receiptBytes = 0L
        val receipts = JSONObject()
        bundle.receiptFilesByTransactionUid.forEach { (uid, bytes) ->
            require(uid.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Identifiant de justificatif invalide" }
            require(uid in transactionUidsWithReceipts) {
                "Justificatif sans écriture correspondante"
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_RECEIPT_BYTES) { "Un justificatif est vide ou dépasse la taille autorisée" }
            receiptBytes += bytes.size
            require(receiptBytes <= MAX_TOTAL_RECEIPT_BYTES) { "Les justificatifs dépassent la taille autorisée" }
            receipts.put(uid, Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        require(bundle.transactions.filter { it.hasReceipt }.all { it.uid in bundle.receiptFilesByTransactionUid }) {
            "Un justificatif est absent. Le fichier d'échange n'a pas été créé."
        }
        root.put("receiptFiles", receipts)

        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SyncBundle {
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            throw UnsupportedFormat("Contenu illisible")
        }
        if (root.optString("format") != FORMAT) {
            throw UnsupportedFormat("Ce fichier n'est pas un échange Lumina")
        }
        val version = root.optInt("version", 1)
        if (version > VERSION) {
            throw UnsupportedFormat("Fichier produit par une version plus récente de l'application")
        }
        if (version < 1) {
            throw UnsupportedFormat("Version d'échange Lumina invalide")
        }

        val registers = root.optJSONArray("registers").toList { obj ->
            CashRegisterEntity(
                id = 0,
                uid = obj.optString("uid"),
                name = obj.optString("name"),
                description = obj.optString("description"),
                initialBalanceCents = obj.optLong("initialBalanceCents"),
                iconType = obj.optString("iconType", "cash"),
                isDefault = obj.optBoolean("isDefault"),
                updatedAt = obj.optLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted")
            )
        }

        val categories = root.optJSONArray("categories").toList { obj ->
            CategoryEntity(
                id = 0,
                uid = obj.optString("uid"),
                name = obj.optString("name"),
                type = runCatching { TransactionType.valueOf(obj.optString("type")) }
                    .getOrDefault(TransactionType.EXPENSE),
                colorHex = obj.optString("colorHex", "#3730A3"),
                isDefault = obj.optBoolean("isDefault"),
                iconName = obj.optString("iconName", ""),
                updatedAt = obj.optLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted")
            )
        }

        val members = root.optJSONArray("members").toList { obj ->
            MemberEntity(
                id = 0,
                uid = obj.optString("uid"),
                firstName = obj.optString("firstName"),
                lastName = obj.optString("lastName"),
                maritalStatus = obj.optString("maritalStatus"),
                spouseName = obj.optString("spouseName"),
                phone = obj.optString("phone"),
                address = obj.optString("address"),
                residence = obj.optString("residence"),
                profession = obj.optString("profession"),
                notes = obj.optString("notes"),
                joinedAt = obj.optLong("joinedAt"),
                isActive = obj.optBoolean("isActive", true),
                updatedAt = obj.optLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted")
            )
        }

        val budgets = root.optJSONArray("budgets").toList { obj ->
            BudgetEntity(
                id = 0,
                categoryName = obj.optString("categoryName"),
                amountCents = obj.optLong("amountCents"),
                monthYearKey = obj.optString("monthYearKey"),
                alertThresholdPercent = obj.optInt("alertThresholdPercent", 80),
                isHardLimit = obj.optBoolean("isHardLimit", false),
                notes = obj.optString("notes", "")
            )
        }

        val projects = root.optJSONArray("projects").toList { obj ->
            ProjectEntity(
                id = 0,
                uid = obj.optString("uid"),
                name = obj.optString("name"),
                description = obj.optString("description"),
                targetCents = obj.optLong("targetCents"),
                isClosed = obj.optBoolean("isClosed"),
                createdAt = obj.optLong("createdAt"),
                updatedAt = obj.optLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted"),
                kind = obj.optString("kind", "PROJECT")
            )
        }

        val transactionProjectUids = mutableMapOf<String, String>()
        val transactionRegisterUids = mutableMapOf<String, String>()
        val transactionUidsWithReceipts = mutableSetOf<String>()
        val transactions = root.optJSONArray("transactions").toList { obj ->
            val uid = obj.optString("uid")
            val projectUid = obj.optString("projectUid")
            val registerUid = obj.optString("registerUid")
            if (uid.isNotBlank() && projectUid.isNotBlank()) transactionProjectUids[uid] = projectUid
            if (uid.isNotBlank() && registerUid.isNotBlank()) transactionRegisterUids[uid] = registerUid
            if (uid.isNotBlank() && obj.optBoolean("hasReceipt")) transactionUidsWithReceipts += uid
            TransactionEntity(
                id = 0,
                uid = obj.optString("uid"),
                title = obj.optString("title"),
                type = runCatching { TransactionType.valueOf(obj.optString("type")) }
                    .getOrDefault(TransactionType.EXPENSE),
                amountCents = obj.optLong("amountCents"),
                category = obj.optString("category"),
                timestamp = obj.optLong("timestamp"),
                note = obj.optString("note"),
                cashRegisterId = 0L,
                cashRegisterName = obj.optString("cashRegisterName"),
                transferGroupId = obj.optString("transferGroupId"),
                receiptPhotoUri = "",
                updatedAt = obj.optLong("updatedAt"),
                updatedBy = obj.optString("updatedBy"),
                isDeleted = obj.optBoolean("isDeleted"),
                reversalOfUid = obj.optString("reversalOfUid", ""),
                isCancelled = obj.optBoolean("isCancelled", false),
                projectId = 0L
            )
        }

        require(transactions.all { FinancialAmount.isValidTransactionAmount(it.amountCents) }) {
            "Le fichier d'échange contient un montant d'opération invalide ou hors limite"
        }

        val receiptFilesByTransactionUid = linkedMapOf<String, ByteArray>()
        val receiptObject = root.optJSONObject("receiptFiles") ?: JSONObject()
        var totalReceiptBytes = 0L
        val transactionUids = transactions.map { it.uid }.toSet()
        val receiptKeys = receiptObject.keys()
        while (receiptKeys.hasNext()) {
            val uid = receiptKeys.next()
            require(uid.matches(Regex("[A-Za-z0-9_-]{1,128}")) &&
                uid in transactionUids && uid in transactionUidsWithReceipts
            ) {
                "Le fichier d'échange contient un justificatif sans écriture correspondante"
            }
            val encoded = receiptObject.optString(uid)
            val bytes = try {
                Base64.decode(encoded, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                throw UnsupportedFormat("Justificatif illisible dans le fichier d'échange")
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_RECEIPT_BYTES) { "Un justificatif est vide ou dépasse la taille autorisée" }
            totalReceiptBytes += bytes.size
            require(totalReceiptBytes <= MAX_TOTAL_RECEIPT_BYTES) { "Les justificatifs dépassent la taille autorisée" }
            receiptFilesByTransactionUid[uid] = bytes
        }
        require(receiptFilesByTransactionUid.size <= MAX_RECEIPTS) { "Trop de justificatifs dans le fichier d'échange" }
        require(transactionUidsWithReceipts.all { it in receiptFilesByTransactionUid }) {
            "Ce fichier de synchronisation ne contient pas tous les justificatifs annoncés. Aucune fusion effectuée."
        }

        return SyncBundle(
            churchName = root.optString("churchName"),
            churchUid = root.optString("churchUid", ""),
            deviceId = root.optString("deviceId"),
            author = root.optString("author"),
            authorRole = root.optString("authorRole"),
            exportedAt = root.optLong("exportedAt"),
            registers = registers,
            categories = categories,
            members = members,
            budgets = budgets,
            projects = projects,
            transactions = transactions,
            projectUidByTransactionUid = transactionProjectUids,
            registerUidByTransactionUid = transactionRegisterUids,
            receiptFilesByTransactionUid = receiptFilesByTransactionUid
        )
    }

    /** Association uid de caisse → nom, utilisée à l'import. */
    fun registerUidsByName(bundle: SyncBundle): Map<String, String> =
        bundle.registers.filter { it.uid.isNotBlank() }.associate { it.name to it.uid }

    private fun <T> JSONArray?.toList(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val items = mutableListOf<T>()
        for (index in 0 until length()) {
            val obj = optJSONObject(index) ?: continue
            items.add(transform(obj))
        }
        return items
    }
}
