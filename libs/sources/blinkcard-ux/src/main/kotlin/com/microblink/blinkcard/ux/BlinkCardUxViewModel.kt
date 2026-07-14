/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux

import android.content.Context
import android.os.CountDownTimer
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.microblink.blinkcard.core.BlinkCardSdk
import com.microblink.blinkcard.core.ping.config.PingSendTriggerPoint
import com.microblink.blinkcard.core.ping.pinglets.UxEvent
import com.microblink.blinkcard.core.session.BlinkCardScanningResult
import com.microblink.blinkcard.core.session.BlinkCardSessionSettings
import com.microblink.blinkcard.core.utils.sendPingletsIfAllowed
import com.microblink.blinkcard.ux.camera.CameraHardwareInfoHelper
import com.microblink.blinkcard.ux.camera.CameraInputDetails
import com.microblink.blinkcard.ux.camera.CameraViewModel
import com.microblink.blinkcard.ux.camera.TimeoutCause
import com.microblink.blinkcard.ux.components.needHelpTooltipDefaultTimeToAppearMs
import com.microblink.blinkcard.ux.components.uiCountingWindowDurationMs
import com.microblink.blinkcard.ux.scanning.BlinkCardAnalyzer
import com.microblink.blinkcard.ux.scanning.BlinkCardScanningDoneHandler
import com.microblink.blinkcard.ux.scanning.BlinkCardScanningUxEvent
import com.microblink.blinkcard.ux.scanning.BlinkCardScanningUxEventHandler
import com.microblink.blinkcard.ux.settings.BlinkCardUxSettings
import com.microblink.blinkcard.ux.state.BlinkCardUiState
import com.microblink.blinkcard.ux.state.CardAnimationState
import com.microblink.blinkcard.ux.state.CommonStatusMessage
import com.microblink.blinkcard.ux.state.ErrorState
import com.microblink.blinkcard.ux.state.HapticFeedbackState
import com.microblink.blinkcard.ux.state.ScanSoundState
import com.microblink.blinkcard.ux.state.MbTorchState
import com.microblink.blinkcard.ux.state.ProcessingState
import com.microblink.blinkcard.ux.state.ReticleState
import com.microblink.blinkcard.ux.state.StatusMessage
import com.microblink.blinkcard.ux.state.StatusMessageCounter
import com.microblink.blinkcard.ux.state.UiScanningSide
import com.microblink.blinkcard.ux.state.UiScanningSide.First
import com.microblink.blinkcard.ux.state.UiScanningSide.Second
import com.microblink.blinkcard.ux.utils.ErrorReason
import com.microblink.blinkcard.ux.utils.ScreenOrientation
import com.microblink.blinkcard.ux.utils.UxPingletTracker
import com.microblink.blinkcard.ux.utils.pingletOrientationDelayMs
import com.microblink.blinkcard.ux.utils.toErrorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class BlinkCardUxViewModel(
    blinkCardSdkInstance: BlinkCardSdk,
    sessionSettings: BlinkCardSessionSettings,
    uxSettings: BlinkCardUxSettings
) : CameraViewModel() {
    private var imageAnalyzer: BlinkCardAnalyzer? = null

    private var firstImageTimestamp: Long? = null
    private val stepTimeoutDuration: Duration? =
        if (uxSettings.stepTimeoutDuration == Duration.ZERO) null else uxSettings.stepTimeoutDuration

    private val _uiState = MutableStateFlow(BlinkCardUiState())
    val uiState: StateFlow<BlinkCardUiState> = _uiState.asStateFlow()

    var uiStateStartTime: Duration = Duration.ZERO
    val countingWindowDuration: Duration = uiCountingWindowDurationMs.milliseconds
    private var lastScreenOrientationPingTime: Long = 0L

    private val statusCounter: StatusMessageCounter = StatusMessageCounter()
    private val appearanceCounter: StatusMessageCounter = StatusMessageCounter()

    private var isCountingActive: Boolean = true

    private var currentScreenOrientation: ScreenOrientation? = null

    private var lastTrackedErrorType: UxEvent.ErrorMessageType? = null

    private var cameraHardwareInfoReported = false

    val helpTooltipTimeToDisplayInMs =
        if (stepTimeoutDuration == null) {
            needHelpTooltipDefaultTimeToAppearMs
        } else {
            uxSettings.stepTimeoutDuration.inWholeMilliseconds / 2
        }

    private val helpTooltipTimer =
        object : CountDownTimer(helpTooltipTimeToDisplayInMs, helpTooltipTimeToDisplayInMs) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                UxPingletTracker.UxEvent.trackSimpleEvent(
                    UxPingletTracker.UxEvent.SimpleUxEventType.HelpTooltipDisplayed,
                    getSessionNumber()
                )
                changeHelpTooltipVisibility(true)
            }

        }

    init {
        imageAnalyzer = BlinkCardAnalyzer(
            blinkCardSdk = blinkCardSdkInstance,
            sessionSettings = sessionSettings,
            scanningDoneHandler = object : BlinkCardScanningDoneHandler {
                override fun onScanningFinished(result: BlinkCardScanningResult) {
                    _uiState.update {
                        it.copy(blinkCardScanningResult = result)
                    }
                }

                override fun onError(error: ErrorReason) {
                    lifecyclePauseAnalysis()
                    appearanceCounter.reset()
                    UxPingletTracker.UxEvent.trackAlertDisplayedEvent(
                        alertType = when (error) {
                            ErrorReason.ErrorInvalidLicense -> UxEvent.AlertType.INVALIDLICENSEKEY
                            ErrorReason.ErrorStepTimeoutExpired -> UxEvent.AlertType.STEPTIMEOUT
                            ErrorReason.ErrorInactivityTimeoutExpired -> UxEvent.AlertType.INACTIVITYTIMEOUT
                            ErrorReason.ErrorNetworkError -> UxEvent.AlertType.NETWORKERROR
                            ErrorReason.ErrorDocumentClassFiltered -> UxEvent.AlertType.DOCUMENTCLASSNOTALLOWED
                            // TODO: add new AlertTypes
                            ErrorReason.ErrorSettingsValidationFailed -> UxEvent.AlertType.NETWORKERROR
                            ErrorReason.ErrorGetResultFailed -> UxEvent.AlertType.NETWORKERROR

                        },
                        sessionNumber = getSessionNumber()
                    )
                    _uiState.update {
                        it.copy(
                            errorState = error.toErrorState(),
                            processingState = ProcessingState.ErrorDialog,
                            hapticFeedbackState = HapticFeedbackState.VibrationOneTimeLong,
                        )
                    }
                }

                override fun onScanningCanceled() {}
            },
            uxEventHandler = object : BlinkCardScanningUxEventHandler {
                override fun onUxEvents(events: List<BlinkCardScanningUxEvent>) {
                    var newStatusMessage: StatusMessage? = null
                    var newProcessingState: ProcessingState? = null
                    var newCurrentSide: UiScanningSide? = null
                    stepTimeoutDuration?.let {
                        if (firstImageTimestamp == null && isCountingActive) {
                            firstImageTimestamp = System.nanoTime()
                        }
                        if (isCountingActive) {
                            firstImageTimestamp?.let { timestamp ->
                                val currentDuration =
                                    (System.nanoTime() - timestamp).toDuration(DurationUnit.NANOSECONDS)
                                if (currentDuration > stepTimeoutDuration) {
                                    imageAnalyzer?.timeoutAnalysis(TimeoutCause.Step)
                                    firstImageTimestamp = null
                                }
                            }
                        }
                    }
                    for (event in events) {
                        when (event) {
                            is BlinkCardScanningUxEvent.ScanningDone -> {
                                lifecyclePauseAnalysis()
                                newStatusMessage = CommonStatusMessage.Empty
                                newProcessingState = ProcessingState.SuccessAnimation(false)
                            }

                            is BlinkCardScanningUxEvent.CardNotFound -> {
                                newProcessingState = ProcessingState.Sensing

                                when (uiState.value.currentSide) {
                                    First -> newStatusMessage = CommonStatusMessage.ScanFirstSide
                                    Second -> newStatusMessage = CommonStatusMessage.ScanSecondSide
                                    else -> {}
                                }
                            }

                            is BlinkCardScanningUxEvent.CardLocated, is BlinkCardScanningUxEvent.CardLocatedLocation -> {
                                // newProcessingState = ProcessingState.Processing
                                // Not used in this UI implementation.
                                // Can be used for processing state.
                            }

                            is BlinkCardScanningUxEvent.BlurDetected -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.EliminateBlur
                            }

                            is BlinkCardScanningUxEvent.CardNotFullyVisible -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.KeepVisible
                            }

                            is BlinkCardScanningUxEvent.CardTooClose -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.MoveFarther
                            }

                            is BlinkCardScanningUxEvent.CardTooFar -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.MoveCloser
                            }

                            is BlinkCardScanningUxEvent.CardTooTilted -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.Align
                            }

                            is BlinkCardScanningUxEvent.ScanningWrongSide -> {
                                newProcessingState = ProcessingState.Error
                                newStatusMessage = CommonStatusMessage.ScanningWrongSide
                            }


                            is BlinkCardScanningUxEvent.RequestSide -> {
                                if (uiState.value.currentSide == First && event.side == Second) {
                                    // TODO: support for portrait animations
                                    newProcessingState = ProcessingState.SuccessAnimation(true)
                                    newStatusMessage = CommonStatusMessage.Empty
                                    lifecyclePauseAnalysis()
                                }
                            }

                            is BlinkCardScanningUxEvent.CardProcessResult -> {
                                // Not used in this UI implementation.
                                // Can be used for additional frame debugging.
                            }

                        }
                    }

                    updateUiState(
                        newProcessingState,
                        newStatusMessage,
                        newCurrentSide
                    )
                }
            }
        )

        viewModelScope.launch {
            isTorchSupported.collect { isTorchSupported ->
                _uiState.update {
                    it.copy(
                        torchState = if (isTorchSupported) MbTorchState.Off else MbTorchState.NotSupportedByCamera
                    )
                }
            }
        }
    }

    private fun updateUiState(
        newProcessingState: ProcessingState?,
        newStatusMessage: StatusMessage?,
        newCurrentSide: UiScanningSide?
    ) {
        newProcessingState?.let {
            if (newProcessingState is ProcessingState.SuccessAnimation) {
                isCountingActive = false
                runBlocking {
                    waitForMinimumStateDuration(newProcessingState)
                }
            } else if (isCountingActive || shouldStartCounting(uiState.value.processingState)) {
                newStatusMessage?.let {
                    statusCounter.increment(it)
                }
            }

            newStatusMessage?.let {
                val stateRemDur = remainingStateDuration(
                    uiState.value.processingState,
                    newProcessingState,
                    newStatusMessage
                )
                if (stateRemDur <= Duration.ZERO || newProcessingState is ProcessingState.SuccessAnimation) {
                    val (selectedProcessingState, selectedStatusMessage) =
                        if (newProcessingState.reticleState == ReticleState.Success || newProcessingState.reticleState == ReticleState.SuccessFirstSide) {
                            Pair(
                                newProcessingState,
                                newStatusMessage
                            )
                        } else pickNewState()
                    selectedProcessingState?.let {
                        val newHapticFeedbackState = when (selectedProcessingState) {
                            ProcessingState.Error -> HapticFeedbackState.VibrationOneTimeShort
                            is ProcessingState.SuccessAnimation -> {
                                if (selectedProcessingState.isFirstSide) {
                                    HapticFeedbackState.VibrationOneTimeShort
                                } else {
                                    HapticFeedbackState.VibrationOneTimeLong
                                }
                            }

                            else -> null
                        }
                        val newScanSoundState = when (selectedProcessingState) {
                            is ProcessingState.SuccessAnimation -> ScanSoundState.PlayScanBeep
                            else -> null
                        }
                        selectedStatusMessage?.let {
                            if (selectedProcessingState == ProcessingState.Error) {
                                val errorType: UxEvent.ErrorMessageType? =
                                    when (selectedStatusMessage) {
                                        is CommonStatusMessage -> when (selectedStatusMessage) {
                                            CommonStatusMessage.MoveCloser -> UxEvent.ErrorMessageType.MOVECLOSER
                                            CommonStatusMessage.MoveFarther -> UxEvent.ErrorMessageType.MOVEFARTHER
                                            CommonStatusMessage.KeepVisible
                                                -> UxEvent.ErrorMessageType.KEEPVISIBLE
                                            CommonStatusMessage.ScanningWrongSide -> UxEvent.ErrorMessageType.FLIPSIDE
                                            CommonStatusMessage.Align -> UxEvent.ErrorMessageType.ALIGNDOCUMENT
                                            CommonStatusMessage.EliminateBlur -> UxEvent.ErrorMessageType.ELIMINATEBLUR
                                            else -> null
                                        }

                                        else -> null
                                    }
                                errorType?.let {
                                    // We track error message event only when the errorMessageType changes
                                    if (errorType != lastTrackedErrorType) {
                                        lastTrackedErrorType = errorType
                                        UxPingletTracker.UxEvent.trackErrorMessageEvent(
                                            it,
                                            getSessionNumber()
                                        )
                                    }
                                }
                            }
                            _uiState.update {
                                it.copy(
                                    reticleState = selectedProcessingState.reticleState,
                                    processingState = selectedProcessingState,
                                    statusMessage = selectedStatusMessage,
                                    hapticFeedbackState = newHapticFeedbackState
                                        ?: it.hapticFeedbackState,
                                    scanSoundState = newScanSoundState
                                        ?: it.scanSoundState,
                                    currentSide = newCurrentSide ?: it.currentSide
                                )
                            }

                            appearanceCounter.incrementIfNotPresent(selectedStatusMessage)
                            updateStateStartTime()
                        }
                    }
                }
            }
        }
    }


    fun setInitialUiStateFromUiSettings(uiSettings: UiSettings) {
        _uiState.update {
            it.copy(
                helpButtonDisplayed = uiSettings.showHelpButton,
                onboardingDialogDisplayed = uiSettings.showOnboardingDialog
            )
        }
        if (_uiState.value.onboardingDialogDisplayed) {
            changeOnboardingDialogVisibility(true)
        } else {
            changeOnboardingDialogVisibility(false)
        }
    }

    fun pickNewState(): Pair<ProcessingState?, StatusMessage?> {
        val max = statusCounter.getAllCounts().values.maxOrNull()
        val mostFrequent = statusCounter.getAllCounts().filterValues { it == max }.keys.toList()
        statusCounter.reset()
        return if (mostFrequent.isNotEmpty()) {
            when (mostFrequent[0]) {
                CommonStatusMessage.ScanFirstSide, CommonStatusMessage.ScanSecondSide -> {
                    Pair(ProcessingState.Sensing, mostFrequent[0])
                }

                else -> {
                    Pair(ProcessingState.Error, mostFrequent[0])
                }

            }
        } else Pair(null, null)
    }

    fun setScreenOrientation(screenOrientation: ScreenOrientation) {
        val currentTime = System.currentTimeMillis()
        if (currentScreenOrientation != screenOrientation) {
            currentScreenOrientation = screenOrientation
            // track pinglet only when screen orientation changes (with a delay)
            if (currentTime - lastScreenOrientationPingTime >= pingletOrientationDelayMs) {
                UxPingletTracker.ScanningConditions.trackScreenOrientationChange(
                    screenOrientation = screenOrientation,
                    sessionNumber = getSessionNumber()
                )
                lastScreenOrientationPingTime = currentTime
            }
        }
    }

    override fun analyzeImage(image: ImageProxy) {
        image.use {
            imageAnalyzer?.analyze(it)
        }
    }

    fun lifecyclePauseAnalysis() {
        imageAnalyzer?.pauseAnalysis()
        firstImageTimestamp = null
        helpTooltipTimer.cancel()
        statusCounter.reset()
        isCountingActive = false
    }

    fun lifecycleResumeAnalysis() {
        if (!_uiState.value.onboardingDialogDisplayed && !_uiState.value.helpDisplayed && _uiState.value.errorState == ErrorState.NoError) {
            imageAnalyzer?.resumeAnalysis()
            helpTooltipTimer.start()
        }
    }

    suspend fun waitForMinimumStateDuration(newProcessingState: ProcessingState) {
        var remainingDuration: Duration
        do {
            remainingDuration = remainingStateDuration(
                uiState.value.processingState,
                newProcessingState,
                uiState.value.statusMessage
            )
            if (remainingDuration > Duration.ZERO) {
                delay(remainingDuration.inWholeMilliseconds)
            }
        } while (remainingDuration > Duration.ZERO)
    }

    fun remainingStateDuration(
        currentState: ProcessingState,
        newState: ProcessingState,
        newStatusMessage: StatusMessage
    ): Duration {
        if ((newState.reticleState == ReticleState.Success || newState.reticleState == ReticleState.SuccessFirstSide) && appearanceCounter.getAllCounts()
                .contains(newStatusMessage)
        ) {
            val remainingDuration =
                System.nanoTime().nanoseconds - uiStateStartTime - currentState.minDuration
            return -remainingDuration
        } else {
            val remainingDuration =
                System.nanoTime().nanoseconds - uiStateStartTime - currentState.duration
            return -remainingDuration
        }
    }

    fun shouldStartCounting(currentState: ProcessingState): Boolean {
        if ((System.nanoTime().nanoseconds - uiStateStartTime + countingWindowDuration) >= currentState.duration) {
            isCountingActive =
                true
            return true
        } else {
            return false
        }
    }

    fun onFlipAnimationCompleted() {
        _uiState.update {
            it.copy(
                processingState = ProcessingState.Sensing,
                cardAnimationState = CardAnimationState.Hidden,
                statusMessage =
                    when (it.currentSide) {
                        First -> CommonStatusMessage.ScanFirstSide
                        Second -> CommonStatusMessage.ScanSecondSide
                        else -> CommonStatusMessage.Empty
                    }
            )
        }
        updateStateStartTime()
        if (!_uiState.value.onboardingDialogDisplayed && !_uiState.value.helpDisplayed) {
            lifecycleResumeAnalysis()
        }
    }

    private fun updateStateStartTime() {
        uiStateStartTime =
            System.nanoTime()
                .toDuration(DurationUnit.NANOSECONDS)
    }

    fun changeTorchState() {
        val (newTorchState, hapticState) = when (_uiState.value.torchState) {
            MbTorchState.On -> MbTorchState.Off to HapticFeedbackState.VibrationOff
            MbTorchState.Off -> MbTorchState.On to HapticFeedbackState.VibrationOneTimeShort
            MbTorchState.NotSupportedByCamera -> return
        }
        _torchOn.value = newTorchState == MbTorchState.On
        _uiState.update {
            it.copy(
                torchState = newTorchState,
                hapticFeedbackState = hapticState
            )
        }
        UxPingletTracker.ScanningConditions.trackTorchStateUpdate(
            newTorchState == MbTorchState.On,
            getSessionNumber()
        )
    }

    fun changeHelpTooltipVisibility(show: Boolean) {
        if (_uiState.value.helpButtonDisplayed) {
            if (show) {
                helpTooltipTimer.cancel()
            } else {
                helpTooltipTimer.start()
            }
            _uiState.update {
                it.copy(helpTooltipDisplayed = show)
            }
        }
    }

    fun changeOnboardingDialogVisibility(show: Boolean) {
        _uiState.update {
            it.copy(onboardingDialogDisplayed = show)
        }
        if (show) {
            UxPingletTracker.UxEvent.trackSimpleEvent(
                UxPingletTracker.UxEvent.SimpleUxEventType.OnboardingInfoDisplayed,
                getSessionNumber()
            )
            lifecyclePauseAnalysis()
        } else {
            lifecycleResumeAnalysis()
        }
    }

    fun onHelpScreensDisplayRequested() {
        UxPingletTracker.UxEvent.trackSimpleEvent(
            UxPingletTracker.UxEvent.SimpleUxEventType.HelpOpened,
            getSessionNumber()
        )
        _uiState.update {
            it.copy(helpDisplayed = true)
        }
        lifecyclePauseAnalysis()
    }

    fun onHelpScreensCloseRequested(allPagesVisited: Boolean) {
        UxPingletTracker.UxEvent.trackHelpCloseEvent(
            helpCloseType = if (allPagesVisited) {
                UxEvent.HelpCloseType.CONTENTFULLYVIEWED
            } else {
                UxEvent.HelpCloseType.CONTENTSKIPPED
            },
            sessionNumber = getSessionNumber()
        )
        _uiState.update {
            it.copy(helpDisplayed = false)
        }
        lifecycleResumeAnalysis()
    }

    fun onRetryTimeout() {
        helpTooltipTimer.cancel()
        _uiState.update {
            it.copy(
                errorState = ErrorState.NoError,
                processingState = ProcessingState.Sensing,
                statusMessage = CommonStatusMessage.ScanFirstSide,
                currentSide = First,
            )
        }
        updateStateStartTime()
        imageAnalyzer?.restartAnalysis()
        helpTooltipTimer.start()
    }

    fun onHapticFeedbackCompleted() {
        _uiState.update {
            it.copy(
                hapticFeedbackState = HapticFeedbackState.VibrationOff
            )
        }
    }

    fun onScanSoundCompleted() {
        _uiState.update {
            it.copy(
                scanSoundState = ScanSoundState.SoundOff
            )
        }
    }

    fun onReticleSuccessAnimationCompleted() {
        if (_uiState.value.processingState is ProcessingState.SuccessAnimation && (_uiState.value.processingState as ProcessingState.SuccessAnimation).isFirstSide) {
            _uiState.update {
                it.copy(
                    processingState =
                        ProcessingState.CardAnimation,
                    statusMessage = CommonStatusMessage.Flip,
                    currentSide = Second,
                    cardAnimationState = CardAnimationState.ShowFlipLandscape(
                        firstSideDrawable = R.drawable.mb_blinkcard_card_first,
                        secondSideDrawable = R.drawable.mb_blinkcard_card_second
                    )
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    processingState = ProcessingState.Success
                )
            }
        }
    }

    fun onCameraInputInfoAvailable(context: Context, cameraInputDetails: CameraInputDetails) {
        UxPingletTracker.CameraInfo.trackCameraInputInfo(
            cameraInputDetails,
            getSessionNumber()
        )
        if (!cameraHardwareInfoReported) {
            cameraHardwareInfoReported = true
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val cameraDetailsList = CameraHardwareInfoHelper.getCameraHardwareInfo(context)
                    UxPingletTracker.CameraInfo.trackCameraHardwareInfo(cameraDetailsList)
                }
            }
        }

    }

    fun getSessionNumber(): Int = imageAnalyzer?.getSessionNumber() ?: 0

    override fun onCleared() {
        super.onCleared()
        BlinkCardSdk.sendPingletsIfAllowed(PingSendTriggerPoint.CameraScreenClosed)
        lifecyclePauseAnalysis()
        imageAnalyzer?.cancel()
        imageAnalyzer?.close()
        imageAnalyzer = null
    }

    companion object {
        private const val TAG = "BlinkCardUxViewModel"

        // Define a custom key for your dependency
        val BLINKCARD_SDK =
            object : CreationExtras.Key<BlinkCardSdk> {}
        val BLINKCARD_SESSION_SETTINGS =
            object : CreationExtras.Key<BlinkCardSessionSettings> {}
        val BLINKCARD_UX_SETTINGS =
            object : CreationExtras.Key<BlinkCardUxSettings> {}
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BlinkCardUxViewModel(
                    this[BLINKCARD_SDK] as BlinkCardSdk,
                    this[BLINKCARD_SESSION_SETTINGS] as BlinkCardSessionSettings,
                    this[BLINKCARD_UX_SETTINGS] as BlinkCardUxSettings
                )
            }
        }
    }
}