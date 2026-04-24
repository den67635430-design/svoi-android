/*
 * SVOI Version Checker - проверяет обновления при старте приложения.
 * При наличии новой версии показывает диалог.
 */
package im.vector.app.features.svoi.updates

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.BuildConfig
import im.vector.app.features.svoi.api.SvoiApiClient
import im.vector.app.features.svoi.api.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SvoiVersionChecker(
        private val activity: Activity,
        private val apiClient: SvoiApiClient,
) {

    private val prefs: SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun checkForUpdates(scope: CoroutineScope) {
        if (!shouldCheckNow()) {
            Timber.d("SVOI update check skipped (throttled)")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val remote = apiClient.getAppVersion()
                val local = BuildConfig.VERSION_NAME
                Timber.i("SVOI version check: local=%s, remote=%s", local, remote.version)

                if (compareVersions(remote.version, local) > 0) {
                    val skipped = prefs.getString(KEY_SKIPPED_VERSION, null)
                    if (!remote.critical && skipped == remote.version) {
                        Timber.d("SVOI update %s skipped by user", remote.version)
                        prefs.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            showUpdateDialog(remote)
                        }
                    }
                }
                prefs.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
            } catch (e: Exception) {
                Timber.w(e, "SVOI version check failed (non-fatal)")
            }
        }
    }

    private fun shouldCheckNow(): Boolean {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        val elapsed = System.currentTimeMillis() - last
        return elapsed >= CHECK_INTERVAL_MS
    }

    private fun showUpdateDialog(info: VersionInfo) {
        val title = if (info.critical) "Обновление обязательно" else "Доступно обновление"
        val message = buildString {
            append("Версия ")
            append(info.version)
            if (info.build > 0) append(" (build ").append(info.build).append(")")
            append("\n\n")
            append(info.changelog.ifEmpty { "Улучшения и исправления ошибок." })
        }

        val builder = MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Обновить") { _, _ ->
                    openDownloadUrl(info.downloadUrl)
                }
                .setCancelable(!info.critical)

        if (!info.critical) {
            builder.setNegativeButton("Позже") { dialog, _ -> dialog.dismiss() }
            builder.setNeutralButton("Пропустить") { _, _ ->
                prefs.edit { putString(KEY_SKIPPED_VERSION, info.version) }
            }
        }
        builder.show()
    }

    private fun openDownloadUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Cannot open download URL: %s", url)
        }
    }

    /**
     * Сравнение версий типа "1.2.3" с "1.2.4".
     * Возвращает: 1 если remote > local, -1 если local > remote, 0 если равны.
     */
    private fun compareVersions(remote: String, local: String): Int {
        val rp = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val lp = local.split(".").map { it.toIntOrNull() ?: 0 }
        val n = maxOf(rp.size, lp.size)
        for (i in 0 until n) {
            val r = rp.getOrNull(i) ?: 0
            val l = lp.getOrNull(i) ?: 0
            if (r > l) return 1
            if (r < l) return -1
        }
        return 0
    }

    companion object {
        private const val PREFS_NAME = "svoi_updates"
        private const val KEY_LAST_CHECK = "last_version_check"
        private const val KEY_SKIPPED_VERSION = "skipped_version"
        private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L // 6 часов
    }
}
