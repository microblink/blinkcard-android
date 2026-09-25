package com.microblink.blinkcard.ux.utils

import com.microblink.blinkcard.ux.state.ErrorState

/**
 * Specifies the error reason.
 */
enum class ErrorReason {
    ErrorInvalidLicense,
    ErrorNetworkError,
    ErrorStepTimeoutExpired,
    ErrorInactivityTimeoutExpired,
    ErrorDocumentClassFiltered,
    ErrorSettingsValidationFailed,
    ErrorGetResultFailed
}

fun ErrorReason.toErrorState(): ErrorState {
    return when(this) {
        ErrorReason.ErrorInvalidLicense -> ErrorState.ErrorInvalidLicense
        ErrorReason.ErrorNetworkError -> ErrorState.ErrorNetworkError
        ErrorReason.ErrorStepTimeoutExpired,
        ErrorReason.ErrorInactivityTimeoutExpired -> ErrorState.ErrorTimeoutExpired
        ErrorReason.ErrorDocumentClassFiltered -> ErrorState.ErrorDocumentClassFiltered
        ErrorReason.ErrorSettingsValidationFailed -> ErrorState.ErrorInvalidSettings
        ErrorReason.ErrorGetResultFailed -> ErrorState.ErrorGetResult
    }
}