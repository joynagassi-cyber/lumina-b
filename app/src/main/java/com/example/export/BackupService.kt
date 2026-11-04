package com.example.export

import android.content.Context
import android.net.Uri
import com.example.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sauvegarde et restauration du dossier.
 *
 * Deux filets de sécurité :
 *  - une copie automatique silencieuse, gardée dans l'application et rafraîchie
 *    au plus une fois par jour (elle sert à revenir en arrière après une
 *    fausse manipulation) ;
 *  - une sauvegarde à emporter, que le trésorier enregistre où il veut
 *    (clé USB, messagerie, stockage du téléphone), protégée ou non par mot de
 *    passe, et qu'il peut réinstaller plus tard.
 */
object BackupService {

    private const val EXPORT_DIR = "sauvegardes"
    private const val AUTO_DIR = "copies-automatiques"
    private const val AUTO_KEEP = 3
    private const val AUTO_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val AUTO_PREFIX = "copie-auto-"

    data class ExportResult(
        val file: File,
        val encrypted: Boolean,
        val snapshot: BackupSnapshot
    )

    // ------------------------------------------------------------------
    // Copie automatique
    // ------------------------------------------------------------------

    /** Écrit une copie silencieuse si la dernière date de plus d'une journée. */
    suspend fun autoSnapshot(context: Context, repository: TransactionRepository): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, AUTO_DIR).apply { mkdirs() }
                val existing = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                val newest = existing.lastOrNull()
                val fresh = newest != null &&
                    System.currentTimeMillis() - newest.lastModified() < AUTO_INTERVAL_MS
                if (fresh) return@withContext null

                val snapshot = repository.snapshotAll()
                // Rien à sauvegarder tant que le dossier est vide.
                if (snapshot.transactions.isEmpty() && snapshot.members.isEmpty()) {
                    return@withContext null
                }
                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.FRANCE).format(Date())
                val file = File(dir, "$AUTO_PREFIX$stamp.json")
                file.writeText(BackupCodec.encode(snapshot))

                // On ne garde que les copies les plus récentes, par prudence
                // comme par souci de place.
                val all = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                all.dropLast(AUTO_KEEP.coerceAtLeast(1))
                    .forEach { if (it.name != file.name) it.delete() }
                file
            }.getOrNull()
        }

    fun latestAutoFile(context: Context): File? {
        val dir = File(context.filesDir, AUTO_DIR)
        return dir.listFiles()?.sortedBy { it.name }?.lastOrNull()
    }

    suspend fun restoreLatestAuto(
        context: Context,
        repository: TransactionRepository
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val file = latestAutoFile(context)
            ?: throw IllegalArgumentException("Aucune copie automatique disponible.")
        val snapshot = BackupCodec.decode(file.readText())
        repository.restoreAll(snapshot)
        snapshot
    }

    // ------------------------------------------------------------------
    // Sauvegarde à emporter
    // ------------------------------------------------------------------

    suspend fun create(
        context: Context,
        repository: TransactionRepository,
        password: String
    ): ExportResult = withContext(Dispatchers.IO) {
        val snapshot = repository.snapshotAll()
        val encrypted = password.isNotBlank()
        val json = BackupCodec.encode(snapshot)
        val content = if (encrypted) BackupCrypto.encrypt(json, password) else json

        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.FRANCE).format(Date())
        val file = File(dir, "lumina-sauvegarde-$stamp" + if (encrypted) ".lumina" else ".json")
        file.writeText(content)
        ExportResult(file, encrypted, snapshot)
    }

    suspend fun restore(
        context: Context,
        repository: TransactionRepository,
        uri: Uri,
        password: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            String(it.readBytes(), Charsets.UTF_8)
        } ?: throw IllegalArgumentException("Fichier inaccessible.")

        val clear = if (BackupCrypto.isEncrypted(text)) {
            if (password.isBlank()) {
                throw IllegalArgumentException(
                    "Cette sauvegarde est protégée : saisissez son mot de passe."
                )
            }
            BackupCrypto.decrypt(text, password)
        } else {
            text
        }
        val snapshot = BackupCodec.decode(clear)
        repository.restoreAll(snapshot)
        snapshot
    }

    /** Décrit une sauvegarde sans rien modifier : sert à rassurer avant d'écraser. */
    suspend fun inspect(
        context: Context,
        uri: Uri,
        password: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            String(it.readBytes(), Charsets.UTF_8)
        } ?: throw IllegalArgumentException("Fichier inaccessible.")
        val clear = if (BackupCrypto.isEncrypted(text)) {
            if (password.isBlank()) {
                throw IllegalArgumentException(
                    "Cette sauvegarde est protégée : saisissez son mot de passe."
                )
            }
            BackupCrypto.decrypt(text, password)
        } else {
            text
        }
        BackupCodec.decode(clear)
    }
}
