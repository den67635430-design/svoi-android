/*
 * СВОи Donate — ViewState
 */
package im.vector.app.features.svoi.donate

import com.airbnb.mvrx.MavericksState

data class DonateViewState(
    val amountRub: Int = 100,
    val qrCodeBase64: String? = null,
    val orderId: String? = null,
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
) : MavericksState {
    val isValid: Boolean get() = amountRub in 10..10000
}
