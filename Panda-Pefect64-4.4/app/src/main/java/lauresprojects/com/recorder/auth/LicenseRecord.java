package lauresprojects.com.recorder.auth;

public final class LicenseRecord {
    public final String keyHash;
    public final String expiryDate;
    public final String expiryTime;
    public final long expiresAtMs;
    public final boolean enabled;
    public final int maxDevices;

    public LicenseRecord(String keyHash, String expiryDate, String expiryTime,
                         long expiresAtMs, boolean enabled, int maxDevices) {
        this.keyHash = keyHash;
        this.expiryDate = expiryDate;
        this.expiryTime = expiryTime;
        this.expiresAtMs = expiresAtMs;
        this.enabled = enabled;
        this.maxDevices = maxDevices;
    }

    public boolean isExpired() {
        return expiresAtMs > 0L && System.currentTimeMillis() >= expiresAtMs;
    }

    public String formattedExpiry() {
        if (expiryDate == null || expiryDate.length() != 8) {
            return (expiryDate == null ? "" : expiryDate) + " "
                    + (expiryTime == null ? "" : expiryTime);
        }
        return expiryDate.substring(0, 4) + "-"
                + expiryDate.substring(4, 6) + "-"
                + expiryDate.substring(6, 8) + " "
                + (expiryTime == null ? "" : expiryTime);
    }
}
