package com.Lia.assistant.voice

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.Lia.assistant.action.InstalledAppLabelCache
import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import com.Lia.assistant.data.PersonalityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import com.Lia.assistant.action.ActionExecutor

/**
 * Manages Lia's voice session lifecycle.
 *
 * ── Design decisions ─────────────────────────────────────────────────────────
 * sessionEpoch  : AtomicInteger bumped on every (re)connect so stale callbacks
 *                 from a previous session are silently dropped.
 * sessionMutex  : Ensures connectSession() never runs concurrently.
 * reconnectJob  : Single-flight — only one pending reconnect at a time.
 * micStarted    : Set to true after the first setupComplete to prevent double-starting.
 * watchdogJob   : 300 ms silence watchdog; re-armed on every audio chunk.
 *
 * ── Voice name mapping ───────────────────────────────────────────────────────
 * UI names  : Aria, Luna, Zara, Kavya, Priya, Maya, Nova, Echo, Atlas, Orion
 * Gemini    : Aoede, Fenrir, Kore, Charon, Puck, Leda, Kore, Aoede, Charon, Puck
 * Unmapped names fall back to "Aoede".
 */
object VoiceSessionManager {

    // ── Constants ─────────────────────────────────────────────────────────────
    private const val MAX_RECONNECTS        = 5
    private const val RECONNECT_DELAY_MS    = 1200L
    private const val PLAYBACK_WATCHDOG_MS  = 300L

    private val VOICE_NAME_MAP = mapOf(
        "Aria"   to "Aoede",
        "Luna"   to "Fenrir",
        "Zara"   to "Kore",
        "Kavya"  to "Charon",
        "Priya"  to "Puck",
        "Maya"   to "Leda",
        "Nova"   to "Kore",
        "Echo"   to "Aoede",
        "Atlas"  to "Charon",
        "Orion"  to "Puck",
    )

    // ── Coroutine scope ───────────────────────────────────────────────────────
    private val scope          = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex   = Mutex()

    // ── Session state ─────────────────────────────────────────────────────────
    private val sessionEpoch   = AtomicInteger(0)
    private var reconnectCount = 0
    private var reconnectJob   : Job? = null
    private var watchdogJob    : Job? = null
    private var client         : GeminiLiveClient? = null
    private var audioIO        : LiveAudioIO? = null
    @Volatile private var micStarted = false

    // ── Public StateFlows ─────────────────────────────────────────────────────
    private val _state               = MutableStateFlow(NovaOrbState.IDLE)
    val state                        : StateFlow<NovaOrbState>             = _state

    private val _amplitude           = MutableStateFlow(0f)
    val amplitude                    : StateFlow<Float>                    = _amplitude

    private val _muted               = MutableStateFlow(false)
    val muted                        : StateFlow<Boolean>                  = _muted

    private val _statusText          = MutableStateFlow("Tap to start")
    val statusText                   : StateFlow<String>                   = _statusText

    private val _isActive            = MutableStateFlow(false)
    val isActive                     : StateFlow<Boolean>                  = _isActive

    private val _lastError           = MutableStateFlow<String?>(null)
    val lastError                    : StateFlow<String?>                  = _lastError

    private val _currentPersonality  = MutableStateFlow(Personality.NORMAL)
    val currentPersonality           : StateFlow<Personality>              = _currentPersonality

    private val _currentLanguage     = MutableStateFlow(LanguagePreference.AUTO)
    val currentLanguage              : StateFlow<LanguagePreference>       = _currentLanguage

    private val _detectedLanguage    = MutableStateFlow(LanguageDetector.DetectedLanguage.UNKNOWN)
    val detectedLanguage             : StateFlow<LanguageDetector.DetectedLanguage> = _detectedLanguage

    private val _vadActive           = MutableStateFlow(false)
    val vadActive                    : StateFlow<Boolean>                  = _vadActive

    // In-session conversation memory (cleared on each new start, NOT on reconnect)
    val memory                       = ConversationMemory()

    // ── Public API ────────────────────────────────────────────────────────────

