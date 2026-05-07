/*
 * СВОи Sticker API Client — HTTP клиент для управления стикерами.
 * Расширяет SvoiApiClient методами для работы со стикерами.
 */
package im.vector.app.features.svoi.api

import im.vector.app.features.svoi.SvoiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class Sticker(
        val id: String,
        val code: String,
        val url: String,
        val category: String? = null,
        val isUserSticker: Boolean = false,
)

data class StickerUploadResponse(
        val id: String,
        val code: String,
        val url: String,
        val sizeBytes: Int,
)

class SvoiStickerApi {

    private val baseUrl: String
        get() = "${SvoiConfig.STICKERS_API_ENDPOINT}/api/stickers"

    /** Получить список встроенных стикеров. */
    suspend fun getBuiltinStickers(): List<Sticker> = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/builtin")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val response = conn.inputStream.bufferedReader().readText()
            val arr = JSONObject(response).getJSONArray("stickers")
            val list = mutableListOf<Sticker>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                list.add(
                        Sticker(
                                id = s.getString("id"),
                                code = s.getString("code"),
                                url = s.getString("url"),
                                category = s.optString("category", null),
                                isUserSticker = false,
                        )
                )
            }
            list
        } catch (e: Exception) {
            Timber.e(e, "Failed to load builtin stickers")
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    /** Получить стикеры конкретного юзера. */
    suspend fun getUserStickers(userId: String): List<Sticker> = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/user/${java.net.URLEncoder.encode(userId, "UTF-8")}")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val response = conn.inputStream.bufferedReader().readText()
            val arr = JSONObject(response).getJSONArray("stickers")
            val list = mutableListOf<Sticker>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                list.add(
                        Sticker(
                                id = s.getString("id"),
                                code = s.getString("code"),
                                url = s.getString("url"),
                                isUserSticker = true,
                        )
                )
            }
            list
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user stickers")
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    /** Загрузить новый стикер юзера (PNG/JPG). */
    suspend fun uploadSticker(
            userId: String,
            code: String,
            imageBytes: ByteArray,
            filename: String = "sticker.png",
            isPremium: Boolean = false,
    ): StickerUploadResponse? = withContext(Dispatchers.IO) {
        val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
        val url = URL("$baseUrl/upload")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000

            val out = ByteArrayOutputStream()
            // user_id field
            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"user_id\"\r\n\r\n".toByteArray())
            out.write("$userId\r\n".toByteArray())
            // code field
            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"code\"\r\n\r\n".toByteArray())
            out.write("$code\r\n".toByteArray())
            // is_premium
            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"is_premium\"\r\n\r\n".toByteArray())
            out.write("$isPremium\r\n".toByteArray())
            // file
            out.write("--$boundary\r\n".toByteArray())
            out.write(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n".toByteArray()
            )
            out.write("Content-Type: image/png\r\n\r\n".toByteArray())
            out.write(imageBytes)
            out.write("\r\n".toByteArray())
            out.write("--$boundary--\r\n".toByteArray())

            conn.outputStream.use { it.write(out.toByteArray()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                StickerUploadResponse(
                        id = json.getString("id"),
                        code = json.getString("code"),
                        url = json.getString("url"),
                        sizeBytes = json.optInt("size_bytes", imageBytes.size),
                )
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                Timber.w("Sticker upload failed: $responseCode $err")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Sticker upload exception")
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Удалить стикер юзера. */
    suspend fun deleteSticker(userId: String, stickerId: String): Boolean = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(userId, "UTF-8")
        val url = URL("$baseUrl/$stickerId?user_id=$encoded")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "DELETE"
            conn.connectTimeout = 10_000
            conn.responseCode == 200
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete sticker")
            false
        } finally {
            conn.disconnect()
        }
    }
}
