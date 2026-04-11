package com.hotspottv.data.model

object TvCodeValidator {
    fun normalize(rawCode: String): String = rawCode.trim().uppercase()

    fun isValid(rawCode: String): Boolean {
        val code = normalize(rawCode)
        return code.startsWith("TV") && code.length >= 6
    }
}

