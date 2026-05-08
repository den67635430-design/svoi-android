/*
 * СВОи Subscription Manager
 */
package im.vector.app.features.svoi.subscription

import android.content.SharedPreferences
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.features.svoi.PremiumFeature
import im.vector.app.features.svoi.SubscriptionTier
import im.vector.app.features.svoi.SvoiConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    @DefaultPreferences private val prefs: SharedPreferences,
    private val testModeManager: TestModeManager,
) {
    fun getCurrentTier(): SubscriptionTier {
        if (isAdmin()) return SubscriptionTier.BUSINESS
        // SVOi: runtime-флаг из API (admin может toggle) заменяет compile-time SvoiConfig.DEMO_MODE
        if (testModeManager.isEnabled()) return SubscriptionTier.BUSINESS
        val tierName = prefs.getString(SvoiConfig.PREF_SUBSCRIPTION_TIER, SubscriptionTier.FREE.name)
        return runCatching { SubscriptionTier.valueOf(tierName!!) }.getOrDefault(SubscriptionTier.FREE)
    }

    fun isAdmin(): Boolean {
        val email = prefs.getString(SvoiConfig.PREF_USER_EMAIL, "") ?: ""
        return email.equals(SvoiConfig.ADMIN_EMAIL, ignoreCase = true)
    }

    fun hasAccess(feature: PremiumFeature): Boolean = getCurrentTier().hasAccess(feature)

    fun setUserEmail(email: String) {
        prefs.edit()
            .putString(SvoiConfig.PREF_USER_EMAIL, email)
            .putBoolean(SvoiConfig.PREF_IS_ADMIN, email.equals(SvoiConfig.ADMIN_EMAIL, ignoreCase = true))
            .apply()
    }

    fun activateSubscription(tier: SubscriptionTier, expiresAt: Long) {
        prefs.edit()
            .putString(SvoiConfig.PREF_SUBSCRIPTION_TIER, tier.name)
            .putLong(SvoiConfig.PREF_SUBSCRIPTION_EXPIRES, expiresAt)
            .apply()
    }

    fun isSubscriptionActive(): Boolean {
        if (isAdmin() || testModeManager.isEnabled()) return true
        return prefs.getLong(SvoiConfig.PREF_SUBSCRIPTION_EXPIRES, 0L) > System.currentTimeMillis()
    }
}
