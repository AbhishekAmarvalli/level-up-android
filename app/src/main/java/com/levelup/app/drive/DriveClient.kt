package com.levelup.app.drive

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DriveQuota(val usageBytes: Long, val limitBytes: Long?) {
    val fraction: Float
        get() = if (limitBytes == null || limitBytes <= 0L) 0f
        else (usageBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)

    val percent: Int
        get() = if (limitBytes == null || limitBytes <= 0L) 0 else (fraction * 100f).toInt()
}

class DriveException(message: String) : Exception(message)

/**
 * Minimal Google Drive REST v3 client using the drive.file scope only.
 *
 * There is deliberately no Google API client dependency: HTTP calls are made with
 * HttpURLConnection and the access token comes from GoogleAuthUtil. Uploads use the
 * resumable protocol in 2 MB chunks, so a dropped connection can continue.
 */
class DriveClient(private val context: Context) {
    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.file"

        private const val API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val FOLDER_NAME = "Level Up"
        private const val CHUNK_SIZE = 2 * 1024 * 1024
    }

    @Volatile
    private var folderId: String? = null

    fun signedInEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    fun hasAccount(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    private fun accessToken(): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw DriveException("Not signed in to Google Drive")
        val email = account.email ?: throw DriveException("Google account has no email")
        return GoogleAuthUtil.getToken(context, email, "oauth2:$SCOPE")
    }

    suspend fun storageQuota(): DriveQuota = withContext(Dispatchers.IO) {
        val token = accessToken()
        val json = getJson("$API_BASE/about?fields=storageQuota", token)
        val quota = json.optJSONObject("storageQuota")
        val usage = quota?.optString("usage")?.toLongOrNull()
            ?: quota?.optString("usageInDrive")?.toLongOrNull()
            ?: 0L
        val limit = quota?.optString("limit")?.toLongOrNull()
        DriveQuota(usageBytes = usage, limitBytes = limit)
    }

    /** Finds or creates the app-owned "Level Up" folder. */
    suspend fun ensureFolder(): String = withContext(Dispatchers.IO) {
        folderId?.let { return@withContext it }
        val token = accessToken()
        ensureFolder(token)
    }

    private fun ensureFolder(token: String): String {
        folderId?.let { return it }
        val query = URLEncoder.encode(
            "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            "UTF-8",
        )
        val list = getJson("$API_BASE/files?q=$query&fields=files(id)&spaces=drive", token)
        val files = list.optJSONArray("files")
        if (files != null && files.length() > 0) {
            val id = files.getJSONObject(0).optString("id")
            if (id.isNotBlank()) {
                folderId = id
                return id
            }
        }
        val body = JSONObject()
            .put("name", FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder")
        val created = postJson("$API_BASE/files?fields=id", token, body.toString())
        val id = created.optString("id")
        if (id.isBlank()) throw DriveException("Could not create the Level Up folder")
        folderId = id
        return id
    }

    /** Uploads a clip with the resumable protocol and returns the Drive file id. */
    suspend fun upload(
        file: File,
        displayName: String,
        onProgress: (Int) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) {
            throw DriveException("Clip file is missing")
        }
        val token = accessToken()
        val folder = ensureFolder(token)
        val total = file.length()

        val start = URL("$UPLOAD_BASE/files?uploadType=resumable&fields=id").openConnection() as HttpURLConnection
        start.requestMethod = "POST"
        start.connectTimeout = 20_000
        start.readTimeout = 60_000
        start.doOutput = true
        start.setRequestProperty("Authorization", "Bearer $token")
        start.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        start.setRequestProperty("X-Upload-Content-Type", "video/mp4")
        start.setRequestProperty("X-Upload-Content-Length", total.toString())
        val metadata = JSONObject()
            .put("name", displayName)
            .put("parents", JSONArray().put(folder))
        start.outputStream.use { it.write(metadata.toString().toByteArray(Charsets.UTF_8)) }
        val startCode = start.responseCode
        val location = start.getHeaderField("Location")
        if (startCode !in 200..299 || location.isNullOrBlank()) {
            val error = start.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            start.disconnect()
            throw DriveException("Could not start the Drive upload ($startCode) $error")
        }
        start.disconnect()

        RandomAccessFile(file, "r").use { raf ->
            var offset = 0L
            while (offset < total) {
                val length = minOf(CHUNK_SIZE.toLong(), total - offset).toInt()
                val bytes = ByteArray(length)
                raf.seek(offset)
                raf.readFully(bytes)

                val connection = URL(location).openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.connectTimeout = 20_000
                connection.readTimeout = 120_000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "video/mp4")
                connection.setRequestProperty(
                    "Content-Range",
                    "bytes $offset-${offset + length - 1}/$total",
                )
                connection.setFixedLengthStreamingMode(length)
                connection.outputStream.use { it.write(bytes) }
                val code = connection.responseCode
                val range = connection.getHeaderField("Range")
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                connection.disconnect()

                when {
                    code in 200..299 -> {
                        onProgress(100)
                        val id = runCatching { JSONObject(body).optString("id") }.getOrNull()
                        if (id.isNullOrBlank()) throw DriveException("Upload finished without a file id")
                        return@withContext id
                    }

                    code == 308 -> {
                        val received = range
                            ?.substringAfterLast('-')
                            ?.trim()
                            ?.toLongOrNull()
                        offset = if (received != null && received >= 0) received + 1 else offset + length
                        onProgress(((offset.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 99))
                    }

                    else -> throw DriveException("Drive upload failed ($code)")
                }
            }
        }
        throw DriveException("Drive upload did not finish")
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        val token = accessToken()
        val connection = URL("$API_BASE/files/$fileId").openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        val code = connection.responseCode
        connection.disconnect()
        // 404 means it is already gone, which is fine for our purposes.
        if (code !in 200..299 && code != 404) {
            throw DriveException("Could not delete the Drive file ($code)")
        }
    }

    private fun getJson(url: String, token: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) throw DriveException("Drive request failed ($code)")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun postJson(url: String, token: String, body: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        try {
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) throw DriveException("Drive request failed ($code)")
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}
