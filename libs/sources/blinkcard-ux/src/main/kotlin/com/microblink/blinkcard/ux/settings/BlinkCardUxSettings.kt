/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */

package com.microblink.blinkcard.ux.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration settings for the scanning UX.
 *
 * @param stepTimeoutDuration Duration of the scanning session before a timeout is triggered.
 * Resets every time the scanning is paused (dialogs, side change). Defaults to 15 seconds.
 * @param allowHapticFeedback Whether haptic feedback is allowed during the scanning process. Defaults to true.
 * @param allowScanSound Whether scan success sounds are allowed during the scanning process. Defaults to true.
 */
@Parcelize
data class BlinkCardUxSettings(
    val stepTimeoutDuration: Duration = 15000.milliseconds,
    val allowHapticFeedback: Boolean = true,
    val allowScanSound: Boolean = true,
) : Parcelable {
    /**
     * Constructor for easier Java implementation.
     *
     * This secondary constructor allows Java developers to create a [BlinkCardUxSettings]
     * instance by providing the `stepTimeoutDuration` as an `Int` in milliseconds.
     *
     * @param stepTimeoutDurationMs Duration of the scanning session before a timeout is triggered
     * in milliseconds. Resets every time the scanning is paused (dialogs, side change). If set to 0, the scanning will not timeout.
     * @param allowHapticFeedback Whether haptic feedback is allowed during the scanning process. Defaults to true.
     * @param allowScanSound Whether scan success sounds are allowed during the scanning process. Defaults to true.
     */
    @JvmOverloads
    constructor(stepTimeoutDurationMs: Int, allowHapticFeedback: Boolean = true, allowScanSound: Boolean = true) : this(
        stepTimeoutDuration = stepTimeoutDurationMs.milliseconds,
        allowHapticFeedback = allowHapticFeedback,
        allowScanSound = allowScanSound
    )
}