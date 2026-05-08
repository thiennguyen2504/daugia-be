package com.example.daugia.bidding.util;

public final class EmailMaskingUtils {

    private EmailMaskingUtils() {
    }

    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = local.length() <= 2 ? local : local.substring(0, 2);
        return prefix + "***" + domain;
    }
}
