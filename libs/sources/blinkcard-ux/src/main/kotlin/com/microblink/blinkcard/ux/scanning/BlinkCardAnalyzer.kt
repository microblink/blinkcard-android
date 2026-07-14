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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
    private var analysisPaused = false
    private val scanningUxTranslator = BlinkCardScanningUxTranslator()

    /**
     * Analyzes an image from the camera.
     *
     * This function is called for each frame captured by the camera. It sends the
     * image to the BlinkCard SDK for processing and handles the results,
     * timeouts and cancellations.
     *
     * Current implementation of the analyzer cancels the session if the timeout occurs.
     * The timeout timer restarts every time the scanning is paused (onboarding dialog,
     * help dialog, card flip animation). Default timeout value can be checked at
     * [BlinkCardUxSettings.stepTimeoutDuration].
     *
     * @param image The [ImageProxy] containing the image to be analyzed.
     *
     */
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
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

    override fun pauseAnalysis() {
        analysisPaused = true
    }

    override fun resumeAnalysis() {
        analysisPaused = false
    }

    override fun timeoutAnalysis(cause: TimeoutCause) {
        MbLog.e(TAG) { "processing timeout occurred: $cause" }
        analysisPaused = true
        // TODO: BlinkCard currently only supports a single (step) timeout. When the distinct
        //  step vs. inactivity timeout feature is implemented, map [TimeoutCause] to the
        //  appropriate error reason / pinglet instead of always reporting ErrorStepTimeoutExpired.
        // Jira ticket: https://microblink.atlassian.net/browse/MSDKS-2067
        scanningDoneHandler.onError(ErrorReason.ErrorStepTimeoutExpired)
    }

    fun getSessionNumber(): Int? {
        return session?.sessionNumber
    }

    override fun cancel() {
        session?.cancelActiveProcess()
        scanningDoneHandler.onScanningCanceled()
    }

    override fun restartAnalysis() {
        CoroutineScope(Default).launch {
            session?.restartSession()
        }
        analysisPaused = false
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
