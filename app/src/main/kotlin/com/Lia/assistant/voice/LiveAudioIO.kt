package com.Lia.assistant.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlin.math.sqrt

/**
 * Manages the mic (AudioRecord) and speaker (AudioTrack) for Lia's voice session.
 *
 * ── Mic ───────────────────────────────────────────────────────────────────────
 * 16 000 Hz · mono · PCM16 · VOICE_COMMUNICATION source
 * AEC, NS and AGC are attached where the hardware supports them.
 * Mic chunks are 2 048 bytes (= 512 PCM samples ≈ 32 ms).
 * The read loop runs on the dedicated [micThread] — never the main thread.
 * [EchoGuard.isMicMuted] is checked before forwarding each chunk so Lia
 * does not hear herself through the room.
 *
 * ── Speaker ───────────────────────────────────────────────────────────────────
 * 24 000 Hz · mono · PCM16 · USAGE_ASSISTANT + CONTENT_TYPE_SPEECH
 * (NOT USAGE_VOICE_COMMUNICATION — that path adds latency-inducing processing).
 * The AudioTrack is created lazily on the first [playChunk] call.
 * Buffer = minBufSize × 4 to absorb network jitter.
 *
 * ── Latency fixes applied ─────────────────────────────────────────────────────
 * - USAGE_ASSISTANT avoids the voice-call DSP path.
 * - 4× buffer avoids underrun glitches.
 * - No logging inside the audio read/write loops.
 */
class LiveAudioIO(private val listener: Listener) {

    interface Listener {
        /** Called for every mic chunk that is NOT muted.  Bytes are raw PCM16 @ 16 kHz. */
        fun onMicChunk(bytes: ByteArray)

        /**
         * Called for every mic chunk (muted or not) with the RMS amplitude 0..1.
         * Use this to animate the orb in response to the user's voice.
         */
        fun onMicAmplitude(rms: Float)
    }

    companion object {
        private const val MIC_SAMPLE_RATE    = 16_000
        private const val SPK_SAMPLE_RATE    = 24_000
        private const val CHANNEL_IN         = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT        = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING           = AudioFormat.ENCODING_PCM_16BIT
        private const val MIC_CHUNK_BYTES    = 2048
    }

    // ── Mic state ─────────────────────────────────────────────────────────────

    private var audioRecord : AudioRecord? = null
    private var aec         : AcousticEchoCanceler? = null
    private var ns          : NoiseSuppressor?       = null
    private var agc         : AutomaticGainControl?  = null
    @Volatile private var micRunning = false
    private var micThread   : Thread? = null

    // ── Speaker state ─────────────────────────────────────────────────────────

    private var audioTrack  : AudioTrack? = null

    // ── Public: mic ───────────────────────────────────────────────────────────

    /**
     * Creates AudioRecord, attaches audio effects and starts the read loop.
     *
     * Must be called from a thread that holds RECORD_AUDIO permission (checked
     * by the system; the call itself does not block the main thread because the
     * actual reading runs on [micThread]).
     */
    fun startMic() {
        val minBuf = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, CHANNEL_IN, ENCODING)
        val bufSize = maxOf(minBuf, MIC_CHUNK_BYTES * 2)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MIC_SAMPLE_RATE,
            CHANNEL_IN,
            ENCODING,
            bufSize,
        )
        audioRecord = record

        // Attach audio effects where hardware supports them (each may throw).
        val sessionId = record.audioSessionId
        runCatching { if (AcousticEchoCanceler.isAvailable())  aec = AcousticEchoCanceler.create(sessionId) }
        runCatching { if (NoiseSuppressor.isAvailable())        ns  = NoiseSuppressor.create(sessionId) }
        runCatching { if (AutomaticGainControl.isAvailable())   agc = AutomaticGainControl.create(sessionId) }

        record.startRecording()
        micRunning = true

        micThread = Thread({
            val buf = ByteArray(MIC_CHUNK_BYTES)
            while (micRunning) {
                val read = record.read(buf, 0, buf.size)
                if (read <= 0) continue

                val chunk = buf.copyOf(read)
                val rms   = rms(chunk)

                // Always deliver amplitude so the orb reacts even while muted.
                listener.onMicAmplitude(rms)

                // Only forward audio when EchoGuard says we are not in echo territory.
                if (!EchoGuard.isMicMuted()) {
                    listener.onMicChunk(chunk)
                }
            }
        }, "lia-mic-reader").also { it.start() }
    }

    /** Stops the mic read loop and releases AudioRecord + effects. */
    fun stopMic() {
        micRunning = false
        micThread?.interrupt()
        micThread = null

        aec?.release(); aec = null
        ns?.release();  ns  = null
        agc?.release(); agc = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    // ── Public: speaker ───────────────────────────────────────────────────────

    /**
     * Writes [bytes] (24 kHz PCM16) to the AudioTrack.
     * Creates the track lazily on the first call.
     *
     * [EchoGuard] is notified before and after the write so the mic is muted
     * while audio is coming out of the speaker.
     *
     * @return RMS amplitude of the chunk (0..1) for orb animation.
     */
    fun playChunk(bytes: ByteArray): Float {
        val track = audioTrack ?: createAudioTrack().also { audioTrack = it }
        EchoGuard.notifyPlaybackStarted()
        track.write(bytes, 0, bytes.size)
        EchoGuard.notifyPlaybackStopped()
        return rms(bytes)
    }

    /**
     * Pauses, flushes and resumes the AudioTrack.
     * Call when a barge-in interrupts Lia mid-sentence.
     */
    fun flushPlayback() {
        audioTrack?.let {
            it.pause()
            it.flush()
            it.play()
        }
        EchoGuard.reset()
    }

    /** Updates the AudioTrack volume (0.0 .. 1.0). */
    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }

    /** Stops and releases the AudioTrack.  Call when the session ends. */
    fun release() {
        stopMic()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        EchoGuard.reset()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun createAudioTrack(): AudioTrack {
        val minBuf  = AudioTrack.getMinBufferSize(SPK_SAMPLE_RATE, CHANNEL_OUT, ENCODING)
        val bufSize = minBuf * 4   // 4× to absorb jitter without underrun

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)          // NOT VOICE_COMMUNICATION
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SPK_SAMPLE_RATE)
            .setChannelMask(CHANNEL_OUT)
            .build()

        return AudioTrack(attrs, format, bufSize, AudioTrack.MODE_STREAM, 0)
            .also { it.play() }
    }

    /**
     * Root-mean-square of a little-endian PCM16 buffer, normalised to 0..1.
     *
     * Safe to call from the audio thread — no allocations beyond the local variables.
     */
    private fun rms(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f
        var sum = 0.0
        var i   = 0
        while (i + 1 < pcm.size) {
            // Little-endian: low byte first, high byte second.
            val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
            sum += sample.toDouble() * sample.toDouble()
            i   += 2
        }
        val sampleCount = pcm.size / 2
        return (sqrt(sum / sampleCount) / 32768.0).toFloat().coerceIn(0f, 1f)
    }
}
