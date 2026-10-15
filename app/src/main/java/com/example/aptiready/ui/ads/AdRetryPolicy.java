package com.example.aptiready.ui.ads;

/** Backoff for failed requests. Call only from the main thread; time is monotonic. */
public final class AdRetryPolicy {
    private static final long BASE_DELAY_MS = 60_000L;
    private static final long MAX_DELAY_MS = 300_000L;
    private int failures;
    private long nextAttemptMs;
    private boolean permanentFailure;

    public boolean canAttempt(long nowMs) {
        return !permanentFailure && nowMs >= nextAttemptMs;
    }

    public long delayUntilAttempt(long nowMs) {
        if (permanentFailure) return Long.MAX_VALUE;
        return Math.max(0L, nextAttemptMs - nowMs);
    }

    public void recordFailure(long nowMs, boolean retryable) {
        if (!retryable) {
            permanentFailure = true;
            nextAttemptMs = Long.MAX_VALUE;
            return;
        }
        failures = Math.min(4, failures + 1);
        long delay = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * (1L << (failures - 1)));
        nextAttemptMs = nowMs + delay;
    }

    public void reset() {
        failures = 0;
        permanentFailure = false;
        nextAttemptMs = 0L;
    }
}
