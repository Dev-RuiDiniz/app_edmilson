package com.hotspottv.ui.renderer

import com.hotspottv.data.model.TvRenderContent

internal data class RendererPlaylistSnapshot(
    val playlist: List<TvRenderContent>,
    val currentIndex: Int,
    val activeContent: TvRenderContent?,
    val failedContentKeys: Set<String>
)

internal sealed interface RendererPlaylistRefreshResult {
    data object Empty : RendererPlaylistRefreshResult

    data class Keep(
        val playlist: List<TvRenderContent>,
        val currentIndex: Int,
        val failedContentKeys: Set<String>
    ) : RendererPlaylistRefreshResult

    data class Rerender(
        val playlist: List<TvRenderContent>,
        val currentIndex: Int,
        val failedContentKeys: Set<String>
    ) : RendererPlaylistRefreshResult
}

internal object RendererPlaylistReconciler {
    fun reconcile(
        snapshot: RendererPlaylistSnapshot,
        freshPlaylist: List<TvRenderContent>
    ): RendererPlaylistRefreshResult {
        if (freshPlaylist.isEmpty()) {
            return RendererPlaylistRefreshResult.Empty
        }

        val freshKeys = freshPlaylist.map(::renderContentKey).toSet()
        val prunedFailedKeys = snapshot.failedContentKeys.filterTo(linkedSetOf()) { key ->
            key in freshKeys
        }

        if (snapshot.playlist == freshPlaylist) {
            return RendererPlaylistRefreshResult.Keep(
                playlist = freshPlaylist,
                currentIndex = snapshot.currentIndex.coerceIn(0, freshPlaylist.lastIndex),
                failedContentKeys = prunedFailedKeys
            )
        }

        snapshot.activeContent?.let { activeContent ->
            val activeIndex = freshPlaylist.indexOfFirst { it == activeContent }
            if (activeIndex >= 0) {
                return RendererPlaylistRefreshResult.Keep(
                    playlist = freshPlaylist,
                    currentIndex = activeIndex,
                    failedContentKeys = prunedFailedKeys
                )
            }
        }

        val startIndex = if (snapshot.currentIndex in freshPlaylist.indices) {
            snapshot.currentIndex
        } else {
            0
        }
        val nextIndex = findNextPlayableIndex(
            playlist = freshPlaylist,
            startIndex = startIndex,
            failedContentKeys = prunedFailedKeys
        ) ?: return RendererPlaylistRefreshResult.Empty

        return RendererPlaylistRefreshResult.Rerender(
            playlist = freshPlaylist,
            currentIndex = nextIndex,
            failedContentKeys = prunedFailedKeys
        )
    }

    private fun findNextPlayableIndex(
        playlist: List<TvRenderContent>,
        startIndex: Int,
        failedContentKeys: Set<String>
    ): Int? {
        if (playlist.isEmpty()) {
            return null
        }

        var candidateIndex = startIndex.coerceIn(0, playlist.lastIndex)
        repeat(playlist.size) { iteration ->
            val index = if (iteration == 0) {
                candidateIndex
            } else {
                (candidateIndex + iteration) % playlist.size
            }
            if (renderContentKey(playlist[index]) !in failedContentKeys) {
                return index
            }
        }
        return null
    }
}

internal fun renderContentKey(content: TvRenderContent): String {
    content.impressionId?.let { return "id:$it" }
    return when (content) {
        is TvRenderContent.Url -> "url:${content.value.trim()}"
        is TvRenderContent.Html -> "html:${content.value.trim()}"
        is TvRenderContent.Image -> "image:${content.value.trim()}"
        is TvRenderContent.Video -> "video:${content.value.trim()}"
    }
}
