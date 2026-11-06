package com.example.export

import com.example.data.BudgetEntity
import com.example.data.CashRegisterEntity
import com.example.data.CategoryEntity
import com.example.data.ChurchProfileEntity
import com.example.data.MemberEntity
import com.example.data.ProjectEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sauvegarde intégrale du dossier, sous une forme indépendante de la base
 * locale : un simple texte balisé. Elle peut donc être conservée sur une clé,
 * envoyée par messagerie, puis relue même après une réinstallation.
 */
data class BackupSnapshot(
    val createdAt: Long = System.currentTimeMillis(),
    val churchName: String = "",
    val users: List<UserEntity> = emptyList(),
    val profile: ChurchProfileEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val registers: List<CashRegisterEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    /** Marqueur transitoire local; n'est jamais sérialisé dans une sauvegarde complète. */
    val importedAsLightInvitation: Boolean = false
) {
    val transactionCount: Int get() = transactions.size
    val memberCount: Int get() = members.size
}

/**
 * Conversion de et vers le texte de sauvegarde.
 *
 * Le format est volontairement tolérant : une clé absente prend sa valeur par
 * défaut, ce qui permet de relire une sauvegarde faite par une version
 * antérieure sans échouer.
 */
object BackupCodec {

    const val FORMAT = "lumina-sauvegarde"
    const val VERSION = 2

    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("createdAt", snapshot.createdAt)
        root.put("churchName", snapshot.churchName)

        snapshot.profile?.let { p ->
            root.put(
                "profile",
                JSONObject().apply {
                    put("churchName", p.churchName)
                    put("churchUid", p.churchUid)
                    put("denomination", p.denomination)
                    put("city", p.city)
                    put("address", p.address)
                    put("phone", p.phone)
                    put("email", p.email)
                    put("pastorName", p.pastorName)
                    put("treasurerName", p.treasurerName)
                    put("logoBase64", p.logoBase64)
                    put("accentColorHex", p.accentColorHex)
                    put("onboardingCompleted", p.onboardingCompleted)
                    put("themeMode", p.themeMode)
                    put("amountDisplayMode", p.amountDisplayMode)
                    put("notificationsEnabled", p.notificationsEnabled)
                    put("currencyCode", p.currencyCode)
                    put("booksClosedUntilMillis", p.booksClosedUntilMillis)
                    put("updatedAt", p.updatedAt)
                }
            )
        }

        root.put(
            "users",
            JSONArray().apply {
                snapshot.users.forEach { u ->
                    put(
                        JSONObject().apply {
                            put("id", u.id)
                            put("uid", u.uid)
                            put("displayName", u.displayName)
                            put("role", u.role)
                            put("pinHash", u.pinHash)
                            put("pinSalt", u.pinSalt)
                            put("isActive", u.isActive)
                            put("createdAt", u.createdAt)
                            put("lastLoginAt", u.lastLoginAt)
                            put("updatedAt", u.updatedAt)
                            put("isDeleted", u.isDeleted)
                        }
                    )
                }
            }
        )

        root.put(
            "members",
            JSONArray().apply {
                snapshot.members.forEach { m ->
                    put(
                        JSONObject().apply {
                            put("id", m.id)
                            put("uid", m.uid)
                            put("firstName", m.firstName)
                            put("lastName", m.lastName)
                            put("maritalStatus", m.maritalStatus)
                            put("spouseName", m.spouseName)
                            put("phone", m.phone)
                            put("address", m.address)
                            put("residence", m.residence)
                            put("profession", m.profession)
                            put("notes", m.notes)
                            put("joinedAt", m.joinedAt)
                            put("isActive", m.isActive)
                            put("updatedAt", m.updatedAt)
                            put("isDeleted", m.isDeleted)
                        }
                    )
                }
            }
        )

        root.put(
            "categories",
            JSONArray().apply {
                snapshot.categories.forEach { c ->
                    put(
                        JSONObject().apply {
                            put("id", c.id)
                            put("uid", c.uid)
                            put("name", c.name)
                            put("type", c.type.name)
                            put("colorHex", c.colorHex)
                            put("isDefault", c.isDefault)
                            put("iconName", c.iconName)
                            put("updatedAt", c.updatedAt)
                            put("isDeleted", c.isDeleted)
                        }
                    )
                }
            }
        )

        root.put(
            "budgets",
            JSONArray().apply {
                snapshot.budgets.forEach { b ->
                    put(
                        JSONObject().apply {
                            put("id", b.id)
                            put("categoryName", b.categoryName)
                            put("amountCents", b.amountCents)
                            put("monthYearKey", b.monthYearKey)
                            put("alertThresholdPercent", b.alertThresholdPercent)
                            put("isHardLimit", b.isHardLimit)
                            put("notes", b.notes)
                        }
                    )
                }
            }
        )

