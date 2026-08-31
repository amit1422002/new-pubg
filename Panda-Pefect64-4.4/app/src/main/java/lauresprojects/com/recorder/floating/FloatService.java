package lauresprojects.com.recorder.floating;

import static lauresprojects.com.recorder.activity.MainActivity.bitversi;
import static lauresprojects.com.recorder.activity.MainActivity.gameint;
import static lauresprojects.com.recorder.activity.MainActivity.modestatus;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import lauresprojects.com.recorder.R;
import lauresprojects.com.recorder.activity.MainActivity;
import lauresprojects.com.recorder.utils.FLog;

import com.topjohnwu.superuser.Shell;

import java.util.Locale;

public class FloatService extends Service {

    static {
        System.loadLibrary("safecheat");
    }

    Context ctx;
    private View mainView;
    private PowerManager.WakeLock mWakeLock;
    private WindowManager windowManagerMainView;
    private WindowManager.LayoutParams paramsMainView;
    private LinearLayout layout_main_view;
    private RelativeLayout layout_icon_control_view;
    public static String typelogin;

    private void setLokasi(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("bahasa", lang);
        editor.apply();

    }

    private void loadbahasa() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String bahasa = sharedPreferences.getString("bahasa", "");
        setLokasi(bahasa);
    }

    private static int getLayoutType() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        return LAYOUT_FLAG;
    }

    private void StartAimTouch() {
        startService(new Intent(getApplicationContext(), ToggleSimulation.class));
    }

    private void StopAimTouch() {
        stopService(new Intent(getApplicationContext(), ToggleSimulation.class));
    }

    private void StartAimFloat() {
        startService(new Intent(getApplicationContext(), ToggleAim.class));
    }

    private void StopAimFloat() {
        stopService(new Intent(getApplicationContext(), ToggleAim.class));
    }

    private void StartAimBulletFloat() {
        startService(new Intent(getApplicationContext(), ToggleBullet.class));
    }

    private void StopAimBulletFloat() {
        stopService(new Intent(getApplicationContext(), ToggleBullet.class));
    }

    public native void SettingValue(int setting_code, boolean value);

    public native void SettingMemory(int setting_code, boolean value);

    public native void SettingAim(int setting_code, boolean value);

    public native void SkinHack(int setting_code);
    public native void Skinbag(int setting_code);
    public native void Skinhelmet(int setting_code);
    
    public native void RadarSize(int size);

    public native void Range(int range);

    public native void recoil(int recoil);

    public native void recoil2(int recoil);

    public native void recoil3(int recoil);

    public native void Target(int target);

    public native void AimBy(int aimby);

    public native void AimWhen(int aimwhen);

    public native void distances(int distances);

    public native void Bulletspeed(int bulletspeed);

    public native void WideView(int wideview);

    public native void AimingSpeed(int aimingspeed);

    public native void Smoothness(int smoothness);

    public native void TouchSize(int touchsize);

    public native void TouchPosX(int touchposx);

    public native void TouchPosY(int touchposy);


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ctx = getApplicationContext();
        InitShowMainView();
        loadbahasa();
        
     }

    private void InitShowMainView() {
        mainView = LayoutInflater.from(this).inflate(R.layout.float_service, null);
        paramsMainView = getparams();
        windowManagerMainView = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManagerMainView.addView(mainView, paramsMainView);
        layout_icon_control_view = mainView.findViewById(R.id.layout_icon_control_view);
        layout_main_view = mainView.findViewById(R.id.layout_main_view);
        
        if (MainActivity.Record) {
            HideRecorder.setFakeRecorderWindowLayoutParams(paramsMainView);
        }
        
        View layout_close_main_view = mainView.findViewById(R.id.layout_close_main_view);
        layout_close_main_view.setSelected(true);
        layout_close_main_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                layout_main_view.setVisibility(View.GONE);
                layout_icon_control_view.setVisibility(View.VISIBLE);
            }
        });

        LinearLayout drag_area = mainView.findViewById(R.id.drag_area);
        View.OnTouchListener mTouch = onTouchListener();
        drag_area.setOnTouchListener(mTouch);
        layout_icon_control_view.setOnTouchListener(mTouch);

        initDesign();

        // Attach drag handles from tabs
        if (mainView.findViewById(R.id.esp_new_title) != null) mainView.findViewById(R.id.esp_new_title).setOnTouchListener(mTouch);
        if (mainView.findViewById(R.id.aimbot_title) != null) mainView.findViewById(R.id.aimbot_title).setOnTouchListener(mTouch);
        if (mainView.findViewById(R.id.visual_title) != null) mainView.findViewById(R.id.visual_title).setOnTouchListener(mTouch);
        if (mainView.findViewById(R.id.memory_title) != null) mainView.findViewById(R.id.memory_title).setOnTouchListener(mTouch);
        if (mainView.findViewById(R.id.inventory_title) != null) mainView.findViewById(R.id.inventory_title).setOnTouchListener(mTouch);
        if (mainView.findViewById(R.id.skin_title) != null) mainView.findViewById(R.id.skin_title).setOnTouchListener(mTouch);

    }

    public void initDesign() {
        // Sidebar Navigation
        ImageView navMain = mainView.findViewById(R.id.nav_main);
        ImageView navEsp = mainView.findViewById(R.id.nav_esp_new);
        ImageView navSettings = mainView.findViewById(R.id.nav_settings_new);

        navMain.setOnClickListener(v -> {
            mainView.findViewById(R.id.tab_esp_new_container).setVisibility(View.GONE);
            mainView.findViewById(R.id.tab_original_container).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf1).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf2).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf3).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf4).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf5).setVisibility(View.GONE);
            navMain.setSelected(true);
            navEsp.setSelected(false);
            navSettings.setSelected(false);
        });

        navEsp.setOnClickListener(v -> {
            mainView.findViewById(R.id.tab_esp_new_container).setVisibility(View.GONE);
            mainView.findViewById(R.id.tab_original_container).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf1).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf2).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf3).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf4).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf5).setVisibility(View.GONE);
            navEsp.setSelected(true);
            navMain.setSelected(false);
            navSettings.setSelected(false);
        });

        navSettings.setOnClickListener(v -> {
            mainView.findViewById(R.id.tab_esp_new_container).setVisibility(View.GONE);
            mainView.findViewById(R.id.tab_original_container).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf1).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.menuf2).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf3).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf4).setVisibility(View.GONE);
            mainView.findViewById(R.id.menuf5).setVisibility(View.GONE);
            navSettings.setSelected(true);
            navEsp.setSelected(false);
            navMain.setSelected(false);
        });

        // Binding new ESP Tab
        View espTabNew = mainView.findViewById(R.id.tab_esp_new_container);
        if (espTabNew != null) {
            CheckBox isenableespNew = espTabNew.findViewById(R.id.isenableesp_new);
            if (isenableespNew != null) {
                isenableespNew.setChecked(getConfig("isenableesp"));
                isenableespNew.setOnCheckedChangeListener((p1, isChecked) -> {
                    setValue("isenableesp", isChecked);
                    if (isChecked) {
                        startService(new Intent(ctx, Overlay.class));
                    } else {
                        stopService(new Intent(ctx, Overlay.class));
                    }
                });
            }

            final SeekBar distanceSeekBar = espTabNew.findViewById(R.id.distances_new);
            final TextView distanceText = espTabNew.findViewById(R.id.lbl_distance_new);
            if (distanceSeekBar != null && distanceText != null) {
                setupSeekBar(distanceSeekBar, distanceText, getDistances(), new Runnable() {
                    @Override
                    public void run() {
                        int pos = distanceSeekBar.getProgress();
                        setDistances(pos);
                        distances(pos);
                        distanceText.setText("Distance : " + pos);
                    }
                });
            }

            final SeekBar recoilSeekBar = espTabNew.findViewById(R.id.recoil2_new);
            final TextView recoilText = espTabNew.findViewById(R.id.lbl_recoil_new);
            if (recoilSeekBar != null && recoilText != null) {
                setupSeekBar(recoilSeekBar, recoilText, getrecoilAim2(), new Runnable() {
                    @Override
                    public void run() {
                        int pos = recoilSeekBar.getProgress();
                        getrecoilAim2(pos);
                        recoil2(pos);
                        recoilText.setText("Recoil Control : " + pos);
                    }
                });
            }

            View btnSettingsNew = espTabNew.findViewById(R.id.btn_settings_new);
            if (btnSettingsNew != null) {
                btnSettingsNew.setOnClickListener(v -> {
                    mainView.findViewById(R.id.tab_esp_new_container).setVisibility(View.GONE);
                    mainView.findViewById(R.id.tab_original_container).setVisibility(View.VISIBLE);
                    mainView.findViewById(R.id.menuf1).setVisibility(View.VISIBLE);
                    mainView.findViewById(R.id.menuf2).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf3).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf4).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf5).setVisibility(View.GONE);
                    navSettings.setSelected(true);
                    navEsp.setSelected(false);
                    navMain.setSelected(false);
                });
            }

            View btnOtherSettingsNew = espTabNew.findViewById(R.id.btn_other_settings_new);
            if (btnOtherSettingsNew != null) {
                btnOtherSettingsNew.setOnClickListener(v -> {
                    mainView.findViewById(R.id.tab_esp_new_container).setVisibility(View.GONE);
                    mainView.findViewById(R.id.tab_original_container).setVisibility(View.VISIBLE);
                    mainView.findViewById(R.id.menuf1).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf2).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf3).setVisibility(View.VISIBLE);
                    mainView.findViewById(R.id.menuf4).setVisibility(View.GONE);
                    mainView.findViewById(R.id.menuf5).setVisibility(View.GONE);
                    navSettings.setSelected(true);
                    navEsp.setSelected(false);
                    navMain.setSelected(false);
                });
            }
        }

        // Original initialization for other tabs
        visual(mainView.findViewById(R.id.menuf1));
        items(mainView.findViewById(R.id.menuf2));
        aimbot(mainView.findViewById(R.id.menuf3));
        memory(mainView.findViewById(R.id.menuf4));
        skin(mainView.findViewById(R.id.menuf5));
    }

    private void skin(View skin) {
        if (skin == null) return;
        skinvisual((CheckBox) skin.findViewById(R.id.bloodraven), 1);
        skinvisual((CheckBox) skin.findViewById(R.id.goldenpharaoh), 2);
        skinvisual((CheckBox) skin.findViewById(R.id.avalanche), 3);
        skinvisual((CheckBox) skin.findViewById(R.id.poseidon), 4);
        skinvisual((CheckBox) skin.findViewById(R.id.arcanejester), 5);
        skinvisual((CheckBox) skin.findViewById(R.id.silvanus), 6);
        skinvisual((CheckBox) skin.findViewById(R.id.marmoris), 7);
        skinvisual((CheckBox) skin.findViewById(R.id.fiore), 8);
        skinvisual((CheckBox) skin.findViewById(R.id.ignis), 9);
        skinvisual((CheckBox) skin.findViewById(R.id.whitemummy), 10);
        skinvisual((CheckBox) skin.findViewById(R.id.galadria), 11);
        skinvisual((CheckBox) skin.findViewById(R.id.flamewraith), 12);
        skinvisual((CheckBox) skin.findViewById(R.id.majestic), 13);
        skinvisual((CheckBox) skin.findViewById(R.id.bramble), 14);
        skinvisual((CheckBox) skin.findViewById(R.id.nether), 15);
        skinvisual((CheckBox) skin.findViewById(R.id.swan), 16);
        skinvisual((CheckBox) skin.findViewById(R.id.celestial), 17);
        skinvisual((CheckBox) skin.findViewById(R.id.snowstar), 18);
        skinvisual((CheckBox) skin.findViewById(R.id.arctic), 19);
        skinvisual((CheckBox) skin.findViewById(R.id.feral), 20);
        skinvisual((CheckBox) skin.findViewById(R.id.vampyra), 21);
        skinvisual((CheckBox) skin.findViewById(R.id.serene), 22);
        skinvisual((CheckBox) skin.findViewById(R.id.mercury), 23);
        skinvisual((CheckBox) skin.findViewById(R.id.luminous), 24);
        skinvisual((CheckBox) skin.findViewById(R.id.origin), 25);
        skinvisual((CheckBox) skin.findViewById(R.id.serpengleam), 26);
        skinvisual((CheckBox) skin.findViewById(R.id.shinobi), 27);
        skinvisual((CheckBox) skin.findViewById(R.id.foxy), 28);
        skinvisual((CheckBox) skin.findViewById(R.id.glacial), 29);
        skinvisual((CheckBox) skin.findViewById(R.id.boxerbolt), 30);
        skinvisual((CheckBox) skin.findViewById(R.id.dandy), 31);
        skinvisual((CheckBox) skin.findViewById(R.id.neptune), 32);
        skinvisual((CheckBox) skin.findViewById(R.id.noctum), 33);
        skinvisual((CheckBox) skin.findViewById(R.id.crimson), 34);

        skinvisualbag((CheckBox) skin.findViewById(R.id.bag_poseidon), 1);
        skinvisualbag((CheckBox) skin.findViewById(R.id.bag_mystique), 2);
        skinvisualbag((CheckBox) skin.findViewById(R.id.bag_ancient), 3);
        skinvisualbag((CheckBox) skin.findViewById(R.id.bag_galadria), 4);
        skinvisualbag((CheckBox) skin.findViewById(R.id.bag_alfheim), 5);

        skinvisualhelmet((CheckBox) skin.findViewById(R.id.helmet_inferno), 1);
        skinvisualhelmet((CheckBox) skin.findViewById(R.id.helmet_auric), 2);
        skinvisualhelmet((CheckBox) skin.findViewById(R.id.helmet_galadria), 3);
        skinvisualhelmet((CheckBox) skin.findViewById(R.id.helmet_shining), 4);
        skinvisualhelmet((CheckBox) skin.findViewById(R.id.helmet_kingdom), 5);
    }



    private View.OnTouchListener onTouchListener() {
        return new View.OnTouchListener() {
            final View collapsedView = layout_icon_control_view;
            final View expandedView = layout_main_view;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = paramsMainView.x;
                        initialY = paramsMainView.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        paramsMainView.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsMainView.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManagerMainView.updateViewLayout(mainView, paramsMainView);
                        return true;

                }
                return false;
            }
        };
    }

    private boolean isViewCollapsed() {
        return mainView == null || layout_icon_control_view.getVisibility() == View.VISIBLE;
    }

    private WindowManager.LayoutParams getparams() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getLayoutType(),
                getFlagsType(),
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;

        return params;
    }

    private int getFlagsType() {
        int LAYOUT_FLAG = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        return LAYOUT_FLAG;
    }

       @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable(){
                @Override
                public void run() {

                }
            }).start();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        
        if (mainView != null){
            windowManagerMainView.removeView(mainView);
		}
		
        
    }
    

    boolean getConfig(String key) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }

    private int getFps() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("fps", 100);
    }

    private void setFps(int fps) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("fps", fps);
        ed.apply();
    }

    private void setValue(String key, boolean b) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(key, b);
        ed.apply();

    }

    private void setradarSize(int radarSize) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("radarSize", radarSize);
        ed.apply();
    }

    private int getradarSize() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("radarSize", 0);
    }

    private int getrangeAim() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getrangeAim", 0);
    }

    private void getrangeAim(int getrangeAim) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getrangeAim", getrangeAim);
        ed.apply();
    }

    private int getDistances() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("Distances", 0);
    }

    private void setDistances(int Distances) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("Distances", Distances);
        ed.apply();
    }

    private int getrecoilAim() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getrecoilAim", 0);
    }

    private void getrecoilAim(int getrecoilAim) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getrecoilAim", getrecoilAim);
        ed.apply();
    }

    private int getrecoilAim2() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getrecoilAim2", 0);
    }

    private void getrecoilAim2(int getrecoilAim) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getrecoilAim2", getrecoilAim);
        ed.apply();
    }

    private int getrecoilAim3() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getrecoilAim2", 0);
    }

    private void getrecoilAim3(int getrecoilAim) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getrecoilAim2", getrecoilAim);
        ed.apply();
    }

    private int getbulletspeedAim() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getbulletspeedAim", 0);
    }

    private void getbulletspeedAim(int getbulletspeedAim) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getbulletspeedAim", getbulletspeedAim);
        ed.apply();
    }

    private int getwideview() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("getwideview", 0);
    }

    private void getwideview(int getwideview) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("getwideview", getwideview);
        ed.apply();
    }

    void setTouchSize(int touchsize) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("touchsize", touchsize);
        ed.apply();
    }

    int getTouchSize() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("touchsize", 600);
    }

    void setTouchPosX(int posX) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("posX", posX);
        ed.apply();
    }

    int getTouchPosX() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("posX", 650);
    }

    void setTouchPosY(int posY) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("posY", posY);
        ed.apply();
    }

    int getTouchPosY() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("posY", 1400);
    }

    private boolean getConfigitem(String key, boolean a) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getBoolean(key, a);
    }

    private void setConfigitem(String a, boolean b) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(a, b);
        ed.apply();
    }

    private int getEspValue(String a, int b) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt(a, b);
    }

    private void setEspValue(String a, int b) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt(a, b);
        ed.apply();
    }

    private int getAimSpeed() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("AimingSpeed", 18);
    }

    private void setAimSpeed(int AimingSpeed) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("AimingSpeed", AimingSpeed);
        ed.apply();
    }

    private int getSmoothness() {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        return sp.getInt("smoothness", 20);
    }

    private void setSmoothness(int smoothness) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("smoothness", smoothness);
        ed.apply();
    }

    public void skinvisual(final CheckBox a, final int b) {
        a.setChecked(getConfig((String) a.getText()));
        SkinHack(b);
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                SkinHack(b);
            }
        });
    }
    public void skinvisualbag(final CheckBox a, final int b) {
        a.setChecked(getConfig((String) a.getText()));
        Skinbag(b);
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                Skinbag(b);
            }
        });
    }
    public void skinvisualhelmet(final CheckBox a, final int b) {
        a.setChecked(getConfig((String) a.getText()));
        Skinhelmet(b);
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                Skinhelmet(b);
            }
        });
    }

    public void espvisual(final CheckBox a, final int b) {
        a.setChecked(getConfig((String) a.getText()));
        SettingValue(b, getConfig((String) a.getText()));
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                SettingValue(b, a.isChecked());
            }
        });
    }

    public void setaim(final CompoundButton a, final int b) {
        a.setChecked(getConfig((String) a.getText()));
        SettingAim(b, getConfig((String) a.getText()));
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                SettingAim(b, a.isChecked());
            }
        });
    }

    public void vehicless(final CheckBox checkBox) {
        checkBox.setChecked(getConfig((String) checkBox.getText()));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setValue(String.valueOf(checkBox.getText()), checkBox.isChecked());
            }
        });
    }

    public void itemss(final CheckBox checkBox) {
        checkBox.setChecked(getConfig((String) checkBox.getText()));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setValue(String.valueOf(checkBox.getText()), checkBox.isChecked());
            }
        });
    }

    public void memory(final Switch a, final int b) {
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                SettingMemory(b, a.isChecked());
            }
        });
    }

    void setupSeekBar(final SeekBar seekBar, final TextView textView, final int initialValue, final Runnable onChangeFunction) {
        seekBar.setProgress(initialValue);
        textView.setText(String.valueOf(initialValue));
        onChangeFunction.run();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(String.valueOf(progress));
                onChangeFunction.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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
			
    public void Exec(String path, String toast) {
        try {
            ExecuteElf("su -c chmod 777 " + getFilesDir() + path);
            ExecuteElf("su -c " + getFilesDir() + path);
            ExecuteElf("chmod 777 " + getFilesDir() + path);
            ExecuteElf(getFilesDir() + path);
        } catch (Exception e) {
        }
    }

    

    private void StopESP() {
        stopService(new Intent(this, Overlay.class));
    }

    private void visual(View visual) {

        final Switch iscrash = visual.findViewById(R.id.iscrash);
        final Switch isisland = visual.findViewById(R.id.isisland);
        final Switch drawesp = visual.findViewById(R.id.isenableesp);
        final LinearLayout menuisland = visual.findViewById(R.id.menuisland);
        final LinearLayout menucrash = visual.findViewById(R.id.menucrash);
        final ImageView imgisland = visual.findViewById(R.id.imgisland);

        
        if (drawesp != null) {
            drawesp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    setValue("isenableesp", isChecked);
                    if (isChecked) {
                        startService(new Intent(ctx, Overlay.class));
                    } else {
                        stopService(new Intent(ctx, Overlay.class));
                    }
                }
            });
        }

        // Show all functions regardless of premium status
        if (isisland != null) {
            isisland.setEnabled(true);
            isisland.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                runant("BB");
            });
        }


