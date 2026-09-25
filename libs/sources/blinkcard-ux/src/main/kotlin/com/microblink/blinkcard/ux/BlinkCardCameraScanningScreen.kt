/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.microblink.blinkcard.core.BlinkCardSdk
import com.microblink.blinkcard.core.session.BlinkCardScanningResult
import com.microblink.blinkcard.core.session.BlinkCardSessionSettings
import com.microblink.blinkcard.ux.ScanningUx
import com.microblink.blinkcard.ux.UiSettings
import com.microblink.blinkcard.ux.camera.CameraInputDetails
import com.microblink.blinkcard.ux.camera.CameraSettings
import com.microblink.blinkcard.ux.camera.compose.CameraInputDetailsCallback
import com.microblink.blinkcard.ux.camera.compose.CameraPermissionCallbacks
import com.microblink.blinkcard.ux.camera.compose.CameraPreviewCallbacks
import com.microblink.blinkcard.ux.camera.compose.CameraScreen
import com.microblink.blinkcard.ux.settings.BlinkCardUxSettings
import com.microblink.blinkcard.ux.state.MbTorchState
import com.microblink.blinkcard.ux.state.ProcessingState
import com.microblink.blinkcard.ux.theme.BlinkCardSdkTheme
import com.microblink.blinkcard.ux.theme.BlinkCardTheme
import com.microblink.blinkcard.ux.utils.*
import com.microblink.blinkcard.ux.utils.DeviceOrientationListener
import com.microblink.blinkcard.ux.utils.fillErrorDialogsBlinkCard
import com.microblink.blinkcard.ux.utils.fillHelpScreensBlinkCard
import com.microblink.blinkcard.ux.utils.onAppMovedToBackground
import com.microblink.blinkcard.ux.utils.onCameraPermissionRequest
import com.microblink.blinkcard.ux.utils.onCloseButtonClicked
import kotlinx.coroutines.launch

private const val TAG = "BlinkCardCameraScanningScreen"

/**
 * Composable function that provides a complete camera scanning screen using
 * the BlinkCard SDK.
 *
 * This composable function sets up and manages the entire camera scanning
 * process, including UI elements, camera interaction, and result handling. It
 * uses the provided [blinkCardSdk] and [BlinkCardSessionSettings] to
 * configure the scanning session and provides callbacks for handling
 * successful capture and cancellation.
 *
 * @param blinkCardSdk The [BlinkCardSdk] instance used for card scanning.
 * @param uxSettings The [BlinkCardUxSettings] used to customize the UX.
 * @param uiSettings The [UiSettings] used to customize the UI.
 *                         Defaults to [UiSettings] with default values.
 * @param cameraSettings The [CameraSettings] used for card scanning.
 *                       Defaults to [CameraSettings] with default values.
 * @param sessionSettings The [BlinkCardSessionSettings] used to configure
 *                        the capture session. Defaults to [BlinkCardSessionSettings] with default values.
 * @param onScanningSuccess A callback function invoked when a card is
 *                         successfully captured. Receives the
 *                         [BlinkCardScanningResult] as a parameter.
 * @param onScanningCanceled A callback function invoked when the user cancels
 *                          the scanning process.
 *
 */
