package com.example.app_edmilson.data.model

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvContentParserTest {

    @Test
    fun `parse standard response with explicit type`() {
        val json = """
            {
              "code": "TV2665487D",
              "type": "url",
              "duracao": 45,
              "url": "https://meusite.com/tv/TV2665487D"
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TV2665487D")
        val resolved = checkNotNull(parsed)

        assertEquals("TV2665487D", resolved.code)
        assertTrue(resolved.content is TvRenderContent.Url)
        assertEquals(
            "https://meusite.com/tv/TV2665487D",
            (resolved.content as TvRenderContent.Url).value
        )
        assertEquals(45_000L, (resolved.content as TvRenderContent.Url).displayDurationMs)
    }

    @Test
    fun `parse simple response without type`() {
        val json = """
            {
              "url": "https://meusite.com/tv/TV45F5738D"
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TV45F5738D")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TV45F5738D")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVA1B2C3D4")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVVID001")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVPLAY001")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVLIVE001")

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

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TV8A855CA4")

        assertNotNull(parsed)
        assertEquals("TV8A855CA4", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Image)
        assertEquals(
            "https://hotspot1.edmilsonti.com.br/uploads/tv/tv_18_1772147471_2097.jpg",
            (parsed?.content as TvRenderContent.Image).value
        )
        assertEquals(1L, (parsed.content as TvRenderContent.Image).impressionId)
    }

    @Test
    fun `parse response when data is an array`() {
        val json = """
            {
              "success": true,
              "data": [
                {
                  "tipo_midia": "video",
                  "video_url": "https://cdn.exemplo.com/midia/spot.mp4"
                }
              ]
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVDATA001")

        assertNotNull(parsed)
        assertEquals("TVDATA001", parsed?.code)
        assertTrue(parsed?.content is TvRenderContent.Video)
    }

    @Test
    fun `parse nested media object`() {
        val json = """
            {
              "success": true,
              "propagandas": [
                {
                  "midia": {
                    "tipo": "imagem",
                    "url": "https://cdn.exemplo.com/img/banner.jpg",
                    "duration": 12
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVMEDIA001")
        val resolved = checkNotNull(parsed)

        assertTrue(resolved.content is TvRenderContent.Image)
        assertEquals(
            "https://cdn.exemplo.com/img/banner.jpg",
            (resolved.content as TvRenderContent.Image).value
        )
        assertEquals(12_000L, (resolved.content as TvRenderContent.Image).displayDurationMs)
    }

    @Test
    fun `ignore invalid or non positive duration`() {
        val json = """
            {
              "success": true,
              "propagandas": [
                {
                  "tipo_midia": "imagem",
                  "imagem_url": "https://cdn.exemplo.com/img/banner.jpg",
                  "duracao": 0
                },
                {
                  "tipo_midia": "url",
                  "url": "https://cdn.exemplo.com/page",
                  "duration": "invalido"
                }
              ]
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TVDUR001")
        val resolved = checkNotNull(parsed)

        assertTrue(resolved.contents[0] is TvRenderContent.Image)
        assertNull((resolved.contents[0] as TvRenderContent.Image).displayDurationMs)
        assertTrue(resolved.contents[1] is TvRenderContent.Url)
        assertNull((resolved.contents[1] as TvRenderContent.Url).displayDurationMs)
    }

    @Test
    fun `parse tempo_exibicao_segundos per propaganda item`() {
        val json = """
            {
              "success": true,
              "codigo": "TV26654B7D",
              "propagandas": [
                {
                  "id": 2,
                  "imagem_url": "https://hotspot1.edmilsonti.com.br/uploads/tv/tv_10_1772804450_5617.jpg",
                  "tipo_midia": "imagem",
                  "tempo_exibicao_segundos": 10
                },
                {
                  "id": 3,
                  "imagem_url": "https://hotspot1.edmilsonti.com.br/uploads/tv/tv_10_1772805225_1360.jpg",
                  "tipo_midia": "imagem",
                  "tempo_exibicao_segundos": 15
                }
              ]
            }
        """.trimIndent()

        val parsed = TvContentParser.parse(JsonParser.parseString(json), requestedCode = "TV26654B7D")
        val resolved = checkNotNull(parsed)

        assertEquals(2, resolved.contents.size)
        assertEquals(10_000L, (resolved.contents[0] as TvRenderContent.Image).displayDurationMs)
        assertEquals(15_000L, (resolved.contents[1] as TvRenderContent.Image).displayDurationMs)
        assertEquals(2L, (resolved.contents[0] as TvRenderContent.Image).impressionId)
        assertEquals(3L, (resolved.contents[1] as TvRenderContent.Image).impressionId)
    }
}
