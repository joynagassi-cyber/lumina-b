package com.example.cloud

import android.content.Context
import com.example.data.TransactionRepository
import com.example.export.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Coordinates immutable archive uploads and explicitly confirmed restore operations. */
object CloudBackupCoordinator {
    private const val FORMAT_VERSION = 1
    private const val MAX_CLOUD_BACKUP_BYTES = 146_800_640L

    /** Creates the shared church space on first use; existing server data is never overwritten implicitly. */
    suspend fun enableForCurrentWorkspace(context: Context, repository: TransactionRepository): String =
        withContext(Dispatchers.IO) {
            CloudBackupLocks.withWorkspaceLock(repository.workspaceId) {
            val app = context.applicationContext
            val profile = repository.getProfile()
                ?.takeIf { it.onboardingCompleted && it.churchName.isNotBlank() }
                ?: throw IllegalStateException("Configurez d'abord le profil de l'église.")
            require(profile.churchUid.matches(Regex("[A-Za-z0-9_-]{8,128}"))) {
                "L'identité locale de l'église n'est pas valide pour la sauvegarde cloud."
            }
            val client = SupabaseCloudClient(app)
            check(client.isConfigured()) { "Le service cloud n'est pas configuré." }
            client.ensureAuthenticated()
            val existing = CloudBackupLocalStore.link(app, repository.workspaceId)
            if (existing != null) {
                if (existing.generation < 0L) {
                    throw IllegalStateException("Cet appareil doit d'abord restaurer la sauvegarde partagée avant de pouvoir publier.")
                }
                CloudBackupLocalStore.setEnabled(app, repository.workspaceId, true)
                CloudBackupLocalStore.setStatus(app, repository.workspaceId, "Sauvegarde cloud activée. La prochaine copie sera envoyée en arrière-plan.")
                return@withWorkspaceLock "Sauvegarde cloud activée. La première mise à jour est programmée en arrière-plan."
            }

            val churchId = client.ensureWorkspace(
                profileUid = profile.churchUid,
                churchName = profile.displayName,
                creationKey = CloudBackupLocalStore.creationKey(app, repository.workspaceId)
            )
            val head = client.currentHead(churchId)
            if (head != null) {
                CloudBackupLocalStore.setLink(app, repository.workspaceId, churchId, -1L, enabled = true)
                CloudBackupLocalStore.setStatus(app, repository.workspaceId, "Une version partagée existe. Restaurez-la avant de publier depuis cet appareil.")
                throw IllegalStateException("Une sauvegarde de cette église existe déjà. Restaurez-la avant de publier depuis cet appareil.")
            }
            CloudBackupLocalStore.setLink(app, repository.workspaceId, churchId, 0L, enabled = true)
            CloudBackupLocalStore.setStatus(app, repository.workspaceId, "Sauvegarde cloud activée. La première copie est programmée en arrière-plan.")
            "Sauvegarde cloud activée pour ${profile.displayName}. La première copie démarre en arrière-plan."
            }
        }

