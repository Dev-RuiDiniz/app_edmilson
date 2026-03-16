package com.example.app_edmilson.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.Locale

object TvContentParser {
    private val urlPattern = Regex("^https?://.+", RegexOption.IGNORE_CASE)
    private val videoExtensions = setOf(
        "mp4", "m4v", "webm", "mkv", "mov", "avi", "3gp", "m3u8", "mpd", "ts"
    )

    fun parse(payload: JsonElement?, requestedCode: String): ResolvedTvContent? {
        val candidates = extractCandidates(payload)
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

    private fun extractCandidates(payload: JsonElement?): List<JsonObject> {
        if (payload == null || payload.isJsonNull) return emptyList()
        val result = mutableListOf<JsonObject>()

        fun collect(element: JsonElement?) {
            if (element == null || element.isJsonNull) return
            when {
                element.isJsonObject -> result += element.asJsonObject
                element.isJsonArray -> element.asJsonArray.forEach { item ->
                    if (item.isJsonObject) {
                        result += item.asJsonObject
                    }
                }
            }
        }

        collect(payload)
        if (payload.isJsonObject) {
            val root = payload.asJsonObject
            collect(root["data"])
            collect(root["propaganda"])
            collect(root["propagandas"])
            collect(root["items"])
            collect(root["itens"])
            collect(root["playlist"])
            collect(root["midias"])
        }

        return result
    }

    private fun parseItem(item: JsonObject, requestedCode: String): ResolvedTvContent? {
        val media = item.getObject("midia", "media")
        val impressionId = item.readLong("id")
            ?: media?.readLong("id")
        val displayDurationMs = item.readDurationMs("duration", "duracao")
            ?: media?.readDurationMs("duration", "duracao")

        val code = item.readString("code", "codigo")
            .ifBlank { media?.readString("code", "codigo").orEmpty() }
            .ifBlank { requestedCode }

        val type = item.readString("type", "tipo", "tipo_midia", "tipoMidia")
            .ifBlank { media?.readString("type", "tipo", "tipo_midia", "tipoMidia").orEmpty() }
            .lowercase()

        val url = item.readString(
            "url",
            "link",
            "urlCompleta",
            "arquivo",
            "mediaUrl",
            "urlArquivo",
            "url_arquivo",
            "midia_url",
            "media_url"
        ).ifBlank {
            media?.readString(
                "url",
                "link",
                "urlCompleta",
                "arquivo",
                "mediaUrl",
                "urlArquivo",
                "url_arquivo",
                "midia_url",
                "media_url"
            ).orEmpty()
        }
        val html = item.readString("html", "conteudoHtml", "conteudo")
            .ifBlank { media?.readString("html", "conteudoHtml", "conteudo").orEmpty() }
        val imageUrl = item.readString(
            "imageUrl",
            "imagemUrl",
            "image_url",
            "imagem_url",
            "urlImagem",
            "imagem",
            "imagemUrlCompleta",
            "url_imagem"
        ).ifBlank {
            media?.readString(
                "imageUrl",
                "imagemUrl",
                "image_url",
                "imagem_url",
                "urlImagem",
                "imagem",
                "imagemUrlCompleta",
                "url_imagem"
            ).orEmpty()
        }
        val videoUrl = item.readString(
            "videoUrl",
            "video_url",
            "urlVideo",
            "video",
            "arquivoVideo",
            "videoUrlCompleta",
            "url_video"
        ).ifBlank {
            media?.readString(
                "videoUrl",
                "video_url",
                "urlVideo",
                "video",
                "arquivoVideo",
                "videoUrlCompleta",
                "url_video"
            ).orEmpty()
        }

        val content = when (type) {
            "video", "vídeo", "mp4", "m3u8", "hls", "dash", "stream" -> {
                val video = videoUrl.ifBlank { url }
                video.takeIf { it.isNotBlank() }?.let {
                    TvRenderContent.Video(it, displayDurationMs, impressionId)
                }
                    ?: parseWithoutExplicitType(
                        url = url,
                        html = html,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
                        displayDurationMs = displayDurationMs,
                        impressionId = impressionId
                    )
            }
            "url", "web" -> when {
                url.isBlank() -> parseWithoutExplicitType(
                    url = url,
                    html = html,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    displayDurationMs = displayDurationMs,
                    impressionId = impressionId
                )
                isVideoUrl(url) -> TvRenderContent.Video(url, displayDurationMs, impressionId)
                else -> TvRenderContent.Url(url, displayDurationMs, impressionId)
            }
            "html" -> html.takeIf { it.isNotBlank() }?.let {
                TvRenderContent.Html(it, displayDurationMs, impressionId)
            }
                ?: parseWithoutExplicitType(
                    url = url,
                    html = html,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    displayDurationMs = displayDurationMs,
                    impressionId = impressionId
                )
            "image", "imagem" -> {
                val image = imageUrl.ifBlank { url }
                image.takeIf { it.isNotBlank() }?.let {
                    TvRenderContent.Image(it, displayDurationMs, impressionId)
                }
                    ?: parseWithoutExplicitType(
                        url = url,
                        html = html,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
                        displayDurationMs = displayDurationMs,
                        impressionId = impressionId
                    )
            }
            else -> parseWithoutExplicitType(
                url = url,
                html = html,
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                displayDurationMs = displayDurationMs,
                impressionId = impressionId
            )
        }

        return content?.let { ResolvedTvContent(code = code, contents = listOf(it)) }
    }

    private fun parseWithoutExplicitType(
        url: String,
        html: String,
        imageUrl: String,
        videoUrl: String,
        displayDurationMs: Long?,
        impressionId: Long?
    ): TvRenderContent? {
        if (videoUrl.isNotBlank()) {
            return TvRenderContent.Video(videoUrl, displayDurationMs, impressionId)
        }
        if (url.isNotBlank() && isVideoUrl(url)) {
            return TvRenderContent.Video(url, displayDurationMs, impressionId)
        }
        if (url.isNotBlank() && urlPattern.matches(url)) {
            return TvRenderContent.Url(url, displayDurationMs, impressionId)
        }
        if (html.isNotBlank()) {
            return TvRenderContent.Html(html, displayDurationMs, impressionId)
        }
        if (imageUrl.isNotBlank()) {
            return TvRenderContent.Image(imageUrl, displayDurationMs, impressionId)
        }
        return null
    }

    private fun isVideoUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#')
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in videoExtensions
    }

