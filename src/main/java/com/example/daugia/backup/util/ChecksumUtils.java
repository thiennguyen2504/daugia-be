package com.example.daugia.backup.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ChecksumUtils {

    private ChecksumUtils() {
    }

    public static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path)) {
                return sha256(inputStream, digest);
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public static String sha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return sha256(inputStream, digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String sha256(InputStream inputStream, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return toHex(digest.digest());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
