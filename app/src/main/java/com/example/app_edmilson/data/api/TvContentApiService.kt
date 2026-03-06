package com.example.app_edmilson.data.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface TvContentApiService {
    @GET
    suspend fun getTvContent(@Url endpoint: String): Response<JsonElement>
}