@Composable
fun BlinkCardCameraScanningScreen(
    blinkCardSdk: BlinkCardSdk,
    uxSettings: BlinkCardUxSettings = BlinkCardUxSettings(),
    uiSettings: UiSettings = UiSettings(),
    cameraSettings: CameraSettings = CameraSettings(),
    sessionSettings: BlinkCardSessionSettings = BlinkCardSessionSettings(),
    onScanningSuccess: (BlinkCardScanningResult) -> Unit,
    onScanningCanceled: () -> Unit,
) {
    val viewModel: BlinkCardUxViewModel = viewModel(
        factory = BlinkCardUxViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(
                BlinkCardUxViewModel.BLINKCARD_SDK,
                blinkCardSdk
            )
            set(
                BlinkCardUxViewModel.BLINKCARD_SESSION_SETTINGS,
                sessionSettings
            )
            set(
                BlinkCardUxViewModel.BLINKCARD_UX_SETTINGS,
                uxSettings
            )
        }
    )

    val applicationContext = LocalContext.current.applicationContext

    DeviceOrientationListener(applicationContext) {
        viewModel.setScreenOrientation(it)
    }

    var initialUiStateSet by rememberSaveable { mutableStateOf(false) }
    if (!initialUiStateSet) {
        viewModel.setInitialUiStateFromUiSettings(uiSettings)
        initialUiStateSet = true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        val observer = getDefaultLifecycleObserver(viewModel)
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        processLifecycle.addObserver(observer)

        onDispose {
            processLifecycle.removeObserver(observer)
        }
    }

    BlinkCardSdkTheme(uiSettings) {
        val snackbarWarningMessage =
            stringResource(BlinkCardTheme.sdkStrings.scanningStrings.snackbarFlashlightWarning)
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            CameraScreen(
                cameraViewModel = viewModel,
                cameraSettings = cameraSettings,
                onCameraScreenLongPress = { viewModel.changeHelpTooltipVisibility(true) },
                cameraPermissionCallbacks = rememberCameraPermissionCallbacks(viewModel),
                cameraPreviewCallbacks = rememberCameraPreviewCallbacks(viewModel),
                cameraInputDetailsCallback = rememberCameraInputDetailsCallback(
                    viewModel,
                    applicationContext
                )
            ) {
                val overlayUiState = viewModel.uiState.collectAsStateWithLifecycle()

                if (overlayUiState.value.processingState == ProcessingState.Success) {
                    overlayUiState.value.blinkCardScanningResult?.let {
                        onScanningSuccess(it)
                    }
                }
                BackHandler {
                    onScanningCanceled()
                }
                ScanningUx(
                    Modifier.padding(paddingValues),
                    overlayUiState.value,
                    {
                        onCloseButtonClicked(viewModel.getSessionNumber())
                        onScanningCanceled()
                    },
                    uiSettings,
                    fillHelpScreensBlinkCard(),
                    fillErrorDialogsBlinkCard(viewModel::onRetryTimeout, onScanningCanceled),
                    uxSettings.allowHapticFeedback,
                    allowScanSound = uxSettings.allowScanSound,
                    showProductionOverlay = !blinkCardSdk.getLicenseToken().licenseRights.allowRemoveProductionOverlay,
                    showDemoOverlay = !blinkCardSdk.getLicenseToken().licenseRights.allowRemoveDemoOverlay,
                    {
                        viewModel.changeTorchState()
                        viewModel.viewModelScope.launch {
                            if (viewModel.uiState.value.torchState == MbTorchState.On) {
                                snackbarHostState.showSnackbar(
                                    snackbarWarningMessage,
                                    null,
                                    false,
                                    SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    viewModel::onFlipAnimationCompleted,
                    viewModel::onReticleSuccessAnimationCompleted,
                    viewModel::onHapticFeedbackCompleted,
                    viewModel::onScanSoundCompleted,
                    viewModel::changeOnboardingDialogVisibility,
                    viewModel::onHelpScreensDisplayRequested,
                    viewModel::onHelpScreensCloseRequested,
                    viewModel::changeHelpTooltipVisibility
                )
            }
        }
    }
}

@Composable
private fun rememberCameraPreviewCallbacks(viewModel: BlinkCardUxViewModel) = remember(viewModel) {
    object : CameraPreviewCallbacks {
        override fun onCameraPreviewStarted() {
            onCameraPreviewStarted(viewModel.getSessionNumber())
        }

        override fun onCameraPreviewStopped() {
            onCameraPreviewStopped(viewModel.getSessionNumber())
        }
    }
}

@Composable
private fun rememberCameraPermissionCallbacks(viewModel: BlinkCardUxViewModel) =
    remember(viewModel) {
        object : CameraPermissionCallbacks {
            override fun onCameraPermissionCheck() {
                onCameraPermissionCheck(viewModel.getSessionNumber())
            }

            override fun onCameraPermissionRequested() {
                onCameraPermissionRequest(viewModel.getSessionNumber())
            }

            override fun onCameraPermissionUserResponse(cameraPermissionGranted: Boolean) {
                onCameraPermissionUserResponse(
                    viewModel.getSessionNumber(),
                    cameraPermissionGranted
                )
            }

        }
    }

@Composable
private fun rememberCameraInputDetailsCallback(
    viewModel: BlinkCardUxViewModel,
    applicationContext: Context
) = remember(viewModel) {
    object : CameraInputDetailsCallback {
        override fun onCameraInputDetailsAvailable(cameraInputDetails: CameraInputDetails) {
            viewModel.onCameraInputInfoAvailable(
                applicationContext.applicationContext,
                cameraInputDetails
            )
        }
    }
}

private fun getDefaultLifecycleObserver(viewModel: BlinkCardUxViewModel): LifecycleObserver {
    val observer = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            onAppMovedToBackground(viewModel.getSessionNumber())
        }

        override fun onPause(owner: LifecycleOwner) {
            viewModel.lifecyclePauseAnalysis()
        }

        override fun onResume(owner: LifecycleOwner) {
            viewModel.lifecycleResumeAnalysis()
        }
    }
    return observer
}