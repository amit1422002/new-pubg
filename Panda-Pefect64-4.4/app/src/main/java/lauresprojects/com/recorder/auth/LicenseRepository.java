package lauresprojects.com.recorder.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public final class LicenseRepository {
    private static final String DB_URL =
            "https://valorant-639b2-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String LICENSES_NODE = "licenses";
    private static final String DEVICES_NODE = "devices";

    public interface LoginCallback {
        void onSuccess(LicenseRecord record);

        void onFailure(String message);
    }

    private LicenseRepository() {
    }

    public static void activate(@NonNull Context context,
                                @NonNull String licenseKey,
                                @NonNull LoginCallback callback) {
        String keyHash = LicenseHasher.licenseKey(licenseKey);
        if (keyHash.isEmpty()) {
            callback.onFailure("Enter a valid license key");
            return;
        }
        fetchAndBind(context, keyHash, DeviceIdHelper.getDeviceHash(context), callback);
    }

    private static FirebaseDatabase database(Context context) {
        FirebaseApp app;
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000000000")
                    .setApiKey("REPLACE_WITH_YOUR_FIREBASE_ANDROID_API_KEY")
                    .setProjectId("valorant-639b2")
                    .setDatabaseUrl(DB_URL)
                    .build();
            app = FirebaseApp.initializeApp(context.getApplicationContext(), options);
        } else {
            app = FirebaseApp.getInstance();
        }
        return FirebaseDatabase.getInstance(app, DB_URL);
    }

    private static void fetchAndBind(Context context, String keyHash,
                                     String deviceHash, LoginCallback callback) {
        final DatabaseReference licenseRef;
        try {
            licenseRef = database(context).getReference(LICENSES_NODE).child(keyHash);
        } catch (Exception e) {
            callback.onFailure("License server is not configured");
            return;
        }

        licenseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailure("Invalid license key");
                    return;
                }

                LicenseRecord record = parseRecord(keyHash, snapshot);
                if (!record.enabled) {
                    callback.onFailure("This key is disabled");
                    return;
                }
                if (record.isExpired()) {
                    callback.onFailure("Key expired on " + record.formattedExpiry());
                    return;
                }

                DataSnapshot devices = snapshot.child(DEVICES_NODE);
                if (devices.hasChild(deviceHash)) {
                    callback.onSuccess(record);
                    return;
                }

                Long storedCount = snapshot.child("deviceCount").getValue(Long.class);
                long deviceCount = storedCount != null
                        ? storedCount : devices.getChildrenCount();
                if (deviceCount >= record.maxDevices) {
                    callback.onFailure("Device limit reached (" + record.maxDevices + ")");
                    return;
                }

                Map<String, Object> devicePayload = new HashMap<>();
                devicePayload.put("boundAt", ServerValue.TIMESTAMP);
                devicePayload.put("model", DeviceIdHelper.getDeviceLabel());

                Map<String, Object> updates = new HashMap<>();
                updates.put(DEVICES_NODE + "/" + deviceHash, devicePayload);
                updates.put("deviceCount", deviceCount + 1);

                licenseRef.updateChildren(updates)
                        .addOnSuccessListener(unused -> callback.onSuccess(record))
                        .addOnFailureListener(e -> callback.onFailure(
                                e.getMessage() != null
                                        ? e.getMessage() : "Could not bind device"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // With the license security rules, an unknown/disabled/expired
                // key hash is rejected as PERMISSION_DENIED rather than
                // returned as a missing node. Surface that as a clear message.
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    callback.onFailure("Invalid, disabled or expired license key");
                    return;
                }
                String message = error.getMessage();
                callback.onFailure(message != null && !message.isEmpty()
                        ? message : "Could not reach license server");
            }
        });
    }

    private static LicenseRecord parseRecord(String keyHash, DataSnapshot snapshot) {
        Boolean enabled = snapshot.child("enabled").getValue(Boolean.class);
        String expiryDate = valueAsString(snapshot.child("expiryDate"));
        String expiryTime = valueAsString(snapshot.child("expiryTime"));
        Long expiresAtMs = snapshot.child("expiresAtMs").getValue(Long.class);
        Long maxDevices = snapshot.child("maxDevices").getValue(Long.class);
        return new LicenseRecord(
                keyHash,
                expiryDate,
                expiryTime,
                expiresAtMs != null ? expiresAtMs : 0L,
                enabled == null || enabled,
                maxDevices != null ? maxDevices.intValue() : 1);
    }

    private static String valueAsString(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        return value != null ? String.valueOf(value) : "";
    }
}
