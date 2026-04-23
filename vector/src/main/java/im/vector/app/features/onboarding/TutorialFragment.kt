/*
 * СВОи Мессенджер — Onboarding Tutorial Fragment
 * Overlay поверх главного экрана с пошаговым обучением
 */
package im.vector.app.features.onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.databinding.FragmentTutorialBinding
import timber.log.Timber

@AndroidEntryPoint
class TutorialFragment : Fragment() {

    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TutorialViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        viewModel.onEach { state -> renderState(state) }
    }

    private fun setupClickListeners() {
        binding.tutorialNextButton.setOnClickListener {
            viewModel.handle(TutorialAction.NextStep)
        }
        binding.tutorialSkipButton.setOnClickListener {
            viewModel.handle(TutorialAction.Skip)
        }
        binding.tutorialOverlay.setOnClickListener {
            // Блокируем прокидывание тапов на фон
        }
    }

    private fun renderState(state: TutorialViewState) {
        if (!state.isVisible || state.isCompleted) {
            view?.isVisible = false
            return
        }
        view?.isVisible = true

        val step = state.currentStep
        binding.tutorialTitle.setText(step.titleRes)
        binding.tutorialDescription.setText(step.descRes)
        binding.tutorialProgress.text = state.progress
        binding.tutorialNextButton.setText(
            if (state.isLastStep) im.vector.lib.strings.CommonStrings.tutorial_finish
            else im.vector.lib.strings.CommonStrings.tutorial_next
        )

        // Позиционирование стрелки на целевой элемент
        positionArrow(step)
    }

    private fun positionArrow(step: TutorialStep) {
        val targetView = activity?.window?.decorView?.findViewWithTag<View>(step.targetTag)
        if (targetView == null) {
            Timber.w("Tutorial: target element not found for step %s — skipping highlight", step.name)
            binding.tutorialArrow.isVisible = false
            return
        }
        try {
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)
            val targetX = location[0].toFloat() + targetView.width / 2f
            val targetY = location[1].toFloat() - binding.tutorialArrow.height

            binding.tutorialArrow.x = targetX - binding.tutorialArrow.width / 2f
            binding.tutorialArrow.y = targetY
            binding.tutorialArrow.isVisible = true
        } catch (e: Exception) {
            Timber.e(e, "Tutorial: failed to position arrow for step %s", step.name)
            binding.tutorialArrow.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TutorialFragment()
    }
}
