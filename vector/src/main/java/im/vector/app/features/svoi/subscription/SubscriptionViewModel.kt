/*
 * СВОи Subscription — ViewModel (Hilt, без Mavericks)
 */
package im.vector.app.features.svoi.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import im.vector.app.features.svoi.SubscriptionTier
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.api.SvoiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val apiClient: SvoiApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionViewState())
    val state: StateFlow<SubscriptionViewState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            currentTier = subscriptionManager.getCurrentTier(),
            isAdmin = subscriptionManager.isAdmin(),
            isDemoMode = SvoiConfig.DEMO_MODE,
        )
    }

    fun selectTier(tier: SubscriptionTier) {
        if (subscriptionManager.isAdmin() || SvoiConfig.DEMO_MODE) {
            val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            subscriptionManager.activateSubscription(tier, expiresAt)
            _state.value = _state.value.copy(currentTier = tier, errorMessage = null)
            Timber.i("Subscription ${tier.name} activated (demo/admin)")
            return
        }
        _state.value = _state.value.copy(isProcessing = true)
        viewModelScope.launch {
            runCatching {
                val userId = "user_${System.currentTimeMillis()}"
                val resp = apiClient.createPayment(userId, tier.price)
                _state.value = _state.value.copy(
                    isProcessing = false,
                    qrCodeBase64 = resp.qrImageBase64,
                    orderId = resp.orderId,
                    currentTier = tier,
                )
            }.onFailure { e ->
                Timber.e(e, "createPayment failed")
                _state.value = _state.value.copy(isProcessing = false, errorMessage = e.message)
            }
        }
    }

    fun simulatePayment(orderId: String) {
        viewModelScope.launch {
            runCatching {
                apiClient.simulatePayment(orderId)
                val tier = _state.value.currentTier
                val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                subscriptionManager.activateSubscription(tier, expiresAt)
                _state.value = _state.value.copy(qrCodeBase64 = null, orderId = null)
            }.onFailure { e ->
                _state.value = _state.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
