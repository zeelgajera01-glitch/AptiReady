package com.example.aptiready.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Main-thread entry points; writes finish in tap order, before a queued finish/discard. */
class SessionActions(scope: CoroutineScope) {
    private data class Action(val key: String?, val terminal: Boolean, val work: suspend () -> Unit)
    private val queue = Channel<Action>(Channel.UNLIMITED)
    private val pendingKeys = mutableSetOf<String>()
    private var finishing = false
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    init {
        scope.launch {
            for (action in queue) {
                try {
                    action.work()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (action.terminal) finishing = false
                    _error.value = "Could not save progress. Please try again."
                    runCatching { android.util.Log.e("SessionActions", "Session operation failed", failure) }
                } finally {
                    action.key?.let { pendingKeys.remove(it) }
                    if (action.terminal) _busy.value = false
                }
            }
        }.invokeOnCompletion { queue.cancel() }
    }

    fun reportError(message: String) { _error.value = message }

    fun reopen() {
        if (!_busy.value) finishing = false
    }

    fun submit(key: String? = null, terminal: Boolean = false, work: suspend () -> Unit) {
        if (finishing || (key != null && key in pendingKeys)) return
        _error.value = null
        if (terminal) { finishing = true; _busy.value = true }
        key?.let { pendingKeys.add(it) }
        if (queue.trySend(Action(key, terminal, work)).isFailure) {
            key?.let { pendingKeys.remove(it) }
            if (terminal) { finishing = false; _busy.value = false }
        }
    }
}
