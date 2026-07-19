package lauresprojects.com.recorder.activity;

// Standard Android Imports
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import lauresprojects.com.recorder.activity.LibAnogsPatcher;  // ✅ SAHI
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import lauresprojects.com.recorder.utils.PermissionsHelper;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX & Jetpack
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.FrameLayout;

// Material Design & Third Party

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

// Java Utilities
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// BlackBox Core
import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.system.DaemonService;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.utils.FileUtils;
import com.anubis.skin.BgmiLogoutHelper;

// Project Specific
import lauresprojects.com.recorder.App;
import lauresprojects.com.recorder.BuildConfig;
import lauresprojects.com.recorder.R;
import lauresprojects.com.recorder.auth.LicenseSession;
import lauresprojects.com.recorder.floating.FightMode;
import lauresprojects.com.recorder.floating.FloatService;
import lauresprojects.com.recorder.floating.Overlay;
import lauresprojects.com.recorder.floating.ToggleAim;
import lauresprojects.com.recorder.floating.ToggleBullet;
import lauresprojects.com.recorder.floating.ToggleSimulation;
import lauresprojects.com.recorder.utils.FLog;
import lauresprojects.com.recorder.utils.FPrefs;
import lauresprojects.com.recorder.utils.InstallActivity;
import lauresprojects.com.recorder.utils.myTools;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// Static Imports
import static lauresprojects.com.recorder.activity.LoginActivity.PASSKEY;
import static lauresprojects.com.recorder.activity.LoginActivity.USERKEY;
import static lauresprojects.com.recorder.activity.LoginActivity.Sufii;
import static lauresprojects.com.recorder.activity.LoginActivity.mahyong;
import static lauresprojects.com.recorder.server.ApiServer.EXP;
import static lauresprojects.com.recorder.server.ApiServer.getOwner;
import static lauresprojects.com.recorder.server.ApiServer.mainURL;
import static lauresprojects.com.recorder.server.ApiServer.getTelegram;
import static lauresprojects.com.recorder.server.ApiServer.getGrup;
import static com.anubis.loader.core.env.BEnvironment.getDataFilesDir;
// Fixes 'Shell' errors (libsu or similar root library)
import com.topjohnwu.superuser.Shell; 

// Fixes 'AppCompatButton' errors
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.PopupMenu;

// Fixes 'StandardCharsets' errors
import java.nio.charset.StandardCharsets;

// Fixes 'FileNotFoundException' errors
import java.io.FileNotFoundException;
;

@Obfuscate
public class MainActivity extends AppCompatActivity {

static {
        
            System.loadLibrary("safecheat");
}


    DrawerLayout drawerLayout;
    ImageView sidebar;

    private GestureDetector gestureDetector;
    
    static myTools m;
    private AlertDialog obbProgressDialog;
    
    public static String socket;
    public static String daemonPath;
    public static boolean fixinstallint = false;
    public static boolean check = false;
   // public static int //hiderecord = 0;
    public static boolean Record = false;
    static MainActivity instance;
    private long backPressedTime = 0;
    //  public static int game_ver = 0;

    Context ctx;
    InstallResult installResult;
    AnubisCore core;

    private TextView statusText;

AppCompatButton InstallBgmiBtn, InstallGlobalBtn, InstallKRBtn, InstallVNGBtn, InstallTWBtn;
TextView vrIndia, vrGlobal, vrKorea, vrVNG, vrTW;
boolean[] isPlaying = {false};

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_MIC_PERMISSION = 2025;


    int Storage_Permission = 142;
    private static final String PREF_NAME = "espValue";
    private SharedPreferences sharedPreferences;
    String Launch = "Launch";
    String[] appPackage = {"com.tencent.ig", "com.pubg.krmobile", "com.pubg.imobile", "abcd", "com.abcd.katana", "com.vng.pubgmobile", "com.rekoo.pubgm", "mark.via.gp", "telegram @ayansy3d"};
    String[] googlePackage = {"com.google.android.gms", "com.google.android.gsf", "com.android.vending", "com.google.android.gm", "telegram @pakgamerz"};
    public String nameGame = "PROTECTION GLOBAL";
    public String CURRENT_PACKAGE = "";
    public LinearProgressIndicator progres;
    public CardView enable, disable;
    public static int gameint = 0;
    public static String bypassmode = "manual";
    public static int bitversi = 64;
    public static boolean noroot = false;
    public static int device = 1;
    public static String game = "com.tencent.ig";
    public static String pkg = "com.tencent.ig";
    TextView root;
    public static boolean kernel = false;
    public static boolean Ischeck = false;
    public static boolean modestatus = false;
    public LinearLayout container;

    public static String modeselect;
    public static String typelogin;
    
    public FPrefs prefs;
    public boolean isLogin = false; // Added back for MainActivity
    private BottomSheetDialog bottomSheetDialog; // Added back for dialog logic
    public static final int REQUEST_OVERLAY_PERMISSION = 5469;
    public static final int PERMISSION_REQUEST_STORAGE = 100;


    public static MainActivity get() {
        return instance;
    }

 @Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    writeLog("========================================");
    writeLog("📱 APP STARTED - onCreate() CALLED");
    
    m = new myTools(this);
    setTheme(m.geInt("myTheme","myTheme",R.style.AppTheme));
    
    setContentView(R.layout.act_main);
    
    writeLog("📄 setContentView() done");
    
    if (!PermissionsHelper.allGranted(this)) {
        writeLog("❌ Permissions NOT granted, showing permissions sheet");
        showPermissionsSheet();
        return;
    }
    writeLog("✅ All permissions granted");
    
    
    InstallBgmiBtn = findViewById(R.id.InstallBgmi);
InstallGlobalBtn = findViewById(R.id.InstallGlobal);
InstallKRBtn = findViewById(R.id.InstallKR);
InstallVNGBtn = findViewById(R.id.InstallVNG);
InstallTWBtn = findViewById(R.id.InstallTW);

vrIndia = findViewById(R.id.vrindia);
vrGlobal = findViewById(R.id.vrglobal);
vrKorea = findViewById(R.id.vrkr);
vrVNG = findViewById(R.id.vrvng);
vrTW = findViewById(R.id.vrtw);
        
        
        
