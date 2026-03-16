package com.example.app_edmilson.data.repository

import com.example.app_edmilson.data.model.ResolvedTvContent
import com.example.app_edmilson.data.model.TvRenderContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CachedTvContentTest {

    @Test
    fun `cache snapshot preserves display duration`() {
        val resolved = ResolvedTvContent(
            code = "TVCACHE001",
            contents = listOf(
                TvRenderContent.Url("https://cdn.exemplo.com/page", 25_000L),
                TvRenderContent.Image("https://cdn.exemplo.com/image.jpg", 40_000L)
            )
        )

        val restored = CachedTvContent.from(resolved).toResolved()

        assertNotNull(restored)
        assertEquals(resolved, restored)
    }

    @Test
    fun `legacy cache entry restores content without duration`() {
        val restored = CachedTvContent(
            code = "TVCACHELEGACY",
            type = "html",
            value = "<html><body>legacy</body></html>"
        ).toResolved()
        val resolved = checkNotNull(restored)

        assertEquals("TVCACHELEGACY", resolved.code)
        assertNull((resolved.content as TvRenderContent.Html).displayDurationMs)
    }

    @Test
    fun `cache item restores per item duration`() {
        val restored = CachedTvContent(
            code = "TVCACHE002",
            items = listOf(
                CachedTvContentItem(
                    type = "video",
                    value = "https://cdn.exemplo.com/video.mp4",
                    displayDurationMs = 90_000L,
                    impressionId = 77L
                )
            )
        ).toResolved()
        val resolved = checkNotNull(restored)

        assertEquals(
            90_000L,
            (resolved.content as TvRenderContent.Video).displayDurationMs
        )
        assertEquals(77L, (resolved.content as TvRenderContent.Video).impressionId)
        assertNull((resolved.content as TvRenderContent.Video).displayDurationMs?.takeIf { it <= 0 })
    }
}
