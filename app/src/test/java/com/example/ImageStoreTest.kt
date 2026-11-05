package com.example

import android.graphics.Bitmap
import com.example.util.ImageStore
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ImageStoreTest {
    @Test
    fun logos_et_justificatifs_sont_decodes_a_taille_bornee() {
        val source = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { output ->
            source.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        source.recycle()

        val logo = ImageStore.base64ToBitmap(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        assertNotNull(logo)
        assertTrue(maxOf(logo!!.width, logo.height) <= 512)
        logo.recycle()

        val context = RuntimeEnvironment.getApplication()
        val receiptFile = File(context.cacheDir, "test-large-receipt.png")
        receiptFile.writeBytes(bytes)
        val receipt = ImageStore.fileToBitmap(receiptFile.absolutePath)
        assertNotNull(receipt)
        assertTrue(maxOf(receipt!!.width, receipt.height) <= 1600)
        receipt.recycle()
        receiptFile.delete()
    }
}
