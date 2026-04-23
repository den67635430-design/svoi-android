/*
 * СВОи Subscription — ViewState
 */
package im.vector.app.features.svoi.subscription

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.svoi.SubscriptionTier

data class SubscriptionViewState(
    val currentTier: SubscriptionTier = SubscriptionTier.FREE,
    val isAdmin: Boolean = false,
    val isDemoMode: Boolean = true,
    val isProcessing: Boolean = false,
    val qrCodeBase64: String? = null,
    val orderId: String? = null,
    val errorMessage: String? = null,
) : MavericksState
