/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux.utils

import androidx.compose.runtime.Composable
import com.microblink.blinkcard.ux.R
import com.microblink.blinkcard.ux.components.ErrorDialog
import com.microblink.blinkcard.ux.components.HelpScreenPage
import com.microblink.blinkcard.ux.components.HelpScreens
import com.microblink.blinkcard.ux.state.ErrorState
import com.microblink.blinkcard.ux.theme.BlinkCardTheme

@Composable
fun fillHelpScreensBlinkCard(): HelpScreens {
    val helpDialogStrings = BlinkCardTheme.sdkStrings.blinkCardHelpDialogsStrings
    return HelpScreens(
        onboardingDialogPage = HelpScreenPage(
            pageImage = R.drawable.mb_blinkcard_onboarding,
            pageTitle = helpDialogStrings.onboardingTitle,
            pageMessage = helpDialogStrings.onboardingMessage,
        ),
        helpDialogPages = listOf(
            HelpScreenPage(
                pageImage = R.drawable.mb_blinkcard_help_page_one,
                pageTitle = helpDialogStrings.helpTitles[0],
                pageMessage = helpDialogStrings.helpMessages[0]
            ), HelpScreenPage(
                pageImage = R.drawable.mb_blinkcard_help_page_two,
                pageTitle = helpDialogStrings.helpTitles[1],
                pageMessage = helpDialogStrings.helpMessages[1]
            ), HelpScreenPage(
                pageImage = R.drawable.mb_blinkcard_help_page_three,
                pageTitle = helpDialogStrings.helpTitles[2],
                pageMessage = helpDialogStrings.helpMessages[2]
            ), HelpScreenPage(
                pageImage = R.drawable.mb_blinkcard_help_page_four,
                pageTitle = helpDialogStrings.helpTitles[3],
                pageMessage = helpDialogStrings.helpMessages[3]
            )
        )
    )
}

@Composable
fun fillErrorDialogsBlinkCard(
    onRetry: () -> Unit,
    onDoneError: () -> Unit
): Map<ErrorState, @Composable () -> Unit> {
    return mapOf(
        ErrorState.NoError to {},
        ErrorState.ErrorInvalidLicense to
                {
                    ErrorDialog(
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_license_locked,
                        null,
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_close,
                        onButtonClick = onDoneError
                    )
                },

        ErrorState.ErrorNetworkError to
                {
                    ErrorDialog(
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_license_locked,
                        null,
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_close,
                        onButtonClick = onDoneError
                    )
                },

        ErrorState.ErrorTimeoutExpired to
                {
                    ErrorDialog(
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_recognition_timeout_dialog_title,
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_recognition_timeout_dialog_message,
                        com.microblink.blinkcard.ux.R.string.mb_blinkcard_recognition_timeout_dialog_retry_button,
                        onButtonClick = onRetry
                    )
                },

        ErrorState.ErrorDocumentClassFiltered to
                {
                }
    )
}