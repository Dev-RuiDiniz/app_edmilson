package com.hotspottv.ui.renderer

import com.hotspottv.data.model.TvRenderContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererPlaylistReconcilerTest {

    @Test
    fun `same playlist keeps current content without rerender`() {
        val first = TvRenderContent.Image("https://cdn.exemplo.com/img-1.jpg")
        val second = TvRenderContent.Video("https://cdn.exemplo.com/video-1.mp4")
        val playlist = listOf(first, second)
        val result = RendererPlaylistReconciler.reconcile(
            RendererPlaylistSnapshot(
                playlist = playlist,
                currentIndex = 1,
                activeContent = second,
                failedContentKeys = emptySet()
            ),
            playlist
        )

        assertTrue(result is RendererPlaylistRefreshResult.Keep)
        val keep = result as RendererPlaylistRefreshResult.Keep
        assertEquals(1, keep.currentIndex)
        assertEquals(playlist, keep.playlist)
    }

    @Test
    fun `current content preserved when playlist changes elsewhere`() {
        val first = TvRenderContent.Image("https://cdn.exemplo.com/img-1.jpg")
        val current = TvRenderContent.Video("https://cdn.exemplo.com/video-1.mp4")
        val fresh = listOf(
            TvRenderContent.Image("https://cdn.exemplo.com/img-new.jpg"),
            first,
            current
        )

        val result = RendererPlaylistReconciler.reconcile(
            RendererPlaylistSnapshot(
                playlist = listOf(first, current),
                currentIndex = 1,
                activeContent = current,
                failedContentKeys = emptySet()
            ),
            fresh
        )

        assertTrue(result is RendererPlaylistRefreshResult.Keep)
        val keep = result as RendererPlaylistRefreshResult.Keep
        assertEquals(2, keep.currentIndex)
        assertEquals(fresh, keep.playlist)
    }

    @Test
    fun `removed current content advances to next available item`() {
        val current = TvRenderContent.Image("https://cdn.exemplo.com/img-1.jpg")
        val next = TvRenderContent.Video("https://cdn.exemplo.com/video-1.mp4")
        val result = RendererPlaylistReconciler.reconcile(
            RendererPlaylistSnapshot(
                playlist = listOf(current, next),
                currentIndex = 0,
                activeContent = current,
                failedContentKeys = emptySet()
            ),
            listOf(next)
        )

        assertTrue(result is RendererPlaylistRefreshResult.Rerender)
        val rerender = result as RendererPlaylistRefreshResult.Rerender
        assertEquals(0, rerender.currentIndex)
        assertEquals(listOf(next), rerender.playlist)
    }

    @Test
    fun `removed current content skips failed replacement and advances`() {
        val current = TvRenderContent.Image("https://cdn.exemplo.com/img-1.jpg")
        val failedNext = TvRenderContent.Image("https://cdn.exemplo.com/img-2.jpg")
        val fallback = TvRenderContent.Video("https://cdn.exemplo.com/video-1.mp4")
        val result = RendererPlaylistReconciler.reconcile(
            RendererPlaylistSnapshot(
                playlist = listOf(current, failedNext, fallback),
                currentIndex = 0,
                activeContent = current,
                failedContentKeys = setOf(renderContentKey(failedNext))
            ),
            listOf(failedNext, fallback)
        )

        assertTrue(result is RendererPlaylistRefreshResult.Rerender)
        val rerender = result as RendererPlaylistRefreshResult.Rerender
        assertEquals(1, rerender.currentIndex)
        assertEquals(listOf(failedNext, fallback), rerender.playlist)
    }
}
