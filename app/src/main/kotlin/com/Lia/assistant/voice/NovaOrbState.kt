package com.Lia.assistant.voice

/**
 * All visual states the 3-D orb and the screen-edge glow can represent.
 *
 * The UI observes [VoiceSessionManager.orbState] and maps each value to
 * a shader parameter / colour in the orb composable (Part 10).
 */
enum class NovaOrbState {
    /** No active session. Orb is dim and gently pulsing. */
    IDLE,

    /** WebSocket is connecting. Orb spins slowly. */
    CONNECTING,

    /** Session is open. Microphone is live. Orb reacts to mic amplitude. */
    LISTENING,

    /** Gemini is generating audio. Orb reacts to speaker amplitude. */
    SPEAKING,

    /** A tool call is being executed (e.g. opening an app). */
    THINKING,

    /** The session dropped and is waiting before a reconnect attempt. */
    RECONNECTING,

    /** A fatal error occurred. Orb is red. */
    ERROR,
}
