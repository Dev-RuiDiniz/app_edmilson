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
}
