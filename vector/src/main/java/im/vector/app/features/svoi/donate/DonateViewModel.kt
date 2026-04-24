package im.vector.app.features.svoi.donate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.api.SvoiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DonateViewModel @Inject constructor(
    private val apiClient: SvoiApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(DonateViewState())
    val state: StateFlow<DonateViewState> = _state.asStateFlow()

    fun setAmount(amount: Int) {
        _state.value = _state.value.copy(amountRub = amount.coerceIn(10, 10000))
    }

    fun generateQr() {
        val s = _state.value
        if (!s.isValid) {
            _state.value = s.copy(errorMessage = "Amount must be between 10 and 10000 RUB")
            return
        }
        _state.value = s.copy(isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val resp = apiClient.createDonation("donor_${System.currentTimeMillis()}", s.amountRub)
                _state.value = _state.value.copy(
                    isProcessing = false,
                    qrCodeBase64 = resp.qrImageBase64,
                    orderId = resp.orderId,
                )
            }.onFailure { e ->
                Timber.e(e, "donate QR failed")
                _state.value = _state.value.copy(isProcessing = false, errorMessage = e.message)
            }
        }
    }

    fun simulatePayment() {
        val orderId = _state.value.orderId ?: return
        if (!SvoiConfig.DEMO_MODE) return
        viewModelScope.launch {
            runCatching {
                apiClient.simulatePayment(orderId)
                _state.value = _state.value.copy(isCompleted = true, qrCodeBase64 = null, orderId = null)
            }.onFailure { e ->
                _state.value = _state.value.copy(errorMessage = e.message)
            }
        }
    }

    fun close() {
        _state.value = DonateViewState()
    }
}