    /** Upload only when enabled. Conflicts are surfaced and never auto-merged or overwritten. */
    suspend fun publishIfEnabled(context: Context, repository: TransactionRepository): Boolean =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val workspaceId = repository.workspaceId
            try {
                val didPublish = CloudBackupLocks.withWorkspaceLock(workspaceId) {
                    val link = CloudBackupLocalStore.link(app, workspaceId)
                        ?: return@withWorkspaceLock false
                    if (!link.enabled) return@withWorkspaceLock false
                    val fingerprint = BackupService.cloudContentFingerprint(app, repository)
                    if (CloudBackupLocalStore.lastFingerprint(app, workspaceId) == fingerprint) {
                        CloudBackupLocalStore.setStatus(app, workspaceId, "Sauvegarde cloud à jour.")
                        return@withWorkspaceLock false
                    }
                    if (!CloudBackupLocalStore.claimSyncDay(app, workspaceId)) {
                        CloudBackupLocalStore.setStatus(
                            app,
                            workspaceId,
                            "Limite de synchronisation atteinte : les transferts sont répartis sur trois jours maximum par période de sept jours."
                        )
                        return@withWorkspaceLock false
                    }
                    publishCurrent(app, repository, SupabaseCloudClient(app), link)
                    true
                }
                if (didPublish) {
                    CloudBackupLocalStore.finishProgress(workspaceId, "Sauvegarde cloud terminée")
                    com.example.notifications.LuminaNotifier.notifySyncProgress(
                        app,
                        workspaceId,
                        100,
                        "La copie est publiée et vérifiée.",
                        finished = true
                    )
                }
                didPublish
            } catch (error: Exception) {
                CloudBackupLocalStore.clearProgress(workspaceId)
                com.example.notifications.LuminaNotifier.cancelSyncProgress(app, workspaceId)
                throw error
            }
        }

    /** Read the latest manifest and archive, checking its checksum and Lumina format without restoring it. */
    suspend fun verifyLatest(context: Context, repository: TransactionRepository): String =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val link = CloudBackupLocalStore.link(app, repository.workspaceId)
                ?: throw IllegalStateException("Activez d'abord la sauvegarde cloud sur cet appareil.")
            val client = SupabaseCloudClient(app)
            client.ensureAuthenticated()
            val head = client.currentHead(link.churchId)
                ?: throw IllegalStateException("Aucune sauvegarde n'existe encore dans le cloud. Activez-la et attendez la première copie.")
            require(head.byteSize in 1..MAX_CLOUD_BACKUP_BYTES) { "La taille de la sauvegarde cloud est invalide." }
            val bytes = client.downloadObject(head.objectPath, MAX_CLOUD_BACKUP_BYTES)
            require(sha256(bytes) == head.sha256) {
                "La vérification a échoué : l'empreinte de la copie téléchargée est incorrecte. Aucune restauration n'a été effectuée."
            }
            val archived = BackupService.inspectCloudArchive(bytes)
            val localChurchUid = repository.getProfile()?.churchUid
            if (!localChurchUid.isNullOrBlank() && !archived.profile?.churchUid.isNullOrBlank()) {
                require(archived.profile?.churchUid == localChurchUid) {
                    "La copie vérifiée appartient à une autre identité d'église. Aucune donnée n'a été modifiée."
                }
            }
            val message = "Vérification réussie : copie cloud lisible, empreinte valide, version ${head.generation} (${archived.transactionCount} écritures, ${archived.memberCount} membres)."
            CloudBackupLocalStore.setStatus(app, repository.workspaceId, message)
            message
        }

    suspend fun createInvite(context: Context, repository: TransactionRepository, role: String = "editor"): String =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val link = CloudBackupLocalStore.link(app, repository.workspaceId)
                ?: throw IllegalStateException("Activez d'abord la sauvegarde cloud de l'église.")
            SupabaseCloudClient(app).createInvite(link.churchId, role)
        }

    /** Link an invitation recipient. A current backup requires restore before uploads are allowed. */
    suspend fun acceptInvite(context: Context, repository: TransactionRepository, token: String): String =
        withContext(Dispatchers.IO) {
            CloudBackupLocks.withWorkspaceLock(repository.workspaceId) {
            val app = context.applicationContext
            require(token.matches(Regex("[0-9a-fA-F-]{36}"))) { "Code d'invitation cloud invalide." }
            val client = SupabaseCloudClient(app)
            val churchId = client.acceptInvite(token)
            val head = client.currentHead(churchId)
            CloudBackupLocalStore.setLink(
                app,
                repository.workspaceId,
                churchId,
                head?.generation?.let { -1L } ?: 0L,
                enabled = true
            )
            CloudBackupLocalStore.setStatus(
                app,
                repository.workspaceId,
                if (head == null) "Appareil associé. La première sauvegarde sera créée après l'activation." else "Appareil associé. Restaurez la version partagée avant de publier."
            )
            churchId
            }
        }

    /** Explicit restore: leave a local safety archive, verify SHA-256, then restore atomically. */
    suspend fun restoreLatest(context: Context, repository: TransactionRepository) = withContext(Dispatchers.IO) {
        CloudBackupLocks.withWorkspaceLock(repository.workspaceId) {
        val app = context.applicationContext
        val link = CloudBackupLocalStore.link(app, repository.workspaceId)
            ?: throw IllegalStateException("Cet appareil n'est pas associé à un espace cloud.")
        val client = SupabaseCloudClient(app)
        val head = client.currentHead(link.churchId)
            ?: throw IllegalStateException("Aucune sauvegarde n'est encore disponible pour cette église.")
        require(head.byteSize in 1..MAX_CLOUD_BACKUP_BYTES) { "Taille de sauvegarde cloud invalide." }
        BackupService.createCloudRestoreSafetyCopy(app, repository)
        val bytes = client.downloadObject(head.objectPath, MAX_CLOUD_BACKUP_BYTES)
        require(sha256(bytes) == head.sha256) { "La sauvegarde téléchargée ne correspond pas à son empreinte. Aucune donnée n'a été restaurée." }
        val restored = BackupService.restoreCloudArchive(app, repository, bytes)
        CloudBackupLocalStore.setLink(app, repository.workspaceId, head.churchId, head.generation, enabled = true)
        CloudBackupLocalStore.setLastFingerprint(
            app,
            repository.workspaceId,
            BackupService.cloudContentFingerprint(app, repository)
        )
        CloudBackupLocalStore.setStatus(app, repository.workspaceId, "Sauvegarde cloud restaurée et vérifiée.")
        restored
        }
    }

    private suspend fun publishCurrent(
        context: Context,
        repository: TransactionRepository,
        client: SupabaseCloudClient,
        link: CloudBackupLocalStore.Link
    ) {
        val currentLink = CloudBackupLocalStore.link(context, repository.workspaceId)
        if (currentLink == null || !currentLink.enabled || currentLink.generation != link.generation) {
            throw IllegalStateException("La liaison cloud a changé; le téléversement a été annulé pour protéger les données.")
        }
        if (link.generation < 0L) {
            throw IllegalStateException("Restaurez d'abord la sauvegarde partagée de l'église avant de publier.")
        }
        val remoteHead = client.currentHead(link.churchId)
        val remoteGeneration = remoteHead?.generation ?: 0L
        if (remoteGeneration != link.generation) {
            throw IOException("Conflit de sauvegarde : une autre version est déjà publiée. Restaurez la version partagée avant de réessayer.")
        }
        val fingerprint = BackupService.cloudContentFingerprint(context, repository)
        if (CloudBackupLocalStore.lastFingerprint(context, repository.workspaceId) == fingerprint) {
            CloudBackupLocalStore.setStatus(context, repository.workspaceId, "Sauvegarde cloud à jour.")
            return
        }
        CloudBackupLocalStore.beginProgress(repository.workspaceId, "Préparation de la sauvegarde…")
        reportProgress(context, repository.workspaceId, 8, "Préparation des données et justificatifs…")
        val bytes = BackupService.createCloudArchive(context, repository)
        require(bytes.size.toLong() in 1..MAX_CLOUD_BACKUP_BYTES) {
            "La sauvegarde dépasse la limite de stockage cloud."
        }
        val checksum = sha256(bytes)
        val objectPath = "churches/${link.churchId}/${UUID.randomUUID()}.lumina"
        reportProgress(context, repository.workspaceId, 12, "Envoi de la sauvegarde…")
        var lastReportedPercent = 12
        var lastReportedAt = android.os.SystemClock.elapsedRealtime()
        client.uploadObject(objectPath, bytes) { sent, total ->
            val percent = if (total <= 0L) 85 else 12 + ((sent * 73L / total).toInt())
            val now = android.os.SystemClock.elapsedRealtime()
            if (percent >= lastReportedPercent + 1 || sent >= total || now - lastReportedAt >= 120L) {
                reportProgress(context, repository.workspaceId, percent, "Envoi des données vers l'église…")
                lastReportedPercent = percent
                lastReportedAt = now
            }
        }
        reportProgress(context, repository.workspaceId, 92, "Validation de la copie dans le cloud…")
        val latestLocalLink = CloudBackupLocalStore.link(context, repository.workspaceId)
        if (latestLocalLink == null || !latestLocalLink.enabled || latestLocalLink.generation != link.generation) {
            throw IllegalStateException("La liaison cloud a changé; la publication a été annulée pour protéger les données.")
        }
        val published = client.publish(
            churchId = link.churchId,
            expectedGeneration = link.generation,
            objectPath = objectPath,
            formatVersion = FORMAT_VERSION,
            byteSize = bytes.size.toLong(),
            sha256 = checksum
        )
        CloudBackupLocalStore.setLink(context, repository.workspaceId, link.churchId, published.generation, enabled = true)
        CloudBackupLocalStore.setLastFingerprint(context, repository.workspaceId, fingerprint)
        CloudBackupLocalStore.setStatus(context, repository.workspaceId, "Nouvelle sauvegarde cloud publiée (version ${published.generation}).")
    }

    private fun reportProgress(context: Context, workspaceId: String, percent: Int, message: String) {
        CloudBackupLocalStore.setProgress(workspaceId, percent, message)
        com.example.notifications.LuminaNotifier.notifySyncProgress(context, workspaceId, percent, message)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
