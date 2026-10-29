package app.lumio.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.min

/** 5-point dB curves (60Hz..14kHz) interpolated to the device's band count. Spec: docs/08. */
object EqPresets {
    val curves: Map<String, FloatArray> = linkedMapOf(
        "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Bass" to floatArrayOf(6f, 4f, 0f, -1f, -2f),
        "Treble" to floatArrayOf(-2f, -1f, 0f, 4f, 6f),
        "Vocal" to floatArrayOf(-2f, 0f, 4f, 3f, 0f),
        "Rock" to floatArrayOf(5f, 2f, -1f, 3f, 5f),
        "Pop" to floatArrayOf(-1f, 3f, 4f, 2f, -1f),
        "Jazz" to floatArrayOf(3f, 2f, -2f, 2f, 4f),
        "Classical" to floatArrayOf(4f, 3f, -1f, 3f, 4f),
        "Electronic" to floatArrayOf(5f, 3f, 0f, 2f, 5f),
    )

    fun levelsMb(name: String, bands: Int, minMb: Int, maxMb: Int): IntArray {
        val c = curves[name] ?: curves.getValue("Flat")
        return IntArray(bands) { i ->
            val pos = if (bands <= 1) 0f else i.toFloat() / (bands - 1) * (c.size - 1)
            val lo = floor(pos).toInt()
            val hi = min(lo + 1, c.size - 1)
            val f = pos - lo
            val db = c[lo] * (1 - f) + c[hi] * f
            (db * 100).toInt().coerceIn(minMb, maxMb)
        }
    }
}

class AudioEffectsController(private val settings: SettingsRepository, private val scope: CoroutineScope) {
    data class Info(
        val supported: Boolean = false,
        val bands: Int = 0,
        val minMb: Int = 0,
        val maxMb: Int = 0,
        val centerHz: List<Int> = emptyList(),
        val bassSupported: Boolean = false,
        val virtSupported: Boolean = false,
        val loudSupported: Boolean = false,
    )

    private val _info = MutableStateFlow(Info())
    val info: StateFlow<Info> = _info.asStateFlow()

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var sessionId = 0
    private var last = AppSettings()

    init {
        scope.launch { settings.flow.collect { s -> last = s; applyNow() } }
    }

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == sessionId) return
        releaseAll()
        sessionId = audioSessionId
        try { eq = Equalizer(0, audioSessionId) } catch (t: Throwable) { Log.w(TAG, "EQ unavailable", t) }
        try { bass = BassBoost(0, audioSessionId) } catch (t: Throwable) { Log.w(TAG, "Bass unavailable", t) }
        try { virt = Virtualizer(0, audioSessionId) } catch (t: Throwable) { Log.w(TAG, "Virtualizer unavailable", t) }
        try { loud = LoudnessEnhancer(audioSessionId) } catch (t: Throwable) { Log.w(TAG, "Loudness unavailable", t) }
        publishInfo()
        applyNow()
    }

    private fun publishInfo() {
        val e = eq
        _info.value = if (e != null) try {
            val range = e.bandLevelRange
            Info(
                supported = true,
                bands = e.numberOfBands.toInt(),
                minMb = range[0].toInt(),
                maxMb = range[1].toInt(),
                centerHz = (0 until e.numberOfBands).map { e.getCenterFreq(it.toShort()) / 1000 },
                bassSupported = bass?.strengthSupported == true,
                virtSupported = virt?.strengthSupported == true,
                loudSupported = loud != null,
            )
        } catch (t: Throwable) { Info() } else Info(loudSupported = loud != null)
    }

    @Synchronized
    private fun applyNow() {
        val s = last
        try {
            eq?.let { e ->
                e.enabled = s.eqEnabled
                if (s.eqEnabled) {
                    val n = e.numberOfBands.toInt()
                    val range = e.bandLevelRange
                    val levels = parseBands(s.eqBands, n)
                    for (i in 0 until n) e.setBandLevel(i.toShort(), levels[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                }
            }
        } catch (t: Throwable) { Log.w(TAG, "EQ apply failed", t) }
        try {
            bass?.let { b -> b.enabled = s.bassBoost > 0; if (b.strengthSupported) b.setStrength(s.bassBoost.coerceIn(0, 1000).toShort()) }
        } catch (t: Throwable) { Log.w(TAG, "Bass apply failed", t) }
        try {
            virt?.let { v -> v.enabled = s.virtualizer > 0; if (v.strengthSupported) v.setStrength(s.virtualizer.coerceIn(0, 1000).toShort()) }
        } catch (t: Throwable) { Log.w(TAG, "Virtualizer apply failed", t) }
        try {
            loud?.let { l ->
                val gain = s.loudnessMb + if (s.normalizeVolume) 500 else 0
                l.enabled = gain > 0
                l.setTargetGain(gain.coerceIn(0, 3000))
            }
        } catch (t: Throwable) { Log.w(TAG, "Loudness apply failed", t) }
    }

    @Synchronized
    fun releaseAll() {
        listOf<android.media.audiofx.AudioEffect?>(eq, bass, virt, loud).forEach { try { it?.release() } catch (_: Throwable) {} }
        eq = null; bass = null; virt = null; loud = null
        sessionId = 0
    }

    // ---- UI helpers ----
    fun setPreset(name: String) {
        val i = _info.value
        if (!i.supported) return
        val levels = EqPresets.levelsMb(name, i.bands, i.minMb, i.maxMb)
        scope.launch { settings.update { copy(eqPreset = name, eqBands = levels.joinToString(","), eqEnabled = true) } }
    }

    fun setBand(index: Int, mb: Int) {
        val i = _info.value
        if (!i.supported) return
        val cur = parseBands(last.eqBands, i.bands).also { it[index] = mb.coerceIn(i.minMb, i.maxMb) }
        scope.launch { settings.update { copy(eqPreset = "Custom", eqBands = cur.joinToString(","), eqEnabled = true) } }
    }

    companion object {
        private const val TAG = "AudioEffects"
        fun parseBands(csv: String, n: Int): IntArray {
            val parts = csv.split(',').mapNotNull { it.trim().toIntOrNull() }
            return IntArray(n) { parts.getOrElse(it) { 0 } }
        }
    }
}
