package com.hotspottv.ui.renderer

import com.hotspottv.data.repository.TvApiException
import org.junit.Assert.assertEquals
import org.junit.Test

class RendererErrorClassifierTest {

    @Test
    fun `classify tv limit reached error`() {
        val result = RendererErrorClassifier.classify(TvApiException.TvLimitReached())

        assertEquals(RendererErrorType.TV_LIMIT_REACHED, result)
    }

    @Test
    fun `classify missing device id error`() {
        val result = RendererErrorClassifier.classify(TvApiException.DeviceIdRequired())

        assertEquals(RendererErrorType.DEVICE_ID_REQUIRED, result)
    }

    @Test
    fun `classify generic error as default`() {
        val result = RendererErrorClassifier.classify(IllegalStateException("falha"))

        assertEquals(RendererErrorType.DEFAULT, result)
    }
}