    /** Starts a new voice session.  No-op if already active. */
    fun start(ctx: Context) {
        if (_isActive.value) return
        val appCtx = ctx.applicationContext
        scope.launch {
            _isActive.value    = true
            _lastError.value   = null
            _state.value       = NovaOrbState.CONNECTING
            _statusText.value  = "Connecting…"
            reconnectCount     = 0
            micStarted         = false
            memory.clear()
            VoiceLatencyTracker.reset()
            InstalledAppLabelCache.prewarm(appCtx)
            audioIO = LiveAudioIO(audioListener)
            connectSession(appCtx, isResuming = false)
        }
    }

    /** Stops the session, cancels all coroutines and releases hardware. */
    fun stop(ctx: Context) {
        val epoch = sessionEpoch.incrementAndGet()
        reconnectJob?.cancel(); reconnectJob = null
        watchdogJob?.cancel();  watchdogJob  = null
        client?.disconnect();   client       = null
        audioIO?.release();     audioIO      = null
        micStarted              = false
        EchoGuard.reset()
        memory.clear()
        VoiceLatencyTracker.reset()
        _isActive.value        = false
        _state.value           = NovaOrbState.IDLE
        _statusText.value      = "Tap to start"
        _amplitude.value       = 0f
        _vadActive.value       = false
    }

    /** Mutes or unmutes the microphone without stopping the session. */
    fun toggleMute() {
        _muted.value = !_muted.value
        _statusText.value = if (_muted.value) "Muted" else "Listening"
    }

    /**
     * Reconnects with updated system instruction.
     * Called by [PersonalityRepository] when personality/language/name changes.
     */
    fun refreshInstructions(ctx: Context) {
        if (!_isActive.value) return
        scope.launch {
            connectSession(ctx.applicationContext, isResuming = true)
        }
    }

    // ── Private: session connect ──────────────────────────────────────────────

    private suspend fun connectSession(ctx: Context, isResuming: Boolean) =
        sessionMutex.withLock {
            val epoch = sessionEpoch.incrementAndGet()
            client?.disconnect()

            val personality   = PersonalityRepository.getPersonality(ctx)
            val language      = PersonalityRepository.getLanguage(ctx)
            val assistantName = PersonalityRepository.getAssistantName(ctx)
            val uiVoiceName   = PersonalityRepository.getVoiceName(ctx)
            val geminiVoice   = VOICE_NAME_MAP[uiVoiceName] ?: "Aoede"

            _currentPersonality.value = personality
            _currentLanguage.value    = language

            val recap = if (isResuming) memory.recap() else null
            val prompt = PersonalityPromptBuilder.build(
                personality      = personality,
                language         = language,
                detectedLanguage = _detectedLanguage.value,
                recap            = recap,
                assistantName    = assistantName,
            )

            val apiKey = com.Lia.assistant.data.ApiKeyStore.getKey(ctx)
            if (apiKey.isBlank()) {
                _state.value      = NovaOrbState.ERROR
                _statusText.value = "No API key — add one in Settings"
                _lastError.value  = "No API key"
                _isActive.value   = false
                return@withLock
            }

            VoiceLatencyTracker.mark(VoiceLatencyTracker.Event.SESSION_START)
            _state.value      = NovaOrbState.CONNECTING
            _statusText.value = if (isResuming) "Reconnecting…" else "Connecting…"

            val newClient = GeminiLiveClient(buildListener(ctx, epoch))
            client = newClient
            newClient.connect(
                systemInstruction = prompt,
                voiceName         = geminiVoice,
                apiKey            = apiKey,
            )
        }

    // ── Private: GeminiLiveClient.Listener ───────────────────────────────────

