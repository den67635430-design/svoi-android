/*
 * СВОи Subscription — Actions
 */
package im.vector.app.features.svoi.subscription

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.svoi.SubscriptionTier

sealed class SubscriptionAction : VectorViewModelAction {
    data class SelectTier(val tier: SubscriptionTier) : SubscriptionAction()
    data class SimulatePayment(val orderId: String) : SubscriptionAction()
    object ClearError : SubscriptionAction()
}
