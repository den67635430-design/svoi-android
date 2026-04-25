/*
 * СВОи API Client — HTTP клиент для VPS backend
 */
package im.vector.app.features.svoi.api

import im.vector.app.features.svoi.SvoiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class PaymentResponse(
    val orderId: String,
    val qrUrl: String,
    val qrImageBase64: String?,
    val amountRub: Int,
    val demoMode: Boolean,
)

data class SubscriptionStatusResponse(
    val userId: String,
    val isPremium: Boolean,
    val tier: String?,
    val expiresAt: String?,
)

data class VersionInfo(
    val version: String,
    val build: Int,
    val downloadUrl: String,
    val changelog: String,
    val critical: Boolean,
)

@Singleton
class SvoiApiClient @Inject constructor() {

    suspend fun createPayment(userId: String, amountRub: Int): PaymentResponse = withContext(Dispatchers.IO) {
        val url = URL("${SvoiConfig.API_ENDPOINT}/api/payments/create")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("user_id", userId)
                put("amount_kopecks", amountRub * 100)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            PaymentResponse(
                orderId = json.getString("order_id"),
                qrUrl = json.getString("qr_url"),
                qrImageBase64 = json.optString("qr_image_base64").ifEmpty { null },
                amountRub = json.getInt("amount_rub"),
                demoMode = json.getBoolean("demo_mode"),
            )
        } finally {
            conn.disconnect()
        }
    }

    suspend fun simulatePayment(orderId: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("${SvoiConfig.API_ENDPOINT}/api/payments/$orderId/simulate")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.use { it.write("{}".toByteArray()) }
            val response = conn.inputStream.bufferedReader().readText()
            JSONObject(response).optBoolean("success", false)
        } catch (e: Exception) {
            Timber.e(e, "Simulate payment failed")
            false
        } finally {
            conn.disconnect()
        }
    }

    suspend fun createDonation(userId: String, amountRub: Int): PaymentResponse {
        return createPayment(userId, amountRub)  // тот же эндпоинт
    }
    suspend fun getAppVersion(): VersionInfo = withContext(Dispatchers.IO) {
        val url = URL("${SvoiConfig.VERSION_API_ENDPOINT}/api/app-version")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            VersionInfo(
                    version = json.optString("version", "1.0.0"),
                    build = json.optInt("build", 0),
                    downloadUrl = json.optString("downloadUrl", "https://kodkontenta.ru/apk/svoi.apk"),
                    changelog = json.optString("changelog", ""),
                    critical = json.optBoolean("critical", false),
            )
        } finally {
            conn.disconnect()
        }
    }

}
