package com.hotspottv.data.model

object RecentTvCodeHistory {
    const val MAX_RECENT_CODES = 3

    fun normalizeAndLimit(codes: List<String>): List<String> {
        val normalized = buildList {
            for (code in codes) {
                val normalizedCode = TvCodeValidator.normalize(code)
                if (TvCodeValidator.isValid(normalizedCode)) {
                    add(normalizedCode)
                }
            }
        }

        val deduplicated = mutableListOf<String>()
        for (code in normalized) {
            if (deduplicated.none { it.equals(code, ignoreCase = true) }) {
                deduplicated.add(code)
            }
        }
        return deduplicated.take(MAX_RECENT_CODES)
    }

    fun addRecentCode(codes: List<String>, code: String): List<String> {
        val normalizedCode = TvCodeValidator.normalize(code)
        if (!TvCodeValidator.isValid(normalizedCode)) {
            return normalizeAndLimit(codes)
        }

        return normalizeAndLimit(listOf(normalizedCode) + codes)
    }
}
