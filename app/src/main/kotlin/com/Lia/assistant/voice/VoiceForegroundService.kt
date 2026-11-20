package com.Lia.assistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.Lia.assistant.MainActivity
import com.Lia.assistant.data.AssistantBrand

/**
 * Keeps Lia's voice session alive when the user leaves the Voice screen.
 *
 * ── Notification ─────────────────────────────────────────────────────────────
 * Channel ID  : "lia_voice_session"
 * Importance  : LOW  (no sound, no heads-up — just a persistent chip)
 * Title       : "<AssistantName> is listening"
 * Actions     : [Stop]
 *
 * ── Intent contract ──────────────────────────────────────────────────────────
 * Start with no extra   → start foreground + VoiceSessionManager.start()
 * Start with ACTION_STOP → VoiceSessionManager.stop() + stopForeground + stopSelf
 *
 * START_STICKY: if the process is killed (OOM), the service is restarted and
 * VoiceSessionManager.start() reconnects automatically.
 */
class VoiceForegroundService : Service() {

    companion object {
        const val ACTION_STOP       = "com.Lia.assistant.VOICE_STOP"
        private const val CHANNEL_ID   = "lia_voice_session"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            VoiceSessionManager.stop(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        VoiceSessionManager.start(this)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceSessionManager.stop(this)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VoiceForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${AssistantBrand.NAME} is listening")
            .setContentText("Tap to open · Stop to end session")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "${AssistantBrand.NAME} voice session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description  = "Shown while ${AssistantBrand.NAME} is in a voice session"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
