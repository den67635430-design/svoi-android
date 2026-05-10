/*
 * SVOi Phone API client. Сохраняет номер телефона юзера в Matrix-профиль (3PID)
 * без SMS-верификации (тестовый режим).
 */
package im.vector.app.features.svoi.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

object SvoiPhoneApi {

    private const val BASE_URL = "https://kodkontenta.ru/api/users/me/phone"

    /** Возвращает текущий привязанный phone (или null). */
    suspend fun getPhone(synapseAccessToken: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(BASE_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $synapseAccessToken")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                JSONObject(body).optString("phone").takeIf { it.isNotEmpty() && it != "null" }
            } finally { conn.disconnect() }
        }.onFailure { Timber.w(it, "SvoiPhoneApi.getPhone") }.getOrNull()
    }

    /** Сохраняет phone в Matrix-профиль. Возвращает true при успехе. */
    suspend fun setPhone(synapseAccessToken: String, phone: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(BASE_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $synapseAccessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            try {
                val body = JSONObject().put("phone", phone).toString()
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode in 200..299) true
                else {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "?"
                    Timber.w("SvoiPhoneApi.setPhone HTTP ${conn.responseCode}: $err")
                    false
                }
            } finally { conn.disconnect() }
        }.onFailure { Timber.w(it, "SvoiPhoneApi.setPhone") }.getOrDefault(false)
    }
}