/*
        final SeekBar radarSizeSeekBar = visual.findViewById(R.id.strokeradar);
        final TextView radarSizeText = visual.findViewById(R.id.radartext);

        setupSeekBar(radarSizeSeekBar, radarSizeText, getradarSize(), new Runnable() {
            @Override
            public void run() {
                int pos = radarSizeSeekBar.getProgress();
                setradarSize(pos);
                RadarSize(pos);
                String a = String.valueOf(pos);
                radarSizeText.setText(a);
            }
        });
*/


        // Set default FPS to 90
        setFps(90);
        ESPView.ChangeFps(90);

        SharedPreferences sharedPreferences = visual.getContext().getSharedPreferences("espValue", Context.MODE_PRIVATE);
        String bypassmode = sharedPreferences.getString("bypassmode", "manual");

        if (Shell.rootAccess()) {

            visual.findViewById(R.id.menucrash).setVisibility(View.GONE);
        } else {
            visual.findViewById(R.id.menucrash).setVisibility(View.GONE);
        }


        final CheckBox isLine = visual.findViewById(R.id.isline);
        espvisual(isLine, 2);
        final CheckBox isbox = visual.findViewById(R.id.isBox);
        espvisual(isbox, 3);
        final CheckBox isskeleton = visual.findViewById(R.id.isskeleton);
        espvisual(isskeleton, 4);
        final CheckBox isdistance = visual.findViewById(R.id.isdistance);
        espvisual(isdistance, 5);
        final CheckBox ishealth = visual.findViewById(R.id.ishealth);
        espvisual(ishealth, 6);
        final CheckBox isname = visual.findViewById(R.id.isName);
        espvisual(isname, 7);
        final CheckBox ishead = visual.findViewById(R.id.ishead);
        espvisual(ishead, 8);
        final CheckBox isalert = visual.findViewById(R.id.isalert);
        espvisual(isalert, 9);
        final CheckBox isweapon = visual.findViewById(R.id.isweapon);
        espvisual(isweapon, 10);
        final CheckBox isthrowables = visual.findViewById(R.id.isthrowables);
        espvisual(isthrowables, 11);
        final CheckBox isnobot = visual.findViewById(R.id.isnobot);
        espvisual(isnobot, 15);
        final CheckBox isweaponicon = visual.findViewById(R.id.isweaponicon);
        espvisual(isweaponicon, 16);
        final CheckBox isAura = visual.findViewById(R.id.isaura);
        espvisual(isAura, 17); 
        final CheckBox isPlayerElectric = visual.findViewById(R.id.isPlayerElectric);
        espvisual(isPlayerElectric, 18); 
        final CheckBox islootbox = visual.findViewById(R.id.islootbox);
        espvisual(islootbox, 14);


    }

    private void items(View items) {
        View menui1 = items.findViewById(R.id.items1);
        View menui2 = items.findViewById(R.id.lyvehicle);
        View bottomi1 = items.findViewById(R.id.bottomi1);
        View bottomi2 = items.findViewById(R.id.bottomi2);

        final CheckBox lootbox = items.findViewById(R.id.lootbox);
        espvisual(lootbox, 14);

        final CheckBox Desert = items.findViewById(R.id.Desert);
        itemss(Desert);

        final CheckBox M416 = items.findViewById(R.id.m416);
        itemss(M416);

        final CheckBox QBZ = items.findViewById(R.id.QBZ);
        itemss(QBZ);

        final CheckBox SCARL = items.findViewById(R.id.SCARL);
        itemss(SCARL);

        final CheckBox AKM = items.findViewById(R.id.AKM);
        itemss(AKM);

        final CheckBox M16A4 = items.findViewById(R.id.M16A4);
        itemss(M16A4);

        final CheckBox AUG = items.findViewById(R.id.AUG);
        itemss(AUG);

        final CheckBox M249 = items.findViewById(R.id.M249);
        itemss(M249);

        final CheckBox Groza = items.findViewById(R.id.Groza);
        itemss(Groza);

        final CheckBox MK47 = items.findViewById(R.id.MK47);
        itemss(MK47);

        final CheckBox M762 = items.findViewById(R.id.M762);
        itemss(M762);

        final CheckBox G36C = items.findViewById(R.id.G36C);
        itemss(G36C);

        final CheckBox DP28 = items.findViewById(R.id.DP28);
        itemss(DP28);

        final CheckBox MG3 = items.findViewById(R.id.MG3);
        itemss(MG3);

        final CheckBox FAMAS = items.findViewById(R.id.FAMAS);
        itemss(FAMAS);


        final CheckBox HoneyBadger = items.findViewById(R.id.HoneyBadger);
        itemss(HoneyBadger);


        final CheckBox AC32 = items.findViewById(R.id.AC32);
        itemss(AC32);
        

        final CheckBox UMP = items.findViewById(R.id.UMP);
        itemss(UMP);

        final CheckBox bizon = items.findViewById(R.id.bizon);
        itemss(bizon);

        final CheckBox MP5K = items.findViewById(R.id.MP5K);
        itemss(MP5K);

        final CheckBox TommyGun = items.findViewById(R.id.TommyGun);
        itemss(TommyGun);

        final CheckBox vector = items.findViewById(R.id.vector);
        itemss(vector);

        final CheckBox P90 = items.findViewById(R.id.P90);
        itemss(P90);

        final CheckBox UZI = items.findViewById(R.id.UZI);
        itemss(UZI);


        //Snipers

        final CheckBox AWM = items.findViewById(R.id.AWM);
        itemss(AWM);

        final CheckBox QBU = items.findViewById(R.id.QBU);
        itemss(QBU);

        final CheckBox Kar98k = items.findViewById(R.id.Kar98k);
        itemss(Kar98k);

        final CheckBox M24 = items.findViewById(R.id.M24);
        itemss(M24);

        final CheckBox SLR = items.findViewById(R.id.SLR);
        itemss(SLR);

        final CheckBox SKS = items.findViewById(R.id.SKS);
        itemss(SKS);

        final CheckBox MK14 = items.findViewById(R.id.MK14);
        itemss(MK14);

        final CheckBox Mini14 = items.findViewById(R.id.Mini14);
        itemss(Mini14);

        final CheckBox Mosin = items.findViewById(R.id.Mosin);
        itemss(Mosin);

        final CheckBox VSS = items.findViewById(R.id.VSS);
        itemss(VSS);

        final CheckBox AMR = items.findViewById(R.id.AMR);
        itemss(AMR);

        final CheckBox Win94 = items.findViewById(R.id.Win94);
        itemss(Win94);

        final CheckBox MK12 = items.findViewById(R.id.MK12);
        itemss(MK12);

        //Scopes

        final CheckBox x2 = items.findViewById(R.id.x2);
        itemss(x2);

        final CheckBox x3 = items.findViewById(R.id.x3);
        itemss(x3);

        final CheckBox x4 = items.findViewById(R.id.x4);
        itemss(x4);

        final CheckBox x6 = items.findViewById(R.id.x6);
        itemss(x6);

        final CheckBox x8 = items.findViewById(R.id.x8);
        itemss(x8);

        final CheckBox canted = items.findViewById(R.id.canted);
        itemss(canted);

        final CheckBox hollow = items.findViewById(R.id.hollow);
        itemss(hollow);

        final CheckBox reddot = items.findViewById(R.id.reddot);
        itemss(reddot);

        //Armor

        final CheckBox bag1 = items.findViewById(R.id.bag1);
        itemss(bag1);

        final CheckBox bag2 = items.findViewById(R.id.bag2);
        itemss(bag2);

        final CheckBox bag3 = items.findViewById(R.id.bag3);
        itemss(bag3);

        final CheckBox helmet1 = items.findViewById(R.id.helmet1);
        itemss(helmet1);

        final CheckBox helmet2 = items.findViewById(R.id.helmet2);
        itemss(helmet2);

        final CheckBox helmet3 = items.findViewById(R.id.helmet3);
        itemss(helmet3);

        final CheckBox vest1 = items.findViewById(R.id.vest1);
        itemss(vest1);

        final CheckBox vest2 = items.findViewById(R.id.vest2);
        itemss(vest2);

        final CheckBox vest3 = items.findViewById(R.id.vest3);
        itemss(vest3);

        //Ammo
        final CheckBox a9 = items.findViewById(R.id.a9);
        itemss(a9);

        final CheckBox a7 = items.findViewById(R.id.a7);
        itemss(a7);

        final CheckBox a5 = items.findViewById(R.id.a5);
        itemss(a5);

        final CheckBox a300 = items.findViewById(R.id.a300);
        itemss(a300);

        final CheckBox a45 = items.findViewById(R.id.a45);
        itemss(a45);

        final CheckBox Arrow = items.findViewById(R.id.arrow);
        itemss(Arrow);

        final CheckBox BMG50 = items.findViewById(R.id.BMG50);
        itemss(BMG50);

        final CheckBox a12 = items.findViewById(R.id.a12);
        itemss(a12);

        //Shotgun
        final CheckBox DBS = items.findViewById(R.id.DBS);
        itemss(DBS);

        final CheckBox NS2000 = items.findViewById(R.id.NS2000);
        itemss(NS2000);

        final CheckBox S686 = items.findViewById(R.id.S686);
        itemss(S686);

        final CheckBox sawed = items.findViewById(R.id.sawed);
        itemss(sawed);

        final CheckBox M1014 = items.findViewById(R.id.M1014);
        itemss(M1014);

        final CheckBox S1897 = items.findViewById(R.id.S1897);
        itemss(S1897);

        final CheckBox S12K = items.findViewById(R.id.S12K);
        itemss(S12K);

        //Throwables
        final CheckBox grenade = items.findViewById(R.id.grenade);
        itemss(grenade);

        final CheckBox molotov = items.findViewById(R.id.molotov);
        itemss(molotov);

        final CheckBox stun = items.findViewById(R.id.stun);
        itemss(stun);

        final CheckBox smoke = items.findViewById(R.id.smoke);
        itemss(smoke);

        //Medics

        final CheckBox painkiller = items.findViewById(R.id.painkiller);
        itemss(painkiller);

        final CheckBox medkit = items.findViewById(R.id.medkit);
        itemss(medkit);

        final CheckBox firstaid = items.findViewById(R.id.firstaid);
        itemss(firstaid);

        final CheckBox bandage = items.findViewById(R.id.bandage);
        itemss(bandage);

        final CheckBox injection = items.findViewById(R.id.injection);
        itemss(injection);

        final CheckBox energydrink = items.findViewById(R.id.energydrink);
        itemss(energydrink);

        //Handy
        final CheckBox Pan = items.findViewById(R.id.Pan);
        itemss(Pan);

        final CheckBox Crowbar = items.findViewById(R.id.Crowbar);
        itemss(Crowbar);

        final CheckBox Sickle = items.findViewById(R.id.Sickle);
        itemss(Sickle);

        final CheckBox Machete = items.findViewById(R.id.Machete);
        itemss(Machete);

        final CheckBox Crossbow = items.findViewById(R.id.Crossbow);
        itemss(Crossbow);

        final CheckBox Explosive = items.findViewById(R.id.Explosive);
        itemss(Explosive);

        //Pistols
        final CheckBox P92 = items.findViewById(R.id.P92);
        itemss(P92);

        final CheckBox R45 = items.findViewById(R.id.R45);
        itemss(R45);

        final CheckBox P18C = items.findViewById(R.id.P18C);
        itemss(P18C);

        final CheckBox P1911 = items.findViewById(R.id.P1911);
        itemss(P1911);

        final CheckBox R1895 = items.findViewById(R.id.R1895);
        itemss(R1895);

        final CheckBox Scorpion = items.findViewById(R.id.Scorpion);
        itemss(Scorpion);

        //Other
        final CheckBox CheekPad = items.findViewById(R.id.CheekPad);
        itemss(CheekPad);

        final CheckBox Choke = items.findViewById(R.id.Choke);
        itemss(Choke);

        final CheckBox CompensatorSMG = items.findViewById(R.id.CompensatorSMG);
        itemss(CompensatorSMG);


        final CheckBox FlashHiderSMG = items.findViewById(R.id.FlashHiderSMG);
        itemss(FlashHiderSMG);


        final CheckBox FlashHiderAr = items.findViewById(R.id.FlashHiderAr);
        itemss(FlashHiderAr);

        final CheckBox ArCompensator = items.findViewById(R.id.ArCompensator);
        itemss(ArCompensator);

        final CheckBox TacticalStock = items.findViewById(R.id.TacticalStock);
        itemss(TacticalStock);

        final CheckBox Duckbill = items.findViewById(R.id.Duckbill);
        itemss(Duckbill);

        final CheckBox FlashHiderSniper = items.findViewById(R.id.FlashHiderSniper);
        itemss(FlashHiderSniper);

        final CheckBox SuppressorSMG = items.findViewById(R.id.SuppressorSMG);
        itemss(SuppressorSMG);

        final CheckBox HalfGrip = items.findViewById(R.id.HalfGrip);
        itemss(HalfGrip);

        final CheckBox StockMicroUZI = items.findViewById(R.id.StockMicroUZI);
        itemss(StockMicroUZI);

        final CheckBox SuppressorSniper = items.findViewById(R.id.SuppressorSniper);
        itemss(SuppressorSniper);

        final CheckBox SuppressorAr = items.findViewById(R.id.SuppressorAr);
        itemss(SuppressorAr);

        final CheckBox SniperCompensator = items.findViewById(R.id.SniperCompensator);
        itemss(SniperCompensator);

        final CheckBox ExQdSniper = items.findViewById(R.id.ExQdSniper);
        itemss(ExQdSniper);

        final CheckBox QdSMG = items.findViewById(R.id.QdSMG);
        itemss(QdSMG);

        final CheckBox ExSMG = items.findViewById(R.id.ExSMG);
        itemss(ExSMG);

        final CheckBox QdSniper = items.findViewById(R.id.QdSniper);
        itemss(QdSniper);

        final CheckBox ExSniper = items.findViewById(R.id.ExSniper);
        itemss(ExSniper);

        final CheckBox ExAr = items.findViewById(R.id.ExAr);
        itemss(ExAr);

        final CheckBox ExQdAr = items.findViewById(R.id.ExQdAr);
        itemss(ExQdAr);

        final CheckBox QdAr = items.findViewById(R.id.QdAr);
        itemss(QdAr);

        final CheckBox ExQdSMG = items.findViewById(R.id.ExQdSMG);
        itemss(ExQdSMG);

        final CheckBox QuiverCrossBow = items.findViewById(R.id.QuiverCrossBow);
        itemss(QuiverCrossBow);

        final CheckBox BulletLoop = items.findViewById(R.id.BulletLoop);
        itemss(BulletLoop);

        final CheckBox ThumbGrip = items.findViewById(R.id.ThumbGrip);
        itemss(ThumbGrip);

        final CheckBox LaserSight = items.findViewById(R.id.LaserSight);
        itemss(LaserSight);

        final CheckBox AngledGrip = items.findViewById(R.id.AngledGrip);
        itemss(AngledGrip);

        final CheckBox LightGrip = items.findViewById(R.id.LightGrip);
        itemss(LightGrip);

        final CheckBox VerticalGrip = items.findViewById(R.id.VerticalGrip);
        itemss(VerticalGrip);

        final CheckBox GasCan = items.findViewById(R.id.GasCan);
        itemss(GasCan);

        //Vehicle
        final CheckBox UTV = items.findViewById(R.id.UTV);
        vehicless(UTV);

        final CheckBox Buggy = items.findViewById(R.id.Buggy);
        vehicless(Buggy);

        final CheckBox UAZ = items.findViewById(R.id.UAZ);
        vehicless(UAZ);

        final CheckBox Trike = items.findViewById(R.id.Trike);
        vehicless(Trike);

        final CheckBox Bike = items.findViewById(R.id.Bike);
        vehicless(Bike);

        final CheckBox Dacia = items.findViewById(R.id.Dacia);
        vehicless(Dacia);

        final CheckBox Jet = items.findViewById(R.id.Jet);
        vehicless(Jet);

        final CheckBox Boat = items.findViewById(R.id.Boat);
        vehicless(Boat);

        final CheckBox Scooter = items.findViewById(R.id.Scooter);
        vehicless(Scooter);

        final CheckBox Bus = items.findViewById(R.id.Bus);
        vehicless(Bus);

        final CheckBox Mirado = items.findViewById(R.id.Mirado);
        vehicless(Mirado);

        final CheckBox Rony = items.findViewById(R.id.Rony);
        vehicless(Rony);

        final CheckBox Snowbike = items.findViewById(R.id.Snowbike);
        vehicless(Snowbike);

        final CheckBox Snowmobile = items.findViewById(R.id.Snowmobile);
        vehicless(Snowmobile);

        final CheckBox Tempo = items.findViewById(R.id.Tempo);
        vehicless(Tempo);

        final CheckBox Truck = items.findViewById(R.id.Truck);
        vehicless(Truck);

        final CheckBox MonsterTruck = items.findViewById(R.id.MonsterTruck);
        vehicless(MonsterTruck);

        final CheckBox BRDM = items.findViewById(R.id.BRDM);
        vehicless(BRDM);

        final CheckBox ATV = items.findViewById(R.id.ATV);
        vehicless(ATV);

        final CheckBox LadaNiva = items.findViewById(R.id.LadaNiva);
        vehicless(LadaNiva);

        final CheckBox Motorglider = items.findViewById(R.id.Motorglider);
        vehicless(Motorglider);

        final CheckBox CoupeRB = items.findViewById(R.id.CoupeRB);
        vehicless(CoupeRB);

        //Special
        final CheckBox Crate = items.findViewById(R.id.Crate);
        itemss(Crate);

        final CheckBox Airdrop = items.findViewById(R.id.Airdrop);
        itemss(Airdrop);

        final CheckBox DropPlane = items.findViewById(R.id.DropPlane);
        itemss(DropPlane);

        final CheckBox FlareGun = items.findViewById(R.id.FlareGun);
        itemss(FlareGun);

        final View checkall = items.findViewById(R.id.itemscheckall);
        final View noneall = items.findViewById(R.id.itemsblockall);
        final View checkallv = items.findViewById(R.id.mobilscheckall);
        final View noneallv = items.findViewById(R.id.mobilsblockall);

        checkallv.setOnClickListener(v -> {
            Buggy.setChecked(true);
            UAZ.setChecked(true);
            Trike.setChecked(true);
            Bike.setChecked(true);
            Dacia.setChecked(true);
            Jet.setChecked(true);
            Boat.setChecked(true);
            Scooter.setChecked(true);
            Bus.setChecked(true);
            Mirado.setChecked(true);
            Rony.setChecked(true);
            Snowbike.setChecked(true);
            Snowmobile.setChecked(true);
            Tempo.setChecked(true);
            Truck.setChecked(true);
            MonsterTruck.setChecked(true);
            BRDM.setChecked(true);
            LadaNiva.setChecked(true);
            ATV.setChecked(true);
            UTV.setChecked(true);
            CoupeRB.setChecked(true);
            Motorglider.setChecked(true);
        });

        noneallv.setOnClickListener(v -> {
            Buggy.setChecked(false);
            UAZ.setChecked(false);
            Trike.setChecked(false);
            Bike.setChecked(false);
            Dacia.setChecked(false);
            Jet.setChecked(false);
            Boat.setChecked(false);
            Scooter.setChecked(false);
            Bus.setChecked(false);
            Mirado.setChecked(false);
            Rony.setChecked(false);
            Snowbike.setChecked(false);
            Snowmobile.setChecked(false);
            Tempo.setChecked(false);
            Truck.setChecked(false);
            MonsterTruck.setChecked(false);
            BRDM.setChecked(false);
            LadaNiva.setChecked(false);
            ATV.setChecked(false);
            UTV.setChecked(false);
            CoupeRB.setChecked(false);
            Motorglider.setChecked(false);
        });

        checkall.setOnClickListener(v -> {

            /* Other */
            Crate.setChecked(true);
            Airdrop.setChecked(true);
            DropPlane.setChecked(true);
            CheekPad.setChecked(true);
            lootbox.setChecked(true);
            Choke.setChecked(true);


            /* Scope */
            canted.setChecked(true);
            reddot.setChecked(true);
            hollow.setChecked(true);
            x2.setChecked(true);
            x3.setChecked(true);
            x4.setChecked(true);
            x6.setChecked(true);
            x8.setChecked(true);

            /* Weapon */
            AWM.setChecked(true);
            QBU.setChecked(true);
            SLR.setChecked(true);
            SKS.setChecked(true);
            Mini14.setChecked(true);
            M24.setChecked(true);
            Kar98k.setChecked(true);
            VSS.setChecked(true);
            Win94.setChecked(true);
            AUG.setChecked(true);
            M762.setChecked(true);
            SCARL.setChecked(true);
            M416.setChecked(true);
            M16A4.setChecked(true);
            MK47.setChecked(true);
            G36C.setChecked(true);
            QBZ.setChecked(true);
            AKM.setChecked(true);
            Groza.setChecked(true);
            S12K.setChecked(true);
            DBS.setChecked(true);
            S686.setChecked(true);
            S1897.setChecked(true);
            sawed.setChecked(true);
            TommyGun.setChecked(true);
            MP5K.setChecked(true);
            vector.setChecked(true);
            UZI.setChecked(true);
            R1895.setChecked(true);
            Explosive.setChecked(true);
            P92.setChecked(true);
            P18C.setChecked(true);
            R45.setChecked(true);
            P1911.setChecked(true);
            Desert.setChecked(true);
            Sickle.setChecked(true);
            Machete.setChecked(true);
            Pan.setChecked(true);
            MK14.setChecked(true);
            Scorpion.setChecked(true);

            Mosin.setChecked(true);
            MK12.setChecked(true);
            AMR.setChecked(true);

            M1014.setChecked(true);
            NS2000.setChecked(true);
            P90.setChecked(true);
            MG3.setChecked(true);
            AC32.setChecked(true);
            HoneyBadger.setChecked(true);
            FAMAS.setChecked(true);

            /* Ammo */
            a45.setChecked(true);
            a9.setChecked(true);
            a7.setChecked(true);
            a300.setChecked(true);
            a5.setChecked(true);
            BMG50.setChecked(true);
            a12.setChecked(true);

            SniperCompensator.setChecked(true);
            DP28.setChecked(true);
            M249.setChecked(true);
            grenade.setChecked(true);
            smoke.setChecked(true);
            molotov.setChecked(true);
            painkiller.setChecked(true);
            injection.setChecked(true);
            energydrink.setChecked(true);
            firstaid.setChecked(true);
            bandage.setChecked(true);
            medkit.setChecked(true);
            FlareGun.setChecked(true);
            UMP.setChecked(true);
            bizon.setChecked(true);
            CompensatorSMG.setChecked(true);
            FlashHiderSMG.setChecked(true);
            FlashHiderAr.setChecked(true);
            ArCompensator.setChecked(true);
            TacticalStock.setChecked(true);
            Duckbill.setChecked(true);
            FlashHiderSniper.setChecked(true);
            SuppressorSMG.setChecked(true);
            HalfGrip.setChecked(true);
            StockMicroUZI.setChecked(true);
            SuppressorSniper.setChecked(true);
            SuppressorAr.setChecked(true);
            ExQdSniper.setChecked(true);
            QdSMG.setChecked(true);
            ExSMG.setChecked(true);
            QdSniper.setChecked(true);
            ExSniper.setChecked(true);
            ExAr.setChecked(true);
            ExQdAr.setChecked(true);
            QdAr.setChecked(true);
            ExQdSMG.setChecked(true);
            QuiverCrossBow.setChecked(true);
            BulletLoop.setChecked(true);
            ThumbGrip.setChecked(true);
            LaserSight.setChecked(true);
            AngledGrip.setChecked(true);
            LightGrip.setChecked(true);
            VerticalGrip.setChecked(true);
            GasCan.setChecked(true);
            Arrow.setChecked(true);
            Crossbow.setChecked(true);
            bag1.setChecked(true);
            bag2.setChecked(true);
            bag3.setChecked(true);
            helmet1.setChecked(true);
            helmet2.setChecked(true);
            helmet3.setChecked(true);
            vest1.setChecked(true);
            vest2.setChecked(true);
            vest3.setChecked(true);
            stun.setChecked(true);
            Crowbar.setChecked(true);
        });

        noneall.setOnClickListener(v -> {
            /* Other */
            Crate.setChecked(false);
            Airdrop.setChecked(false);
            DropPlane.setChecked(false);
            CheekPad.setChecked(false);
            lootbox.setChecked(false);
            Choke.setChecked(false);


            /* Scope */
            canted.setChecked(false);
            reddot.setChecked(false);
            hollow.setChecked(false);
            x2.setChecked(false);
            x3.setChecked(false);
            x4.setChecked(false);
            x6.setChecked(false);
            x8.setChecked(false);

            /* Weapon */
            AWM.setChecked(false);
            QBU.setChecked(false);
            SLR.setChecked(false);
            SKS.setChecked(false);
            Mini14.setChecked(false);
            M24.setChecked(false);
            Kar98k.setChecked(false);
            VSS.setChecked(false);
            Win94.setChecked(false);
            AUG.setChecked(false);
            M762.setChecked(false);
            SCARL.setChecked(false);
            M416.setChecked(false);
            M16A4.setChecked(false);
            MK47.setChecked(false);
            G36C.setChecked(false);
            QBZ.setChecked(false);
            AKM.setChecked(false);
            Groza.setChecked(false);
            S12K.setChecked(false);
            DBS.setChecked(false);
            S686.setChecked(false);
            S1897.setChecked(false);
            sawed.setChecked(false);
            TommyGun.setChecked(false);
            MP5K.setChecked(false);
            vector.setChecked(false);
            UZI.setChecked(false);
            R1895.setChecked(false);
            Explosive.setChecked(false);
            P92.setChecked(false);
            P18C.setChecked(false);
            R45.setChecked(false);
            P1911.setChecked(false);
            Desert.setChecked(false);
            Sickle.setChecked(false);
            Machete.setChecked(false);
            Pan.setChecked(false);
            MK14.setChecked(false);
            Scorpion.setChecked(false);

            Mosin.setChecked(false);
            MK12.setChecked(false);
            AMR.setChecked(false);

            M1014.setChecked(false);
            NS2000.setChecked(false);
            P90.setChecked(false);
            MG3.setChecked(false);
            AC32.setChecked(false);
            HoneyBadger.setChecked(false);
            FAMAS.setChecked(false);

            /* Ammo */
            a45.setChecked(false);
            a9.setChecked(false);
            a7.setChecked(false);
            a300.setChecked(false);
            a5.setChecked(false);
            BMG50.setChecked(false);
            a12.setChecked(false);

            SniperCompensator.setChecked(false);
            DP28.setChecked(false);
            M249.setChecked(false);
            grenade.setChecked(false);
            smoke.setChecked(false);
            molotov.setChecked(false);
            painkiller.setChecked(false);
            injection.setChecked(false);
            energydrink.setChecked(false);
            firstaid.setChecked(false);
            bandage.setChecked(false);
            medkit.setChecked(false);
            FlareGun.setChecked(false);
            UMP.setChecked(false);
            bizon.setChecked(false);
            CompensatorSMG.setChecked(false);
            FlashHiderSMG.setChecked(false);
            FlashHiderAr.setChecked(false);
            ArCompensator.setChecked(false);
            TacticalStock.setChecked(false);
            Duckbill.setChecked(false);
            FlashHiderSniper.setChecked(false);
            SuppressorSMG.setChecked(false);
            HalfGrip.setChecked(false);
            StockMicroUZI.setChecked(false);
            SuppressorSniper.setChecked(false);
            SuppressorAr.setChecked(false);
            ExQdSniper.setChecked(false);
            QdSMG.setChecked(false);
            ExSMG.setChecked(false);
            QdSniper.setChecked(false);
            ExSniper.setChecked(false);
            ExAr.setChecked(false);
            ExQdAr.setChecked(false);
            QdAr.setChecked(false);
            ExQdSMG.setChecked(false);
            QuiverCrossBow.setChecked(false);
            BulletLoop.setChecked(false);
            ThumbGrip.setChecked(false);
            LaserSight.setChecked(false);
            AngledGrip.setChecked(false);
            LightGrip.setChecked(false);
            VerticalGrip.setChecked(false);
            GasCan.setChecked(false);
            Arrow.setChecked(false);
            Crossbow.setChecked(false);
            bag1.setChecked(false);
            bag2.setChecked(false);
            bag3.setChecked(false);
            helmet1.setChecked(false);
            helmet2.setChecked(false);
            helmet3.setChecked(false);
            vest1.setChecked(false);
            vest2.setChecked(false);
            vest3.setChecked(false);
            stun.setChecked(false);
            Crowbar.setChecked(false);
        });
    }

    private void aimbot(View aimbot) {
        SharedPreferences sp = this.getSharedPreferences("espValue", Context.MODE_PRIVATE);
        TextView aimpre = aimbot.findViewById(R.id.aimpre);
        LinearLayout aimsec = aimbot.findViewById(R.id.aimsec);

        // ALWAYS show all functions
        if (aimpre != null) aimpre.setVisibility(View.GONE);
        if (aimsec != null) aimsec.setVisibility(View.VISIBLE);


        Switch aimSwitch = aimbot.findViewById(R.id.aim_switch);
        aimSwitch.setText(R.string.aim_bot_3_0);
        
        // Initialize from preferences
        boolean aimStarted = getConfig("aimbot_master");
        aimSwitch.setChecked(aimStarted);
        
        aimSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setValue("aimbot_master", isChecked);
                if (isChecked) {
                    StartAimFloat();
                    StopAimBulletFloat();
                    StopAimTouch();
                } else {
                    StopAimBulletFloat();
                    StopAimFloat();
                    StopAimTouch();
                }
            }
        });


        final CompoundButton aimKnocked = aimbot.findViewById(R.id.aimknocked);
        setaim(aimKnocked, 3);

        final CompoundButton aimignore = aimbot.findViewById(R.id.aimignorebot);
        setaim(aimignore, 4);





        final SeekBar rangeSeekBar = aimbot.findViewById(R.id.range);
        final TextView rangeText = aimbot.findViewById(R.id.rangetext);
        setupSeekBar(rangeSeekBar, rangeText, getrangeAim(), new Runnable() {
            @Override
            public void run() {
                Range(rangeSeekBar.getProgress());
                getrangeAim(rangeSeekBar.getProgress());
            }
        });

        final SeekBar distancesSeekBar = aimbot.findViewById(R.id.distances);
        final TextView distancesText = aimbot.findViewById(R.id.distancetext);
        setupSeekBar(distancesSeekBar, distancesText, getDistances(), new Runnable() {
            @Override
            public void run() {
                distances(distancesSeekBar.getProgress());
                setDistances(distancesSeekBar.getProgress());
            }
        });


        final SeekBar recoilSeekBar2 = aimbot.findViewById(R.id.Recoil2);
        final TextView recoilText2 = aimbot.findViewById(R.id.recoiltext2);
        setupSeekBar(recoilSeekBar2, recoilText2, getrecoilAim(), new Runnable() {
            @Override
            public void run() {
                recoil(recoilSeekBar2.getProgress());
                getrecoilAim(recoilSeekBar2.getProgress());
            }
        });

        final SeekBar recoilSeekBar = aimbot.findViewById(R.id.Recoil);
        final TextView recoilText = aimbot.findViewById(R.id.recoiltext);
        setupSeekBar(recoilSeekBar, recoilText, getrecoilAim(), new Runnable() {
            @Override
            public void run() {
                recoil2(recoilSeekBar.getProgress());
                getrecoilAim2(recoilSeekBar.getProgress());
            }
        });

        final SeekBar recoilSeekBars2 = aimbot.findViewById(R.id.Recoils2);
        final TextView recoilTexts2 = aimbot.findViewById(R.id.recoiltexts2);
        setupSeekBar(recoilSeekBars2, recoilTexts2, getrecoilAim(), new Runnable() {
            @Override
            public void run() {
                recoil3(recoilSeekBars2.getProgress());
                getrecoilAim3(recoilSeekBars2.getProgress());
            }
        });

        final SeekBar bulletSpeedSeekBar = aimbot.findViewById(R.id.bulletspeed);
        final TextView bulletSpeedText = aimbot.findViewById(R.id.bulletspeedtext);
        setupSeekBar(bulletSpeedSeekBar, bulletSpeedText, getbulletspeedAim(), new Runnable() {
            @Override
            public void run() {
                Bulletspeed(bulletSpeedSeekBar.getProgress());
                getbulletspeedAim(bulletSpeedSeekBar.getProgress());
            }
        });












        final RadioGroup aimby = aimbot.findViewById(R.id.aimby);
        // Initialize aimby
        int savedAimBy = sp.getInt("aimby_val", 0);
        for (int i = 0; i < aimby.getChildCount(); i++) {
            RadioButton rb = (RadioButton) aimby.getChildAt(i);
            if (Integer.parseInt(rb.getTag().toString()) == savedAimBy) {
                rb.setChecked(true);
                break;
            }
        }
        aimby.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimby.getCheckedRadioButtonId();
                if (chkdId != -1) {
                    RadioButton btn = aimbot.findViewById(chkdId);
                    int val = Integer.parseInt(btn.getTag().toString());
                    AimBy(val);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putInt("aimby_val", val);
                    ed.apply();
                }
            }
        });

        final RadioGroup aimwhen = aimbot.findViewById(R.id.aimwhen);
        // Initialize aimwhen
        int savedAimWhen = sp.getInt("aimwhen_val", 0);
        for (int i = 0; i < aimwhen.getChildCount(); i++) {
            RadioButton rb = (RadioButton) aimwhen.getChildAt(i);
            if (Integer.parseInt(rb.getTag().toString()) == savedAimWhen) {
                rb.setChecked(true);
                break;
            }
        }
        aimwhen.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimwhen.getCheckedRadioButtonId();
                if (chkdId != -1) {
                    RadioButton btn = aimbot.findViewById(chkdId);
                    int val = Integer.parseInt(btn.getTag().toString());
                    AimWhen(val);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putInt("aimwhen_val", val);
                    ed.apply();
                }
            }
        });

        final RadioGroup aimbotmode = aimbot.findViewById(R.id.aimbotmode);
        // Initialize aimbotmode
        int savedTarget = sp.getInt("target_val", 0);
        for (int i = 0; i < aimbotmode.getChildCount(); i++) {
            RadioButton rb = (RadioButton) aimbotmode.getChildAt(i);
            if (Integer.parseInt(rb.getTag().toString()) == savedTarget) {
                rb.setChecked(true);
                break;
            }
        }
        aimbotmode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimbotmode.getCheckedRadioButtonId();
                if (chkdId != -1) {
                    RadioButton btn = aimbot.findViewById(chkdId);
                    int val = Integer.parseInt(btn.getTag().toString());
                    Target(val);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putInt("target_val", val);
                    ed.apply();
                }
            }
        });
    }

    private void memory(View memory) {
       // final Switch less = memory.findViewById(R.id.isreducerecoil);
    //    memory(less, 1);
        final Switch Cross = memory.findViewById(R.id.issmallcross);
        memory(Cross, 2);
   //     final Switch amms = memory.findViewById(R.id.isaimlock);
   //     memory(amms, 3);
    //    final Switch ismagic = memory.findViewById(R.id.ismagichead);
    //    final Switch ishitx = memory.findViewById(R.id.ishitx);
        final SeekBar wideviewSeekBar = memory.findViewById(R.id.rangewide);
        final TextView wideviewText = memory.findViewById(R.id.rangetextwide);
      //  final TextView aimpresdk = memory.findViewById(R.id.aimpresdk);
     //   LinearLayout memsec = memory.findViewById(R.id.memsec);

        // Show all functions
        typelogin = "PREMIUM";
        Cross.setEnabled(true);
        wideviewSeekBar.setEnabled(true);
        Cross.setAlpha(1.0f);
        wideviewSeekBar.setAlpha(1.0f);


/*
        ismagic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                   
                } else {
                }
            }
        });


        ishitx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                } else {
                }
            }
        });
*/

        setupSeekBar(wideviewSeekBar, wideviewText, getwideview(), new Runnable() {
            @Override
            public void run() {
                WideView(wideviewSeekBar.getProgress());
                getwideview(wideviewSeekBar.getProgress());
            }
        });
    }

    public static void enableESP(Context context) {
        if (context != null) {
            Intent intent = new Intent(context, FloatService.class);
            intent.setAction("ENABLE_ESP");
            context.startService(intent);
        }
    }

}
