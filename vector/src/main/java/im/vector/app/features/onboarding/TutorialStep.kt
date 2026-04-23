/*
 * СВОи Мессенджер — Onboarding Tutorial
 * 8 шагов обучения по функциям мессенджера
 */
package im.vector.app.features.onboarding

import androidx.annotation.StringRes
import im.vector.lib.strings.CommonStrings

enum class TutorialStep(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val targetTag: String,          // accessibility tag целевого элемента
) {
    SEARCH(
        titleRes = CommonStrings.tutorial_step_1_title,
        descRes = CommonStrings.tutorial_step_1_desc,
        targetTag = "tutorial_target_search",
    ),
    DIRECT_CHAT(
        titleRes = CommonStrings.tutorial_step_2_title,
        descRes = CommonStrings.tutorial_step_2_desc,
        targetTag = "tutorial_target_direct_chat",
    ),
    GROUP(
        titleRes = CommonStrings.tutorial_step_3_title,
        descRes = CommonStrings.tutorial_step_3_desc,
        targetTag = "tutorial_target_group",
    ),
    VIDEO_CALL(
        titleRes = CommonStrings.tutorial_step_4_title,
        descRes = CommonStrings.tutorial_step_4_desc,
        targetTag = "tutorial_target_video_call",
    ),
    FILES(
        titleRes = CommonStrings.tutorial_step_5_title,
        descRes = CommonStrings.tutorial_step_5_desc,
        targetTag = "tutorial_target_files",
    ),
    STICKERS(
        titleRes = CommonStrings.tutorial_step_6_title,
        descRes = CommonStrings.tutorial_step_6_desc,
        targetTag = "tutorial_target_stickers",
    ),
    SETTINGS(
        titleRes = CommonStrings.tutorial_step_7_title,
        descRes = CommonStrings.tutorial_step_7_desc,
        targetTag = "tutorial_target_settings",
    ),
    BLOCK_MUTE(
        titleRes = CommonStrings.tutorial_step_8_title,
        descRes = CommonStrings.tutorial_step_8_desc,
        targetTag = "tutorial_target_block_mute",
    );

    companion object {
        val STEPS = values().toList()
        val TOTAL = STEPS.size
    }
}
