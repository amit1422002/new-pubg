package lauresprojects.com.recorder.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import lauresprojects.com.recorder.R;
import lauresprojects.com.recorder.auth.DeviceIdHelper;
import lauresprojects.com.recorder.auth.LicenseRecord;
import lauresprojects.com.recorder.auth.LicenseRepository;
import lauresprojects.com.recorder.auth.LicenseSession;
import lauresprojects.com.recorder.utils.myTools;

public class LoginActivity extends AppCompatActivity {
    // The native "safecheat" library (loaded by MainActivity) registers these
    // methods onto this class via RegisterNatives in its JNI_OnLoad. They must
    // stay declared with the exact signature or loadLibrary aborts with
    // NoSuchMethodError, even though the Firebase login flow never calls them.
    public static native String FixCrash();

    private static native String suckmydick(Context mContext, String userKey);

    public static String USERKEY = "";
    public static String PASSKEY = "";
    public static boolean Sufii = true;
    public static boolean mahyong = true;

    public static final int REQUEST_OVERLAY_PERMISSION = 5469;
    private static final int REQUEST_ALL_PERMISSIONS = 100;
    private static final int REQUEST_INSTALL_UNKNOWN = 200;

    private static final String[] ALL_PERMISSIONS =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            }
                    : new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

    private TextInputEditText keyInput;
    private TextView expiryInfoView;
    private ProgressBar progressBar;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myTools tools = new myTools(this);
        setTheme(tools.geInt("myTheme", "myTheme", R.style.AppTheme));
        setFullScreen();
        getWindow().setStatusBarColor(Color.rgb(15, 25, 35));
        getWindow().setNavigationBarColor(Color.rgb(15, 25, 35));

        String deviceHash = DeviceIdHelper.getDeviceHash(this);
        LicenseRecord cached = LicenseSession.load(this, deviceHash);
        if (cached != null && !cached.isExpired()) {
            setRuntimeKey(LicenseSession.getLicenseKey(this));
            openMainAndFinish();
            return;
        }

        setContentView(R.layout.activity_login);
        keyInput = findViewById(R.id.et_license_key);
        TextView deviceIdView = findViewById(R.id.tv_device_id);
        expiryInfoView = findViewById(R.id.tv_expiry_info);
        progressBar = findViewById(R.id.progress_login);
        loginButton = findViewById(R.id.btn_login);

        deviceIdView.setText(getString(R.string.license_device_id, deviceHash));
        loginButton.setOnClickListener(v -> attemptLogin());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        } else {
            requestAllPermissions();
        }
    }

    private void attemptLogin() {
        String key = keyInput != null && keyInput.getText() != null
                ? keyInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(key)) {
            Toast.makeText(this, R.string.license_key_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        LicenseRepository.activate(this, key, new LicenseRepository.LoginCallback() {
            @Override
            public void onSuccess(LicenseRecord record) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String deviceHash = DeviceIdHelper.getDeviceHash(LoginActivity.this);
                    LicenseSession.save(LoginActivity.this, record, deviceHash, key);
                    setRuntimeKey(key);
                    expiryInfoView.setVisibility(View.VISIBLE);
                    expiryInfoView.setText(getString(
                            R.string.license_valid_until, record.formattedExpiry()));
                    Toast.makeText(LoginActivity.this,
                            R.string.license_login_success, Toast.LENGTH_SHORT).show();
                    openMainAndFinish();
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private static void setRuntimeKey(String key) {
        USERKEY = key == null ? "" : key;
        PASSKEY = USERKEY;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        keyInput.setEnabled(!loading);
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void requestOverlayPermission() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Overlay Permission")
                .setMessage("Allow overlay permission")
                .setPositiveButton("Allow", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void requestAllPermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, missing.toArray(new String[0]), REQUEST_ALL_PERMISSIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_INSTALL_UNKNOWN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            requestAllPermissions();
        }
    }

    private void setFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }
}
