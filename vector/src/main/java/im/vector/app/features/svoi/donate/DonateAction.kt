/*
 * СВОи Donate — Actions
 */
package im.vector.app.features.svoi.donate

import im.vector.app.core.platform.VectorViewModelAction

sealed class DonateAction : VectorViewModelAction {
    data class SetAmount(val amount: Int) : DonateAction()
    object GenerateQr : DonateAction()
    object SimulatePayment : DonateAction()
    object Close : DonateAction()
}
