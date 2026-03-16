package com.example.app_edmilson.data.model

import com.google.gson.annotations.SerializedName

data class TvContentResponseDto(
    @SerializedName(value = "code", alternate = ["codigo"]) val code: String? = null,
    @SerializedName(value = "type", alternate = ["tipo", "tipo_midia", "tipoMidia"]) val type: String? = null,
    @SerializedName(value = "duration", alternate = ["duracao"]) val duration: Long? = null,
    @SerializedName(
        value = "url",
        alternate = ["link", "urlCompleta", "arquivo", "mediaUrl", "urlArquivo", "url_arquivo"]
    ) val url: String? = null,
    @SerializedName(value = "html", alternate = ["conteudoHtml", "conteudo"]) val html: String? = null,
    @SerializedName(
        value = "imageUrl",
        alternate = ["imagemUrl", "image_url", "imagem_url", "urlImagem", "imagem", "imagemUrlCompleta"]
    ) val imageUrl: String? = null,
    @SerializedName(
        value = "videoUrl",
        alternate = ["video_url", "urlVideo", "video", "arquivoVideo", "videoUrlCompleta"]
    ) val videoUrl: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName(value = "error", alternate = ["erro"]) val error: String? = null,
    @SerializedName(value = "message", alternate = ["mensagem"]) val message: String? = null,
    @SerializedName("data") val data: TvContentItemDto? = null,
    @SerializedName("propaganda") val propaganda: TvContentItemDto? = null,
    @SerializedName("propagandas") val propagandas: List<TvContentItemDto>? = null
)

data class TvContentItemDto(
    @SerializedName(value = "code", alternate = ["codigo"]) val code: String? = null,
    @SerializedName(value = "type", alternate = ["tipo", "tipo_midia", "tipoMidia"]) val type: String? = null,
    @SerializedName(value = "duration", alternate = ["duracao"]) val duration: Long? = null,
    @SerializedName(
        value = "url",
        alternate = ["link", "urlCompleta", "arquivo", "mediaUrl", "urlArquivo", "url_arquivo"]
    ) val url: String? = null,
    @SerializedName(value = "html", alternate = ["conteudoHtml", "conteudo"]) val html: String? = null,
    @SerializedName(
        value = "imageUrl",
        alternate = ["imagemUrl", "image_url", "imagem_url", "urlImagem", "imagem", "imagemUrlCompleta"]
    ) val imageUrl: String? = null,
    @SerializedName(
        value = "videoUrl",
        alternate = ["video_url", "urlVideo", "video", "arquivoVideo", "videoUrlCompleta"]
    ) val videoUrl: String? = null
)

sealed class TvRenderContent(
    open val value: String,
    open val displayDurationMs: Long? = null,
    open val impressionId: Long? = null
) {
    data class Url(
        override val value: String,
        override val displayDurationMs: Long? = null,
        override val impressionId: Long? = null
    ) : TvRenderContent(value, displayDurationMs, impressionId)

    data class Html(
        override val value: String,
        override val displayDurationMs: Long? = null,
        override val impressionId: Long? = null
    ) : TvRenderContent(value, displayDurationMs, impressionId)

    data class Image(
        override val value: String,
        override val displayDurationMs: Long? = null,
        override val impressionId: Long? = null
    ) : TvRenderContent(value, displayDurationMs, impressionId)

    data class Video(
        override val value: String,
        override val displayDurationMs: Long? = null,
        override val impressionId: Long? = null
    ) : TvRenderContent(value, displayDurationMs, impressionId)
}

data class ResolvedTvContent(
    val code: String,
    val contents: List<TvRenderContent>
) {
    val content: TvRenderContent
        get() = contents.first()
}
