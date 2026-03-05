package com.example.app_edmilson.data.model

object TvContentParser {
    private val urlPattern = Regex("^https?://.+", RegexOption.IGNORE_CASE)

    fun parse(dto: TvContentResponseDto, requestedCode: String): ResolvedTvContent? {
        val code = dto.code?.trim().orEmpty().ifBlank { requestedCode }
        val type = dto.type?.trim().orEmpty().lowercase()
        val url = dto.url?.trim().orEmpty()
        val html = dto.html?.trim().orEmpty()
        val imageUrl = dto.imageUrl?.trim().orEmpty()

        val content = when (type) {
            "url", "web" -> url.takeIf { it.isNotBlank() }?.let { TvRenderContent.Url(it) }
                ?: parseWithoutExplicitType(url = url, html = html, imageUrl = imageUrl)
            "html" -> html.takeIf { it.isNotBlank() }?.let { TvRenderContent.Html(it) }
                ?: parseWithoutExplicitType(url = url, html = html, imageUrl = imageUrl)
            "image" -> {
                val image = imageUrl.ifBlank { url }
                image.takeIf { it.isNotBlank() }?.let { TvRenderContent.Image(it) }
                    ?: parseWithoutExplicitType(url = url, html = html, imageUrl = imageUrl)
            }
            else -> parseWithoutExplicitType(url = url, html = html, imageUrl = imageUrl)
        }

        return content?.let { ResolvedTvContent(code = code, content = it) }
    }

    private fun parseWithoutExplicitType(url: String, html: String, imageUrl: String): TvRenderContent? {
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
}
