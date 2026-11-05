package com.example.data

import android.content.Context
import java.util.UUID

/** Identifiant et libellé d'un dossier d'église, stockés hors des bases métiers. */
data class ChurchWorkspace(
    val id: String,
    val displayName: String,
    val databaseName: String
)

/**
 * Petit catalogue local des dossiers. Chaque dossier possède son propre fichier
 * Room : les tables financières, les membres, les utilisateurs et les paramètres
 * ne sont jamais partagés entre deux églises.
 */
class ChurchWorkspaceCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun ensureInitialWorkspace(): ChurchWorkspace {
        val existing = getWorkspaces()
        if (existing.isNotEmpty()) {
            val activeId = preferences.getString(KEY_ACTIVE_ID, null)
            return existing.firstOrNull { it.id == activeId }
                ?: existing.first().also { setActiveWorkspace(it.id) }
        }

        // La première ouverture conserve le fichier historique en place : aucune
        // copie/renommage risquée de SQLite/WAL n'est nécessaire à la migration.
        val databaseDirectory = appContext.getDatabasePath(LEGACY_DATABASE_NAME).parentFile
        val legacyFileExists = appContext.getDatabasePath(LEGACY_DATABASE_NAME).exists()
        val candidates = mutableListOf<ChurchWorkspace>()
        if (legacyFileExists) {
            candidates += ChurchWorkspace(DEFAULT_WORKSPACE_ID, "Dossier existant", LEGACY_DATABASE_NAME)
        }
        databaseDirectory?.listFiles()?.filter {
            it.isFile && it.name.startsWith(DATABASE_PREFIX) && it.name.endsWith(".db")
        }?.sortedBy { it.name }?.forEach { file ->
            val id = file.name.removePrefix(DATABASE_PREFIX).removeSuffix(".db")
            if (id.matches(Regex("[A-Za-z0-9_-]+")) && candidates.none { it.id == id }) {
                candidates += ChurchWorkspace(
                    id = id,
                    displayName = if (id == DEFAULT_WORKSPACE_ID) "Dossier existant" else "Dossier récupéré",
                    databaseName = file.name
                )
            }
        }
        if (candidates.isEmpty()) {
            candidates += ChurchWorkspace(DEFAULT_WORKSPACE_ID, "Nouvelle église", DEFAULT_DATABASE_NAME)
        }
        candidates.forEachIndexed { index, workspace ->
            persistWorkspace(workspace, makeActive = index == 0)
        }
        return candidates.first()
    }

    fun getWorkspaces(): List<ChurchWorkspace> {
        val ids = preferences.getStringSet(KEY_WORKSPACE_IDS, emptySet()).orEmpty().toList().sorted()
        return ids.mapNotNull { id ->
            val databaseName = preferences.getString(databaseKey(id), null) ?: return@mapNotNull null
            ChurchWorkspace(
                id = id,
                displayName = preferences.getString(nameKey(id), null)
                    ?.takeIf { it.isNotBlank() } ?: "Dossier d'église",
                databaseName = databaseName
            )
        }
    }

    fun getActiveWorkspace(): ChurchWorkspace? {
        val activeId = preferences.getString(KEY_ACTIVE_ID, null) ?: return null
        return getWorkspaces().firstOrNull { it.id == activeId }
    }

    @Synchronized
    fun createWorkspace(): ChurchWorkspace {
        val id = UUID.randomUUID().toString().replace("-", "")
        val workspace = ChurchWorkspace(
            id = id,
            displayName = "Nouvelle église",
            databaseName = "lumina_church_$id.db"
        )
        persistWorkspace(workspace, makeActive = true)
        return workspace
    }

    @Synchronized
    fun setActiveWorkspace(id: String) {
        require(getWorkspaces().any { it.id == id }) { "Dossier d'église introuvable" }
        check(preferences.edit().putString(KEY_ACTIVE_ID, id).commit()) {
            "Impossible d'enregistrer le dossier actif"
        }
    }

    @Synchronized
    fun updateDisplayName(id: String, displayName: String) {
        require(getWorkspaces().any { it.id == id }) { "Dossier d'église introuvable" }
        val cleanName = displayName.trim().ifBlank { "Mon église" }.take(MAX_DISPLAY_NAME_LENGTH)
        check(preferences.edit().putString(nameKey(id), cleanName).commit()) {
            "Impossible d'enregistrer le nom du dossier"
        }
    }

    private fun persistWorkspace(workspace: ChurchWorkspace, makeActive: Boolean) {
        val ids = preferences.getStringSet(KEY_WORKSPACE_IDS, emptySet()).orEmpty().toMutableSet()
        ids += workspace.id
        val editor = preferences.edit()
            .putStringSet(KEY_WORKSPACE_IDS, ids)
            .putString(nameKey(workspace.id), workspace.displayName)
            .putString(databaseKey(workspace.id), workspace.databaseName)
        if (makeActive) editor.putString(KEY_ACTIVE_ID, workspace.id)
        check(editor.commit()) { "Impossible d'enregistrer le dossier d'église" }
    }

    private fun nameKey(id: String) = "workspace_name_$id"
    private fun databaseKey(id: String) = "workspace_database_$id"

    private companion object {
        const val PREFERENCES_NAME = "lumina_church_workspaces"
        const val KEY_WORKSPACE_IDS = "workspace_ids"
        const val KEY_ACTIVE_ID = "active_workspace_id"
        const val DEFAULT_WORKSPACE_ID = "default"
        const val LEGACY_DATABASE_NAME = "lumina_financial.db"
        const val DATABASE_PREFIX = "lumina_church_"
        const val DEFAULT_DATABASE_NAME = "lumina_church_default.db"
        const val MAX_DISPLAY_NAME_LENGTH = 80
    }
}
