package com.example.app_edmilson.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvContentParserTest {

    private val gson = Gson()

    @Test
    fun `parse standard response with explicit type`() {
        val json = """
            {
              "code": "TV2665487D",
              "type": "url",
              "url": "https://meusite.com/tv/TV2665487D"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TV2665487D")

        assertEquals("TV2665487D", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Url)
        assertEquals(
            "https://meusite.com/tv/TV2665487D",
            (parsed?.content as TvRenderContent.Url).value
        )
    }

    @Test
    fun `parse simple response without type`() {
        val json = """
            {
              "url": "https://meusite.com/tv/TV45F5738D"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TV45F5738D")

        assertEquals("TV45F5738D", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Url)
        assertEquals(
            "https://meusite.com/tv/TV45F5738D",
            (parsed?.content as TvRenderContent.Url).value
        )
    }

    @Test
    fun `fallback when type is url but url field is empty`() {
        val json = """
            {
              "type": "url",
              "url": "",
              "html": "<html><body>Fallback</body></html>"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TV45F5738D")

        assertNotNull(parsed)
        assertTrue(parsed?.content is TvRenderContent.Html)
    }

    @Test
    fun `parse wrapped propagandas response`() {
        val json = """
            {
              "success": true,
              "propagandas": [
                {
                  "codigo": "TVA1B2C3D4",
                  "tipo": "url",
                  "urlArquivo": "https://meusite.com/tv/TVA1B2C3D4"
                }
              ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TVA1B2C3D4")

        assertNotNull(parsed)
        assertEquals("TVA1B2C3D4", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Url)
        assertEquals(
            "https://meusite.com/tv/TVA1B2C3D4",
            (parsed?.content as TvRenderContent.Url).value
        )
    }

    @Test
    fun `parse explicit video response`() {
        val json = """
            {
              "code": "TVVID001",
              "type": "video",
              "url": "https://cdn.exemplo.com/midia/anuncio.mp4"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TVVID001")

        assertNotNull(parsed)
        assertTrue(parsed?.content is TvRenderContent.Video)
        assertEquals(
            "https://cdn.exemplo.com/midia/anuncio.mp4",
            (parsed?.content as TvRenderContent.Video).value
        )
    }

    @Test
    fun `parse multiple propagandas preserving order`() {
        val json = """
            {
              "success": true,
              "codigo": "TVPLAY001",
              "propagandas": [
                {
                  "tipo_midia": "imagem",
                  "imagem_url": "https://cdn.exemplo.com/imagens/slide-1.jpg"
                },
                {
                  "tipo_midia": "video",
                  "video_url": "https://cdn.exemplo.com/videos/anuncio.mp4"
                }
              ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TVPLAY001")

        assertNotNull(parsed)
        assertEquals(2, parsed?.contents?.size)
        assertTrue(parsed?.contents?.get(0) is TvRenderContent.Image)
        assertTrue(parsed?.contents?.get(1) is TvRenderContent.Video)
    }

    @Test
    fun `infer video when url has m3u8 extension`() {
        val json = """
            {
              "url": "https://cdn.exemplo.com/live/canal.m3u8"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TVLIVE001")

        assertNotNull(parsed)
        assertTrue(parsed?.content is TvRenderContent.Video)
        assertEquals(
            "https://cdn.exemplo.com/live/canal.m3u8",
            (parsed?.content as TvRenderContent.Video).value
        )
    }

    @Test
    fun `parse hotspot payload using tipo_midia and imagem_url`() {
        val json = """
            {
              "success": true,
              "codigo": "TV8A855CA4",
              "propagandas": [
                {
                  "id": 1,
                  "imagem_url": "https://hotspot1.edmilsonti.com.br/uploads/tv/tv_18_1772147471_2097.jpg",
                  "tipo_midia": "imagem",
                  "titulo": "Ali",
                  "descricao": "",
                  "ordem": 1
                }
              ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TvContentResponseDto::class.java)
        val parsed = TvContentParser.parse(dto, requestedCode = "TV8A855CA4")

        assertNotNull(parsed)
        assertEquals("TV8A855CA4", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Image)
        assertEquals(
            "https://hotspot1.edmilsonti.com.br/uploads/tv/tv_18_1772147471_2097.jpg",
            (parsed?.content as TvRenderContent.Image).value
        )
    }
}
