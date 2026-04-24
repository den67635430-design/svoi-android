package im.vector.app.features.svoi.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.BottomSheetDonateBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DonateBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDonateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DonateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetDonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.amountSlider.addOnChangeListener { _, value, _ -> viewModel.setAmount(value.toInt()) }
        binding.generateQrButton.setOnClickListener { viewModel.generateQr() }
        binding.simulateButton.setOnClickListener { viewModel.simulatePayment() }
        binding.closeButton.setOnClickListener { dismiss() }

        binding.amount100.setOnClickListener { binding.amountSlider.value = 100f }
        binding.amount500.setOnClickListener { binding.amountSlider.value = 500f }
        binding.amount1000.setOnClickListener { binding.amountSlider.value = 1000f }
        binding.amount5000.setOnClickListener { binding.amountSlider.value = 5000f }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: DonateViewState) {
        binding.amountLabel.text = state.amountRub.toString() + " RUB"
        binding.progressBar.isVisible = state.isProcessing

        binding.qrContainer.isVisible = state.qrCodeBase64 != null
        state.qrCodeBase64?.let { base64 ->
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.qrImage.setImageBitmap(bitmap)
        }
        binding.simulateButton.isVisible = state.orderId != null
        binding.amountSelector.isVisible = state.qrCodeBase64 == null && !state.isCompleted

        binding.thankYou.isVisible = state.isCompleted
        binding.errorLabel.isVisible = state.errorMessage != null
        state.errorMessage?.let { binding.errorLabel.text = it }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DonateBottomSheet"
        fun newInstance() = DonateBottomSheet()
    }
}
