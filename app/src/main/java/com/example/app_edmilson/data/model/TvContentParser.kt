package com.example.app_edmilson.data.model

import java.util.Locale

object TvContentParser {
    private val urlPattern = Regex("^https?://.+", RegexOption.IGNORE_CASE)
    private val videoExtensions = setOf(
        "mp4", "m4v", "webm", "mkv", "mov", "avi", "3gp", "m3u8", "mpd", "ts"
    )

    fun parse(dto: TvContentResponseDto, requestedCode: String): ResolvedTvContent? {
        val candidates = buildList {
            add(dto.asItem())
            dto.data?.let(::add)
            dto.propaganda?.let(::add)
            dto.propagandas?.let { items -> addAll(items) }
        }

        val contents = mutableListOf<TvRenderContent>()
        val contentKeys = linkedSetOf<String>()
        var resolvedCode = requestedCode

        for (candidate in candidates) {
            val parsed = parseItem(candidate, requestedCode)
            if (parsed != null) {
                if (contents.isEmpty()) {
                    resolvedCode = parsed.code
                }
                val key = contentKey(parsed.content)
                if (contentKeys.add(key)) {
                    contents += parsed.content
                }
            }
        }

        if (contents.isEmpty()) {
            return null
        }
        return ResolvedTvContent(code = resolvedCode, contents = contents)
    }

    private fun TvContentResponseDto.asItem(): TvContentItemDto {
        return TvContentItemDto(
            code = code,
            type = type,
            url = url,
            html = html,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )
    }

    private fun parseItem(item: TvContentItemDto, requestedCode: String): ResolvedTvContent? {
        val code = item.code?.trim().orEmpty().ifBlank { requestedCode }
        val type = item.type?.trim().orEmpty().lowercase()
        val url = item.url?.trim().orEmpty()
        val html = item.html?.trim().orEmpty()
        val imageUrl = item.imageUrl?.trim().orEmpty()
        val videoUrl = item.videoUrl?.trim().orEmpty()

        val content = when (type) {
            "video", "vídeo", "mp4", "m3u8", "hls", "dash", "stream" -> {
                val video = videoUrl.ifBlank { url }
                video.takeIf { it.isNotBlank() }?.let { TvRenderContent.Video(it) }
                    ?: parseWithoutExplicitType(
                        url = url,
                        html = html,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl
                    )
            }
            "url", "web" -> when {
                url.isBlank() -> parseWithoutExplicitType(
                    url = url,
                    html = html,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl
                )
                isVideoUrl(url) -> TvRenderContent.Video(url)
                else -> TvRenderContent.Url(url)
            }
            "html" -> html.takeIf { it.isNotBlank() }?.let { TvRenderContent.Html(it) }
                ?: parseWithoutExplicitType(
                    url = url,
                    html = html,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl
                )
            "image", "imagem" -> {
                val image = imageUrl.ifBlank { url }
                image.takeIf { it.isNotBlank() }?.let { TvRenderContent.Image(it) }
                    ?: parseWithoutExplicitType(
                        url = url,
                        html = html,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl
                    )
            }
            else -> parseWithoutExplicitType(
                url = url,
                html = html,
                imageUrl = imageUrl,
                videoUrl = videoUrl
            )
        }

        return content?.let { ResolvedTvContent(code = code, contents = listOf(it)) }
    }

    private fun parseWithoutExplicitType(
        url: String,
        html: String,
        imageUrl: String,
        videoUrl: String
    ): TvRenderContent? {
        if (videoUrl.isNotBlank()) {
            return TvRenderContent.Video(videoUrl)
        }
        if (url.isNotBlank() && isVideoUrl(url)) {
            return TvRenderContent.Video(url)
        }
        if (url.isNotBlank() && urlPattern.matches(url)) {
            return TvRenderContent.Url(url)
        }
        if (html.isNotBlank()) {
            return TvRenderContent.Html(html)
        }
        if (imageUrl.isNotBlank()) {
            return TvRenderContent.Image(imageUrl)
        }
        return null
    }

    private fun isVideoUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#')
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in videoExtensions
    }

    private fun contentKey(content: TvRenderContent): String {
        return when (content) {
            is TvRenderContent.Url -> "url:${content.value.trim()}"
            is TvRenderContent.Html -> "html:${content.value.trim()}"
            is TvRenderContent.Image -> "image:${content.value.trim()}"
            is TvRenderContent.Video -> "video:${content.value.trim()}"
        }
    }
}
