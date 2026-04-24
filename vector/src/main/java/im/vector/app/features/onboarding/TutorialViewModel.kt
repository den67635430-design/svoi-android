/*
 * СВОи Tutorial — ViewModel
 */
package im.vector.app.features.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import im.vector.app.core.di.DefaultPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val PREF_COMPLETED = "svoi_tutorial_completed"
private const val PREF_STEP = "svoi_tutorial_current_step"

@HiltViewModel
class TutorialViewModel @Inject constructor(
    @DefaultPreferences private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(TutorialViewState())
    val state: StateFlow<TutorialViewState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch(Dispatchers.IO) {
        val completed = prefs.getBoolean(PREF_COMPLETED, false)
        if (completed) {
            _state.value = _state.value.copy(isCompleted = true, isVisible = false)
            return@launch
        }
        val stepName = prefs.getString(PREF_STEP, null)
        val step = stepName?.let { runCatching { TutorialStep.valueOf(it) }.getOrNull() } ?: TutorialStep.SEARCH
        _state.value = _state.value.copy(currentStep = step, isVisible = true)
        Timber.d("Tutorial loaded: step=$step")
    }

    fun nextStep() {
        val current = _state.value
        if (current.isLastStep) { complete() }
        else {
            val next = TutorialStep.STEPS[current.stepIndex + 1]
            _state.value = current.copy(currentStep = next)
            save(next)
        }
    }

    fun skip() = complete()

    fun replay() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean(PREF_COMPLETED, false).putString(PREF_STEP, TutorialStep.SEARCH.name).apply()
        }
        _state.value = TutorialViewState(currentStep = TutorialStep.SEARCH, isVisible = true)
    }

    private fun complete() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean(PREF_COMPLETED, true).remove(PREF_STEP).apply()
        }
        _state.value = _state.value.copy(isCompleted = true, isVisible = false)
    }

    private fun save(step: TutorialStep) {
        viewModelScope.launch(Dispatchers.IO) { prefs.edit().putString(PREF_STEP, step.name).apply() }
    }
}
