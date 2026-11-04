package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
    /** Objectif de collecte, 0 si aucun objectif n'est fixé. */
    val targetCents: Long = 0L,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY isClosed ASC, createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY isClosed ASC, createdAt DESC")
    suspend fun getAllOnce(): List<ProjectEntity>

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
