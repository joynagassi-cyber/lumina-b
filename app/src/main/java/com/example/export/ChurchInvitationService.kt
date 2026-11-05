package com.example.export

import android.content.Context
import android.net.Uri
import com.example.data.TransactionRepository
import com.example.sync.SyncCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Création et acceptation du petit fichier d'invitation, sans transfert de données financières. */
object ChurchInvitationService {
    private const val MAX_ENCRYPTED_INVITE_BYTES = 4 * 1024 * 1024
    private const val MIN_PASSPHRASE_LENGTH = 6

    suspend fun create(
        context: Context,
        repository: TransactionRepository,
        passphrase: String
    ): File = withContext(Dispatchers.IO) {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "La phrase de passe de l'invitation doit contenir au moins 6 caractères."
        }
        val profile = repository.getProfile()
            ?: throw IllegalStateException("Configurez d'abord le profil de l'église.")
        val clear = ChurchInvitationCodec.encode(profile, UUID.randomUUID().toString())
        val encrypted = SyncCrypto.encrypt(clear, passphrase)
        require(encrypted.size <= MAX_ENCRYPTED_INVITE_BYTES) { "Le fichier d'invitation est trop volumineux." }

        val directory = File(context.cacheDir, "invitations/${repository.workspaceId}").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date())
        val safeName = profile.displayName.replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim().replace(Regex("\\s+"), "_").ifBlank { "Eglise" }
        val file = File(directory, "Lumina_${safeName}_$stamp.lumina-invite")
        val temp = File(directory, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeBytes(encrypted)
            check(temp.renameTo(file)) { "Impossible d'enregistrer l'invitation." }
        } finally {
            if (temp.exists()) temp.delete()
        }
        file
    }

    /** Reconnaît le paquet chiffré; sinon délègue les anciennes archives complètes au flux existant. */
    suspend fun importForNewMember(
        context: Context,
        repository: TransactionRepository,
        uri: Uri,
        passphrase: String
    ): BackupSnapshot = withContext(Dispatchers.IO) {
        val prefix = readPrefix(context, uri)
        if (!SyncCrypto.looksEncrypted(prefix)) {
            return@withContext BackupService.importForNewMember(context, repository, uri, passphrase)
        }
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "Saisissez la phrase de passe fournie avec l'invitation."
        }
        val encrypted = readLimited(context, uri, MAX_ENCRYPTED_INVITE_BYTES.toLong())
        require(encrypted.size <= MAX_ENCRYPTED_INVITE_BYTES) {
            "Ce fichier chiffré n'est pas une invitation légère. Pour rejoindre avec un dossier complet, choisissez son archive Lumina ZIP."
        }
        val invitation = ChurchInvitationCodec.decode(SyncCrypto.decrypt(encrypted, passphrase))
        val current = repository.snapshotAll()
        val localProfile = current.profile
        val pristineProfile = localProfile == null || (
            !localProfile.onboardingCompleted && localProfile.churchName.isBlank() &&
                localProfile.denomination.isBlank() && localProfile.city.isBlank() &&
                localProfile.address.isBlank() && localProfile.phone.isBlank() &&
                localProfile.email.isBlank() && localProfile.pastorName.isBlank() &&
                localProfile.treasurerName.isBlank() && localProfile.logoBase64.isBlank()
            )
        val pristine = current.transactions.isEmpty() && current.members.isEmpty() &&
            current.budgets.isEmpty() && current.projects.isEmpty() && current.users.isEmpty() &&
            current.registers.all { it.isDefault && it.initialBalanceCents == 0L } &&
            current.categories.all { it.isDefault } && pristineProfile
        require(pristine) {
            "L'invitation est réservée à un dossier local neuf. Vos données actuelles n'ont pas été modifiées."
        }
        repository.saveProfile(invitation.profile.copy(updatedAt = System.currentTimeMillis()))
        repository.snapshotAll().copy(importedAsLightInvitation = true)
    }

    private fun readPrefix(context: Context, uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Fichier inaccessible.")
        return input.use { stream ->
            val buffer = ByteArray(16)
            var count = 0
            while (count < buffer.size) {
                val read = stream.read(buffer, count, buffer.size - count)
                if (read <= 0) break
                count += read
            }
            if (count <= 0) byteArrayOf() else buffer.copyOf(count)
        }
    }

    private fun readLimited(context: Context, uri: Uri, maximumBytes: Long): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Fichier inaccessible.")
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        input.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                total += read
                require(total <= maximumBytes) {
                    "Ce fichier chiffré n'est pas une invitation légère. Pour rejoindre avec un dossier complet, choisissez son archive Lumina ZIP."
                }
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
    }

}