        root.put(
            "registers",
            JSONArray().apply {
                snapshot.registers.forEach { r ->
                    put(
                        JSONObject().apply {
                            put("id", r.id)
                            put("uid", r.uid)
                            put("name", r.name)
                            put("description", r.description)
                            put("initialBalanceCents", r.initialBalanceCents)
                            put("iconType", r.iconType)
                            put("isDefault", r.isDefault)
                            put("updatedAt", r.updatedAt)
                            put("isDeleted", r.isDeleted)
                        }
                    )
                }
            }
        )

        root.put(
            "projects",
            JSONArray().apply {
                snapshot.projects.forEach { p ->
                    put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("uid", p.uid)
                            put("name", p.name)
                            put("description", p.description)
                            put("targetCents", p.targetCents)
                            put("isClosed", p.isClosed)
                            put("createdAt", p.createdAt)
                            put("updatedAt", p.updatedAt)
                            put("isDeleted", p.isDeleted)
                            put("kind", p.kind)
                            put("startAtMillis", p.startAtMillis)
                            put("endAtMillis", p.endAtMillis)
                            put("actionItemsJson", p.actionItemsJson)
                        }
                    )
                }
            }
        )

        root.put(
            "transactions",
            JSONArray().apply {
                snapshot.transactions.forEach { t ->
                    put(
                        JSONObject().apply {
                            put("id", t.id)
                            put("uid", t.uid)
                            put("title", t.title)
                            put("type", t.type.name)
                            put("amountCents", t.amountCents)
                            put("category", t.category)
                            put("timestamp", t.timestamp)
                            put("note", t.note)
                            put("cashRegisterId", t.cashRegisterId)
                            put("cashRegisterName", t.cashRegisterName)
                            put("transferGroupId", t.transferGroupId)
                            put("receiptPhotoUri", t.receiptPhotoUri)
                            put("updatedAt", t.updatedAt)
                            put("updatedBy", t.updatedBy)
                            put("isDeleted", t.isDeleted)
                            put("reversalOfUid", t.reversalOfUid)
                            put("isCancelled", t.isCancelled)
                            put("projectId", t.projectId)
                        }
                    )
                }
            }
        )

        return root.toString()
    }

    /** @throws IllegalArgumentException si le texte n'est pas une sauvegarde. */
    fun decode(text: String): BackupSnapshot {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Fichier illisible : ce n'est pas une sauvegarde Lumina.")
        }
        val format = root.optString("format", "")
        require(format == FORMAT) { "Fichier illisible : ce n'est pas une sauvegarde Lumina." }
        require(root.optInt("version", 1) <= VERSION) {
            "Sauvegarde créée par une version plus récente de l'application."
        }

        val profile = root.optJSONObject("profile")?.let { p ->
            ChurchProfileEntity(
                id = 1L,
                churchName = p.optString("churchName", ""),
                churchUid = p.optString("churchUid", ""),
                denomination = p.optString("denomination", ""),
                city = p.optString("city", ""),
                address = p.optString("address", ""),
                phone = p.optString("phone", ""),
                email = p.optString("email", ""),
                pastorName = p.optString("pastorName", ""),
                treasurerName = p.optString("treasurerName", ""),
                logoBase64 = p.optString("logoBase64", ""),
                accentColorHex = p.optString("accentColorHex", ChurchProfileEntity.DEFAULT_ACCENT),
                onboardingCompleted = p.optBoolean("onboardingCompleted", false),
                themeMode = p.optString("themeMode", "SYSTEM"),
                amountDisplayMode = p.optString("amountDisplayMode", "STANDARD"),
                notificationsEnabled = p.optBoolean("notificationsEnabled", true),
                currencyCode = p.optString("currencyCode", "FCFA"),
                booksClosedUntilMillis = p.optLong("booksClosedUntilMillis", 0L),
                updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        return BackupSnapshot(
            createdAt = root.optLong("createdAt", System.currentTimeMillis()),
            churchName = root.optString("churchName", profile?.churchName ?: ""),
            profile = profile,
            users = array(root, "users").map { o ->
                UserEntity(
                    id = o.optLong("id", 0L),
                    uid = o.optString("uid", ""),
                    displayName = o.optString("displayName", ""),
                    role = o.optString("role", "TRESORIER"),
                    pinHash = o.optString("pinHash", ""),
                    pinSalt = o.optString("pinSalt", ""),
                    isActive = o.optBoolean("isActive", true),
                    createdAt = o.optLong("createdAt", 0L),
                    lastLoginAt = o.optLong("lastLoginAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L),
                    isDeleted = o.optBoolean("isDeleted", false)
                )
            },
            members = array(root, "members").map { o ->
                MemberEntity(
                    id = o.optLong("id", 0L),
                    firstName = o.optString("firstName", ""),
                    lastName = o.optString("lastName", ""),
                    maritalStatus = o.optString("maritalStatus", ""),
                    spouseName = o.optString("spouseName", ""),
                    phone = o.optString("phone", ""),
                    address = o.optString("address", ""),
                    residence = o.optString("residence", ""),
                    profession = o.optString("profession", ""),
                    notes = o.optString("notes", ""),
                    joinedAt = o.optLong("joinedAt", 0L),
                    isActive = o.optBoolean("isActive", true),
                    uid = o.optString("uid", ""),
                    updatedAt = o.optLong("updatedAt", 0L),
                    isDeleted = o.optBoolean("isDeleted", false)
                )
            },
            categories = array(root, "categories").map { o ->
                CategoryEntity(
                    id = o.optLong("id", 0L),
                    name = o.optString("name", ""),
                    type = transactionType(o.optString("type", "EXPENSE")),
                    colorHex = o.optString("colorHex", "#0F766E"),
                    isDefault = o.optBoolean("isDefault", false),
                    iconName = o.optString("iconName", ""),
                    uid = o.optString("uid", ""),
                    updatedAt = o.optLong("updatedAt", 0L),
                    isDeleted = o.optBoolean("isDeleted", false)
                )
            },
            budgets = array(root, "budgets").map { o ->
                BudgetEntity(
                    id = o.optLong("id", 0L),
                    categoryName = o.optString("categoryName", ""),
                    amountCents = o.optLong("amountCents", 0L),
                    monthYearKey = o.optString("monthYearKey", ""),
                    alertThresholdPercent = o.optInt("alertThresholdPercent", 80),
                    isHardLimit = o.optBoolean("isHardLimit", false),
                    notes = o.optString("notes", "")
                )
            },
            registers = array(root, "registers").map { o ->
                CashRegisterEntity(
                    id = o.optLong("id", 0L),
                    name = o.optString("name", ""),
                    description = o.optString("description", ""),
                    initialBalanceCents = o.optLong("initialBalanceCents", 0L),
                    iconType = o.optString("iconType", "cash"),
                    isDefault = o.optBoolean("isDefault", false),
                    uid = o.optString("uid", ""),
                    updatedAt = o.optLong("updatedAt", 0L),
                    isDeleted = o.optBoolean("isDeleted", false)
                )
            },
            projects = array(root, "projects").map { o ->
                ProjectEntity(
                    id = o.optLong("id", 0L),
                    uid = o.optString("uid", ""),
                    name = o.optString("name", ""),
                    description = o.optString("description", ""),
                    targetCents = o.optLong("targetCents", 0L),
                    isClosed = o.optBoolean("isClosed", false),
                    createdAt = o.optLong("createdAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L),
                    isDeleted = o.optBoolean("isDeleted", false),
                    kind = o.optString("kind", "PROJECT"),
                    startAtMillis = o.takeIf { it.has("startAtMillis") && !it.isNull("startAtMillis") }?.optLong("startAtMillis"),
                    endAtMillis = o.takeIf { it.has("endAtMillis") && !it.isNull("endAtMillis") }?.optLong("endAtMillis"),
                    actionItemsJson = o.optString("actionItemsJson", "[]")
                )
            },
            transactions = array(root, "transactions").map { o ->
                TransactionEntity(
                    id = o.optLong("id", 0L),
                    title = o.optString("title", ""),
                    type = transactionType(o.optString("type", "EXPENSE")),
                    amountCents = o.optLong("amountCents", 0L),
                    category = o.optString("category", ""),
                    timestamp = o.optLong("timestamp", 0L),
                    note = o.optString("note", ""),
                    cashRegisterId = o.optLong("cashRegisterId", 0L),
                    cashRegisterName = o.optString("cashRegisterName", ""),
                    transferGroupId = o.optString("transferGroupId", ""),
                    receiptPhotoUri = o.optString("receiptPhotoUri", ""),
                    uid = o.optString("uid", ""),
                    updatedAt = o.optLong("updatedAt", 0L),
                    updatedBy = o.optString("updatedBy", ""),
                    isDeleted = o.optBoolean("isDeleted", false),
                    reversalOfUid = o.optString("reversalOfUid", ""),
                    isCancelled = o.optBoolean("isCancelled", false),
                    projectId = o.optLong("projectId", 0L)
                )
            }
        )
    }

    private fun array(root: JSONObject, key: String): List<JSONObject> {
        val arr = root.optJSONArray(key) ?: return emptyList()
        val out = ArrayList<JSONObject>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out.add(it) }
        }
        return out
    }

    private fun transactionType(raw: String): TransactionType =
        runCatching { TransactionType.valueOf(raw) }.getOrDefault(TransactionType.EXPENSE)
}