    private fun buildListener(ctx: Context, epoch: Int) = object : GeminiLiveClient.Listener {

        override fun onSetupComplete() {
            if (sessionEpoch.get() != epoch) return
            VoiceLatencyTracker.mark(VoiceLatencyTracker.Event.SETUP_COMPLETE)
            _state.value      = NovaOrbState.LISTENING
            _statusText.value = "Listening"
            if (!micStarted) {
                micStarted = true
                audioIO?.startMic()
            }
        }

        override fun onAudioChunk(bytes: ByteArray) {
            if (sessionEpoch.get() != epoch) return
            VoiceLatencyTracker.mark(VoiceLatencyTracker.Event.FIRST_AUDIO)
            EchoGuard.notifyPlaybackStarted()
            val rms = audioIO?.playChunk(bytes) ?: 0f
            _state.value      = NovaOrbState.SPEAKING
            _amplitude.value  = rms
            // Re-arm 300 ms playback-silence watchdog.
            watchdogJob?.cancel()
            watchdogJob = scope.launch {
                delay(PLAYBACK_WATCHDOG_MS)
                if (sessionEpoch.get() == epoch) {
                    EchoGuard.notifyPlaybackStopped()
                    _state.value      = NovaOrbState.LISTENING
                    _statusText.value = "Listening"
                    _amplitude.value  = 0f
                }
            }
        }

        override fun onInputTranscript(text: String) {
            if (sessionEpoch.get() != epoch) return
            memory.add(ConversationMemory.Role.USER, text)
            _detectedLanguage.value = LanguageDetector.detect(text)
            VoiceLatencyTracker.mark(VoiceLatencyTracker.Event.TURN_START)
            _state.value      = NovaOrbState.THINKING
            _statusText.value = "Thinking…"
        }

        override fun onOutputTranscript(text: String) {
            if (sessionEpoch.get() != epoch) return
            memory.add(ConversationMemory.Role.ASSISTANT, text)
        }

        override fun onToolCall(id: String, name: String, args: JSONObject) {
            if (sessionEpoch.get() != epoch) return
            _state.value      = NovaOrbState.THINKING
            _statusText.value = "Working on it…"
            scope.launch {
                val result: JSONObject = if (name == "build_website") {
                    // Forge wiring arrives in Part 14.
                    JSONObject().put("result", "forge_started")
                } else {
                    withContext(Dispatchers.IO) {
                        ActionExecutor.execute(ctx.applicationContext, name, args)
                    }
                }
                if (sessionEpoch.get() == epoch) {
                    client?.sendToolResponse(id, name, result)
                    _state.value      = NovaOrbState.LISTENING
                    _statusText.value = "Listening"
                }
            }
        }

        override fun onTurnComplete() {
            if (sessionEpoch.get() != epoch) return
            watchdogJob?.cancel()
            EchoGuard.notifyPlaybackStopped()
            _state.value      = NovaOrbState.LISTENING
            _statusText.value = "Listening"
            _amplitude.value  = 0f
        }

        override fun onInterrupted() {
            if (sessionEpoch.get() != epoch) return
            watchdogJob?.cancel()
            audioIO?.flushPlayback()
            EchoGuard.notifyPlaybackStopped()
            _state.value      = NovaOrbState.LISTENING
            _statusText.value = "Listening"
            _amplitude.value  = 0f
        }

        override fun onError(message: String) {
            if (sessionEpoch.get() != epoch) return
            if (_isActive.value) scheduleReconnect(ctx)
        }

        override fun onClosed() {
            if (sessionEpoch.get() != epoch) return
            if (_isActive.value) scheduleReconnect(ctx)
        }
    }

    // ── Private: mic forwarding ───────────────────────────────────────────────

    private val audioListener = object : LiveAudioIO.Listener {
        override fun onMicChunk(bytes: ByteArray) {
            if (_muted.value || EchoGuard.isMicMuted()) return
            client?.sendAudio(bytes)
        }
        override fun onMicAmplitude(rms: Float) {
            _amplitude.value = if (_state.value == NovaOrbState.LISTENING) rms else _amplitude.value
            _vadActive.value = rms > 0.02f
        }
    }

    // ── Private: reconnect ────────────────────────────────────────────────────

    private fun scheduleReconnect(ctx: Context) {
        if (reconnectJob?.isActive == true) return   // already scheduled
        reconnectJob = scope.launch {
            reconnectCount++
            VoiceLatencyTracker.incrementReconnect()
            if (reconnectCount > MAX_RECONNECTS) {
                _state.value      = NovaOrbState.ERROR
                _statusText.value = "Connection lost — tap Stop and reopen Voice Mode"
                _lastError.value  = "Connection lost after $MAX_RECONNECTS attempts"
                _isActive.value   = false
                return@launch
            }
            _state.value      = NovaOrbState.RECONNECTING
            _statusText.value = "Reconnecting… (attempt $reconnectCount)"
            delay(RECONNECT_DELAY_MS)
            connectSession(ctx, isResuming = true)
        }
    }
}
