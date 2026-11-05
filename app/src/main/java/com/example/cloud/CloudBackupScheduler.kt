package com.example.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.LuminaDatabase
import com.example.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Queue retryable cloud uploads for the next network connection and periodic safety checks. */
object CloudBackupScheduler {
    fun schedule(context: Context, workspaceId: String, databaseName: String) {
        val app = context.applicationContext
        val link = CloudBackupLocalStore.link(app, workspaceId) ?: return
        if (!link.enabled) return
        val safeId = workspaceId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val input = Data.Builder()
            .putString(CloudBackupWorker.KEY_WORKSPACE_ID, workspaceId)
            .putString(CloudBackupWorker.KEY_DATABASE_NAME, databaseName)
            .build()

        val once = OneTimeWorkRequestBuilder<CloudBackupWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(
            "lumina-cloud-backup-once-$safeId",
            ExistingWorkPolicy.KEEP,
            once
        )

        val workManager = WorkManager.getInstance(app)
        // Remove the legacy 15-minute schedule once; the daily cadence produces attempts
        // on distinct weekdays while content fingerprints suppress unchanged uploads.
        workManager.cancelUniqueWork("lumina-cloud-backup-periodic-$safeId")
        val periodic = PeriodicWorkRequestBuilder<CloudBackupWorker>(24, TimeUnit.HOURS)
            .setInputData(input)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "lumina-cloud-backup-daily-$safeId",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }

    fun cancel(context: Context, workspaceId: String) {
        val safeId = workspaceId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("lumina-cloud-backup-once-$safeId")
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("lumina-cloud-backup-periodic-$safeId")
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("lumina-cloud-backup-daily-$safeId")
    }
}

class CloudBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val workspaceId = inputData.getString(KEY_WORKSPACE_ID)
            ?: return@withContext Result.failure()
        val databaseName = inputData.getString(KEY_DATABASE_NAME)
            ?: return@withContext Result.failure()
        val link = CloudBackupLocalStore.link(applicationContext, workspaceId)
        if (link == null || !link.enabled) return@withContext Result.success()
        try {
            val database = LuminaDatabase.getDatabaseNamed(applicationContext, databaseName)
            val repository = TransactionRepository(
                database.transactionDao(),
                database.categoryDao(),
                database.budgetDao(),
                database.cashRegisterDao(),
                database.churchProfileDao(),
                database.memberDao(),
                database.userDao(),
                database.projectDao(),
                workspaceId = workspaceId,
                database = database,
                syncContributorDao = database.syncContributorDao()
            )
            CloudBackupCoordinator.publishIfEnabled(applicationContext, repository)
            Result.success()
        } catch (error: Exception) {
            CloudBackupLocalStore.setStatus(
                applicationContext,
                workspaceId,
                error.message ?: "La sauvegarde cloud sera réessayée lorsque le réseau sera disponible."
            )
            if (error is IOException && !error.message.orEmpty().contains("Conflit de sauvegarde", ignoreCase = true) &&
                !error.message.orEmpty().contains("Restaurez d'abord", ignoreCase = true)
            ) Result.retry() else Result.success()
        }
    }

    companion object {
        const val KEY_WORKSPACE_ID = "cloud_workspace_id"
        const val KEY_DATABASE_NAME = "cloud_database_name"
    }
}
