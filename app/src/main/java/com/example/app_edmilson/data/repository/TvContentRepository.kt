package com.example.app_edmilson.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.app_edmilson.BuildConfig
import com.example.app_edmilson.data.api.NetworkModule
import com.example.app_edmilson.data.api.TvContentApiService
import com.example.app_edmilson.data.model.ResolvedTvContent
import com.example.app_edmilson.data.model.TvCodeValidator
import com.example.app_edmilson.data.model.TvContentParser
import com.example.app_edmilson.data.model.TvRenderContent
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class TvContentRepository(
    private val apiService: TvContentApiService,
    private val context: Context,
    private val gson: Gson = Gson()
) {
    data class FetchResult(
        val content: ResolvedTvContent,
        val fromCache: Boolean
    )

    private val cachePrefs = context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)
    private val memoryCache = mutableMapOf<String, ResolvedTvContent>()

    suspend fun fetchContent(rawCode: String): Result<FetchResult> {
        val code = TvCodeValidator.normalize(rawCode)
        val endpoint = resolveEndpoint(code)
        val fullUrl = buildApiUrl(endpoint)

        return try {
            Log.d(TAG, "O que foi enviado para a API: metodo=GET endpoint=$endpoint url=$fullUrl")

            val response = apiService.getTvContent(fullUrl)
            if (!response.isSuccessful) {
                val errorPayload = response.errorBody()?.string().orEmpty().trim()
                val details = if (errorPayload.isBlank()) "" else " - $errorPayload"
                return failureOrCached(
                    code = code,
                    error = IllegalStateException(
                        "API retornou status ${response.code()} em $fullUrl$details"
                    )
                )
            }

            val body = response.body()
                ?: return failureOrCached(
                    code = code,
                    error = IllegalStateException("Resposta vazia da API")
                )
            if (body.isApiFailure()) {
                val apiMessage = body.apiErrorMessage()
                    .ifBlank { "Resposta inválida da API" }
                return failureOrCached(
                    code = code,
                    error = IllegalStateException(apiMessage)
                )
            }
            val parsed = TvContentParser.parse(body, requestedCode = code)
                ?: return failureOrCached(
                    code = code,
                    error = IllegalStateException("API sem conteúdo renderizável")
                )

            saveCache(parsed)
            Result.success(FetchResult(content = parsed, fromCache = false))
        } catch (error: Throwable) {
            failureOrCached(code = code, error = error)
        }
    }

    private fun resolveEndpoint(code: String): String {
        val encodedCode = Uri.encode(code)
        val template = BuildConfig.API_TV_CONTENT_PATH_TEMPLATE.trim()
        return when {
            template.contains("%s") -> template.format(encodedCode)
            template.contains("{code}") -> template.replace("{code}", encodedCode)
            else -> "${template.trimEnd('/')}/$encodedCode"
        }
    }

    private fun buildApiUrl(endpoint: String): String {
        val trimmedEndpoint = endpoint.trim()
        if (trimmedEndpoint.startsWith("http://") || trimmedEndpoint.startsWith("https://")) {
            return trimmedEndpoint
        }
        val base = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        var normalizedEndpoint = trimmedEndpoint.trimStart('/')

        if (base.endsWith("/api", ignoreCase = true) &&
            normalizedEndpoint.startsWith("api/", ignoreCase = true)
        ) {
            normalizedEndpoint = normalizedEndpoint.substring("api/".length)
        }

        if (normalizedEndpoint.isBlank()) {
            return base
        }
        return "$base/$normalizedEndpoint"
    }

    private fun failureOrCached(code: String, error: Throwable): Result<FetchResult> {
        loadCachedResult(code)?.let { return Result.success(it) }
        return Result.failure(error)
    }

    private fun saveCache(content: ResolvedTvContent) {
        memoryCache[content.code] = content
        val cachePayload = CachedTvContent.from(content)
        cachePrefs.edit()
            .putString(cacheKey(content.code), gson.toJson(cachePayload))
            .apply()
    }

    private fun loadCachedResult(code: String): FetchResult? {
        memoryCache[code]?.let { return FetchResult(content = it, fromCache = true) }

        val cacheJson = cachePrefs.getString(cacheKey(code), null) ?: return null
        val cachePayload = runCatching {
            gson.fromJson(cacheJson, CachedTvContent::class.java)
        }.getOrNull() ?: return null

        val restored = cachePayload.toResolved() ?: return null
        memoryCache[code] = restored
        return FetchResult(content = restored, fromCache = true)
    }

    private fun cacheKey(code: String): String = "content_$code"

    private fun JsonElement.isApiFailure(): Boolean {
        val root = asJsonObjectOrNull() ?: return false
        val successFlag = root["success"]?.asBooleanOrNull()
        return successFlag == false
    }

    private fun JsonElement.apiErrorMessage(): String {
        val root = asJsonObjectOrNull() ?: return ""
        return root.readString("error", "erro", "message", "mensagem")
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return if (isJsonObject) asJsonObject else null
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

    private fun JsonElement.asStringOrNull(): String? {
        if (isJsonNull || !isJsonPrimitive) return null
        return runCatching { asString }.getOrNull()
    }

    private fun JsonElement.asBooleanOrNull(): Boolean? {
        if (isJsonNull || !isJsonPrimitive) return null
        return runCatching { asBoolean }.getOrNull()
    }

    private data class CachedTvContent(
        val code: String,
        val items: List<CachedTvContentItem>? = null,
        val type: String? = null,
        val value: String? = null
    ) {
        fun toResolved(): ResolvedTvContent? {
            val parsedItems = when {
                !items.isNullOrEmpty() -> items.mapNotNull { it.toRenderContent() }
                !type.isNullOrBlank() && !value.isNullOrBlank() -> {
                    listOf(CachedTvContentItem(type = type, value = value).toRenderContent() ?: return null)
                }
                else -> return null
            }
            if (parsedItems.isEmpty()) {
                return null
            }
            return ResolvedTvContent(code = code, contents = parsedItems)
        }

        companion object {
            fun from(content: ResolvedTvContent): CachedTvContent {
                val cacheItems = content.contents.map { payload ->
                    when (payload) {
                        is TvRenderContent.Url -> CachedTvContentItem("url", payload.value)
                        is TvRenderContent.Html -> CachedTvContentItem("html", payload.value)
                        is TvRenderContent.Image -> CachedTvContentItem("image", payload.value)
                        is TvRenderContent.Video -> CachedTvContentItem("video", payload.value)
                    }
                }
                return CachedTvContent(
                    code = content.code,
                    items = cacheItems
                )
            }
        }
    }

    private data class CachedTvContentItem(
        val type: String,
        val value: String
    ) {
        fun toRenderContent(): TvRenderContent? {
            return when (type) {
                "url" -> TvRenderContent.Url(value)
                "html" -> TvRenderContent.Html(value)
                "image" -> TvRenderContent.Image(value)
                "video" -> TvRenderContent.Video(value)
                else -> null
            }
        }
    }

    companion object {
        private const val CACHE_PREFS_NAME = "tv_content_cache"
        private const val TAG = "TvContentRepository"

        fun create(context: Context): TvContentRepository {
            val apiService = NetworkModule.createTvContentApiService()
            return TvContentRepository(
                apiService = apiService,
                context = context.applicationContext
            )
        }
    }
}