    init();

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);





        core = AnubisCore.get();
        core.doCreate();
        
    handleAnoTmp(this);

        drawerLayout = findViewById(R.id.DrawerLayout);
        sidebar = findViewById(R.id.sidebar);

        ctx = this;
        EnableCars();
        EnableItems();
        initMenu1();
        initMenu2();
        Loadssets();
        devicecheck();
        SettingESP();

        instance = this;
      //  isLogin = true;
        

        Makedir();
        
        
        
        
        

        
        
        if (Shell.rootAccess()) {
        
    Dialog rootedDialog = new Dialog(MainActivity.this); // Use activity context
    rootedDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    rootedDialog.setContentView(R.layout.dialog_rooted_info);
    rootedDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    rootedDialog.setCancelable(true);

    // Auto dismiss after 2 seconds
    new Handler(Looper.getMainLooper()).postDelayed(rootedDialog::dismiss, 5000);

    rootedDialog.show();
    
}


        sidebar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });



        TextView footerVersion = findViewById(R.id.footerversion);
        if (footerVersion != null) {
            String versionName = BuildConfig.VERSION_NAME;
            footerVersion.setText(getString(R.string.app_version_) + versionName);
        }

        TextView mainver = findViewById(R.id.mainver);
        if (footerVersion != null) {
            String versionName = BuildConfig.VERSION_NAME;
            mainver.setText(getString(R.string.app_version_) + versionName);
        }

        LinearLayout menu1 = findViewById(R.id.imenu1);
        LinearLayout menu2 = findViewById(R.id.imenu2);
        ImageView home = findViewById(R.id.imghome);
        ImageView sett = findViewById(R.id.imgsett);
        TextView txtsett = findViewById(R.id.txtsett);
        TextView txthome = findViewById(R.id.txthome);

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.logout) {
                    drawerLayout.close();
                    LicenseSession.clear(MainActivity.this);
                    LoginActivity.USERKEY = "";
                    LoginActivity.PASSKEY = "";
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                } else if (id == R.id.homei) {
                    drawerLayout.close();
                    menu1.setVisibility(View.VISIBLE);
                    menu2.setVisibility(View.GONE);
                    txthome.setTextColor(getResources().getColor(R.color.main));
                    txtsett.setTextColor(getResources().getColor(R.color.gray));
                    home.setBackgroundResource(R.drawable.ic_home);
                    sett.setBackgroundResource(R.drawable.outline_settings_24);
                    return true;
                } else if (id == R.id.setti) {
                    drawerLayout.close();
                    menu1.setVisibility(View.GONE);
                    menu2.setVisibility(View.VISIBLE);
                    txthome.setTextColor(getResources().getColor(R.color.gray));
                    txtsett.setTextColor(getResources().getColor(R.color.main));
                    home.setBackgroundResource(R.drawable.ic_home_outline);
                    sett.setBackgroundResource(R.drawable.ic_helpon);
                    return true;
                } else if (id == R.id.link_tg) {
                    drawerLayout.close();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getTelegram()));
                    startActivity(intent);
                } else if (id == R.id.link_community) {
                    drawerLayout.close();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getGrup()));
                    startActivity(intent);
                } else if (id == R.id.link_dev) {
                    drawerLayout.close();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getOwner()));
                    startActivity(intent);
                } else if (id == R.id.install_gsm) {
    drawerLayout.close();
   // AnubisCore.get().installGms(0);
    //TastyToast.makeText(MainActivity.this, "Installed.", TastyToast.LENGTH_LONG, TastyToast.DEFAULT);

    



}
                return false;
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float screenWidth = getResources().getDisplayMetrics().widthPixels;
                float touchArea = screenWidth * 0.6f; // 60% tak swipe enable

                if (e1 != null && e2 != null && e1.getX() < touchArea && e2.getX() > e1.getX()) {
                    drawerLayout.openDrawer(navigationView);
                    return true;
                }
                return false;
            }
        });

        // sock64: bundled from sock src/ (assets) — no GitHub download

    }




    public void devicecheck() {
        root = findViewById(R.id.textroot);
        

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd ", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        if (Shell.rootAccess()) {
            FLog.info("Root granted");
            modeselect = currentDateTime + "- A" + Build.VERSION.RELEASE;
            root.setText(getString(R.string.root));
            Ischeck = true;
            noroot = true;
            device = 1;
        } else {
            FLog.info("Root not granted");
            modeselect = currentDateTime + "- A" + Build.VERSION.RELEASE;
            root.setText(getString(R.string.notooroot));
            Ischeck = false;
            device = 2;
            
        }
    }




    






    @SuppressLint({"SetTextI18n", "ResourceType"})
    public void initMenu1() {
        LinearLayout layhome3 = findViewById(R.id.hackkkk);

        MaterialButton esp64Button = findViewById(R.id.esp64);
        MaterialButton esp32Button = findViewById(R.id.esp32);
        MaterialButton systemModeBtn = findViewById(R.id.system);
        MaterialButton kernelModeBtn = findViewById(R.id.kernel);
        MaterialButton espsafe = findViewById(R.id.espsafe);
        MaterialButton espunsafe = findViewById(R.id.espunsafe);

        
if (Shell.rootAccess()) {

    InstallBgmiBtn.setVisibility(View.GONE);
    InstallGlobalBtn.setVisibility(View.GONE);
    InstallKRBtn.setVisibility(View.GONE);
    InstallVNGBtn.setVisibility(View.GONE);
    InstallTWBtn.setVisibility(View.GONE);

} else {

    InstallBgmiBtn.setVisibility(View.VISIBLE);
    InstallGlobalBtn.setVisibility(View.VISIBLE);
    InstallKRBtn.setVisibility(View.VISIBLE);
    InstallVNGBtn.setVisibility(View.VISIBLE);
    InstallTWBtn.setVisibility(View.VISIBLE);

}

        TextView keytext = findViewById(R.id.licencekey);

        Shader textShader = new LinearGradient(0, 0, keytext.getPaint().measureText(keytext.getText().toString()), keytext.getTextSize(), new int[]{Color.parseColor("#0a95fc"), Color.parseColor("#04285a")}, null, Shader.TileMode.CLAMP);

        keytext.getPaint().setShader(textShader);

        TextView devicetext = findViewById(R.id.devicetext);

        Shader textShader1 = new LinearGradient(0, 0, keytext.getPaint().measureText(devicetext.getText().toString()), devicetext.getTextSize(), new int[]{Color.parseColor("#0a95fc"), Color.parseColor("#04285a")}, null, Shader.TileMode.CLAMP);

        devicetext.getPaint().setShader(textShader1);


        // SharedPreferences initialization
        SharedPreferences sharedPreferences = getSharedPreferences("espValue", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

// Load saved preferences on activity start
        boolean isEsp64Selected = sharedPreferences.getBoolean("esp64Selected", true);
        boolean isKernelMode = sharedPreferences.getBoolean("kernelSelected", false);
        boolean isEspSafe = sharedPreferences.getBoolean("espSafe", true);


        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.black, typedValue, true);
        int blackColor = typedValue.data; // Extract resolved color

        int mainColor = ContextCompat.getColor(this, R.color.blue);

// ESP Selection
        if (isEsp64Selected) {
            esp64Button.setTextColor(mainColor);
            esp64Button.setIconTint(ColorStateList.valueOf(mainColor));
            esp32Button.setTextColor(blackColor);
            esp32Button.setIconTint(ColorStateList.valueOf(blackColor));
        } else {
            esp32Button.setTextColor(mainColor);
            esp32Button.setIconTint(ColorStateList.valueOf(mainColor));
            esp64Button.setTextColor(blackColor);
            esp64Button.setIconTint(ColorStateList.valueOf(blackColor));
        }

// Kernel Selection
        if (!isKernelMode) {
            systemModeBtn.setTextColor(mainColor);
            systemModeBtn.setIconTint(ColorStateList.valueOf(mainColor));
            kernelModeBtn.setTextColor(blackColor);
            kernelModeBtn.setIconTint(ColorStateList.valueOf(blackColor));
        } else {
            kernelModeBtn.setTextColor(mainColor);
            kernelModeBtn.setIconTint(ColorStateList.valueOf(mainColor));
            systemModeBtn.setTextColor(blackColor);
            systemModeBtn.setIconTint(ColorStateList.valueOf(blackColor));
        }

// ESP Safe Mode
        if (isEspSafe) {
            espsafe.setTextColor(mainColor);
            espsafe.setIconTint(ColorStateList.valueOf(mainColor));
            espunsafe.setTextColor(blackColor);
            espunsafe.setIconTint(ColorStateList.valueOf(blackColor));
        } else {
            espunsafe.setTextColor(mainColor);
            espunsafe.setIconTint(ColorStateList.valueOf(mainColor));
            espsafe.setTextColor(blackColor);
            espsafe.setIconTint(ColorStateList.valueOf(blackColor));
        }

// Click Listeners (Fixed setIconTint issue)
        esp64Button.setOnClickListener(v -> {
            bitversi = 64;
            esp64Button.setTextColor(mainColor);
            esp64Button.setIconTint(ColorStateList.valueOf(mainColor));
            esp32Button.setTextColor(blackColor);
            esp32Button.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("esp64Selected", true).apply();
        });

        esp32Button.setOnClickListener(v -> {
            bitversi = 32;
            esp32Button.setTextColor(mainColor);
            esp32Button.setIconTint(ColorStateList.valueOf(mainColor));
            esp64Button.setTextColor(blackColor);
            esp64Button.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("esp64Selected", false).apply();
        });

        systemModeBtn.setOnClickListener(view -> {
            kernel = false;
            systemModeBtn.setTextColor(mainColor);
            systemModeBtn.setIconTint(ColorStateList.valueOf(mainColor));
            kernelModeBtn.setTextColor(blackColor);
            kernelModeBtn.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("kernelSelected", false).apply();
        });

        kernelModeBtn.setOnClickListener(view -> {
            kernel = true;
            kernelModeBtn.setTextColor(mainColor);
            kernelModeBtn.setIconTint(ColorStateList.valueOf(mainColor));
            systemModeBtn.setTextColor(blackColor);
            systemModeBtn.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("kernelSelected", true).apply();
        });

        espsafe.setOnClickListener(v -> {
            espsafe.setTextColor(mainColor);
            espsafe.setIconTint(ColorStateList.valueOf(mainColor));
            espunsafe.setTextColor(blackColor);
            espunsafe.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("espSafe", true).apply();
        });

        espunsafe.setOnClickListener(v -> {
            espunsafe.setTextColor(mainColor);
            espunsafe.setIconTint(ColorStateList.valueOf(mainColor));
            espsafe.setTextColor(blackColor);
            espsafe.setIconTint(ColorStateList.valueOf(blackColor));
            editor.putBoolean("espSafe", false).apply();
        });


        TextView keylicence = findViewById(R.id.keylicence);
        ImageView eyeButton = findViewById(R.id.eye_button);

        String fullKey = PASSKEY + ":" + USERKEY;
        keylicence.setText(fullKey);

// By Default Blur Text
        keylicence.setTransformationMethod(new android.text.method.PasswordTransformationMethod());

        eyeButton.setOnClickListener(new View.OnClickListener() {
            boolean isVisible = false; // Track Visibility

            @Override
            public void onClick(View v) {
                if (isVisible) {
                    // Apply Blur (Hide Text)
                    keylicence.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
                    eyeButton.setImageResource(R.drawable.ic_eye_off); // Change Icon
                } else {
                    // Remove Blur (Show Text)
                    keylicence.setTransformationMethod(null);
                    eyeButton.setImageResource(R.drawable.ic_eye); // Change Icon
                }
                isVisible = !isVisible; // Toggle State
            }
        });


    }


    @SuppressLint("ResourceAsColor")
    void initMenu2() {


        TextView deviceInfoTextView = findViewById(R.id.deviceInfoTextView);

// Get manufacturer, model, and Android version
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;

// Combine the information into a single string
        String deviceInfo = manufacturer + " - Android " + androidVersion;

// Set the text to the TextView
        deviceInfoTextView.setText(deviceInfo);


        MaterialButton play = findViewById(R.id.play);
boolean[] isPlaying = {false};


    play.setVisibility(View.VISIBLE);




play.setOnClickListener(v -> {
    if (!Shell.rootAccess()) {
        Toast.makeText(this, "Nonroot Use Below PUBG Buttons!", Toast.LENGTH_SHORT).show();
        return;
    }

    if (!isPlaying[0]) {
        // Ultra tiny dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_select);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialog.setCancelable(true);

        // Set click listeners for each game
        int[] ids = {R.id.game_global, R.id.game_korea, R.id.game_vietnam, R.id.game_taiwan, R.id.game_bgmi};
        String[] packages = {"com.tencent.ig","com.pubg.krmobile","com.vng.pubgmobile","com.pubg.taiwan","com.pubg.imobile"};

        for (int i = 0; i < ids.length; i++) {
            final int index = i;
            dialog.findViewById(ids[i]).setOnClickListener(view -> {
                dialog.dismiss();

                // Launch game
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packages[index]);
                if (launchIntent != null) startActivity(launchIntent);
                else Toast.makeText(this, "Game not installed!", Toast.LENGTH_SHORT).show();

                // Update button immediately
                play.setIcon(getResources().getDrawable(R.drawable.pause, null));
                play.setText("PAUSE");
                play.setIconTintResource(R.color.blazered);
                play.setTextColor(getResources().getColor(R.color.blazered, null));

                // Delay 30 seconds before starting patcher
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startPatcher();
                    isPlaying[0] = true;
                }, 6000); // 30000 ms = 30 seconds
            });
        }

        dialog.show();

    } else {
        // Stop patcher
        stopPatcher();
        isPlaying[0] = false;

        play.setIcon(getResources().getDrawable(R.drawable.play, null));
        play.setText("PLAY");
        play.setIconTintResource(R.color.green);
        play.setTextColor(getResources().getColor(R.color.green, null));
    }
});


        setupGameButton(InstallGlobalBtn, vrGlobal, "com.tencent.ig", 1, null);
        setupGameButton(InstallKRBtn, vrKorea, "com.pubg.krmobile", 2, null);
        setupGameButton(InstallVNGBtn, vrVNG, "com.vng.pubgmobile", 3, null);
        setupGameButton(InstallTWBtn, vrTW, "com.rekoo.pubgm", 4, null);
        setupGameButton(InstallBgmiBtn, vrIndia, "com.pubg.imobile", 5, null);

        setupGameMoreMenu(findViewById(R.id.moreBgmi), "com.pubg.imobile");
        setupGameMoreMenu(findViewById(R.id.moreGlobal), "com.tencent.ig");
        setupGameMoreMenu(findViewById(R.id.moreKR), "com.pubg.krmobile");
        setupGameMoreMenu(findViewById(R.id.moreVNG), "com.vng.pubgmobile");
        setupGameMoreMenu(findViewById(R.id.moreTW), "com.rekoo.pubgm");
        


        SharedPreferences sharedPreferences = getSharedPreferences("espValue", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int savedHideRecord = sharedPreferences.getInt("hiderecord", 0);
        MaterialButton hideRecordButton = findViewById(R.id.hiderecord);

// Set initial UI based on saved preference
        if (savedHideRecord == 1) {
            //hideRecordButton.setTextColor(getResources().getColor(R.color.white));
            hideRecordButton.setIconTintResource(R.color.green);
        } else {
            //hideRecordButton.setTextColor(getResources().getColor(R.color.white));
            hideRecordButton.setIconTintResource(R.color.blazered);
        }

        findViewById(R.id.hiderecord).setOnClickListener(v -> {

                showBottomSheetDialog(getResources().getDrawable(R.drawable.icon_toast_alert), getString(R.string.confirm), getString(R.string.did_you_want_hide), false, sv -> {
                    //hiderecord = 1;
                    Record = true ;
                    //hideRecordButton.setTextColor(getResources().getColor(R.color.white));
                    hideRecordButton.setIconTintResource(R.color.green);
                    editor.putInt("hiderecord", 1).apply(); // Save hiderecord as 1
                    dismissBottomSheetDialog();
                }, v1 -> {
                    //hiderecord = 0;
                    Record = false ;
                    //hideRecordButton.setTextColor(getResources().getColor(R.color.white));
                    hideRecordButton.setIconTintResource(R.color.blazered);
                    editor.putInt("hiderecord", 0).apply(); // Save hiderecord as 0
                    dismissBottomSheetDialog();
                });

        });


        findViewById(R.id.fixinstall).setOnClickListener(v -> {
    if (Sufii) {
        showBottomSheetDialog4(
                getResources().getDrawable(R.drawable.icon_toast_alert),
                "Please allow this permission",
                "It will fix the permission for OBB",
                false,
                sv -> {
                    fixinstallint = true;
                    String pkg = null;
                    if (gameint == 1) pkg = "com.tencent.ig";
                    else if (gameint == 2) pkg = "com.pubg.krmobile";
                    else if (gameint == 3) pkg = "com.vng.pubgmobile";
                    else if (gameint == 4) pkg = "com.rekoo.pubgm";
                    else if (gameint == 5) pkg = "com.pubg.imobile";
                    else if (gameint == 0) {
                        Toast.makeText(this, "First select game please", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent i = new Intent();
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    i.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    Uri muri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid/document/primary%3AAndroid%2Fobb%2F" + pkg);
                    i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, muri);
                    startActivityForResult(i, 0);
                },
                v1 -> {
                    fixinstallint = false;
                }
        );
    } else {
        //toastImage(R.drawable.notife, getString(R.string.please_upgrade_to_premium_));
    }
});

    }

// Helper method to launch game by package name
private void openGame(String game) {
    writeLog("========================================");
    writeLog("🎮 openGame() CALLED - game: " + game);
    
    String packageName = "";
    switch (game) {
        case "Global": packageName = "com.tencent.ig"; break;
        case "Korea": packageName = "com.pubg.krmobile"; break;
        case "Vietnam": packageName = "com.vng.pubgmobile"; break;
        case "Taiwan": packageName = "com.pubg.taiwan"; break;
        case "BGMI": packageName = "com.pubg.imobile"; break;
    }
    writeLog("📦 packageName: " + packageName);

    if (!packageName.isEmpty()) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            writeLog("✅ Launch intent found, starting game...");
            startActivity(launchIntent);
            writeLog("🔄 Calling LibAnogsPatcher.resetSession()");
            LibAnogsPatcher.resetSession();
            LibAnogsPatcher.startOnGameLaunch(this);
            GamePidHelper.refreshPidFile(this);
            writeLog("✅ Game launch complete");
        } else {
            writeLog("❌ Launch intent NOT found - game not installed");
            Toast.makeText(this, game + " is not installed!", Toast.LENGTH_SHORT).show();
        }
    } else {
        writeLog("❌ packageName is empty");
    }
}




