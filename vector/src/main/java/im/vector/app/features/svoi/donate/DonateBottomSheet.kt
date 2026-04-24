/*
 * СВОи Donate — Bottom Sheet
 */
package im.vector.app.features.svoi.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.BottomSheetDonateBinding

@AndroidEntryPoint
class DonateBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDonateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DonateViewModel by fragmentViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetDonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.amountSlider.addOnChangeListener { _, value, _ ->
            viewModel.handle(DonateAction.SetAmount(value.toInt()))
            render()
        }
        binding.generateQrButton.setOnClickListener {
            viewModel.handle(DonateAction.GenerateQr)
            render()
        }
        binding.simulateButton.setOnClickListener {
            viewModel.handle(DonateAction.SimulatePayment)
            render()
        }
        binding.closeButton.setOnClickListener { dismiss() }

        binding.amount100.setOnClickListener { binding.amountSlider.value = 100f }
        binding.amount500.setOnClickListener { binding.amountSlider.value = 500f }
        binding.amount1000.setOnClickListener { binding.amountSlider.value = 1000f }
        binding.amount5000.setOnClickListener { binding.amountSlider.value = 5000f }

        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() = withState(viewModel) { state ->
        binding.amountLabel.text = "${state.amountRub} RUB"
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
