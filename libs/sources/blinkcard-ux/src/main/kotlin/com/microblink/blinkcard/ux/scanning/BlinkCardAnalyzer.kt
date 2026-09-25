/**
 * Copyright (c) Microblink. All rights reserved. This code is provided for
 * use as-is and may not be copied, modified, or redistributed.
 */

package com.microblink.blinkcard.ux.scanning

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.microblink.blinkcard.core.BlinkCardSdk
import com.microblink.blinkcard.core.RemoteLicenseCheckException
import com.microblink.blinkcard.core.image.InputImage
import com.microblink.blinkcard.core.session.BlinkCardScanningSession
import com.microblink.blinkcard.core.session.BlinkCardSessionSettings
import com.microblink.blinkcard.core.utils.MbLog
import com.microblink.blinkcard.ux.camera.ImageAnalyzer
import com.microblink.blinkcard.ux.camera.TimeoutCause
import com.microblink.blinkcard.ux.utils.ErrorReason
import com.microblink.blinkcard.ux.utils.UxPingletTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "BlinkCardAnalyzer"

/**
 * Analyzes images from the camera and processes them using the BlinkCard SDK.
 *
 * This class implements the [ImageAnalyzer] interface and is responsible for
 * receiving image frames from the camera, sending them to the BlinkCard
 * SDK for processing and results handling. It also manages the scanning
 * session, timeouts, and dispatches UI events.
 *
 * @param blinkCardSdk An instance of the [BlinkCardSdk] used for processing images.
 * @param sessionSettings The [BlinkCardSessionSettings] used to configure the scanning session.
 * @property scanningDoneHandler A [BlinkCardScanningDoneHandler] to handle the completion
 *                                of the scanning process.
 * @property uxEventHandler An optional [BlinkCardScanningUxEventHandler] to handle UI events.
 *
 */
class BlinkCardAnalyzer(
    blinkCardSdk: BlinkCardSdk,
    private val sessionSettings: BlinkCardSessionSettings,
    private val scanningDoneHandler: BlinkCardScanningDoneHandler,
    private val uxEventHandler: BlinkCardScanningUxEventHandler? = null,
) : ImageAnalyzer {

    private var session: BlinkCardScanningSession? =
        runBlocking { blinkCardSdk.createScanningSession(sessionSettings) }

    @Volatile
    private var analysisPaused = false
    private val scanningUxTranslator = BlinkCardScanningUxTranslator()

    /**
     * Analyzes a single camera frame.
     *
     * Called by CameraX for each frame delivered to the analyzer. It sends the
     * image to the BlinkCard SDK for processing and handles the results,
     * timeouts and cancellations.
     *
     * This implementation closes the provided [ImageProxy] before returning. Custom
     * analyzer implementations must also close the image after processing it.
     *
     * Timeout handling is driven by [BlinkCardUxSettings.stepTimeoutDuration] and
     * [BlinkCardUxSettings.inactivityTimeoutDuration].
     *
     * @param image The camera frame to analyze.
     *
     */
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.use {
            if (analysisPaused) return
            runBlocking {
                val inputImage = InputImage.createFromCameraXImageProxy(image)
                inputImage.use {
                    session?.let { session ->
                        try {
                            val sessionProcessResult = session.process(inputImage)
                            if (session.isCanceled) {
                                MbLog.w(TAG) { "processing has been canceled" }
                            } else {
                                sessionProcessResult.getOrNull()?.let { processResult ->
                                    val events = scanningUxTranslator.translate(
                                        processResult,
                                        inputImage,
                                        sessionSettings.scanningSettings
                                    )
                                    uxEventHandler?.onUxEvents(events)

                                    if (processResult.resultCompleteness.isComplete()) {
                                        val sessionResult = session.getResult()
                                        pauseAnalysis()
                                        scanningDoneHandler.onScanningFinished(sessionResult)
                                    } else {
                                        MbLog.v(TAG) { "Neither complete nor timeout, continuing..." }
                                    }
                                }
                            }
                        } catch (e: RemoteLicenseCheckException) {
                            scanningDoneHandler.onError(ErrorReason.ErrorInvalidLicense)
                        }
                    }
                }
            }
        }
    }

    override fun pauseAnalysis() {
        analysisPaused = true
    }

    override fun resumeAnalysis() {
        analysisPaused = false
    }

    override fun timeoutAnalysis(cause: TimeoutCause) {
        MbLog.e(TAG) { "processing timeout occurred: $cause" }

        val timeoutEvent = when (cause) {
            TimeoutCause.Step -> UxPingletTracker.UxEvent.SimpleUxEventType.StepTimeout
            TimeoutCause.Inactivity -> UxPingletTracker.UxEvent.SimpleUxEventType.InactivityTimeout
        }
        getSessionNumber()?.let { sessionNumber ->
            UxPingletTracker.UxEvent.trackSimpleEvent(timeoutEvent, sessionNumber)
        }

        onErrorAnalysis(
            when (cause) {
                TimeoutCause.Step -> ErrorReason.ErrorStepTimeoutExpired
                TimeoutCause.Inactivity -> ErrorReason.ErrorInactivityTimeoutExpired
            }
        )
    }

    fun getSessionNumber(): Int? {
        return session?.sessionNumber
    }

    private fun onErrorAnalysis(errorReason: ErrorReason) {
        pauseAnalysis()
        scanningDoneHandler.onError(errorReason)
    }

    override fun cancel() {
        session?.cancelActiveProcess()
        scanningDoneHandler.onScanningCanceled()
    }

    override suspend fun restartAnalysis() {
        analysisPaused = true
        try {
            withContext(Default) {
                session?.restartSession()
            }
        } finally {
            analysisPaused = false
        }
    }

    override fun close() {
        session?.also { s ->
            session = null
            CoroutineScope(IO).launch {
                s.close()
            }
        }
    }
}