    private fun contentKey(content: TvRenderContent): String {
        content.impressionId?.let { return "id:$it" }
        return when (content) {
            is TvRenderContent.Url -> "url:${content.value.trim()}"
            is TvRenderContent.Html -> "html:${content.value.trim()}"
            is TvRenderContent.Image -> "image:${content.value.trim()}"
            is TvRenderContent.Video -> "video:${content.value.trim()}"
        }
    }

    private fun JsonObject.readString(vararg keys: String): String {
        for (key in keys) {
            val raw = this[key]?.asStringOrNull()?.trim().orEmpty()
            if (raw.isNotBlank()) {
                return raw
            }
        }
        return ""
    }

    private fun JsonObject.readDurationMs(vararg keys: String): Long? {
        for (key in keys) {
            val rawValue = this[key] ?: continue
            val seconds = rawValue.asLongOrNull() ?: rawValue.asStringOrNull()?.toLongOrNull()
            if (seconds != null && seconds > 0) {
                return seconds * 1_000L
            }
        }
        return null
    }

    private fun JsonObject.readLong(vararg keys: String): Long? {
        for (key in keys) {
            val rawValue = this[key] ?: continue
            val parsedValue = rawValue.asLongOrNull() ?: rawValue.asStringOrNull()?.toLongOrNull()
            if (parsedValue != null && parsedValue > 0) {
                return parsedValue
            }
        }
        return null
    }

    private fun JsonObject.getObject(vararg keys: String): JsonObject? {
        for (key in keys) {
            val candidate = this[key]
            if (candidate != null && candidate.isJsonObject) {
                return candidate.asJsonObject
            }
        }
        return null
    }

    private fun JsonElement.asStringOrNull(): String? {
        if (isJsonNull || !isJsonPrimitive) return null
        return runCatching { asString }.getOrNull()
    }

    private fun JsonElement.asLongOrNull(): Long? {
        if (isJsonNull || !isJsonPrimitive) return null
        return runCatching { asLong }.getOrNull()
    }
}
