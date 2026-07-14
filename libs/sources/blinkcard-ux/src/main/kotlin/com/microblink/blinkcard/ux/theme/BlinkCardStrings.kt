/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux.theme

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.microblink.blinkcard.ux.R
import kotlinx.parcelize.Parcelize

/**
 * Data class contains all the strings used throughout the SDK.
 * [Default] can be used to keep the original strings if only some of the elements are to be changed.
 *
 * This class shouldn't be modified, but rather a new instance should be
 * created and used in [com.microblink.ux.UiSettings.sdkStrings].
 *
 * @property blinkCardScanningStrings Strings that appear as instruction messages during the scanning session.
 *           These instructions are triggered by specific UX events and will appear on screen accordingly.
 *           Includes both BlinkCard specific and common SDK strings.
 * @property blinkCardHelpDialogsStrings Strings used in onboarding and help dialogs. These strings shouldn't
 *           be customized as they provide adequate instructions tailored specifically to our scanning experience.
 *           However, if the scanning experience is changed in any way, onboarding and help screen instructions
 *           may also be adjusted.
 * @property blinkCardAccessibilityStrings Strings that are used by accessibility TalkBack service for specific
 *           buttons, labels, and actions.
 */
@Immutable
@Parcelize
data class BlinkCardSdkStrings(
    val blinkCardScanningStrings: ScanningStrings,
    val blinkCardHelpDialogsStrings: HelpDialogsStrings,
    val blinkCardAccessibilityStrings: AccessibilityStrings
) : Parcelable, SdkStrings(
    blinkCardScanningStrings,
    blinkCardAccessibilityStrings
) {

    companion object {
        /**
         * Default onboarding and help dialog strings.
         *
         * Can be used as a base when customizing [blinkCardHelpDialogsStrings].
         */
        @JvmStatic
        val HelpDialogsDefaults = HelpDialogsStrings(
            onboardingTitle = R.string.mb_blinkcard_onboarding_dialog_title,
            onboardingMessage = R.string.mb_blinkcard_onboarding_dialog_message,
            helpTitles = listOf(
                R.string.mb_blinkcard_help_screen_title1,
                R.string.mb_blinkcard_help_screen_title2,
                R.string.mb_blinkcard_help_screen_title3,
                R.string.mb_blinkcard_help_screen_title4
            ),
            helpMessages = listOf(
                R.string.mb_blinkcard_help_screen_msg1,
                R.string.mb_blinkcard_help_screen_msg2,
                R.string.mb_blinkcard_help_screen_msg3,
                R.string.mb_blinkcard_help_screen_msg4
            )
        )

        @JvmStatic
        val Default: BlinkCardSdkStrings =
            BlinkCardSdkStrings(
                blinkCardScanningStrings = ScanningStrings.BlinkCardDefault,
                blinkCardHelpDialogsStrings = HelpDialogsDefaults,
                blinkCardAccessibilityStrings = AccessibilityStrings.Default
            )
    }

    init {
        LocalBaseSdkStrings = staticCompositionLocalOf { Default }
    }
}

val ScanningStrings.Companion.BlinkCardDefault: ScanningStrings
    get() = ScanningStrings(
        instructionsFirstSide = R.string.mb_blinkcard_first_side_instructions,
        instructionsSecondSide = R.string.mb_blinkcard_other_side_instructions,
        instructionsFlip = R.string.mb_blinkcard_scanning_wrong_side,
        instructionsNotFullyVisible = R.string.mb_blinkcard_card_not_fully_visible,
        instructionsTilted = R.string.mb_blinkcard_keep_card_parallel,
        instructionsScanningWrongSide = R.string.mb_blinkcard_scanning_wrong_side,
        instructionsBlurDetected = R.string.mb_blinkcard_blur_detected,
        instructionsMoveFarther = R.string.mb_blinkcard_move_farther,
        instructionsMoveCloser = R.string.mb_blinkcard_move_closer,
        snackbarFlashlightWarning = R.string.mb_blinkcard_flashlight_warning_message,
    )

var LocalBaseBlinkCardSdkStrings = staticCompositionLocalOf {
    BlinkCardSdkStrings.Default
}

