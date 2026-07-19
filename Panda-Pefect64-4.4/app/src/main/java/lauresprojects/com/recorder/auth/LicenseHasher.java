package lauresprojects.com.recorder.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class LicenseHasher {
    private LicenseHasher() {
    }

    static String licenseKey(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.replace(" ", "").toUpperCase(Locale.US);
        return normalized.isEmpty() ? "" : sha256(normalized);
    }

    static String device(String androidId, String board, String brand, String model) {
        String id = androidId == null || androidId.isEmpty() ? "unknown" : androidId;
        return sha256(id + "|" + safe(board) + "|" + safe(brand) + "|" + safe(model));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return out.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
