package com.hotspottv.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object TvApiContract {
    private val deviceIdPattern = Regex("(^|[?&])device_id=")

    fun appendDeviceId(endpoint: String, deviceId: String): String {
        if (deviceId.isBlank() || deviceIdPattern.containsMatchIn(endpoint)) {
            return endpoint
        }
        val separator = if (endpoint.contains("?")) "&" else "?"
        val encodedDeviceId = URLEncoder.encode(deviceId, StandardCharsets.UTF_8.name())
        return "$endpoint${separator}device_id=$encodedDeviceId"
    }

    fun resolveApiFailure(payload: JsonElement?): TvApiException? {
        val root = payload.asJsonObjectOrNull() ?: return null
        val successFlag = root["success"]?.asBooleanOrNull()
        if (successFlag != false) {
            return null
        }

        val message = root.readString("error", "erro", "message", "mensagem")
            .ifBlank { "Resposta invalida da API" }

        return when {
            root["device_id_obrigatorio"]?.asBooleanOrNull() == true ->
                TvApiException.DeviceIdRequired(message)
            root["limite_tvs_atingido"]?.asBooleanOrNull() == true ->
                TvApiException.TvLimitReached(message)
            else -> TvApiException.Generic(message)
        }
    }

    fun parseApiFailure(rawPayload: String): TvApiException? {
        if (rawPayload.isBlank()) {
            return null
        }
        val payload = runCatching { JsonParser.parseString(rawPayload) }.getOrNull() ?: return null
        return resolveApiFailure(payload)
    }

    fun shouldUseCacheFallback(error: Throwable): Boolean {
        return error !is TvApiException.DeviceIdRequired && error !is TvApiException.TvLimitReached
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? {
        if (this == null || isJsonNull || !isJsonObject) {
            return null
        }
        return asJsonObject
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
        if (isJsonNull || !isJsonPrimitive) {
            return null
        }
        return runCatching { asString }.getOrNull()
    }

    private fun JsonElement.asBooleanOrNull(): Boolean? {
        if (isJsonNull || !isJsonPrimitive) {
            return null
        }
        return runCatching { asBoolean }.getOrNull()
    }
}
