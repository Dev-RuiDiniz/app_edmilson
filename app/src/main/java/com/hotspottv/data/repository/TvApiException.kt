package com.hotspottv.data.repository

sealed class TvApiException(message: String) : IllegalStateException(message) {
    class DeviceIdRequired(
        message: String = "Device ID obrigatorio para consultar a API de TV."
    ) : TvApiException(message)

    class TvLimitReached(
        message: String = "Limite de TVs atingido para este cliente."
    ) : TvApiException(message)

    class Generic(message: String) : TvApiException(message)
}
