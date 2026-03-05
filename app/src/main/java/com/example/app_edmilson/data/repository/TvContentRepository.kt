package com.example.app_edmilson.data.repository

import android.content.Context
import android.net.Uri
import com.example.app_edmilson.BuildConfig
import com.example.app_edmilson.data.api.NetworkModule
import com.example.app_edmilson.data.api.TvContentApiService
import com.example.app_edmilson.data.model.ResolvedTvContent
import com.example.app_edmilson.data.model.TvCodeValidator
import com.example.app_edmilson.data.model.TvContentParser
import com.example.app_edmilson.data.model.TvRenderContent
import com.google.gson.Gson

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

        return try {
            val response = apiService.getTvContent(endpoint)
            if (!response.isSuccessful) {
                return failureOrCached(
                    code = code,
                    error = IllegalStateException("API retornou status ${response.code()}")
                )
            }

            val body = response.body()
                ?: return failureOrCached(
                    code = code,
                    error = IllegalStateException("Resposta vazia da API")
                )
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
        val template = BuildConfig.API_TV_CONTENT_PATH_TEMPLATE.trim().trimStart('/')
        return when {
            template.contains("%s") -> template.format(encodedCode)
            template.contains("{code}") -> template.replace("{code}", encodedCode)
            else -> "${template.trimEnd('/')}/$encodedCode"
        }
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

    private data class CachedTvContent(
        val code: String,
        val type: String,
        val value: String
    ) {
        fun toResolved(): ResolvedTvContent? {
            val content = when (type) {
                "url" -> TvRenderContent.Url(value)
                "html" -> TvRenderContent.Html(value)
                "image" -> TvRenderContent.Image(value)
                else -> return null
            }
            return ResolvedTvContent(code = code, content = content)
        }

        companion object {
            fun from(content: ResolvedTvContent): CachedTvContent {
                return when (val payload = content.content) {
                    is TvRenderContent.Url -> CachedTvContent(content.code, "url", payload.value)
                    is TvRenderContent.Html -> CachedTvContent(content.code, "html", payload.value)
                    is TvRenderContent.Image -> CachedTvContent(content.code, "image", payload.value)
                }
            }
        }
    }

    companion object {
        private const val CACHE_PREFS_NAME = "tv_content_cache"

        fun create(context: Context): TvContentRepository {
            val apiService = NetworkModule.createTvContentApiService()
            return TvContentRepository(
                apiService = apiService,
                context = context.applicationContext
            )
        }
    }
}
