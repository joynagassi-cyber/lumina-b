package com.example.export

import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP conteneur autonome pour les dossiers Lumina. Il inclut l'intégralité du
 * snapshot et les justificatifs associés, sans jamais conserver de chemins
 * privés provenant de l'appareil source.
 */
data class DecodedLuminaArchive(
    val snapshot: BackupSnapshot,
    val receiptFilesByTransactionUid: Map<String, ByteArray>
)

object LuminaArchiveCodec {
    const val FORMAT = "lumina-archive"
    const val VERSION = 1
    const val MAX_ARCHIVE_BYTES = 100 * 1024 * 1024
    private const val MAX_ENTRIES = 10_000
    private const val MAX_ENTRY_BYTES = 32 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 100 * 1024 * 1024
    private const val DATA_ENTRY = "dossier.json"
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val RECEIPTS_PREFIX = "receipts/"

    /**
     * Le snapshot passé ici peut contenir les comptes du propriétaire pour les
     * copies automatiques privées; les exports partageables doivent les retirer
     * avant l'appel.
     */
    fun encode(snapshot: BackupSnapshot, receiptFilesByTransactionUid: Map<String, ByteArray>): ByteArray {
        require(receiptFilesByTransactionUid.size <= MAX_ENTRIES - 2) { "Trop de pièces jointes" }
        var totalBytes = 0L
        receiptFilesByTransactionUid.forEach { (uid, bytes) ->
            require(uid.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Identifiant de pièce jointe invalide" }
            require(bytes.size <= MAX_ENTRY_BYTES) { "Une pièce jointe dépasse la taille autorisée" }
            totalBytes += bytes.size
        }
        require(totalBytes <= MAX_TOTAL_BYTES) { "Les pièces jointes dépassent la taille autorisée" }

        val portableTransactions = snapshot.transactions.map { transaction ->
            transaction.copy(
                receiptPhotoUri = if (receiptFilesByTransactionUid.containsKey(transaction.uid)) {
                    "archive:$RECEIPTS_PREFIX${transaction.uid}.jpg"
                } else ""
            )
        }
        val safeSnapshot = snapshot.copy(transactions = portableTransactions)
        val data = BackupCodec.encode(safeSnapshot).toByteArray(Charsets.UTF_8)
        require(data.size <= MAX_ENTRY_BYTES) { "Les données du dossier dépassent la taille autorisée" }

        val manifest = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("createdAt", snapshot.createdAt)
            put("churchName", snapshot.profile?.displayName ?: snapshot.churchName)
            put("transactionCount", snapshot.transactions.size)
            put("memberCount", snapshot.members.size)
            put("categoryCount", snapshot.categories.size)
            put("budgetCount", snapshot.budgets.size)
            put("registerCount", snapshot.registers.size)
            put("projectCount", snapshot.projects.size)
            put("receiptCount", receiptFilesByTransactionUid.size)
        }.toString().toByteArray(Charsets.UTF_8)

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(data)
            zip.closeEntry()
            receiptFilesByTransactionUid.toSortedMap().forEach { (uid, bytes) ->
                zip.putNextEntry(ZipEntry("$RECEIPTS_PREFIX$uid.jpg"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val bytes = output.toByteArray()
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "Le fichier Lumina est trop volumineux" }
        return bytes
    }

    fun decode(bytes: ByteArray): DecodedLuminaArchive {
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "Le fichier Lumina dépasse la taille autorisée" }
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var count = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                count++
                require(count <= MAX_ENTRIES) { "Le fichier Lumina contient trop d'éléments" }
                val name = entry.name
                require(!entry.isDirectory && isAllowedEntry(name)) {
                    "Chemin interdit dans le fichier Lumina"
                }
                require(name !in entries) { "Élément répété dans le fichier Lumina" }
                val contents = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var entryTotal = 0
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    entryTotal += read
                    total += read
                    require(entryTotal <= MAX_ENTRY_BYTES && total <= MAX_TOTAL_BYTES) {
                        "Le fichier Lumina dépasse la taille autorisée après décompression"
                    }
                    contents.write(buffer, 0, read)
                }
                entries[name] = contents.toByteArray()
                zip.closeEntry()
            }
        }

        val manifestBytes = entries[MANIFEST_ENTRY]
            ?: throw IllegalArgumentException("Fichier Lumina incomplet : manifeste absent")
        val manifest = runCatching { JSONObject(String(manifestBytes, Charsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Manifeste Lumina illisible") }
        require(manifest.optString("format") == FORMAT) { "Ce ZIP n'est pas un fichier Lumina" }
        require(manifest.optInt("version", 0) in 1..VERSION) {
            "Fichier Lumina créé par une version incompatible"
        }
        val data = entries[DATA_ENTRY]
            ?: throw IllegalArgumentException("Fichier Lumina incomplet : données absentes")
        val snapshot = BackupCodec.decode(String(data, Charsets.UTF_8))
        require(manifest.optInt("transactionCount", -1) == snapshot.transactions.size &&
            manifest.optInt("memberCount", -1) == snapshot.members.size &&
            manifest.optInt("categoryCount", -1) == snapshot.categories.size &&
            manifest.optInt("budgetCount", -1) == snapshot.budgets.size &&
            manifest.optInt("registerCount", -1) == snapshot.registers.size &&
            manifest.optInt("projectCount", -1) == snapshot.projects.size
        ) { "Le manifeste ne correspond pas aux données du fichier Lumina" }
        val receipts = entries.filterKeys { it.startsWith(RECEIPTS_PREFIX) }
            .mapKeys { (name, _) -> name.removePrefix(RECEIPTS_PREFIX).removeSuffix(".jpg") }
        require(manifest.optInt("receiptCount", -1) == receipts.size) {
            "Le nombre de justificatifs du manifeste ne correspond pas au fichier"
        }
        val referenced = snapshot.transactions.mapNotNull { transaction ->
            transaction.receiptPhotoUri.takeIf { it.startsWith("archive:$RECEIPTS_PREFIX") }
                ?.removePrefix("archive:$RECEIPTS_PREFIX")?.removeSuffix(".jpg")
        }.toSet()
        require(referenced.all { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) && it in receipts }) {
            "Une pièce jointe référencée manque dans le fichier Lumina"
        }
        require(receipts.keys.all { it in referenced }) {
            "Le fichier Lumina contient une pièce jointe orpheline"
        }
        return DecodedLuminaArchive(snapshot, receipts)
    }

    private fun isAllowedEntry(name: String): Boolean = when {
        name == DATA_ENTRY || name == MANIFEST_ENTRY -> true
        name.startsWith(RECEIPTS_PREFIX) -> {
            val fileName = name.removePrefix(RECEIPTS_PREFIX)
            fileName.matches(Regex("[A-Za-z0-9_-]{1,128}\\.jpg"))
        }
        else -> false
    }
}
