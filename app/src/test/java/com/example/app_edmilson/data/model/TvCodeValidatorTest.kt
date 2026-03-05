package com.example.app_edmilson.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvCodeValidatorTest {

    @Test
    fun `accepts valid tv code`() {
        assertTrue(TvCodeValidator.isValid("TV2665487D"))
    }

    @Test
    fun `rejects invalid tv code`() {
        assertFalse(TvCodeValidator.isValid("AB1234"))
        assertFalse(TvCodeValidator.isValid("TV12"))
    }
}

