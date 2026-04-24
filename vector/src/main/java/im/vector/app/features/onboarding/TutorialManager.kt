/*
 * СВОи TutorialManager
 */
package im.vector.app.features.onboarding

import android.content.SharedPreferences
import im.vector.app.core.di.DefaultPreferences
import javax.inject.Inject

class TutorialManager @Inject constructor(
    @DefaultPreferences private val prefs: SharedPreferences,
) {
    fun shouldShowTutorial(): Boolean = !prefs.getBoolean("svoi_tutorial_completed", false)

    fun markCompleted() {
        prefs.edit().putBoolean("svoi_tutorial_completed", true).apply()
    }
}
