package com.example.sync

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Journal privé append-only des conflits détectés et arbitrages. */
object SyncConflictJournal {
    private const val JOURNAL_NAME = "journal-fusions.json"
    private const val MAX_ENTRIES = 2_000

    @Synchronized
    fun recordDetected(
        context: Context,
        workspaceId: String,
        encryptedBundle: ByteArray,
        conflicts: List<SyncConflictItem>
    ): List<SyncConflictItem> {
        if (conflicts.isEmpty()) return emptyList()
        val directory = directory(context, workspaceId).apply { mkdirs() }
        val bundleName = "recu_${UUID.randomUUID()}.lumina"
        val bundleFile = File(directory, bundleName)
        writeAtomically(bundleFile, encryptedBundle)
        return try {
            val current = readAll(directory)
            val firstTimestamp = maxOf(System.currentTimeMillis(), (current.maxOfOrNull { it.detectedAt } ?: 0L) + 1L)
            val persisted = conflicts.mapIndexed { index, item ->
                item.copy(pendingBundleFile = bundleName, detectedAt = firstTimestamp + index)
            }
            val combined = bounded(current + persisted)
            writeJournal(directory, combined)
            persisted
        } catch (error: Throwable) {
            bundleFile.delete()
            throw error
        }
    }

    @Synchronized
    fun pending(context: Context, workspaceId: String): List<SyncConflictItem> =
        readAll(directory(context, workspaceId)).filter { it.resolution.isBlank() }

    @Synchronized
    fun history(context: Context, workspaceId: String): List<SyncConflictItem> =
        readAll(directory(context, workspaceId)).filter { it.resolution.isNotBlank() }.takeLast(20).reversed()

    @Synchronized
    fun readPendingBundle(context: Context, workspaceId: String, conflict: SyncConflictItem): ByteArray {
        require(conflict.pendingBundleFile.matches(Regex("recu_[A-Za-z0-9-]{36}\\.lumina"))) {
            "Référence de fichier de conflit invalide"
        }
        val file = File(directory(context, workspaceId), conflict.pendingBundleFile)
        require(file.isFile && file.length() <= 150L * 1024 * 1024) {
            "Le fichier reçu pour ce conflit n'est plus disponible. Réimportez l'échange d'origine."
        }
        return file.readBytes()
    }

    @Synchronized
    fun markResolved(
        context: Context,
        workspaceId: String,
        conflict: SyncConflictItem,
        resolution: String
    ) {
        require(resolution == "KEEP_LOCAL" || resolution == "USE_IMPORTED")
        val directory = directory(context, workspaceId)
        val all = readAll(directory).toMutableList()
        val index = all.indexOfFirst {
            it.key == conflict.key && it.detectedAt == conflict.detectedAt && it.resolution.isBlank()
        }
        require(index >= 0) { "Ce conflit a déjà été résolu ou n'existe plus." }
        all[index] = all[index].copy(resolution = resolution, resolvedAt = System.currentTimeMillis())
        val trimmed = bounded(all)
        writeJournal(directory, trimmed)
        val referenced = trimmed.filter { it.resolution.isBlank() }.map { it.pendingBundleFile }.toSet()
        directory.listFiles()?.filter { it.name.startsWith("recu_") && it.name.endsWith(".lumina") }
            ?.filter { it.name !in referenced }
            ?.forEach { it.delete() }
    }

    private fun directory(context: Context, workspaceId: String): File {
        require(workspaceId == "default" || workspaceId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) {
            "Identifiant de dossier invalide"
        }
        return File(File(context.filesDir, "echanges"), workspaceId)
    }

    private fun bounded(items: List<SyncConflictItem>): List<SyncConflictItem> {
        val pending = items.filter { it.resolution.isBlank() }
        val resolved = items.filter { it.resolution.isNotBlank() }
            .takeLast((MAX_ENTRIES - pending.size).coerceAtLeast(0))
        return resolved + pending
    }

    private fun readAll(directory: File): List<SyncConflictItem> {
        val file = File(directory, JOURNAL_NAME)
        if (!file.exists()) return emptyList()
        val array = try {
            JSONArray(file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            throw IllegalStateException("Le journal de synchronisation est endommagé; ne l'effacez pas avant d'en faire une copie.")
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    SyncConflictItem(
                        key = item.optString("key"),
                        kind = item.optString("kind"),
                        uid = item.optString("uid"),
                        label = item.optString("label"),
                        localValue = item.optString("localValue"),
                        incomingValue = item.optString("incomingValue"),
                        localUpdatedAt = item.optLong("localUpdatedAt"),
                        incomingUpdatedAt = item.optLong("incomingUpdatedAt"),
                        pendingBundleFile = item.optString("pendingBundleFile"),
                        detectedAt = item.optLong("detectedAt"),
                        resolution = item.optString("resolution"),
                        resolvedAt = item.optLong("resolvedAt")
                    )
                )
            }
        }
    }

    private fun writeJournal(directory: File, items: List<SyncConflictItem>) {
        directory.mkdirs()
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("key", item.key)
                put("kind", item.kind)
                put("uid", item.uid)
                put("label", item.label)
                put("localValue", item.localValue)
                put("incomingValue", item.incomingValue)
                put("localUpdatedAt", item.localUpdatedAt)
                put("incomingUpdatedAt", item.incomingUpdatedAt)
                put("pendingBundleFile", item.pendingBundleFile)
                put("detectedAt", item.detectedAt)
                put("resolution", item.resolution)
                put("resolvedAt", item.resolvedAt)
            })
        }
        writeAtomically(File(directory, JOURNAL_NAME), array.toString().toByteArray(Charsets.UTF_8))
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, ".${target.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeBytes(bytes)
            check(temp.renameTo(target)) { "Impossible d'enregistrer le journal de synchronisation" }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
