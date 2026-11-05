package com.example.export

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Sauvegardes locales et fichiers Lumina à emporter.
 *
 * Les exports portables sont des ZIP autonomes : profil (logo/couleurs),
 * historique, budgets, caisses, projets, membres, catégories et justificatifs.
 * Les PIN sont des accès propres à chaque appareil et ne sont jamais exportés
 * dans un fichier destiné au partage.
 */
object BackupService {

    private const val EXPORT_DIR = "sauvegardes"
    private const val AUTO_DIR = "copies-automatiques"
    private const val AUTO_KEEP = 3
    private const val AUTO_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val AUTO_PREFIX = "copie-auto-"
    private const val MAX_INPUT_BYTES = 140 * 1024 * 1024

    data class ExportResult(
        val file: File,
        val encrypted: Boolean,
        val snapshot: BackupSnapshot,
        val missingReceiptCount: Int = 0
    )

    private data class BackupPayload(
        val snapshot: BackupSnapshot,
        val receiptFilesByTransactionUid: Map<String, ByteArray> = emptyMap(),
        val isArchive: Boolean = false
    )

    // ------------------------------------------------------------------
    // Copie automatique privée de l'appareil (les comptes locaux y restent).
    // ------------------------------------------------------------------

    suspend fun autoSnapshot(context: Context, repository: TransactionRepository): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = workspaceDirectory(context.filesDir, AUTO_DIR, repository.workspaceId).apply { mkdirs() }
                val existing = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                val newest = existing.lastOrNull()
                val fresh = newest != null &&
                    System.currentTimeMillis() - newest.lastModified() < AUTO_INTERVAL_MS
                if (fresh) return@withContext null

                val snapshot = repository.snapshotAll()
                if (snapshot.transactions.isEmpty() && snapshot.members.isEmpty() &&
                    snapshot.profile?.onboardingCompleted != true
                ) return@withContext null