private boolean ensureObb(String packageName, Runnable onReady) {

    File blackboxFolder = new File("/storage/emulated/0/blackbox/Android/obb/" + packageName);

    File[] obbFiles = blackboxFolder.listFiles((dir, name) ->
            name.matches("main\\.\\d+\\." + packageName + "\\.obb"));

    // ✅ Already exists
    if (obbFiles != null && obbFiles.length > 0 && obbFiles[0].length() > 1024 * 1024) {
        if (onReady != null) onReady.run();
        return true;
    }

    File sourceFolder = new File("/storage/emulated/0/Android/obb/" + packageName);

    File[] sourceFiles = sourceFolder.listFiles((dir, name) ->
            name.matches("main\\.\\d+\\." + packageName + "\\.obb"));

    if (sourceFiles != null && sourceFiles.length > 0) {

        File src = sourceFiles[0];

        showObbProgress(); // ⭐ SHOW PROGRESS HERE

        new Thread(() -> {

            boolean copied = copyObbFile(src, packageName);

            runOnUiThread(() -> {

                hideObbProgress(); // ⭐ HIDE PROGRESS

                if (copied) {
                    if (onReady != null) onReady.run();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "OBB copy failed", Toast.LENGTH_SHORT).show();
                }
            });

        }).start();

        return false;
    }

    Toast.makeText(getApplicationContext(),
            "OBB missing. Please allow storage permission or Restart App.",
            Toast.LENGTH_LONG).show();

    return false;
}

