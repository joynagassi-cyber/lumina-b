package com.example.data

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

const val PROJECT_KIND_PROJECT = "PROJECT"

/**
 * Projet de l'église (construction, mission, équipement…).
 *
 * Les dons affectés sont les écritures rattachées à un projet : la somme des
 * entrées donne le montant collecté, la somme des sorties le montant engagé, et
 * la différence le solde disponible du projet.
 */
@Entity(
    tableName = "projects",
    indices = [Index(value = ["uid"], unique = true)]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    /** Collection goal for ordinary projects; event budget for EVENT records; 0 when unset. */
    val targetCents: Long = 0L,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    /** Nature fonctionnelle : projet libre, campagne, événement ou collecte dédiée. */
    @ColumnInfo(defaultValue = "'PROJECT'")
    val kind: String = PROJECT_KIND_PROJECT,
    /** Optional local-day timestamps; populated for scheduled projects such as events. */
    val startAtMillis: Long? = null,
    val endAtMillis: Long? = null,
    /** Compact, forward-compatible checklist serialized as JSON for backup/sync. */
    @ColumnInfo(defaultValue = "'[]'")
    val actionItemsJson: String = "[]"
)

data class ProjectActionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false,
    /** Nested checklist items are kept in the same JSON field for backup/sync compatibility. */
    val subTasks: List<ProjectActionItem> = emptyList()
)

fun decodeProjectActionItems(json: String): List<ProjectActionItem> = runCatching {
    fun parse(items: org.json.JSONArray): List<ProjectActionItem> = (0 until items.length()).mapNotNull { index ->
        val item = items.optJSONObject(index) ?: return@mapNotNull null
        val title = item.optString("title", "").trim()
        if (title.isBlank()) null else ProjectActionItem(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = title,
            isDone = item.optBoolean("isDone", false),
            subTasks = parse(item.optJSONArray("subTasks") ?: org.json.JSONArray())
        )
    }
    parse(org.json.JSONArray(json))
}.getOrDefault(emptyList())

fun encodeProjectActionItems(items: List<ProjectActionItem>): String {
    fun serialize(entries: List<ProjectActionItem>): org.json.JSONArray = org.json.JSONArray().apply {
        entries.filter { it.title.isNotBlank() }.forEach { item ->
            put(org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title.trim())
                put("isDone", item.isDone)
                put("subTasks", serialize(item.subTasks))
            })
        }
    }
    return serialize(items).toString()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY isClosed ASC, createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY isClosed ASC, createdAt DESC")
    suspend fun getAllOnce(): List<ProjectEntity>

    /** Sauvegardes et échanges incluent aussi les suppressions logiques. */
    @Query("SELECT * FROM projects ORDER BY isClosed ASC, createdAt DESC")
    suspend fun getAllForBackup(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    /** Effacement réel, réservé à la restauration d'une sauvegarde. */
    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}
