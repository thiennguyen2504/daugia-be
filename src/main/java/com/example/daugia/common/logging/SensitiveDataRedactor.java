package com.example.daugia.common.logging;

import com.example.daugia.bidding.util.EmailMaskingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Strategy-pattern redactor for sensitive data in log messages.
 *
 * <p>Registers typed {@link RedactionRule}s and applies them in order.
 * Call {@link #redact(String)} to apply all rules to a raw string value.
 * For named-field redaction, use the typed helpers (e.g. {@link #maskEmail}).
 *
 * <p>Rules are stateless lambdas — adding a new type of redaction only
 * requires registering a new rule, with zero changes to callers.
 */
public final class SensitiveDataRedactor {

    // ------------------------------------------------------------------
    // Strategy: a named redaction rule
    // ------------------------------------------------------------------

    private record RedactionRule(String name, Function<String, String> fn) {}

    // ------------------------------------------------------------------
    // Registered rules (applied in declaration order)
    // ------------------------------------------------------------------

    private static final List<RedactionRule> RULES = new ArrayList<>();

    static {
        // JWT Bearer token: keep last 8 chars only
        RULES.add(new RedactionRule("jwt",
                value -> {
                    if (value != null && value.length() > 8 && value.contains(".")) {
                        return "..." + value.substring(value.length() - 8);
                    }
                    return value;
                }));
    }

    // ------------------------------------------------------------------
    // Typed helpers (preferred API — never concatenate raw values in logs)
    // ------------------------------------------------------------------

    /**
     * Mask an email address: first 2 chars + *** + @domain.
     * Delegates to the shared {@link EmailMaskingUtils} for consistency.
     *
     * @param email raw email; null-safe
     * @return masked value, or {@code null} if input is null
     */
    public static String maskEmail(String email) {
        if (email == null) return null;
        return EmailMaskingUtils.mask(email);
    }

    /**
     * Mask a phone number: keep first 3 digits, replace rest with ******.
     * e.g. {@code 0901234567} → {@code 090******}
     *
     * @param phone raw phone; null-safe
     * @return masked value, or {@code null} if input is null
     */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        if (phone.length() <= 3) return "***";
        return phone.substring(0, 3) + "******";
    }

    /**
     * Mask a JWT/opaque token: last 8 chars only, prefixed with {@code ...}.
     *
     * @param token raw token; null-safe
     * @return masked value, or {@code "***"} if token is too short
     */
    public static String maskToken(String token) {
        if (token == null) return null;
        if (token.length() <= 8) return "***";
        return "..." + token.substring(token.length() - 8);
    }

    /**
     * Mask an IPv4 address: keep first 2 octets, replace last two with {@code *.*}.
     * e.g. {@code 192.168.1.100} → {@code 192.168.*.*}
     *
     * @param ip raw IP; null-safe; IPv6 returned as-is
     * @return masked value, or original string if format is unrecognised
     */
    public static String maskIp(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip; // IPv6 or unknown format — return unchanged
    }

    /**
     * Mask a credit-card number: only last 4 digits visible.
     * e.g. {@code 4111111111111111} → {@code ************1111}
     *
     * @param cardNumber raw card number (digits only or with dashes/spaces); null-safe
     * @return masked value, or {@code "***"} if too short
     */
    public static String maskCreditCard(String cardNumber) {
        if (cardNumber == null) return null;
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "***";
        String last4 = digits.substring(digits.length() - 4);
        return "*".repeat(digits.length() - 4) + last4;
    }

    /**
     * Generic redaction for unknown sensitive strings.
     *
     * @param value any value; null-safe
     * @return {@code "***"}
     */
    public static String maskGeneric(String value) {
        if (value == null) return null;
        return "***";
    }

    /**
     * Apply all registered redaction rules to an arbitrary string value.
     * Useful for auto-scanning log messages for token-shaped strings.
     *
     * @param value raw value; null-safe
     * @return redacted value
     */
    public static String redact(String value) {
        if (value == null) return null;
        String result = value;
        for (RedactionRule rule : RULES) {
            result = rule.fn().apply(result);
        }
        return result;
    }

    private SensitiveDataRedactor() {
        // utility class — no instances
    }
}
