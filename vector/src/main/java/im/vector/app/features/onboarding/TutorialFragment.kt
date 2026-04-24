/*
 * СВОи Tutorial Fragment — overlay с 8 шагами
 */
package im.vector.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentTutorialBinding
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TutorialFragment : Fragment() {

    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TutorialViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tutorialNextButton.setOnClickListener { viewModel.nextStep() }
        binding.tutorialSkipButton.setOnClickListener { viewModel.skip() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: TutorialViewState) {
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

        val targetView = activity?.window?.decorView?.findViewWithTag<View>(step.targetTag)
        if (targetView == null) {
            Timber.w("Tutorial: target not found for %s", step.name)
            binding.tutorialArrow.isVisible = false
            return
        }
        try {
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)
            binding.tutorialArrow.x = location[0].toFloat() + targetView.width / 2f - binding.tutorialArrow.width / 2f
            binding.tutorialArrow.y = location[1].toFloat() - binding.tutorialArrow.height
            binding.tutorialArrow.isVisible = true
        } catch (e: Exception) {
            Timber.e(e, "Tutorial arrow position failed")
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
