/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_INACTIVITY_TIMEOUT_DURATION_MS = 10000

/**
 * Configuration settings for the scanning UX.
 *
 * @param stepTimeoutDuration Duration of the scanning session step before a timeout is triggered.
 * Resets on side changes, pauses when onboarding and help screen dialogs appear. If set to [Duration.ZERO], the scanning will not time out.
 * @param inactivityTimeoutDuration Duration without any scanning progress before a timeout is triggered.
 * Resets every time the scanning advances, either because the UI state changes (reticle type or message)
 * or because the card is located and being processed. If set to [Duration.ZERO], the scanning will not time out.
 * @param allowHapticFeedback Whether haptic feedback is allowed during the scanning process. Defaults to true.
 * @param allowScanSound Whether scan success sounds are allowed during the scanning process. Defaults to true.
 */
@Parcelize
data class BlinkCardUxSettings(
    val stepTimeoutDuration: Duration = 60000.milliseconds,
    val inactivityTimeoutDuration: Duration = DEFAULT_INACTIVITY_TIMEOUT_DURATION_MS.milliseconds,
    val allowHapticFeedback: Boolean = true,
    val allowScanSound: Boolean = true,
) : Parcelable {
    /**
     * Constructor retaining the original Java API.
     */
    @JvmOverloads
    constructor(
        stepTimeoutDurationMs: Int,
        allowHapticFeedback: Boolean = true,
        allowScanSound: Boolean = true
    ) : this(
        stepTimeoutDuration = stepTimeoutDurationMs.milliseconds,
        inactivityTimeoutDuration = DEFAULT_INACTIVITY_TIMEOUT_DURATION_MS.milliseconds,
        allowHapticFeedback = allowHapticFeedback,
        allowScanSound = allowScanSound
    )

    /**
     * Constructor for easier Java implementation.
     *
     * This secondary constructor allows Java developers to create a [BlinkCardUxSettings]
     * instance by providing the `stepTimeoutDuration` as an `Int` in milliseconds.
     *
     * @param stepTimeoutDurationMs Duration of the scanning session step before a timeout is triggered in milliseconds.
     * Resets on side changes, pauses when onboarding and help screen dialogs appear. If set to 0, the scanning will not time out.
     * @param inactivityTimeoutDurationMs Duration without any scanning progress before a timeout is triggered in milliseconds.
     * Resets every time the scanning advances, either because the UI state changes (reticle type or message)
     * or because the card is located and being processed. If set to 0, the scanning will not time out.
     * @param allowHapticFeedback Whether haptic feedback is allowed during the scanning process. Defaults to true.
     * @param allowScanSound Whether scan success sounds are allowed during the scanning process. Defaults to true.
     */
    @JvmOverloads
    constructor(
        stepTimeoutDurationMs: Int,
        inactivityTimeoutDurationMs: Int,
        allowHapticFeedback: Boolean = true,
        allowScanSound: Boolean = true
    ) : this(
        stepTimeoutDuration = stepTimeoutDurationMs.milliseconds,
        inactivityTimeoutDuration = inactivityTimeoutDurationMs.milliseconds,
        allowHapticFeedback = allowHapticFeedback,
        allowScanSound = allowScanSound
    )
}