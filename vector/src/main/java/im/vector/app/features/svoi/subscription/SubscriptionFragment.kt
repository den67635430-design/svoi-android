/*
 * СВОи Subscription — Fragment
 */
package im.vector.app.features.svoi.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentSubscriptionBinding
import im.vector.app.features.svoi.SubscriptionTier

@AndroidEntryPoint
class SubscriptionFragment : Fragment() {

    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionViewModel by fragmentViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tierFreeButton.setOnClickListener {
            viewModel.handle(SubscriptionAction.SelectTier(SubscriptionTier.FREE)); render()
        }
        binding.tierPremiumButton.setOnClickListener {
            viewModel.handle(SubscriptionAction.SelectTier(SubscriptionTier.PREMIUM)); render()
        }
        binding.tierBusinessButton.setOnClickListener {
            viewModel.handle(SubscriptionAction.SelectTier(SubscriptionTier.BUSINESS)); render()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() = withState(viewModel) { state ->
        binding.adminBadge.isVisible = state.isAdmin
        binding.demoBadge.isVisible = state.isDemoMode && !state.isAdmin
        binding.currentTierLabel.text = "Current: ${state.currentTier.displayName}"
        binding.progressBar.isVisible = state.isProcessing

        binding.qrContainer.isVisible = state.qrCodeBase64 != null
        state.qrCodeBase64?.let { base64 ->
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.qrImage.setImageBitmap(bitmap)
        }
        binding.simulatePaymentButton.isVisible = state.orderId != null && state.isDemoMode
        binding.simulatePaymentButton.setOnClickListener {
            state.orderId?.let { viewModel.handle(SubscriptionAction.SimulatePayment(it)); render() }
        }

        state.errorMessage?.let { binding.errorLabel.text = it }
        binding.errorLabel.isVisible = state.errorMessage != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SubscriptionFragment()
    }
}
