/*
 * SVOi Test Mode API client.
 * Communicates with /api/admin/test-mode endpoints on https://kodkontenta.ru.
 */
package im.vector.app.features.svoi.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

data class TestModeStatus(
        val enabled: Boolean,
        val since: String,
        val changedBy: String,
)

object SvoiTestModeApi {

    private const val BASE_URL = "https://kodkontenta.ru/api/admin/test-mode"

    /** Public — любой клиент может узнать включён ли тестовый режим. */
    suspend fun getStatus(): TestModeStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(BASE_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                TestModeStatus(
                        enabled = json.optBoolean("enabled", true),
                        since = json.optString("since", ""),
                        changedBy = json.optString("changed_by", ""),
                )
            } finally { conn.disconnect() }
        }.onFailure { Timber.w(it, "TestMode getStatus failed") }.getOrNull()
    }

    /** Admin-only — переключает (server проверит права через Synapse access_token). */
    suspend fun toggle(synapseAccessToken: String): TestModeStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("$BASE_URL/toggle").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $synapseAccessToken")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                if (conn.responseCode !in 200..299) {
                    Timber.w("TestMode toggle failed: HTTP ${conn.responseCode}")
                    return@runCatching null
                }
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                TestModeStatus(
                        enabled = json.optBoolean("enabled", true),
                        since = json.optString("since", ""),
                        changedBy = json.optString("changed_by", ""),
                )
            } finally { conn.disconnect() }
        }.onFailure { Timber.w(it, "TestMode toggle failed") }.getOrNull()
    }
}
