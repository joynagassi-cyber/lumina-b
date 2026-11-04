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
 * Rôle d'un utilisateur de l'application.
 *
 * Les droits sont volontairement simples : le trésorier administre, l'assistant
 * saisit sans supprimer, le lecteur consulte (pasteur, commission de contrôle).
 */
enum class UserRole(
    val label: String,
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canManageUsers: Boolean
) {
    TRESORIER("Trésorier", canWrite = true, canDelete = true, canManageUsers = true),
    ASSISTANT("Assistant", canWrite = true, canDelete = false, canManageUsers = false),
    LECTURE("Lecture seule", canWrite = false, canDelete = false, canManageUsers = false)
}

/**
 * Personne autorisée à utiliser l'application sur cet appareil.
 *
 * Le code PIN n'est jamais stocké en clair : seuls un sel aléatoire et
 * l'empreinte SHA-256 (sel + PIN) sont conservés.
 */
@Entity(
    tableName = "app_users",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["displayName"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String = UUID.randomUUID().toString(),
    val displayName: String,
    val role: String = UserRole.TRESORIER.name,
    val pinHash: String = "",
    val pinSalt: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val roleValue: UserRole
        get() = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.LECTURE)

    val initials: String
        get() = displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }

    val hasPin: Boolean
        get() = pinHash.isNotBlank()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users WHERE isDeleted = 0 ORDER BY displayName COLLATE NOCASE ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM app_users WHERE isDeleted = 0 ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun getUsers(): List<UserEntity>

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM app_users WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UserEntity?

    @Query("SELECT COUNT(*) FROM app_users WHERE isDeleted = 0")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE app_users SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE app_users SET lastLoginAt = :timestamp WHERE id = :id")
    suspend fun touchLastLogin(id: Long, timestamp: Long)

    @Query("DELETE FROM app_users")
    suspend fun deleteAllUsers()
}
