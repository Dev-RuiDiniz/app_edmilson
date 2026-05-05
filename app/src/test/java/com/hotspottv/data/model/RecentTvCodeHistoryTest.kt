package com.hotspottv.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentTvCodeHistoryTest {

    @Test
    fun `keeps empty history empty`() {
        assertEquals(emptyList<String>(), RecentTvCodeHistory.normalizeAndLimit(emptyList()))
    }

    @Test
    fun `keeps one code`() {
        assertEquals(
            listOf("TV000001"),
            RecentTvCodeHistory.normalizeAndLimit(listOf("TV000001"))
        )
    }

    @Test
    fun `keeps two codes in recent first order`() {
        assertEquals(
            listOf("TV000002", "TV000001"),
            RecentTvCodeHistory.normalizeAndLimit(listOf("TV000002", "TV000001"))
        )
    }

    @Test
    fun `keeps three codes in recent first order`() {
        assertEquals(
            listOf("TV000003", "TV000002", "TV000001"),
            RecentTvCodeHistory.normalizeAndLimit(
                listOf("TV000003", "TV000002", "TV000001")
            )
        )
    }

    @Test
    fun `cuts list to three most recent codes`() {
        assertEquals(
            listOf("TV000004", "TV000003", "TV000002"),
            RecentTvCodeHistory.normalizeAndLimit(
                listOf("TV000004", "TV000003", "TV000002", "TV000001")
            )
        )
    }

    @Test
    fun `does not duplicate repeated codes`() {
        assertEquals(
            listOf("TV000003", "TV000002", "TV000001"),
            RecentTvCodeHistory.normalizeAndLimit(
                listOf("TV000003", "TV000002", "TV000003", "TV000001")
            )
        )
    }

    @Test
    fun `adds new code to the top and trims history`() {
        assertEquals(
            listOf("TV000004", "TV000003", "TV000002"),
            RecentTvCodeHistory.addRecentCode(
                listOf("TV000003", "TV000002", "TV000001"),
                "TV000004"
            )
        )
    }

    @Test
    fun `moves repeated code to the top without duplication`() {
        assertEquals(
            listOf("TV000002", "TV000003", "TV000001"),
            RecentTvCodeHistory.addRecentCode(
                listOf("TV000003", "TV000002", "TV000001"),
                "TV000002"
            )
        )
    }
}
