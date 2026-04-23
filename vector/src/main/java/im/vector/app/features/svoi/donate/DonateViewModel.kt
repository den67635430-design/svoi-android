/*
 * СВОи Donate — ViewModel
 * Логика донатов: 10-10000 руб, QR, demo автоподтверждение
 */
package im.vector.app.features.svoi.donate

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.api.SvoiApiClient
import kotlinx.coroutines.launch
import timber.log.Timber

class DonateViewModel @AssistedInject constructor(
    @Assisted initialState: DonateViewState,
    private val apiClient: SvoiApiClient,
) : VectorViewModel<DonateViewState, DonateAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DonateViewModel, DonateViewState> {
        override fun create(initialState: DonateViewState): DonateViewModel
    }

    companion object : MavericksViewModelFactory<DonateViewModel, DonateViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: DonateAction) {
        when (action) {
            is DonateAction.SetAmount -> setState { copy(amountRub = action.amount.coerceIn(10, 10000)) }
            DonateAction.GenerateQr -> handleGenerateQr()
            DonateAction.SimulatePayment -> handleSimulate()
            DonateAction.Close -> setState { copy(qrCodeBase64 = null, orderId = null, isCompleted = false) }
        }
    }

    private fun handleGenerateQr() = withState { state ->
        if (!state.isValid) {
            setState { copy(errorMessage = "Сумма должна быть от 10 до 10000 руб") }
            return@withState
        }
        setState { copy(isProcessing = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val response = apiClient.createDonation("donor_${System.currentTimeMillis()}", state.amountRub)
                setState {
                    copy(
                        isProcessing = false,
                        qrCodeBase64 = response.qrImageBase64,
                        orderId = response.orderId,
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Donate QR generation failed")
                setState { copy(isProcessing = false, errorMessage = e.message) }
            }
        }
    }

    private fun handleSimulate() = withState { state ->
        val orderId = state.orderId ?: return@withState
        if (!SvoiConfig.DEMO_MODE) return@withState  // только в demo
        viewModelScope.launch {
            runCatching {
                apiClient.simulatePayment(orderId)
                setState { copy(isCompleted = true, qrCodeBase64 = null, orderId = null) }
            }.onFailure { e ->
                setState { copy(errorMessage = e.message) }
            }
        }
    }
}
