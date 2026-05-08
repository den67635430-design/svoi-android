/*
 * SVOi Test Mode Manager — кеширует runtime-состояние тестового режима из API.
 * При включённом тестовом режиме все платные функции бесплатны.
 * Состояние обновляется при старте приложения и после toggle.
 */
package im.vector.app.features.svoi.subscription

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.features.svoi.api.SvoiTestModeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestModeManager @Inject constructor(
        @DefaultPreferences private val prefs: SharedPreferences,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Текущее состояние из локального кеша (не делает сетевых запросов). */
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    /** Асинхронно обновляет кеш из API. Вызывать при старте приложения и при открытии админ-настроек. */
    fun refreshAsync() {
        scope.launch {
            val status = SvoiTestModeApi.getStatus() ?: return@launch
            prefs.edit { putBoolean(KEY_ENABLED, status.enabled) }
        }
    }

    /** Синхронный refresh (для случаев когда нужен результат сразу). */
    suspend fun refresh(): Boolean? {
        val status = SvoiTestModeApi.getStatus() ?: return null
        prefs.edit { putBoolean(KEY_ENABLED, status.enabled) }
        return status.enabled
    }

    /** Только для админов: переключает на сервере + обновляет кеш. Возвращает новое состояние или null при ошибке. */
    suspend fun toggle(synapseAccessToken: String): Boolean? {
        val status = SvoiTestModeApi.toggle(synapseAccessToken) ?: return null
        prefs.edit { putBoolean(KEY_ENABLED, status.enabled) }
        return status.enabled
    }

    companion object {
        private const val KEY_ENABLED = "svoi_test_mode_enabled"
    }
}
