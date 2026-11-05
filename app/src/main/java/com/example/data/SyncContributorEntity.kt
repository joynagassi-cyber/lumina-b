package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Identité de l'expéditeur d'un échange, sans compte local ni code PIN. */
@Entity(
    tableName = "sync_contributors",
    indices = [Index(value = ["displayName"])]
)
data class SyncContributorEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val role: String,
    val sourceDeviceId: String = "",
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
) {
    val roleValue: UserRole
        get() = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.LECTURE)
}

@Dao
interface SyncContributorDao {
    @Query("SELECT * FROM sync_contributors ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SyncContributorEntity>>

    @Query("SELECT * FROM sync_contributors WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): SyncContributorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contributor: SyncContributorEntity)
}
