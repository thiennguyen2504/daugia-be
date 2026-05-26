package com.example.daugia.common.utils;

import com.example.daugia.common.logging.SensitiveDataRedactor;

/**
 * Backward-compatibility facade over {@link SensitiveDataRedactor}.
 *
 * <p>Existing callers continue to compile with zero changes.
 * All logic now lives in {@link SensitiveDataRedactor} — do not add new methods here;
 * add them there and add a delegate here only if absolutely necessary.
 *
 * @deprecated Prefer calling {@link SensitiveDataRedactor} directly in new code.
 */
@Deprecated(since = "2.0", forRemoval = false)
public class LogSanitizer {

    /** @see SensitiveDataRedactor#maskEmail(String) */
    public static String maskEmail(String email) {
        return SensitiveDataRedactor.maskEmail(email);
    }

    /** @see SensitiveDataRedactor#maskToken(String) */
    public static String maskToken(String token) {
        return SensitiveDataRedactor.maskToken(token);
    }

    /** @see SensitiveDataRedactor#maskIp(String) */
    public static String maskIp(String ip) {
        return SensitiveDataRedactor.maskIp(ip);
    }

    private LogSanitizer() {}
}
