package com.Lia.assistant.ui.fx

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.Lia.assistant.voice.VoiceSessionManager

/**
 * Direct flavor — shows [ListeningEdgeGlow] in a TYPE_APPLICATION_OVERLAY
 * window so it persists across all apps while the voice session is live.
 *
 * Requires the user to grant "draw over other apps" via
 * Settings.ACTION_MANAGE_OVERLAY_PERMISSION.  Silently does nothing if the
 * permission has not been granted.
 */
object OverlayEdgeGlowController {

    private var overlayView: ComposeView? = null

    fun start(context: Context) {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        val view = ComposeView(context).apply {
            setContent {
                ListeningEdgeGlow()
            }
        }

        runCatching { wm.addView(view, params) }
            .onSuccess { overlayView = view }
    }

    fun stop(context: Context) {
        val view = overlayView ?: return
        val wm   = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        runCatching { wm.removeView(view) }
        overlayView = null
    }

    fun updateState(state: String) {
        // State changes are observed reactively by ListeningEdgeGlow via
        // VoiceSessionManager.state StateFlow — no explicit update needed.
    }
}
