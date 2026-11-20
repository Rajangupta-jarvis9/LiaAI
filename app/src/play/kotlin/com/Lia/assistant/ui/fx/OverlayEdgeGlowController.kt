package com.Lia.assistant.ui.fx

import android.content.Context

/**
 * Play flavor — SYSTEM_ALERT_WINDOW is not declared in the Play manifest,
 * so the overlay controller is a no-op here.
 */
object OverlayEdgeGlowController {
    fun start(context: Context)        = Unit
    fun stop(context: Context)         = Unit
    fun updateState(state: String)     = Unit
}
