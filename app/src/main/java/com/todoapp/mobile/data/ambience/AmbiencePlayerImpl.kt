package com.todoapp.mobile.data.ambience

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.todoapp.mobile.di.MainDispatcher
import com.todoapp.mobile.domain.ambience.AmbiencePlaybackState
import com.todoapp.mobile.domain.ambience.AmbiencePlayer
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single looping [MediaPlayer] plus audio-focus etiquette.
 *
 * Why `MediaPlayer` and not ExoPlayer: this plays one local file on repeat. media3 would add
 * roughly a megabyte to a release bundle that has ~3 MiB of headroom against the CI size gate,
 * and buys nothing here. The loops are seam-matched offline by `tools/prep_ambience.sh`, so
 * `isLooping` restarts on material that already follows on from the file's last sample.
 *
 * Deliberately **not** copied from `RingtoneHolder`: that class bails out when the ringer is in
 * silent/vibrate mode, which is right for an alarm (`USAGE_ALARM` would otherwise punch through
 * silent mode) and wrong here. Ambience is `USAGE_MEDIA`, and the media stream is independent of
 * ringer mode — muting music because the phone is on vibrate would be a bug.
 */
@Singleton
class AmbiencePlayerImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : AmbiencePlayer {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private val _state = MutableStateFlow(AmbiencePlaybackState())
    override val state = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var fadeJob: Job? = null

    /** User-chosen level. [currentGain] rides below it during a fade or a transient duck. */
    private var targetVolume: Float = 1f
    private var currentGain: Float = 0f
    private var duckedByFocus: Boolean = false

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                // Something took over for good (another music app). Give up rather than fight it.
                AudioManager.AUDIOFOCUS_LOSS -> stop()
                // A call, a navigation prompt: hold position and come back.
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    duckedByFocus = true
                    fadeTo(targetVolume * DUCK_FACTOR)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    duckedByFocus = false
                    if (player?.isPlaying == true) fadeTo(targetVolume) else resume()
                }
            }
        }

    private var focusRequest: AudioFocusRequest? = null

    // ── Controls ──────────────────────────────────────────────────────────────

    override fun play(ambience: PomodoroAmbience) {
        if (ambience == PomodoroAmbience.None) {
            stop()
            return
        }
        if (_state.value.current == ambience && player?.isPlaying == true) {
            fadeTo(targetVolume)
            return
        }

        releasePlayer()
        val rawRes = AmbienceAssets.rawResFor(ambience) ?: run {
            stop()
            return
        }
        if (!requestFocus()) {
            Timber.tag(TAG).d("audio focus denied; not starting %s", ambience.id)
            return
        }

        val created =
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    context.resources.openRawResourceFd(rawRes).use { fd ->
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                    setVolume(0f, 0f)
                    setOnPreparedListener { prepared ->
                        // Preparing is asynchronous and stop() fades for 400 ms before releasing,
                        // so a cancelled start can still get its callback. Only the player this
                        // instance currently owns is allowed to begin.
                        if (player !== prepared) return@setOnPreparedListener
                        runCatching { prepared.start() }
                            .onFailure { Timber.tag(TAG).w(it, "start after prepare failed") }
                        _state.update { it.copy(current = ambience, isPlaying = true) }
                        fadeTo(if (duckedByFocus) targetVolume * DUCK_FACTOR else targetVolume)
                    }
                    setOnErrorListener { _, what, extra ->
                        Timber.tag(TAG).w("MediaPlayer error what=%d extra=%d", what, extra)
                        stop()
                        true
                    }
                }
            }.onFailure { Timber.tag(TAG).w(it, "failed to open %s", ambience.id) }.getOrNull()

        if (created == null) {
            abandonFocus()
            return
        }
        // Publish before preparing: the callback checks identity against this field, and
        // prepareAsync can deliver as soon as it is called.
        player = created
        currentGain = 0f
        _state.update { it.copy(current = ambience) }
        runCatching { created.prepareAsync() }
            .onFailure {
                Timber.tag(TAG).w(it, "prepareAsync failed for %s", ambience.id)
                stop()
            }
    }

    override fun pause() {
        val active = player ?: return
        fadeTo(0f) {
            runCatching { if (active.isPlaying) active.pause() }
            _state.update { it.copy(isPlaying = false) }
        }
    }

    override fun resume() {
        val active = player ?: return
        if (active.isPlaying) return
        if (!requestFocus()) return
        val started = runCatching { active.start() }.onFailure { Timber.tag(TAG).w(it, "resume failed") }
        if (started.isFailure) return
        _state.update { it.copy(isPlaying = true) }
        fadeTo(if (duckedByFocus) targetVolume * DUCK_FACTOR else targetVolume)
    }

    override fun stop() {
        val active = player
        if (active == null) {
            _state.value = AmbiencePlaybackState()
            abandonFocus()
            return
        }
        fadeTo(0f) {
            releasePlayer()
            abandonFocus()
            _state.value = AmbiencePlaybackState()
        }
    }

    override fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        if (player?.isPlaying == true) {
            // Slider drags arrive continuously; snap instead of queuing a fade per frame.
            fadeJob?.cancel()
            applyGain(if (duckedByFocus) targetVolume * DUCK_FACTOR else targetVolume)
        }
    }

    override fun shutdown() {
        fadeJob?.cancel()
        releasePlayer()
        abandonFocus()
        _state.value = AmbiencePlaybackState()
        scope.cancel()
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Ramps [currentGain] to [target] so starts and stops breathe instead of clicking. */
    private fun fadeTo(
        target: Float,
        onFinished: (() -> Unit)? = null,
    ) {
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                val from = currentGain
                for (step in 1..FADE_STEPS) {
                    val progress = step.toFloat() / FADE_STEPS
                    applyGain(from + (target - from) * progress)
                    delay(FADE_STEP_MILLIS)
                }
                applyGain(target)
                onFinished?.invoke()
            }
    }

    private fun applyGain(gain: Float) {
        currentGain = gain.coerceIn(0f, 1f)
        runCatching { player?.setVolume(currentGain, currentGain) }
    }

    private fun releasePlayer() {
        val active = player ?: return
        player = null
        currentGain = 0f
        runCatching {
            if (active.isPlaying) active.stop()
            active.release()
        }.onFailure { Timber.tag(TAG).w(it, "release failed") }
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return false
        focusRequest?.let { return manager.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }

        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                // Duck rather than stop: a navigation prompt shouldn't end a focus session.
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
        focusRequest = request
        return manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        focusRequest = null
        duckedByFocus = false
    }

    private companion object {
        /** ~400 ms of fade at roughly one step per frame. */
        const val FADE_STEP_MILLIS: Long = 16L
        const val FADE_STEPS: Int = 25
        const val DUCK_FACTOR: Float = 0.2f
        const val TAG: String = "AmbiencePlayer"
    }
}