                val receipts = collectReceiptFiles(context, snapshot, repository.workspaceId)
                val archive = LuminaArchiveCodec.encode(snapshot, receipts)
                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.FRANCE).format(Date())
                val file = File(dir, "$AUTO_PREFIX$stamp.zip")
                file.writeBytes(archive)

                val all = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                all.dropLast(AUTO_KEEP.coerceAtLeast(1))
                    .forEach { if (it.name != file.name) it.delete() }
                file
            }.getOrNull()
        }

    fun latestAutoFile(context: Context, workspaceId: String = "default"): File? {
        val dir = workspaceDirectory(context.filesDir, AUTO_DIR, workspaceId)
        return dir.listFiles()?.sortedBy { it.name }?.lastOrNull()
    }

    suspend fun restoreLatestAuto(
        context: Context,
        repository: TransactionRepository
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val file = latestAutoFile(context, repository.workspaceId)
            ?: throw IllegalArgumentException("Aucune copie automatique disponible pour ce dossier.")
        val payload = decodePayload(readLimited(file), password = "")
        restorePayload(context, repository, payload)
    }

    // ------------------------------------------------------------------
    // Fichier Lumina portable
    // ------------------------------------------------------------------

    suspend fun create(
        context: Context,
        repository: TransactionRepository,
        password: String
    ): ExportResult = withContext(Dispatchers.IO) {
        val localSnapshot = repository.snapshotAll()
        // Les rôles et PIN sont locaux à l'appareil ; l'équipe recrée ses accès
        // personnels sur le nouveau téléphone après avoir importé le dossier.
        val portableSnapshot = localSnapshot.copy(users = emptyList())
        val receipts = collectReceiptFiles(context, portableSnapshot, repository.workspaceId)
        val archive = LuminaArchiveCodec.encode(portableSnapshot, receipts)
        val encrypted = password.isNotBlank()
        val content = if (encrypted) {
            val base64Zip = Base64.encodeToString(archive, Base64.NO_WRAP)
            BackupCrypto.encrypt(base64Zip, password).toByteArray(Charsets.UTF_8)
        } else archive

        val dir = workspaceDirectory(context.cacheDir, EXPORT_DIR, repository.workspaceId).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.FRANCE).format(Date())
        val safeName = (portableSnapshot.profile?.displayName ?: portableSnapshot.churchName)
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .ifBlank { "Eglise" }
        val file = File(
            dir,
            "Lumina_${safeName}_$stamp" + if (encrypted) ".lumina" else ".lumina.zip"
        )
        file.writeBytes(content)
        ExportResult(
            file = file,
            encrypted = encrypted,
            snapshot = portableSnapshot,
            missingReceiptCount = portableSnapshot.transactions.count {
                it.receiptPhotoUri.isNotBlank() && it.uid !in receipts
            }
        )
    }

    suspend fun restore(
        context: Context,
        repository: TransactionRepository,
        uri: Uri,
        password: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val payload = decodePayload(readLimited(context, uri), password)
        restorePayload(context, repository, payload)
    }

    /**
     * L'import d'accueil ne touche qu'un dossier vierge. Le profil de l'église
     * vient du fichier ; les comptes utilisateur restent vides pour que le
     * nouveau membre définisse son nom et son PIN sur cet appareil.
     */
    suspend fun importForNewMember(
        context: Context,
        repository: TransactionRepository,
        uri: Uri,
        password: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val payload = decodePayload(readLimited(context, uri), password)
        require(payload.isArchive) {
            "Pour inviter un nouveau membre, choisissez le ZIP Lumina complet créé par la version récente de l'application."
        }
        val profile = payload.snapshot.profile
            ?: throw IllegalArgumentException("Ce fichier Lumina ne contient pas l'identité de l'église.")
        require(profile.onboardingCompleted && profile.churchName.isNotBlank()) {
            "Ce fichier ne contient pas encore un dossier d'église configuré."
        }
        val current = repository.snapshotAll()
        val pristine = current.transactions.isEmpty() && current.members.isEmpty() &&
            current.budgets.isEmpty() && current.projects.isEmpty() && current.users.isEmpty() &&
            current.registers.all { it.isDefault && it.initialBalanceCents == 0L } &&
            current.categories.all { it.isDefault } &&
            current.profile?.onboardingCompleted != true
        require(pristine) {
            "L'import d'accueil est réservé à un dossier neuf. Vos données actuelles n'ont pas été modifiées."
        }
        restorePayload(
            context = context,
            repository = repository,
            payload = payload.copy(snapshot = payload.snapshot.copy(users = emptyList())),
            verifyExistingWorkspace = false
        )
    }

    /** Décrit un fichier avant confirmation ; ne crée aucun fichier et ne modifie aucune table. */
    suspend fun inspect(
        context: Context,
        uri: Uri,
        password: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        decodePayload(readLimited(context, uri), password).snapshot
    }

    private suspend fun restorePayload(
        context: Context,
        repository: TransactionRepository,
        payload: BackupPayload,
        verifyExistingWorkspace: Boolean = true
    ): BackupSnapshot {
        if (verifyExistingWorkspace) verifyWorkspace(payload.snapshot, repository)
        val createdFiles = mutableListOf<File>()
        return try {
            val receiptPaths = writeReceiptFiles(context, repository.workspaceId, payload.receiptFilesByTransactionUid)
            createdFiles += receiptPaths.values.map(::File)
            val transactions = payload.snapshot.transactions.map { transaction ->
                val importedPath = receiptPaths[transaction.uid]
                when {
                    importedPath != null -> transaction.copy(receiptPhotoUri = importedPath)
                    transaction.receiptPhotoUri.startsWith("archive:") -> transaction.copy(receiptPhotoUri = "")
                    else -> transaction
                }
            }
            val ready = payload.snapshot.copy(transactions = transactions)
            repository.restoreAll(ready)
            ready
        } catch (error: Throwable) {
            createdFiles.forEach { it.delete() }
            throw error
        }
    }

    private suspend fun verifyWorkspace(snapshot: BackupSnapshot, repository: TransactionRepository) {
        val localProfile = repository.getProfile()?.takeIf { it.onboardingCompleted } ?: return
        val importedUid = snapshot.profile?.churchUid.orEmpty()
        val sameWorkspace = if (importedUid.isNotBlank() && localProfile.churchUid.isNotBlank()) {
            importedUid == localProfile.churchUid
        } else {
            repository.workspaceId == "default" &&
                snapshot.churchName.trim().equals(localProfile.churchName.trim(), ignoreCase = true)
        }
        val localSnapshot = repository.snapshotAll()
        val pristineTarget = localSnapshot.transactions.isEmpty() &&
            localSnapshot.members.isEmpty() &&
            localSnapshot.budgets.isEmpty() &&
            localSnapshot.projects.isEmpty() &&
            localSnapshot.registers.all { it.isDefault && it.initialBalanceCents == 0L } &&
            localSnapshot.categories.all { it.isDefault }
        require(sameWorkspace || pristineTarget) {
            "Cette sauvegarde appartient à une autre église et le dossier actuel contient déjà des données. Ouvrez un dossier vide avant de la restaurer."
        }
    }

    private fun decodePayload(content: ByteArray, password: String): BackupPayload {
        require(content.size <= MAX_INPUT_BYTES) {
            "Le fichier dépasse la taille maximale autorisée."
        }
        if (content.size >= 4 && content[0] == 'P'.code.toByte() && content[1] == 'K'.code.toByte()) {
            return LuminaArchiveCodec.decode(content).let {
                BackupPayload(it.snapshot, it.receiptFilesByTransactionUid, isArchive = true)
            }
        }
        val text = String(content, Charsets.UTF_8)
        val clear = if (BackupCrypto.isEncrypted(text)) {
            require(password.isNotBlank()) { "Cette sauvegarde est protégée : saisissez son mot de passe." }
            BackupCrypto.decrypt(text, password)
        } else null

        if (clear != null) {
            val trimmed = clear.trimStart()
            if (trimmed.startsWith("{")) return BackupPayload(BackupCodec.decode(clear))
            val zipBytes = try {
                Base64.decode(trimmed, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Fichier chiffré illisible ou endommagé.")
            }
            return LuminaArchiveCodec.decode(zipBytes).let {
                BackupPayload(it.snapshot, it.receiptFilesByTransactionUid, isArchive = true)
            }
        }

        return BackupPayload(BackupCodec.decode(text))
    }

    private fun collectReceiptFiles(
        context: Context,
        snapshot: BackupSnapshot,
        workspaceId: String
    ): Map<String, ByteArray> {
        val allowedDirectory = receiptDirectory(context, workspaceId).canonicalFile
        return snapshot.transactions.mapNotNull { transaction ->
            if (transaction.uid.isBlank() || transaction.receiptPhotoUri.isBlank()) return@mapNotNull null
            runCatching {
                val source = File(transaction.receiptPhotoUri).canonicalFile
                require(source.isFile && source.path.startsWith(allowedDirectory.path + File.separator))
                val bytes = source.readBytes()
                require(bytes.size <= 32 * 1024 * 1024)
                transaction.uid to bytes
            }.getOrNull()
        }.toMap()
    }

    private fun writeReceiptFiles(
        context: Context,
        workspaceId: String,
        receipts: Map<String, ByteArray>
    ): Map<String, String> {
        if (receipts.isEmpty()) return emptyMap()
        val directory = receiptDirectory(context, workspaceId).apply { mkdirs() }
        val result = linkedMapOf<String, String>()
        try {
            receipts.forEach { (uid, bytes) ->
                require(uid.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Identifiant de justificatif invalide" }
                require(bytes.size <= 32 * 1024 * 1024) { "Justificatif trop volumineux" }
                val file = File(directory, "lumina_${uid}_${UUID.randomUUID()}.jpg")
                val temp = File(directory, ".${file.name}.tmp")
                try {
                    temp.writeBytes(bytes)
                    check(temp.renameTo(file)) { "Impossible d'enregistrer un justificatif" }
                    result[uid] = file.absolutePath
                } finally {
                    if (temp.exists()) temp.delete()
                }
            }
            return result
        } catch (error: Throwable) {
            result.values.forEach { File(it).delete() }
            throw error
        }
    }

    private fun receiptDirectory(context: Context, workspaceId: String): File {
        require(workspaceId == "default" || workspaceId.matches(Regex("[A-Za-z0-9_-]+"))) {
            "Identifiant de dossier invalide"
        }
        val root = File(context.filesDir, "justificatifs")
        return if (workspaceId == "default") root else File(root, workspaceId)
    }

    private fun readLimited(file: File): ByteArray {
        require(file.length() <= MAX_INPUT_BYTES) {
            "Le fichier dépasse la taille maximale autorisée."
        }
        return file.readBytes()
    }

    private fun readLimited(context: Context, uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Fichier inaccessible.")
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        stream.use {
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_INPUT_BYTES) {
                    "Le fichier dépasse la taille maximale autorisée."
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    private fun workspaceDirectory(baseDirectory: File, folderName: String, workspaceId: String): File {
        val root = File(baseDirectory, folderName)
        if (workspaceId == "default") return root
        require(workspaceId.matches(Regex("[A-Za-z0-9_-]+"))) { "Identifiant de dossier invalide" }
        return File(root, workspaceId)
    }
}
