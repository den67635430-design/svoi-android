/*
 * SVOI Version Checker — проверяет обновления при старте приложения.
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
        // SVOi: убрали throttle — проверяем при каждом onResume (запрос лёгкий, ~50ms)
        scope.launch(Dispatchers.IO) {
            try {
                val remote = apiClient.getAppVersion()
                val local = getLocalVersionName()
                Timber.i("SVOI version check: local=%s, remote=%s", local, remote.version)

                if (compareVersions(remote.version, local) > 0) {
                    // SVOi: skipped_version игнорируем — каждый раз показываем диалог если есть новая версия
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            showUpdateDialog(remote)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "SVOI version check failed (non-fatal)")
            }
        }
    }

    private fun getLocalVersionName(): String {
        return try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            Timber.w(e, "Cannot read local version name")
            "1.0.0"
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
        // SVOi: качаем APK через DownloadManager и сразу открываем installer (вместо браузера)
        try {
            val ctx = activity.applicationContext
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val downloadsDir = java.io.File(ctx.getExternalFilesDir(null), "downloads").apply { mkdirs() }
            val target = java.io.File(downloadsDir, "svoi-update.apk")
            if (target.exists()) target.delete()

            val req = android.app.DownloadManager.Request(Uri.parse(url))
                    .setTitle("СВОи — обновление")
                    .setDescription("Скачивание новой версии…")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(target))
                    .setMimeType("application/vnd.android.package-archive")

            val downloadId = dm.enqueue(req)

            // Слушаем завершение скачивания и запускаем установщик
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != downloadId) return
                    runCatching { c.unregisterReceiver(this) }
                    if (target.exists()) startInstall(target)
                    else Timber.w("SVOi: APK не скачался: %s", target.absolutePath)
                }
            }
            val filter = android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                ctx.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Timber.e(e, "DownloadManager failed, fallback to browser")
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e2: Exception) {
                Timber.e(e2, "Even browser fallback failed")
            }
        }
    }

    private fun startInstall(apkFile: java.io.File) {
        try {
            val ctx = activity.applicationContext
            val authority = "${ctx.packageName}.svoifileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "startInstall failed")
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
