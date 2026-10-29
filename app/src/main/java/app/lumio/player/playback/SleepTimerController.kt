package app.lumio.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerController(private val scope: CoroutineScope) {
    data class State(val active: Boolean = false, val remainingMs: Long = 0L, val endOfTrack: Boolean = false)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when playback should pause. */
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    private var job: Job? = null

    fun start(minutes: Int) {
        cancel()
        job = scope.launch {
            var left = minutes * 60_000L
            while (left > 0) {
                _state.value = State(true, left, false)
                delay(1000)
                left -= 1000
            }
            _state.value = State()
            _events.tryEmit(Unit)
        }
    }

    fun startEndOfTrack() {
        cancel()
        _state.value = State(active = true, remainingMs = 0, endOfTrack = true)
    }

    /** Called by the service on automatic track transitions. Returns true if playback should pause now. */
    fun consumeEndOfTrack(): Boolean {
        if (_state.value.endOfTrack) {
            _state.value = State()
            return true
        }
        return false
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State()
    }
}
