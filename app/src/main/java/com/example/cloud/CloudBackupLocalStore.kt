package com.example.cloud

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Serialize cloud upload/restore against local wipe operations for each church database. */
object CloudBackupLocks {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withWorkspaceLock(workspaceId: String, action: suspend () -> T): T {
        val mutex = locks.getOrPut(workspaceId) { Mutex() }
        mutex.lock()
        try {
            return action()
        } finally {
            mutex.unlock()
        }
    }
}

/** Per-local-workspace cloud link and optimistic concurrency checkpoint. */
object CloudBackupLocalStore {
    data class BackupProgress(val isRunning: Boolean, val percent: Int, val message: String)

    private val progressState = MutableStateFlow<Map<String, BackupProgress>>(emptyMap())
    private val progressScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun progressFlow(workspaceId: String): Flow<BackupProgress?> =
        progressState.map { it[workspaceId] }

    fun setProgress(workspaceId: String, percent: Int, message: String) {
        progressState.value = progressState.value + (
            workspaceId to BackupProgress(true, percent.coerceIn(0, 100), message.take(160))
        )
    }

    fun finishProgress(workspaceId: String, message: String) {
        val completed = BackupProgress(false, 100, message.take(160))
        progressState.value = progressState.value + (workspaceId to completed)
        progressScope.launch {
            delay(3_500)
            if (progressState.value[workspaceId] == completed) {
                progressState.value = progressState.value - workspaceId
            }
        }
    }

    fun clearProgress(workspaceId: String) {
        progressState.value = progressState.value - workspaceId
    }

    data class Link(val churchId: String, val generation: Long, val enabled: Boolean)

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences("lumina_cloud_backup_links", Context.MODE_PRIVATE)

    fun link(context: Context, workspaceId: String): Link? {
        val prefs = preferences(context)
        val key = prefix(workspaceId)
        val id = prefs.getString("${key}church_id", null)?.takeIf(String::isNotBlank) ?: return null
        return Link(
            churchId = id,
            generation = prefs.getLong("${key}generation", 0L),
            enabled = prefs.getBoolean("${key}enabled", false)
        )
    }

    fun setLink(context: Context, workspaceId: String, churchId: String, generation: Long, enabled: Boolean = true) {
        require(churchId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Identifiant cloud invalide." }
        val key = prefix(workspaceId)
        check(preferences(context).edit()
            .putString("${key}church_id", churchId)
            .putLong("${key}generation", generation)
            .putBoolean("${key}enabled", enabled)
            .commit()) { "Impossible d'enregistrer la liaison de sauvegarde cloud." }
    }

    fun setGeneration(context: Context, workspaceId: String, generation: Long) {
        val key = prefix(workspaceId)
        check(preferences(context).edit().putLong("${key}generation", generation).commit()) {
            "Impossible d'enregistrer la version de sauvegarde cloud."
        }
    }

    fun status(context: Context, workspaceId: String): String =
        preferences(context).getString("${prefix(workspaceId)}status", "").orEmpty()

    fun setStatus(context: Context, workspaceId: String, value: String) {
        preferences(context).edit().putString("${prefix(workspaceId)}status", value.take(300)).apply()
    }

    fun lastFingerprint(context: Context, workspaceId: String): String? =
        preferences(context).getString("${prefix(workspaceId)}fingerprint", null)

    fun setLastFingerprint(context: Context, workspaceId: String, fingerprint: String) {
        check(preferences(context).edit().putString("${prefix(workspaceId)}fingerprint", fingerprint).commit()) {
            "Impossible d'enregistrer l'empreinte locale de la sauvegarde cloud."
        }
    }

    fun requireRestoreBeforeUpload(context: Context, workspaceId: String) {
        val current = link(context, workspaceId) ?: return
        setLink(context, workspaceId, current.churchId, -1L, enabled = false)
        setStatus(context, workspaceId, "Les données locales ont été effacées ou modifiées. La publication cloud est bloquée : restaurez d'abord la version partagée.")
    }

    fun setEnabled(context: Context, workspaceId: String, enabled: Boolean) {
        val key = prefix(workspaceId)
        check(preferences(context).edit().putBoolean("${key}enabled", enabled).commit()) {
            "Impossible de modifier la sauvegarde cloud."
        }
    }

    fun creationKey(context: Context, workspaceId: String): String {
        val prefs = preferences(context)
        val key = "${prefix(workspaceId)}creation_key"
        prefs.getString(key, null)?.let { return it }
        val value = UUID.randomUUID().toString()
        check(prefs.edit().putString(key, value).commit()) { "Impossible d'initialiser le dossier cloud." }
        return value
    }

    fun clearWorkspaceLink(context: Context, workspaceId: String) {
        val prefs = preferences(context)
        val key = prefix(workspaceId)
        check(prefs.edit()
            .remove("${key}church_id")
            .remove("${key}generation")
            .remove("${key}enabled")
            .remove("${key}fingerprint")
            .remove("${key}status")
            .commit()) { "Impossible de supprimer le lien cloud local." }
    }

    private fun prefix(workspaceId: String): String {
        require(workspaceId == "default" || workspaceId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) {
            "Identifiant de dossier invalide."
        }
        return "workspace_${workspaceId}_"
    }
}
