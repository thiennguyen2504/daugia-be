package com.example.daugia.backup.util;

public final class SizeFormatter {

    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    private SizeFormatter() {
    }

    public static String formatBytes(Long bytes) {
        if (bytes == null) {
            return "0 B";
        }
        double value = bytes;
        int index = 0;
        while (value >= 1024 && index < UNITS.length - 1) {
            value /= 1024;
            index++;
        }
        return String.format("%.1f %s", value, UNITS[index]);
    }
}
