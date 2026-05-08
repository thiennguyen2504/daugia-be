package com.example.daugia.user.util;

import java.util.Arrays;

public final class UserNameUtils {

    private UserNameUtils() {
    }

    public static String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }
        String[] nameParts = fullName.trim().split("\\s+");
        String firstname = nameParts[0];
        String lastname = nameParts.length > 1
                ? String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length))
                : "";
        return new String[]{firstname, lastname};
    }
}
