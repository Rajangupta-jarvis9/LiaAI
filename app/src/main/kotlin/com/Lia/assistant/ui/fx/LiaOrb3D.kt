package com.Lia.assistant.ui.fx

import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import kotlin.math.sqrt

// ── AGSL shader source ────────────────────────────────────────────────────────
// Language: AGSL (Android Graphics Shading Language) — GLSL ES 3 subset.

private const val ORB_SHADER = """
uniform float2 resolution;
uniform float  time;
uniform float  amplitude;
uniform float2 tilt;         // normalized [-1..1]

uniform half4  uCoreColor;
uniform half4  uRimColor;
uniform half4  uHaloColor;

// ── Noise helpers ─────────────────────────────────────────────────────────────
float hash(float2 p) {
    p = fract(p * float2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}
float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),             hash(i + float2(1,0)), u.x),
               mix(hash(i + float2(0,1)), hash(i + float2(1,1)), u.x), u.y);
}
float fbm(float2 p) {
    float v = 0.0; float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p); p *= 2.1; a *= 0.5;
    }
    return v;
}

// ── Main ──────────────────────────────────────────────────────────────────────
half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - resolution * 0.5) / (resolution.y * 0.5);

    // Tilt-driven light direction
    float3 lightDir = normalize(float3(tilt.x * 0.6, -tilt.y * 0.6, 1.0));

    // Analytic sphere hit test
    float r = 1.0 + amplitude * 0.06;
    float zSq = r * r - dot(uv, uv);
    if (zSq < 0.0) {
        // Outside sphere — soft halo
        float dist  = length(uv);
        float halo  = smoothstep(1.6, r, dist) * 0.35;
        return half4(half3(uHaloColor.rgb) * half(halo), half(halo));
    }
    float z = sqrt(zSq);

    // Analytic normal
    float3 N  = normalize(float3(uv, z));
    float3 V  = float3(0.0, 0.0, 1.0);

    // Diffuse
    float diff = max(dot(N, lightDir), 0.0);

    // Specular
    float3 H   = normalize(lightDir + V);
    float spec = pow(max(dot(N, H), 0.0), 64.0);

    // FBM energy interior
    float2 noiseUV = uv * 1.4 + float2(time * 0.07, time * 0.05);
    float  energy  = fbm(noiseUV + float2(fbm(noiseUV * 2.0)));

    // Fresnel rim
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.5);

    // Assemble colour
    half3 core = half3(uCoreColor.rgb);
    half3 col  = core * half(0.15 + diff * 0.6 + energy * 0.35);
    col += half3(1.0, 0.95, 0.85) * half(spec * 0.8);
    col  = mix(col, half3(uRimColor.rgb), half(fresnel * 0.7));

    // Soft edge alpha
    float alpha = smoothstep(0.0, 0.03, z / r);
    return half4(col * half(alpha), half(alpha));
}
"""

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * On API 33+ renders a full per-pixel lit sphere via AGSL [RuntimeShader].
 * On older APIs falls back to [NovaOrb] (Canvas).
 *
 * Tilt is derived from the accelerometer when available; gracefully degrades
 * to zero tilt. Unregistered on dispose.
 */
@Composable
fun LiaOrb3D(
    state        : NovaOrbState  = NovaOrbState.IDLE,
    amplitude    : Float         = 0f,
    style        : NovaOrbStyle  = NovaOrbStyle.ENERGY,
    assistantName: String        = "Lia",
    modifier     : Modifier      = Modifier,
) {
    val reducedMotion = LocalNovaReducedMotion.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        NovaOrb(state, amplitude, style, assistantName, modifier, reducedMotion)
        return
    }

    LiaOrb3DShader(state, amplitude, assistantName, modifier, reducedMotion)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LiaOrb3DShader(
    state        : NovaOrbState,
    amplitude    : Float,
    assistantName: String,
    modifier     : Modifier,
    reducedMotion: Boolean,
) {
    val c     = LocalNovaColors.current
    val ctx   = LocalContext.current

    // ── Tilt from accelerometer ───────────────────────────────────────────────
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        if (reducedMotion) return@DisposableEffect onDispose {}
        val sm = ctx.getSystemService(SensorManager::class.java) ?: return@DisposableEffect onDispose {}
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return@DisposableEffect onDispose {}
        val listener = object : SensorEventListener {
            private var lx = 0f; private var ly = 0f
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            override fun onSensorChanged(e: SensorEvent) {
                // Low-pass filter α = 0.15
                lx = lx + 0.15f * (e.values[0] / 9.81f - lx)
                ly = ly + 0.15f * (e.values[1] / 9.81f - ly)
                tiltX = lx.coerceIn(-1f, 1f)
                tiltY = ly.coerceIn(-1f, 1f)
            }
        }
        sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // ── Shader colours per state ──────────────────────────────────────────────
    val (coreColor, rimColor, haloColor) = when (state) {
        NovaOrbState.IDLE        -> Triple(c.lotus.copy(alpha = 0.8f), c.lagoon, c.lagoon.copy(alpha = 0.3f))
        NovaOrbState.LISTENING   -> Triple(c.lagoon, c.lagoon.copy(alpha = 0.6f), c.lagoon.copy(alpha = 0.25f))
        NovaOrbState.THINKING    -> Triple(c.accent, c.accentSecondary, c.accentGlow)
        NovaOrbState.SPEAKING    -> Triple(c.lotus, c.accent, c.lotus.copy(alpha = 0.3f))
        NovaOrbState.CONNECTING  -> Triple(c.warning, c.warning.copy(alpha = 0.7f), c.warning.copy(alpha = 0.2f))
        NovaOrbState.RECONNECTING-> Triple(c.warning.copy(alpha = 0.6f), c.textTertiary, c.depthShadow)
        NovaOrbState.ERROR       -> Triple(c.error, c.error.copy(alpha = 0.6f), c.error.copy(alpha = 0.15f))
    }

    // ── Time ─────────────────────────────────────────────────────────────────
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            withFrameMillis { now ->
                time = (now - start) / 1000f
            }
        }
    }

    val shader = remember { RuntimeShader(ORB_SHADER) }
    val desc   = "$assistantName status: ${state.name.lowercase()}"

    Canvas(modifier = modifier.semantics { contentDescription = desc }) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time",       time)
        shader.setFloatUniform("amplitude",  amplitude)
        shader.setFloatUniform("tilt",       tiltX, tiltY)
        shader.setColorUniform("uCoreColor", coreColor.toArgb())
        shader.setColorUniform("uRimColor",  rimColor.toArgb())
        shader.setColorUniform("uHaloColor", haloColor.toArgb())

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply { this.shader = shader }
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}
