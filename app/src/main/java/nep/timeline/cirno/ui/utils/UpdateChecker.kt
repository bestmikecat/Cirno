package nep.timeline.cirno.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nep.timeline.cirno.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val CHECK_URL = "https://cirno-api.vercel.app/api/check-update"
    private const val PREFS_NAME = "cirno_update"
    private const val KEY_SKIPPED = "update_skipped"
    private const val KEY_SKIPPED_VERSION = "skipped_version_code"

    private val gson = Gson()

    suspend fun checkForUpdate(): UpdateResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL(CHECK_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = gson.fromJson(body, JsonObject::class.java) ?: return@withContext null

            val remoteVersionCode = json.get("versionCode")?.asInt ?: return@withContext null
            val versionName = json.get("versionName")?.asString ?: return@withContext null
            val downloadUrl = json.get("downloadUrl")?.asString ?: return@withContext null
            val changelog = json.get("changelog")?.asString
            val publishedAt = json.get("publishedAt")?.asString

            if (remoteVersionCode <= BuildConfig.VERSION_CODE) {
                return@withContext null
            }

            UpdateResult(
                versionCode = remoteVersionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                changelog = changelog,
                publishedAt = publishedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isSkipped(context: Context, versionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SKIPPED, false) && prefs.getInt(KEY_SKIPPED_VERSION, 0) == versionCode
    }

    fun markSkipped(context: Context, versionCode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SKIPPED, true)
            .putInt(KEY_SKIPPED_VERSION, versionCode)
            .apply()
    }

    fun clearSkipped(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SKIPPED, false)
            .remove(KEY_SKIPPED_VERSION)
            .apply()
    }
}

data class UpdateResult(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String?,
    val publishedAt: String?
)
