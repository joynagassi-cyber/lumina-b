package com.example.sync

import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.MemberEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
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
    val exportedAt: Long = System.currentTimeMillis(),
    val registers: List<CashRegisterEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList()
) {
    val transactionCount: Int get() = transactions.size
}

object SyncBundleCodec {

    const val FORMAT = "lumina-sync"
    const val VERSION = 1

    class UnsupportedFormat(message: String) : Exception(message)

    fun encode(bundle: SyncBundle): ByteArray {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("churchName", bundle.churchName)
        root.put("churchUid", bundle.churchUid)
        root.put("deviceId", bundle.deviceId)
        root.put("author", bundle.author)
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
                    put("updatedAt", member.updatedAt)
                    put("isDeleted", member.isDeleted)
                }
            )
        }
        root.put("members", members)

        val registerUidById = bundle.registers.associate { it.id to it.uid }
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
                }
            )
        }
        root.put("transactions", transactions)

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
        if (root.optInt("version", 0) > VERSION) {
            throw UnsupportedFormat("Fichier produit par une version plus récente de l'application")
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
                updatedAt = obj.optLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted")
            )
        }

        val transactions = root.optJSONArray("transactions").toList { obj ->
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
                isDeleted = obj.optBoolean("isDeleted")
            )
        }

        return SyncBundle(
            churchName = root.optString("churchName"),
            churchUid = root.optString("churchUid", ""),
            deviceId = root.optString("deviceId"),
            author = root.optString("author"),
            exportedAt = root.optLong("exportedAt"),
            registers = registers,
            categories = categories,
            members = members,
            transactions = transactions
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
