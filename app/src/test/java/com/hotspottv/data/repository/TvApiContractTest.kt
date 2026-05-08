package com.hotspottv.data.repository

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvApiContractTest {

    @Test
    fun `append device id to endpoint with existing query`() {
        val endpoint = "api/tv/propagandas?codigo=TV123"

        val result = TvApiContract.appendDeviceId(endpoint, "device 01")

        assertEquals(
            "api/tv/propagandas?codigo=TV123&device_id=device+01",
            result
        )
    }

    @Test
    fun `append device id to endpoint without query`() {
        val endpoint = "api/tv/propagandas"

        val result = TvApiContract.appendDeviceId(endpoint, "abc")

        assertEquals("api/tv/propagandas?device_id=abc", result)
    }

    @Test
    fun `api failure maps tv limit reached`() {
        val payload = JsonParser.parseString(
            """
            {
              "success": false,
              "limite_tvs_atingido": true,
              "message": "limite excedido"
            }
            """.trimIndent()
        )

        val error = TvApiContract.resolveApiFailure(payload)

        assertTrue(error is TvApiException.TvLimitReached)
        assertEquals("limite excedido", error?.message)
    }

    @Test
    fun `api failure maps missing device id`() {
        val payload = JsonParser.parseString(
            """
            {
              "success": false,
              "device_id_obrigatorio": true,
              "erro": "device obrigatorio"
            }
            """.trimIndent()
        )

        val error = TvApiContract.resolveApiFailure(payload)

        assertTrue(error is TvApiException.DeviceIdRequired)
        assertEquals("device obrigatorio", error?.message)
    }

    @Test
    fun `api failure does not map successful payload`() {
        val payload = JsonParser.parseString(
            """
            {
              "success": true,
              "propagandas": []
            }
            """.trimIndent()
        )

        val error = TvApiContract.resolveApiFailure(payload)

        assertEquals(null, error)
    }

    @Test
    fun `authorization errors never use cache fallback`() {
        assertFalse(TvApiContract.shouldUseCacheFallback(TvApiException.DeviceIdRequired()))
        assertFalse(TvApiContract.shouldUseCacheFallback(TvApiException.TvLimitReached()))
        assertTrue(TvApiContract.shouldUseCacheFallback(IllegalStateException("falha generica")))
    }
}
