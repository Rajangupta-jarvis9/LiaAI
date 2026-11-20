package com.Lia.assistant.license

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Play flavor — licensing is NOT included in the Play build.
 * This no-op stub satisfies any common code that imports AccessKeyManager
 * while ensuring zero licensing logic ships to the Play Store.
 */
object AccessKeyManager {

    val licenseState: StateFlow<LicenseInfo> =
        MutableStateFlow(LicenseInfo(active = true))

    data class LicenseInfo(val active: Boolean)

    suspend fun refreshInfo(context: Context)                 = Unit
    suspend fun activate(context: Context, key: String)       = true
    suspend fun liveCheck(context: Context)                   = Unit
    fun isActive(context: Context)                            = true
}
