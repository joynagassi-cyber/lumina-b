package com.example

import androidx.core.content.FileProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ChurchInvitationSharingTest {
    @Test
    fun invitation_dans_cache_est_couverte_par_fileprovider() {
        val context = RuntimeEnvironment.getApplication()
        val invitation = File(context.cacheDir, "invitations/test/invitation.lumina-invite")
        invitation.parentFile?.mkdirs()
        invitation.writeText("test")

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            invitation
        )

        assertTrue(uri.toString().startsWith("content://"))
        invitation.delete()
    }
}
