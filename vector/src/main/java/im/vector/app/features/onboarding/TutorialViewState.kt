/*
 * СВОи Tutorial — ViewState
 */
package im.vector.app.features.onboarding

data class TutorialViewState(
    val currentStep: TutorialStep = TutorialStep.SEARCH,
    val isCompleted: Boolean = false,
    val isVisible: Boolean = false,
) {
    val stepIndex: Int get() = TutorialStep.STEPS.indexOf(currentStep)
    val isLastStep: Boolean get() = stepIndex == TutorialStep.TOTAL - 1
    val progress: String get() = "${stepIndex + 1}/${TutorialStep.TOTAL}"
}
