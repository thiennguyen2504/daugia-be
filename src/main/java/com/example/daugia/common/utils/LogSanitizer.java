package com.example.daugia.common.utils;

import com.example.daugia.bidding.util.EmailMaskingUtils;

public class LogSanitizer {

    public static String maskEmail(String email) {
        if (email == null) return null;
        return EmailMaskingUtils.mask(email);
    }

    public static String maskToken(String token) {
        if (token == null) return null;
        if (token.length() <= 8) return "***";
        return "..." + token.substring(token.length() - 8);
    }

    public static String maskIp(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip; // For IPv6 or unknown format, maybe return as is or mask fully.
    }
}
