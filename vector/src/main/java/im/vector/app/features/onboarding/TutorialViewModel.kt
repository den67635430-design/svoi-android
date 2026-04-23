/*
 * СВОи Мессенджер — Onboarding Tutorial ViewModel
 */
package im.vector.app.features.onboarding

import android.content.SharedPreferences
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.EmptyViewEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Named

private const val PREF_TUTORIAL_COMPLETED = "svoi_tutorial_completed"
private const val PREF_TUTORIAL_CURRENT_STEP = "svoi_tutorial_current_step"

class TutorialViewModel @AssistedInject constructor(
    @Assisted initialState: TutorialViewState,
    @Named("VectorPreferences") private val prefs: SharedPreferences,
) : VectorViewModel<TutorialViewState, TutorialAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<TutorialViewModel, TutorialViewState> {
        override fun create(initialState: TutorialViewState): TutorialViewModel
    }

    companion object : MavericksViewModelFactory<TutorialViewModel, TutorialViewState> by hiltMavericksViewModelFactory()

    init {
        loadState()
    }

    private fun loadState() = viewModelScope.launch(Dispatchers.IO) {
        val completed = prefs.getBoolean(PREF_TUTORIAL_COMPLETED, false)
        if (completed) {
            setState { copy(isCompleted = true, isVisible = false) }
            return@launch
        }
        // Восстановление шага после краша
        val stepName = prefs.getString(PREF_TUTORIAL_CURRENT_STEP, null)
        val step = stepName?.let {
            runCatching { TutorialStep.valueOf(it) }.getOrNull()
        } ?: TutorialStep.SEARCH

        setState { copy(currentStep = step, isVisible = true) }
        Timber.d("Tutorial loaded: step=$step, completed=$completed")
    }

    override fun handle(action: TutorialAction) {
        when (action) {
            TutorialAction.NextStep -> handleNextStep()
            TutorialAction.Skip -> handleSkip()
            TutorialAction.Replay -> handleReplay()
        }
    }

    private fun handleNextStep() = withState { state ->
        if (state.isLastStep) {
            completeTutorial()
        } else {
            val nextStep = TutorialStep.STEPS[state.stepIndex + 1]
            setState { copy(currentStep = nextStep) }
            saveCurrentStep(nextStep)
        }
    }

    private fun handleSkip() {
        completeTutorial()
    }

    private fun handleReplay() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit()
                .putBoolean(PREF_TUTORIAL_COMPLETED, false)
                .putString(PREF_TUTORIAL_CURRENT_STEP, TutorialStep.SEARCH.name)
                .apply()
        }
        setState { copy(currentStep = TutorialStep.SEARCH, isCompleted = false, isVisible = true) }
    }

    private fun completeTutorial() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit()
                .putBoolean(PREF_TUTORIAL_COMPLETED, true)
                .remove(PREF_TUTORIAL_CURRENT_STEP)
                .apply()
        }
        setState { copy(isCompleted = true, isVisible = false) }
    }

    private fun saveCurrentStep(step: TutorialStep) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString(PREF_TUTORIAL_CURRENT_STEP, step.name).apply()
        }
    }
}
