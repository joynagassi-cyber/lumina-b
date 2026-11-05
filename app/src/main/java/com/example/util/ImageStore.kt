package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Gestion des images locales : logo de l'église (stocké en Base64 dans le
 * profil) et photos de justificatifs (stockées dans le dossier privé de
 * l'application, seule leur référence étant enregistrée avec l'opération).
 */
object ImageStore {

    private const val LOGO_MAX_DIMENSION = 512
    private const val RECEIPT_MAX_DIMENSION = 1600

    /**
     * Charge une image depuis un [Uri] et renvoie sa version compressée en
     * Base64 (PNG), ou null si la lecture échoue.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = LOGO_MAX_DIMENSION): String? {
        val bitmap = decodeScaled(context, uri, maxDimension) ?: return null
        return try {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    /** Copie un justificatif dans le stockage privé et renvoie son chemin. */
    fun uriToReceiptFile(context: Context, uri: Uri, workspaceId: String = "default"): String? {
        val bitmap = decodeScaled(context, uri, RECEIPT_MAX_DIMENSION) ?: return null
        return try {
            require(workspaceId == "default" || workspaceId.matches(Regex("[A-Za-z0-9_-]+")))
            val root = File(context.filesDir, "justificatifs")
            val directory = (if (workspaceId == "default") root else File(root, workspaceId)).apply { mkdirs() }
            val file = File(directory, "justif_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            val temp = File(directory, ".${file.name}.tmp")
            val compressed = temp.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            }
            if (!compressed || !temp.isFile || temp.length() == 0L || !temp.renameTo(file)) {
                temp.delete()
                file.delete()
                return null
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun fileToBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists()) null else BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeScaled(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val largest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sample = 1
            while (largest / sample > maxDimension * 2) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val currentLargest = maxOf(decoded.width, decoded.height)
            if (currentLargest <= maxDimension) {
                decoded
            } else {
                val ratio = maxDimension.toFloat() / currentLargest.toFloat()
                val width = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val height = (decoded.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            }
        } catch (e: Exception) {
            null
        }
    }
}
