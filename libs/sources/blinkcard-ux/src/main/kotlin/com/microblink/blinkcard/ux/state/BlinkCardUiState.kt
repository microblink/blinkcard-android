/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux.state

import com.microblink.blinkcard.core.session.BlinkCardScanningResult
import com.microblink.blinkcard.ux.DefaultShowHelpButton
import com.microblink.blinkcard.ux.DefaultShowOnboardingDialog
import com.microblink.blinkcard.ux.state.BaseUiState
import com.microblink.blinkcard.ux.state.CancelRequestState
import com.microblink.blinkcard.ux.state.CardAnimationState
import com.microblink.blinkcard.ux.state.CommonStatusMessage
import com.microblink.blinkcard.ux.state.ErrorState
import com.microblink.blinkcard.ux.state.HapticFeedbackState
import com.microblink.blinkcard.ux.state.ScanSoundState
import com.microblink.blinkcard.ux.state.MbTorchState
import com.microblink.blinkcard.ux.state.ProcessingState
import com.microblink.blinkcard.ux.state.ReticleState
import com.microblink.blinkcard.ux.state.StatusMessage
import com.microblink.blinkcard.ux.state.UiScanningSide

data class BlinkCardUiState(
    val blinkCardScanningResult: BlinkCardScanningResult? = null,
    override val reticleState: ReticleState = ReticleState.Hidden,
    override val processingState: ProcessingState = ProcessingState.Sensing,
    override val cardAnimationState: CardAnimationState = CardAnimationState.Hidden,
    override val statusMessage: StatusMessage = CommonStatusMessage.ScanFirstSide,
    override val currentSide: UiScanningSide = UiScanningSide.First,
    override val torchState: MbTorchState = MbTorchState.Off,
    override val cancelRequestState: CancelRequestState = CancelRequestState.CancelNotRequested,
    override val helpButtonDisplayed: Boolean = DefaultShowHelpButton,
    override val helpDisplayed: Boolean = false,
    override val helpTooltipDisplayed: Boolean = false,
    override val onboardingDialogDisplayed: Boolean = DefaultShowOnboardingDialog,
    override val errorState: ErrorState = ErrorState.NoError,
    override val hapticFeedbackState: HapticFeedbackState = HapticFeedbackState.VibrationOff,
    override val scanSoundState: ScanSoundState = ScanSoundState.SoundOff
) : BaseUiState

