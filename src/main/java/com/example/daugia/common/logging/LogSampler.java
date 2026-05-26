package com.example.daugia.common.logging;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Probabilistic sampler for high-volume, low-value log paths.
 *
 * <p>Uses {@link ThreadLocalRandom} — zero contention, no shared state.
 *
 * <pre>{@code
 * // Log only 5% of leaderboard read calls
 * if (LogSampler.shouldLog("leaderboard.read", 0.05)) {
 *     log.debug("getTop called: auctionId={}", auctionId);
 * }
 * }</pre>
 */
public final class LogSampler {

    /**
     * Decide whether to emit a log line for the given operation.
     *
     * @param operationName  a descriptive name used for documentation/debugging
     * @param sampleRate     probability in [0.0, 1.0]:
     *                       {@code 1.0} = always log,
     *                       {@code 0.1} = log ~10% of the time,
     *                       {@code 0.0} = never log
     * @return {@code true} if this call should be logged
     */
    public static boolean shouldLog(String operationName, double sampleRate) {
        if (sampleRate <= 0.0) return false;
        if (sampleRate >= 1.0) return true;
        return ThreadLocalRandom.current().nextDouble() < sampleRate;
    }

    private LogSampler() {
        // utility class — no instances
    }
}
