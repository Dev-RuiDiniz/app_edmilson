package com.hotspottv.ui.renderer

import com.hotspottv.data.repository.TvApiException

internal enum class RendererErrorType {
    DEFAULT,
    DEVICE_ID_REQUIRED,
    TV_LIMIT_REACHED
}

internal object RendererErrorClassifier {
    fun classify(error: Throwable?): RendererErrorType {
        return when (error) {
            is TvApiException.DeviceIdRequired -> RendererErrorType.DEVICE_ID_REQUIRED
            is TvApiException.TvLimitReached -> RendererErrorType.TV_LIMIT_REACHED
            else -> RendererErrorType.DEFAULT
        }
    }
}
