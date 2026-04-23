/*
 * СВОи Subscription — ViewModel
 * Управляет логикой подписок: показ цен, покупка, demo автоподтверждение
 */
package im.vector.app.features.svoi.subscription

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.svoi.SubscriptionTier
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.api.SvoiApiClient
import kotlinx.coroutines.launch
import timber.log.Timber

class SubscriptionViewModel @AssistedInject constructor(
    @Assisted initialState: SubscriptionViewState,
    private val subscriptionManager: SubscriptionManager,
    private val apiClient: SvoiApiClient,
) : VectorViewModel<SubscriptionViewState, SubscriptionAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SubscriptionViewModel, SubscriptionViewState> {
        override fun create(initialState: SubscriptionViewState): SubscriptionViewModel
    }

    companion object : MavericksViewModelFactory<SubscriptionViewModel, SubscriptionViewState> by hiltMavericksViewModelFactory()

    init {
        setState {
            copy(
                currentTier = subscriptionManager.getCurrentTier(),
                isAdmin = subscriptionManager.isAdmin(),
                isDemoMode = SvoiConfig.DEMO_MODE,
            )
        }
    }

    override fun handle(action: SubscriptionAction) {
        when (action) {
            is SubscriptionAction.SelectTier -> handleSelectTier(action.tier)
            is SubscriptionAction.SimulatePayment -> handleSimulate(action.orderId)
            SubscriptionAction.ClearError -> setState { copy(errorMessage = null) }
        }
    }

    private fun handleSelectTier(tier: SubscriptionTier) = viewModelScope.launch {
        // Admin / Demo → автоматическая активация
        if (subscriptionManager.isAdmin() || SvoiConfig.DEMO_MODE) {
            val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            subscriptionManager.activateSubscription(tier, expiresAt)
            setState { copy(currentTier = tier, errorMessage = null) }
            Timber.i("Subscription ${tier.name} activated (admin/demo mode)")
            return@launch
        }

        // Real mode → запрашиваем QR у backend
        setState { copy(isProcessing = true) }
        runCatching {
            val userId = subscriptionManager.let { it::class.simpleName } ?: "unknown"
            val response = apiClient.createPayment(userId, tier.price)
            setState {
                copy(
                    isProcessing = false,
                    qrCodeBase64 = response.qrImageBase64,
                    orderId = response.orderId,
                )
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to create payment")
            setState { copy(isProcessing = false, errorMessage = e.message) }
        }
    }

    private fun handleSimulate(orderId: String) = viewModelScope.launch {
        runCatching {
            apiClient.simulatePayment(orderId)
            val tier = awaitState().currentTier
            val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            subscriptionManager.activateSubscription(tier, expiresAt)
            setState { copy(currentTier = tier, qrCodeBase64 = null, orderId = null) }
        }.onFailure { e ->
            Timber.e(e, "Failed to simulate payment")
            setState { copy(errorMessage = e.message) }
        }
    }
}
