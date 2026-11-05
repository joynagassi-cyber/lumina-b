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
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)) return null
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
            decodeScaled(bytes, LOGO_MAX_DIMENSION)
        } catch (e: Exception) {
            null
        }
    }

    fun fileToBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (!file.isFile) null else decodeScaled(file, RECEIPT_MAX_DIMENSION)
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
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension) }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
            fitToMaxDimension(decoded, maxDimension)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeScaled(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return fitToMaxDimension(decoded, maxDimension)
    }

    private fun decodeScaled(file: File, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return fitToMaxDimension(decoded, maxDimension)
    }

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        val largest = maxOf(width, height).coerceAtLeast(1)
        var sample = 1
        while (largest / sample > maxDimension * 2 && sample <= Int.MAX_VALUE / 2) {
            sample *= 2
        }
        return sample
    }

    private fun fitToMaxDimension(decoded: Bitmap, maxDimension: Int): Bitmap? {
        val currentLargest = maxOf(decoded.width, decoded.height)
        if (currentLargest <= maxDimension) return decoded
        return try {
            val ratio = maxDimension.toFloat() / currentLargest.toFloat()
            val width = (decoded.width * ratio).toInt().coerceAtLeast(1)
            val height = (decoded.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, width, height, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } catch (_: Exception) {
            decoded.recycle()
            null
        }
    }
}
