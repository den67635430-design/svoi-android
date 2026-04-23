/*
 * СВОи Мессенджер — TutorialManager
 * Управляет показом tutorial после первого входа
 */
package im.vector.app.features.onboarding

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named

class TutorialManager @Inject constructor(
    @Named("VectorPreferences") private val prefs: SharedPreferences,
) {
    fun shouldShowTutorial(): Boolean {
        return !prefs.getBoolean("svoi_tutorial_completed", false)
    }

    fun markCompleted() {
        prefs.edit().putBoolean("svoi_tutorial_completed", true).apply()
    }
}
