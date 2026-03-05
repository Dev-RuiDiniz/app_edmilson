package com.example.app_edmilson.data.model

import com.google.gson.annotations.SerializedName

data class TvContentResponseDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("html") val html: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

sealed class TvRenderContent {
    data class Url(val value: String) : TvRenderContent()
    data class Html(val value: String) : TvRenderContent()
    data class Image(val value: String) : TvRenderContent()
}

data class ResolvedTvContent(
    val code: String,
    val content: TvRenderContent
)

