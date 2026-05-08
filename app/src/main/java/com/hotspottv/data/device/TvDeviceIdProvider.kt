package com.hotspottv.data.device

import android.content.Context
import android.provider.Settings

fun interface TvDeviceIdProvider {
    fun getDeviceId(): String?
}

class AndroidTvDeviceIdProvider(
    private val context: Context
) : TvDeviceIdProvider {
    override fun getDeviceId(): String? {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
    }
}