private void showObbProgress() {

    View view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);
    TextView text = view.findViewById(R.id.progressText);

    text.setText("Preparing OBB...");

    obbProgressDialog = new AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create();

    if (obbProgressDialog.getWindow() != null) {
        obbProgressDialog.getWindow()
                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    obbProgressDialog.show();
}

private void hideObbProgress() {
    if (obbProgressDialog != null && obbProgressDialog.isShowing()) {
        obbProgressDialog.dismiss();
    }
}

private boolean copyObbFile(File src, String packageName) {
    try {

        File destFolder = new File("/storage/emulated/0/blackbox/Android/obb/" + packageName);
        if (!destFolder.exists()) destFolder.mkdirs();

        File dest = new File(destFolder, src.getName());

        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dest);

        byte[] buffer = new byte[1024 * 8];
        int len;

        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }

        out.flush();
        in.close();
        out.close();

        // ✅ FINAL VERIFY (VERY IMPORTANT)
        return dest.exists() && dest.length() == src.length();

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

private void setupGameButton(
        AppCompatButton button,
        TextView status,
        String packageName,
        int gameInt,
        Runnable onLaunch
) {

    boolean installedOnDevice = isAppInstalled(packageName);
    boolean installedInBlackbox = core.isInstalled(packageName, 0);

    // Status text
    if (installedInBlackbox) {
        status.setText(R.string.installed_available);
    } else if (installedOnDevice) {
        status.setText(R.string.not_installed_available);
    } else {
        status.setText(R.string.not_installed_unavailable);
    }

    boolean running = isRunning(packageName);
    button.setText(running ? getString(R.string.launch) : getString(R.string.install));

    button.setOnClickListener(v -> {

        gameint = gameInt;

        if (status.getText().toString().equals(getString(R.string.not_installed_unavailable))) {
            return;
        }

        // ================= LAUNCH =================
        if (button.getText().toString().equals(getString(R.string.launch))) {

            ensureObb(packageName, () -> {

                core.launchApk(packageName, 0);
                LibAnogsPatcher.resetSession();
                LibAnogsPatcher.startOnGameLaunch(MainActivity.this);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startPatcher();
                    isPlaying[0] = true;
                }, 6000);

                if (onLaunch != null) onLaunch.run();
            });

        }

        // ================= INSTALL =================
        else {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(LayoutInflater.from(this).inflate(R.layout.animation_launch, null))
                    .setCancelable(false)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                installResult = core.installPackageAsUser(packageName, 0);

                if (installResult.success) {
                    button.setText(getString(R.string.launch));
                    status.setText(R.string.installed_available);
                } else {
                    Toast.makeText(this, installResult.msg, Toast.LENGTH_SHORT).show();
                }

                doShowProgress(true);

                // ❗ FIX: no fake delay anymore, proper copy handling
                ensureObb(packageName, null);

                if (dialog.isShowing()) dialog.dismiss();

            }, 2000);
        }
    });

    // ================= UNINSTALL =================
    button.setOnLongClickListener(v -> {

        showBottomSheetDialogUninstall(
                getResources().getDrawable(R.drawable.icon_toast_alert),
                getString(R.string.confirm),
                getString(R.string.do_you_want_to_uninstall_this_app),
                false,
                v1 -> {

                    unInstallWithDellay(packageName);
                    button.setText(getString(R.string.install));

                    boolean installedNow = isAppInstalled(packageName);
                    boolean blackboxNow = core.isInstalled(packageName, 0);

                    if (blackboxNow) {
                        status.setText(R.string.installed_available);
                    } else if (installedNow) {
                        status.setText(R.string.not_installed_available);
                    } else {
                        status.setText(R.string.not_installed_unavailable);
                    }

                    dismissBottomSheetDialog();
                });

        return true;
    });
}

    private void setupGameMoreMenu(View moreBtn, String packageName) {
        if (moreBtn == null) {
            return;
        }
        moreBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenu().add(0, 1, 0, "Copy data");
            if ("com.pubg.imobile".equals(packageName)) {
                popup.getMenu().add(0, 2, 1, "Logout account");
            }
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    copyGuestDataToSdcard(packageName);
                    return true;
                }
                if (item.getItemId() == 2) {
                    confirmBgmiLogout(packageName);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void confirmBgmiLogout(String packageName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout account")
                .setMessage("Logout the BGMI account from this virtual clone?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    Toast.makeText(this, "Logging out…", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        boolean success = BgmiLogoutHelper.logoutAccount(packageName, 0);
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                success ? "BGMI account logged out" : "BGMI logout failed",
                                Toast.LENGTH_LONG
                        ).show());
                    }, "bgmi-logout").start();
                })
                .show();
    }

    private void copyGuestDataToSdcard(String packageName) {
        File srcData = new File(getFilesDir(), ".vfs/data/user/0/" + packageName);
        File srcExt = new File(getFilesDir(),
                ".vfs/storage/emulated/0/Android/data/" + packageName);
        File destRoot = new File(Environment.getExternalStorageDirectory(),
                "anubisloader/game_data/" + packageName);

        if (!srcData.exists() && !srcExt.exists()) {
            Toast.makeText(this, "No game data found for " + packageName, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Copying data to SD card…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean ok = true;
            String err = null;
            try {
                if (destRoot.exists()) {
                    deleteRecursive(destRoot);
                }
                //noinspection ResultOfMethodCallIgnored
                destRoot.mkdirs();
                if (srcData.exists()) {
                    copyRecursive(srcData, new File(destRoot, "data"));
                }
                if (srcExt.exists()) {
                    copyRecursive(srcExt, new File(destRoot, "android_data"));
                }
            } catch (Exception e) {
                ok = false;
                err = e.getMessage();
                Log.e("BgmiBypass", "copyGuestDataToSdcard failed", e);
            }
            final boolean success = ok;
            final String errorMsg = err;
            final String destPath = destRoot.getAbsolutePath();
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(MainActivity.this,
                            "Copied to " + destPath, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Copy failed: " + (errorMsg != null ? errorMsg : "unknown"),
                            Toast.LENGTH_LONG).show();
                }
            });
        }, "copy-game-data").start();
    }

    private static void copyRecursive(File src, File dest) throws java.io.IOException {
        if (src.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            dest.mkdirs();
            File[] children = src.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                copyRecursive(child, new File(dest, child.getName()));
            }
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        FileUtils.copyFile(src, dest);
    }

    public void SettingESP() {
    // Request Storage + Mic Permissions

    sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

    findViewById(R.id.savesetting).setOnClickListener(v -> {
        try {
            importSharedPreferences();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.failed_to_import, Toast.LENGTH_SHORT).show();
        }
    });

    findViewById(R.id.exportsetting).setOnClickListener(v -> {
   
            Toast.makeText(MainActivity.this, R.string.failed_to_export, Toast.LENGTH_SHORT).show();
        
    });

    findViewById(R.id.resetsetting).setOnClickListener(v -> {
        resetSharedPreferences();
        Toast.makeText(MainActivity.this, R.string.success_reset, Toast.LENGTH_SHORT).show();
    });
}

    private void importSharedPreferences() throws IOException {
        File srcFile = new File(Environment.getExternalStorageDirectory(), PREF_NAME + ".xml");
        File dstFile = new File(getApplication().getDataDir().toString() + "/shared_prefs/" + PREF_NAME + ".xml");

        if (srcFile.exists()) {
            FileChannel src = null;
            FileChannel dst = null;
            try {
                src = new FileInputStream(srcFile).getChannel();
                dst = new FileOutputStream(dstFile).getChannel();
                dst.transferFrom(src, 0, src.size());
                Toast.makeText(MainActivity.this, getString(R.string.imported_from) + srcFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } finally {
                if (src != null) {
                    src.close();
                }
                if (dst != null) {
                    dst.close();
                }
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.setting_esp_file_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    void gameversion(LinearLayout a, LinearLayout b, LinearLayout c, LinearLayout d, LinearLayout e) {
        a.setBackgroundResource(R.drawable.bgfituron);
        b.setBackgroundResource(R.drawable.bgfituroff);
        c.setBackgroundResource(R.drawable.bgfituroff);
        d.setBackgroundResource(R.drawable.bgfituroff);
        e.setBackgroundResource(R.drawable.bgfituroff);
    }


    void init() {
        //Animation animation = AnimationUtils.loadAnimation(this, R.anim.bounce);
        LinearLayout navhome = findViewById(R.id.navhome);
        LinearLayout navsetting = findViewById(R.id.navsetting);
        LinearLayout effecthome = findViewById(R.id.effecthome);
        LinearLayout effectsetting = findViewById(R.id.effectsetting);
        LinearLayout menu1 = findViewById(R.id.imenu1);
        LinearLayout menu2 = findViewById(R.id.imenu2);
        ImageView home = findViewById(R.id.imghome);
        ImageView sett = findViewById(R.id.imgsett);
        TextView txtsett = findViewById(R.id.txtsett);
        TextView txthome = findViewById(R.id.txthome);
        //TextView headtext = findViewById(R.id.headtext);

        navhome.setOnClickListener(v -> {
            menu1.setVisibility(View.VISIBLE);
            menu2.setVisibility(View.GONE);
            //headtext.setText("Home");

            txthome.setTextColor(getResources().getColor(R.color.main));
            txtsett.setTextColor(getResources().getColor(R.color.gray));
            home.setBackgroundResource(R.drawable.ic_home);
            sett.setBackgroundResource(R.drawable.outline_settings_24);
        });

        navsetting.setOnClickListener(v -> {
            menu1.setVisibility(View.GONE);
            menu2.setVisibility(View.VISIBLE);
            //headtext.setText("Settings");


            txthome.setTextColor(getResources().getColor(R.color.gray));
            txtsett.setTextColor(getResources().getColor(R.color.main));
            home.setBackgroundResource(R.drawable.ic_home_outline);
            sett.setBackgroundResource(R.drawable.ic_helpon);
        });

    }


    void Makedir() {
        if (!Shell.rootAccess()) {
            File GlobalFolder = new File("/storage/emulated/0/blackbox/Android/obb/com.tencent.ig/");
            File KoreaFolder = new File("/storage/emulated/0/blackbox/Android/obb/com.pubg.krmobile/");
            File VietnamFolder = new File("/storage/emulated/0/blackbox/Android/obb/com.vng.pubgmobile/");
            File TaiwanFolder = new File("/storage/emulated/0/blackbox/Android/obb/com.rekoo.pubgm/");
            File BgmiFolder = new File("/storage/emulated/0/blackbox/Android/obb/com.pubg.imobile/");
            GlobalFolder.mkdirs();
            KoreaFolder.mkdirs();
            VietnamFolder.mkdirs();
            TaiwanFolder.mkdirs();
            BgmiFolder.mkdirs();
        }
    }



    public void launchApk(String packageName) {
        if (!isPackageInstalled(packageName)) {
            //TastyToast.makeText(getApplicationContext(),String.valueOf((R.string.not_installed)), TastyToast.LENGTH_LONG,TastyToast.ERROR);
            return;
        }
        try {
            core.launchApk(packageName, 0);
            LibAnogsPatcher.resetSession();
            LibAnogsPatcher.startOnGameLaunch(this);
            GamePidHelper.refreshPidFile(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unInstallApp(String packageName) {
        AnubisCore.get().uninstallPackageAsUser(packageName, 0);

    }

    public boolean isRunning(String packageName) {

        return core.isInstalled(packageName, 0);

    }

    public void stopRunningApp(String packageName) {

        AnubisCore.get().stopPackage(packageName, 0);
    }

    public ApplicationInfo getApplicationInfoContainer(String packageName) {
        if (!isPackageInstalled(packageName)) {
            Toast.makeText(getApplicationContext(), R.string.app_not_installed_please_install_first, Toast.LENGTH_LONG).show();
            return null;
        }

        ApplicationInfo applicationInfo = null;

        if (applicationInfo == null) {
            return null;
        }
        return applicationInfo;
    }

    boolean isPackageInstalled(String packageName) {
        return core.isInstalled(packageName, 0);
    }


    /*private void unInstallWithDellay(String packageName) {
        UiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            unInstallApp(packageName);
            time = System.currentTimeMillis() - time;
            long delta = 500L - time;
            if (delta > 0) {
               // UiKit.sleep(delta);
            }
        }).done((res) -> {
            // doInitRecycler();
            doHideProgress();
            toastImage(R.drawable.ic_check, packageName + (R.string.was_successfully_uninstalled));
        });
    }*/

    private void unInstallWithDellay(String packageName) {
        unInstallApp(packageName);
        //TastyToast.makeText(getApplicationContext(), getString(R.string.successfully_uninstalled), TastyToast.LENGTH_LONG,TastyToast.DEFAULT);
    }


  


    private void EnableCars() {
        SharedPreferences prefs = getSharedPreferences("espValue", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

       /* editor.putBoolean("BRDM", true);
        editor.putBoolean("UAZ", true);
        editor.putBoolean("Snowbike", true);
        editor.putBoolean("ATV1", true);
        editor.putBoolean("Mirado", true);
        editor.putBoolean("Dacia", true);
        editor.putBoolean("UTV", true);
        editor.putBoolean("Monster", true);
        editor.putBoolean("Motor Glider", true);
        editor.putBoolean("Buggy", true);
        editor.putBoolean("Bike", true);
        editor.putBoolean("CoupeRB", true);
        editor.putBoolean("Bus", true);
        editor.putBoolean("Truck", true);
        editor.putBoolean("Snowmobile", true);
        editor.putBoolean("LadaNiva", true);
        editor.putBoolean("Trike", true);
        editor.putBoolean("Scooter", true);
        editor.putBoolean("Tempo", true);
        editor.putBoolean("Jet", true);
        editor.putBoolean("Boat", true);
        editor.putBoolean("Rony", true);*/

        editor.apply(); // Save changes asynchronously
    }

    private void EnableItems() {
        SharedPreferences prefs = getSharedPreferences("espValue", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

       /* editor.putBoolean("6x", true);
        editor.putBoolean("5.56mm", true);
        editor.putBoolean("Grenade", true);
        editor.putBoolean("FirstAid", true);
        editor.putBoolean("Helmet L3", true);
        editor.putBoolean("3x", true);
        editor.putBoolean("MedKit", true);
        editor.putBoolean("Vest L3", true);
        editor.putBoolean("UMP", true);
        editor.putBoolean("Molotov", true);
        editor.putBoolean("Bag L3", true);
        editor.putBoolean("M416", true);
        editor.putBoolean("7.62mm", true);
        editor.putBoolean("AWM", true);
        editor.putBoolean("MG3", true);
        editor.putBoolean("AKM", true);
        editor.putBoolean("P90", true);
        editor.putBoolean("Groza", true);
        editor.putBoolean("LootBox", true);
        editor.putBoolean("Groza", true);
        editor.putBoolean("LootBox", true);

        // Bt Settings
        editor.putInt("Distances", 150);
        editor.putInt("getrangeAim", 200);*/

        editor.apply(); // Save changes asynchronously
    }


    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isGlobalInstalled(String packageNameGl) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageNameGl, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isKoreaInstalled(String packageNameKr) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageNameKr, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isVNGInstalled(String packageNameVng) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageNameVng, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isTWInstalled(String packageNameTW) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageNameTW, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void displayRamUsage() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalMemory = memoryInfo.totalMem;
        long availableMemory = memoryInfo.availMem;
        long usedMemory = totalMemory - availableMemory;
        double percentageUsed = (double) usedMemory / totalMemory * 100;

        TextView ramTextView = findViewById(R.id.ramTextView);
        ProgressBar ramProgressBar = findViewById(R.id.storageProgressBar);
        TextView percentageTextView = findViewById(R.id.percentageText);

        if (ramTextView != null && ramProgressBar != null && percentageTextView != null) {
            ramTextView.setText(String.format(Locale.getDefault(), getString(R.string._2f_gb_used_2f_gb_available), usedMemory / (1024.0 * 1024.0 * 1024.0), availableMemory / (1024.0 * 1024.0 * 1024.0)));

            ramProgressBar.setProgress((int) percentageUsed);
            percentageTextView.setText(String.format(Locale.getDefault(), "%.0f%%", percentageUsed));
        }
    }


    private long getTotalMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    private long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    private void displayStorageUsage() {
        try {
            StatFs stat = new StatFs(getFilesDir().getAbsolutePath());
            long totalBytes = stat.getTotalBytes();
            long freeBytes = stat.getFreeBytes();
            long usedBytes = totalBytes - freeBytes;
            double percentageUsed = (double) usedBytes / totalBytes * 100;

            TextView storageInfoTextView = findViewById(R.id.storageInfoText);
            ProgressBar storageProgressBar = findViewById(R.id.InternalProgressBar);
            TextView percentageStorageTextView = findViewById(R.id.percentageStorageText);

            if (storageInfoTextView != null && storageProgressBar != null && percentageStorageTextView != null) {
                storageInfoTextView.setText(String.format(Locale.getDefault(), getString(R.string._2f_gb_used_2f_gb_available_), usedBytes / (1024.0 * 1024.0 * 1024.0), freeBytes / (1024.0 * 1024.0 * 1024.0)));

                storageProgressBar.setProgress((int) percentageUsed);
                percentageStorageTextView.setText(String.format(Locale.getDefault(), "%.0f%%", percentageUsed));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    ////////////////////////// Panel Enc ////////////////////////////////////////

    

    
    

    ////////////////////////// Other ////////////////////////////////////////
    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void launchbypass() {



    }

   void runant(final String nf){
        excpp("/"+nf);
		}

	private void ExecuteElf(String shell) {
	try {
	Runtime.getRuntime().exec(shell);

        } catch (Exception e) {
            e.printStackTrace();
			}
			}
			public void excpp(String path) {
			try {
					ExecuteElf("chmod 777 " + getFilesDir() + path);
					ExecuteElf(getFilesDir() + path);
						ExecuteElf("su -c chmod 777 " + getFilesDir() + path);
						ExecuteElf("su -c " + getFilesDir() + path);
					} catch (Exception e) {

        }
			}
    
    /*
public void launchbypassNoRoot() {
    Handler handler = new Handler(Looper.getMainLooper());

    // After 20 sec
    handler.postDelayed(() -> {
        runant("/ayan 2");
      //  runant("/ayan 7");

        // After another 30 sec
        handler.postDelayed(() -> {
            runant("/ayan 2");

            // After another 38 sec
            handler.postDelayed(() -> {
                runant("/ayan 2");
            }, 38_000);

        }, 30_000);

    }, 20_000);
}
*/
    private void Loadssets() {
        MoveAssets(getFilesDir() + "/", "socs64");
        if (MoveAssets(getFilesDir() + "/", "sock64")) {
            try {
                Runtime.getRuntime().exec("chmod 777 " + getFilesDir() + "/sock64");
            } catch (IOException ignored) {
            }
            Log.i("BgmiBypass", "sock64 loaded from assets (sock src build)");
        } else {
            Log.e("BgmiBypass", "sock64 missing from assets — rebuild sock src");
        }
        MoveAssets(getFilesDir() + "/", "bypass");
        MoveAssets(getFilesDir() + "/", "socu32");
        MoveAssets(getFilesDir() + "/", "TW");
        MoveAssets(getFilesDir() + "/", "via.apk");
        MoveAssets(getFilesDir() + "/", "kernels64");
    }

    private boolean MoveAssets(String outPath, String fileName) {
        File file = new File(outPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("--Method--", "copyAssetsSingleFile: cannot create directory.");
                return false;
            }
        }
        try {
            InputStream inputStream = getAssets().open(fileName);
            File outFile = new File(file, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = inputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPatcher();
        stopService(new Intent(MainActivity.get(), DaemonService.class));
        stopService(new Intent(MainActivity.get(), FloatService.class));
        stopService(new Intent(MainActivity.get(), Overlay.class));
        stopService(new Intent(MainActivity.get(), ToggleBullet.class));
        stopService(new Intent(MainActivity.get(), ToggleAim.class));
        stopService(new Intent(MainActivity.get(), ToggleSimulation.class));
        stopService(new Intent(MainActivity.get(), FightMode.class));

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finishAffinity();
        } else {
            //  KToast.warningToast(MainActivity.this, getString(R.string.press_back_again_to_exit),
            //   Gravity.BOTTOM, KToast.LENGTH_AUTO);
            backPressedTime = System.currentTimeMillis();
        }
    }



    public LinearProgressIndicator getProgresBar() {
        if (progres == null) {
            progres = findViewById(R.id.progress);
        }
        return progres;
    }

    public void doShowProgress(boolean indeterminate) {
        if (progres == null) {
            return;
        }
        progres.setVisibility(View.VISIBLE);
        progres.setIndeterminate(indeterminate);

        if (!indeterminate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                progres.setMin(0);
            }
            progres.setMax(100);
        }
    }

    public void doHideProgress() {
        if (progres == null) {
            return;
        }
        progres.setIndeterminate(true);
        progres.setVisibility(View.GONE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FloatService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPatcher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.get())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 123);
            } else {
                startFloater();
            }
        }
    }



    private void startFloater() {
        
            startService(new Intent(MainActivity.get(), FloatService.class));
            loadAssets();
            
            
           
        } 

    private void stopPatcher() {
        stopService(new Intent(MainActivity.get(), FloatService.class));
        stopService(new Intent(MainActivity.get(), Overlay.class));
        stopService(new Intent(MainActivity.get(), ToggleAim.class));
        stopService(new Intent(MainActivity.get(), ToggleBullet.class));
        stopService(new Intent(MainActivity.get(), ToggleSimulation.class));
        stopService(new Intent(MainActivity.get(), FightMode.class));
    }

   
    public void loadAssets() {
		String filepath =Environment.getExternalStorageDirectory() + "/Android/data/.tyb";
        FileOutputStream fos = null;
        try {
			fos = new FileOutputStream(filepath);
			byte[] buffer = "DO NOT DELETE".getBytes();
			fos.write(buffer, 0, buffer.length);
			fos.close();
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		daemonPath =getFilesDir().toString() +"/sock64";
     //  libPath =getFilesDir().toString() +"/shayan";
		if(Shell.rootAccess()) {
			socket = "su -c " + daemonPath;
		} else {
			socket = daemonPath;
		}
		try {
			Runtime.getRuntime().exec("chmod 777 " + daemonPath);
			//Runtime.getRuntime().exec("chmod 777 " + libPath);
		} catch (IOException e) {
		}
	}

    // obb copy mathod
    
    private void handleAnoTmp(Context context) {

    try {
        File dir = new File("/data/data/lauresprojects.com.recorder/files/");
        
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File target = new File(dir, "ano_tmp");

        // 👉 force delete (file OR folder)
        if (target.exists()) {
            deleteRecursive(target);
        }

        // 👉 recreate clean file
        boolean created = target.createNewFile();

        android.util.Log.d("ANO_TMP", "Path: " + target.getAbsolutePath());
        android.util.Log.d("ANO_TMP", "Created: " + created);

    } catch (Exception e) {
        android.util.Log.e("ANO_TMP", "Error", e);
    }
}
private void deleteRecursive(File file) {

    if (file == null || !file.exists()) return;

    if (file.isDirectory()) {

        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                deleteRecursive(child);
            }
        }
    }

    file.delete();
}

    private class MyCopyTask extends AsyncTask<String, Integer, File> {
    AlertDialog dialog;
    LinearProgressIndicator progressBar;
    TextView percentText;

    @Override
    protected void onPreExecute() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setCancelable(false);
        View v = getLayoutInflater().inflate(R.layout.dialog_pakgamerz_progress, null);
        
        progressBar = v.findViewById(R.id.copy_progress_bar);
        percentText = v.findViewById(R.id.copy_percent_text);
        
        builder.setView(v);
        dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    @Override
    protected File doInBackground(String... params) {
        File source = new File(params[0]);
        String filename = source.getName();
        File destFolder = new File("/storage/emulated/0/blackbox/Android/obb/" + params[1]);
        if (!destFolder.exists()) destFolder.mkdirs();
        
        File destination = new File(destFolder, filename);

        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            
            byte[] buffer = new byte[8192];
            long totalBytes = source.length();
            long bytesCopied = 0;
            int read;
            
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                bytesCopied += read;
                // Calculate percentage
                int progress = (int) ((bytesCopied * 100) / totalBytes);
                publishProgress(progress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return destination;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        progressBar.setProgress(progress);
        percentText.setText(progress + "%");
    }

    @Override
    protected void onPostExecute(File result) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (result != null && result.exists()) {
             Toast.makeText(ctx, "OBB Setup Complete", Toast.LENGTH_SHORT).show();
        }
    }
}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // if (requestCode == Storage_Permission) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (!hasAllPermissionsGranted(grantResults)) {
                Toast.makeText(this, R.string.unable_to_get_storage_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    void RunShell(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // obb mathod end

    @Override
    protected void onResume() {
        super.onResume();
        CountTimerAccout();
        setFullScreen();
        boolean needsRecreate = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("needs_recreate", false);
        if (needsRecreate) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("needs_recreate", false).apply();
        }
    }
    
    // ============ PERMISSIONS ============
private void showPermissionsSheet() {
    // Simple Android permission request
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        };
        
        List<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            // All granted - restart app
            restartApp();
        }
    }
}

private void restartApp() {
    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                       Intent.FLAG_ACTIVITY_NEW_TASK | 
                       Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    finish();
}

    private void setFullScreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    private void CountTimerAccout() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date expiryDate = dateFormat.parse(EXP());

                    TextView keylicencedrawer = findViewById(R.id.keydisplay);

                 //   String fullKey = PASSKEY + ":" + USERKEY;
                 String fullKey = PASSKEY ;
                    keylicencedrawer.setText(fullKey);

                    TextView Datetimer = findViewById(R.id.keyexpiry);
                    if (expiryDate != null) {
                        SimpleDateFormat std = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                        Datetimer.setText(getString(R.string.expiry) + std.format(expiryDate));
                    }
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;
                    long days = distance / (24 * 60 * 60 * 1000);
                    long hours = distance / (60 * 60 * 1000) % 24;
                    long minutes = distance / (60 * 1000) % 60;
                    long seconds = distance / 1000 % 60;
                    if (distance < 0) {
                    } else {
                        TextView Hari = findViewById(R.id.days);
                        TextView Jam = findViewById(R.id.hours);
                        TextView Menit = findViewById(R.id.minutes);
                        TextView Detik = findViewById(R.id.second);
                        if (days > 0) {
                            Hari.setText(" " + String.format("%02d", days));
                        }
                        if (hours > 0) {
                            Jam.setText(" " + String.format("%02d", hours));
                        }
                        if (minutes > 0) {
                            Menit.setText(" " + String.format("%02d", minutes));
                        }
                        if (seconds > 0) {
                            Detik.setText(" " + String.format("%02d", seconds));
                        }
                    }
                    displayRamUsage();
                    displayStorageUsage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }


    public void showBottomSheetDialogUninstall(Drawable icon, String title, String msg, boolean cancelable, View.OnClickListener confirmListener) {
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setCancelable(cancelable);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);

        ImageView img = bottomSheetDialog.findViewById(R.id.icon);
        if (icon != null && img != null) img.setImageDrawable(icon);
        
        TextView title_tv = bottomSheetDialog.findViewById(R.id.title);
        if (title_tv != null) title_tv.setText(title);
        
        TextView msg_tv = bottomSheetDialog.findViewById(R.id.msg);
        if (msg_tv != null) msg_tv.setText(msg);

        MaterialButton confirm = bottomSheetDialog.findViewById(R.id.btn);
        if (confirm != null && confirmListener != null) confirm.setOnClickListener(confirmListener);

        MaterialButton cancel = bottomSheetDialog.findViewById(R.id.btn_cancle);
        if (cancel != null) cancel.setOnClickListener(v -> dismissBottomSheetDialog());

        bottomSheetDialog.show();
    }

    public void showBottomSheetDialog(Drawable icon, String title, String msg, boolean cancelable, View.OnClickListener listener, View.OnClickListener listenerCancle) {
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setCancelable(cancelable);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);
        
        ImageView img = bottomSheetDialog.findViewById(R.id.icon);
        if (icon != null && img != null) img.setImageDrawable(icon);
        
        TextView title_tv = bottomSheetDialog.findViewById(R.id.title);
        if (title_tv != null) title_tv.setText(title);
        
        TextView msg_tv = bottomSheetDialog.findViewById(R.id.msg);
        if (msg_tv != null) msg_tv.setText(msg);
        
        MaterialButton posBtn = bottomSheetDialog.findViewById(R.id.btn);
        if (posBtn != null && listener != null) posBtn.setOnClickListener(listener);
        
        MaterialButton negBtn = bottomSheetDialog.findViewById(R.id.btn_cancle);
        if (negBtn != null) {
            if (listenerCancle != null) negBtn.setOnClickListener(listenerCancle);
            else negBtn.setVisibility(View.GONE);
        }
        bottomSheetDialog.show();
    }

    public void showBottomSheetDialog4(Drawable icon, String title, String msg, boolean cancelable, View.OnClickListener listener, View.OnClickListener listenerCancle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder.setMessage(msg).setCancelable(cancelable)
            .setPositiveButton("Allow Permission", (dialog, which) -> { if (listener != null) listener.onClick(null); })
            .setNegativeButton("Cancel", (dialog, which) -> { if (listenerCancle != null) listenerCancle.onClick(null); });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    public void dismissBottomSheetDialog() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
    }
private static void writeLog(String msg) {
    try {
        File logFile = new File("/storage/emulated/0/anogs_log.txt");
        FileWriter fw = new FileWriter(logFile, true);
        fw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " - " + msg + "\n");
        fw.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}