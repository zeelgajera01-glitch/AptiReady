package com.example.aptiready.ui.ads

/** Main-thread cache shared by ad formats. Late callbacks cannot replace newer requests. */
internal class AdLoadSlot<T>(
    private val now: () -> Long,
    private val dispose: (T) -> Unit = {}
) {
    private val retry = AdRetryPolicy()
    private var key: String? = null
    private var value: T? = null
    private var loadedAt = 0L
    private var startedAt = 0L
    private var loading = false
    private var generation = 0L

    fun cached(requestKey: String): T? {
        select(requestKey)
        if (value != null && now() - loadedAt >= AdConfig.FrequencyPolicy.AD_EXPIRE_DURATION_MS) {
            value?.let(dispose)
            value = null
        }
        return value
    }

    fun begin(requestKey: String): Long? {
        if (cached(requestKey) != null) return null
        if (loading && now() - startedAt < REQUEST_TIMEOUT_MS) return null
        if (loading) {
            loading = false
            generation++
            retry.recordFailure(now(), true)
        }
        if (!retry.canAttempt(now())) return null
        loading = true
        startedAt = now()
        return ++generation
    }

    fun accept(token: Long, ad: T): Boolean {
        if (token != generation || !loading) {
            dispose(ad)
            return false
        }
        loading = false
        value = ad
        loadedAt = now()
        retry.reset()
        return true
    }

    fun fail(token: Long, retryable: Boolean): Boolean {
        if (token != generation || !loading) return false
        loading = false
        retry.recordFailure(now(), retryable)
        return true
    }

    fun take(requestKey: String): T? = cached(requestKey).also { value = null }

    fun clear() {
        generation++
        loading = false
        value?.let(dispose)
        value = null
        key = null
        retry.reset()
    }

    private fun select(requestKey: String) {
        if (key == requestKey) return
        clear()
        key = requestKey
    }

    companion object { private const val REQUEST_TIMEOUT_MS = 60_000L }
}
