package com.example.daugia.common.logging;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AutoCloseable MDC context manager — Builder + AutoCloseable pattern.
 *
 * <p>Manages a scoped set of MDC keys: on {@link #close()} only the keys
 * added by <em>this</em> context are removed, leaving any pre-existing
 * MDC entries untouched.
 *
 * <pre>{@code
 * try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
 *                          .and(LogField.ACTOR, email)
 *                          .build()) {
 *     // MDC populated here
 * } // MDC cleaned automatically on exit
 * }</pre>
 */
public final class LogContext implements AutoCloseable {

    /** Keys added by THIS context instance (not the whole MDC). */
    private final Map<String, String> ownKeys;

    private LogContext(Map<String, String> ownKeys) {
        this.ownKeys = ownKeys;
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    /**
     * Start building a context with the first key-value pair.
     *
     * @param field the log field key
     * @param value the value; null values are skipped
     * @return a builder for chaining
     */
    public static Builder of(LogField field, String value) {
        return new Builder().and(field, value);
    }

    /** Fluent builder that accumulates key-value pairs before committing to MDC. */
    public static final class Builder {

        private final Map<String, String> entries = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Add another key-value pair.
         *
         * @param field the log field key
         * @param value the value; null values are skipped
         * @return this builder
         */
        public Builder and(LogField field, String value) {
            if (field != null && value != null) {
                entries.put(field.key(), value);
            }
            return this;
        }

        /**
         * Commit all accumulated entries to MDC and return an {@link AutoCloseable}
         * that will clean them up on exit.
         *
         * @return a live {@link LogContext}; use in a try-with-resources block
         */
        public LogContext build() {
            entries.forEach(MDC::put);
            return new LogContext(new LinkedHashMap<>(entries));
        }
    }

    // ------------------------------------------------------------------
    // AutoCloseable
    // ------------------------------------------------------------------

    /**
     * Removes only the keys that were added by this context from MDC.
     * Pre-existing MDC state is preserved.
     */
    @Override
    public void close() {
        ownKeys.keySet().forEach(MDC::remove);
    }
}
