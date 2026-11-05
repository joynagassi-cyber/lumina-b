package com.example

import com.example.sync.SyncBundle
import com.example.sync.SyncBundleCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncBundleTest {
    @Test
    fun le_bundle_conserve_l_identifiant_stable_de_l_eglise() {
        val original = SyncBundle(
            churchName = "Église de Kandi",
            churchUid = "eglise-uid-123",
            deviceId = "device-a",
            author = "Trésorier"
        )

        val decoded = SyncBundleCodec.decode(SyncBundleCodec.encode(original))

        assertEquals(original.churchUid, decoded.churchUid)
        assertEquals(original.churchName, decoded.churchName)
    }

    @Test
    fun ancien_bundle_sans_identifiant_reste_lisible() {
        val legacy = JSONObject().apply {
            put("format", SyncBundleCodec.FORMAT)
            put("version", SyncBundleCodec.VERSION)
            put("churchName", "Église historique")
            put("deviceId", "legacy-device")
        }.toString().toByteArray(Charsets.UTF_8)

        val decoded = SyncBundleCodec.decode(legacy)

        assertEquals("", decoded.churchUid)
        assertEquals("Église historique", decoded.churchName)
    }
}
