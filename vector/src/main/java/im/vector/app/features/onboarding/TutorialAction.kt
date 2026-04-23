/*
 * СВОи Мессенджер — Onboarding Tutorial Actions
 */
package im.vector.app.features.onboarding

import im.vector.app.core.platform.VectorViewModelAction

sealed class TutorialAction : VectorViewModelAction {
    object NextStep : TutorialAction()
    object Skip : TutorialAction()
    object Replay : TutorialAction()
}
