package com.example.cloud

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Thin REST client for the private Lumina backup project. Only the publishable key is in the app. */
class SupabaseCloudClient(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("lumina_cloud_session", Context.MODE_PRIVATE)
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    data class BackupHead(
        val churchId: String,
        val manifestId: String,
        val generation: Long,
        val objectPath: String,
        val formatVersion: Int,
        val byteSize: Long,
        val sha256: String,
        val createdAt: String
    )

    data class Published(val manifestId: String, val generation: Long)

    suspend fun isConfigured(): Boolean = withContext(Dispatchers.IO) {
        baseUrl.startsWith("https://") && publishableKey.isNotBlank()
    }

    suspend fun ensureWorkspace(profileUid: String, churchName: String, creationKey: String): String =
        withContext(Dispatchers.IO) {
            ensureAuthenticated()
            val body = JSONObject()
                .put("p_creation_key", creationKey)
                .put("p_display_name", churchName.take(120))
                .put("p_local_church_uid", profileUid)
            rpcScalar("create_church_backup_workspace", body)
        }

    suspend fun createInvite(churchId: String, role: String = "editor"): String =
        withContext(Dispatchers.IO) {
            ensureAuthenticated()
            require(role == "editor" || role == "viewer")
            rpcScalar(
                "create_church_backup_invite",
                JSONObject().put("p_church_id", churchId).put("p_role", role)
            )
        }

    suspend fun acceptInvite(token: String): String = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        rpcScalar("accept_church_backup_invite", JSONObject().put("p_token", token))
    }

    suspend fun currentHead(churchId: String): BackupHead? = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        val query = "cloud_backup_heads?select=church_id,manifest_id,generation,cloud_backup_manifests(object_path,format_version,byte_size,sha256,created_at)&church_id=eq.${encode(churchId)}&limit=1"
        val response = request("GET", "/rest/v1/$query")
        val rows = JSONArray(response)
        if (rows.length() == 0) return@withContext null
        val row = rows.getJSONObject(0)
        val manifest = row.optJSONObject("cloud_backup_manifests")
            ?: throw IOException("Le pointeur cloud ne contient pas les métadonnées de sauvegarde.")
        BackupHead(
            churchId = row.getString("church_id"),
            manifestId = row.getString("manifest_id"),
            generation = row.getLong("generation"),
            objectPath = manifest.getString("object_path"),
            formatVersion = manifest.getInt("format_version"),
            byteSize = manifest.getLong("byte_size"),
            sha256 = manifest.getString("sha256"),
            createdAt = manifest.getString("created_at")
        )
    }

    suspend fun uploadObject(path: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        require(path.startsWith("churches/") && path.endsWith(".lumina"))
        request(
            method = "POST",
            path = "/storage/v1/object/lumina-backups/${path.split('/').joinToString("/") { encode(it) }}",
            body = bytes,
            contentType = "application/octet-stream",
            extraHeaders = mapOf("x-upsert" to "false")
        )
    }

    suspend fun downloadObject(path: String, maximumBytes: Long): ByteArray = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        require(path.startsWith("churches/") && path.endsWith(".lumina"))
        val bytes = requestBytes(
            "GET",
            "/storage/v1/object/authenticated/lumina-backups/${path.split('/').joinToString("/") { encode(it) }}"
        )
        require(bytes.size.toLong() in 1..maximumBytes) { "La sauvegarde cloud est vide ou dépasse la limite autorisée." }
        bytes
    }

    suspend fun publish(
        churchId: String,
        expectedGeneration: Long,
        objectPath: String,
        formatVersion: Int,
        byteSize: Long,
        sha256: String
    ): Published = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        val response = request(
            "POST",
            "/rest/v1/rpc/publish_church_backup",
            JSONObject()
                .put("p_church_id", churchId)
                .put("p_expected_generation", expectedGeneration)
                .put("p_object_path", objectPath)
                .put("p_format_version", formatVersion)
                .put("p_byte_size", byteSize)
                .put("p_sha256", sha256)
                .toString()
                .toByteArray(),
            "application/json"
        )
        val row = JSONArray(response).optJSONObject(0)
            ?: throw IOException("Supabase n'a pas confirmé la publication de la sauvegarde.")
        Published(row.getString("manifest_id"), row.getLong("generation"))
    }

    suspend fun ensureAuthenticated(): String = withContext(Dispatchers.IO) {
        requireConfigured()
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        val expiry = preferences.getLong(KEY_EXPIRES_AT, 0L)
        if (!token.isNullOrBlank() && expiry > System.currentTimeMillis() + 60_000L) return@withContext token

        val refresh = preferences.getString(KEY_REFRESH_TOKEN, null)
        if (!refresh.isNullOrBlank()) {
            val refreshed = try {
                authRequest("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh))
            } catch (error: Exception) {
                throw IOException("Impossible de renouveler la session cloud. Réassociez cet appareil par invitation avant de continuer.", error)
            }
            return@withContext persistSession(refreshed)
        }
        if (!token.isNullOrBlank()) {
            throw IOException("La session cloud de cet appareil n'est plus récupérable. Réassociez-le par invitation.")
        }

        // Supabase anonymous users are ordinary authenticated users subject to the same RLS rules.
        val anonymous = authRequest("/auth/v1/signup", JSONObject().put("data", JSONObject()))
        persistSession(anonymous)
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        preferences.edit().clear().commit()
    }

    private fun rpcScalar(name: String, body: JSONObject): String {
        val response = request("POST", "/rest/v1/rpc/$name", body.toString().toByteArray(), "application/json")
        val value = response.trim().removeSurrounding("\"", "\"")
        require(value.isNotBlank() && value != "null") { "Supabase n'a pas renvoyé d'identifiant valide." }
        return value
    }

    private fun authRequest(path: String, body: JSONObject): JSONObject {
        val response = request(
            "POST", path, body.toString().toByteArray(), "application/json", includeBearer = false
        )
        return JSONObject(response)
    }

    private fun persistSession(json: JSONObject): String {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        require(access.isNotBlank() && refresh.isNotBlank()) { "La connexion cloud anonyme n'a pas pu être établie." }
        val expiresIn = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        check(preferences.edit()
            .putString(KEY_ACCESS_TOKEN, access)
            .putString(KEY_REFRESH_TOKEN, refresh)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000L)
            .commit()) { "Impossible d'enregistrer la session cloud sur cet appareil." }
        return access
    }

    private fun request(
        method: String,
        path: String,
        body: ByteArray? = null,
        contentType: String = "application/json",
        includeBearer: Boolean = true,
        extraHeaders: Map<String, String> = emptyMap()
    ): String = String(requestBytes(method, path, body, contentType, includeBearer, extraHeaders), Charsets.UTF_8)

    private fun requestBytes(
        method: String,
        path: String,
        body: ByteArray? = null,
        contentType: String = "application/json",
        includeBearer: Boolean = true,
        extraHeaders: Map<String, String> = emptyMap()
    ): ByteArray {
        requireConfigured()
        val builder = Request.Builder()
            .url("$baseUrl$path")
            .header("apikey", publishableKey)
            .header("Accept", "application/json")
        if (includeBearer) builder.header("Authorization", "Bearer ${blockingAccessToken()}")
        body?.let { builder.header("Content-Type", contentType) }
        extraHeaders.forEach { (key, value) -> builder.header(key, value) }
        val requestBody = body?.toRequestBody(contentType.toMediaType())
        val response = http.newCall(builder.method(method, requestBody).build()).execute()
        response.use {
            val payload = it.body?.bytes() ?: ByteArray(0)
            if (!it.isSuccessful) {
                val message = runCatching { JSONObject(String(payload)).optString("message") }.getOrNull()
                    ?.takeIf(String::isNotBlank)
                    ?: runCatching { JSONObject(String(payload)).optString("msg") }.getOrNull()
                    ?: "Erreur cloud HTTP ${it.code}."
                throw IOException(message.take(500))
            }
            return payload
        }
    }

    /** Synchronous token lookup only occurs from request() on Dispatchers.IO. */
    private fun blockingAccessToken(): String {
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        if (!token.isNullOrBlank() && preferences.getLong(KEY_EXPIRES_AT, 0L) > System.currentTimeMillis() + 60_000L) {
            return token
        }
        throw IOException("La session cloud a expiré. Réessayez la sauvegarde pour renouveler l'accès.")
    }

    private fun requireConfigured() {
        if (!baseUrl.startsWith("https://") || publishableKey.isBlank()) {
            throw IllegalStateException("La connexion au stockage cloud n'est pas configurée dans cette version.")
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
