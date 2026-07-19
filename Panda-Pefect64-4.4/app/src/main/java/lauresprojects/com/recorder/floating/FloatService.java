package lauresprojects.com.recorder.floating;

import android.graphics.Color;
import static lauresprojects.com.recorder.activity.LoginActivity.USERKEY;
import static lauresprojects.com.recorder.activity.MainActivity.Ischeck;
import static lauresprojects.com.recorder.activity.MainActivity.bitversi;
import static lauresprojects.com.recorder.activity.MainActivity.gameint;
import static lauresprojects.com.recorder.activity.MainActivity.game;
import static lauresprojects.com.recorder.activity.MainActivity.modestatus;
//import static lauresprojects.com.recorder.floating.FloatRei.toastImage;
//import com.sdsmdg.tastytoast.TastyToast;
import android.widget.Toast;

import android.app.Activity;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import lauresprojects.com.recorder.R;
import lauresprojects.com.recorder.activity.MainActivity;
import lauresprojects.com.recorder.utils.FLog;
import lauresprojects.com.recorder.utils.myTools;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.topjohnwu.superuser.Shell;
//import com.topjohnwu.superuser.Shell;

import static lauresprojects.com.recorder.activity.LoginActivity.Sufii;
import android.view.View;
import android.view.View.OnClickListener;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.Handler;
import android.os.Looper;


import java.io.File;

import java.io.IOException;

import nl.joery.animatedbottombar.AnimatedBottomBar;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate

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
    public native void setLineColor(int a,int r, int g, int b);
    

    
    private Switch aimKnocked;
    
    private Switch aimignore;

    private Switch drawesp;
    private RadioGroup aimbotmode;
    
    private RadioGroup aimby;

    private TextView islootitems;
    
    

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

    private void StartFightModeFloat() {
        startService(new Intent(getApplicationContext(), FightMode.class));
    }

    private void StopFightModeFloat() {
        stopService(new Intent(getApplicationContext(), FightMode.class));
    }

    public native void SettingValue(int setting_code, boolean value);

    public native void SettingMemory(int setting_code, boolean value);

    public native void SettingAim(int setting_code, boolean value);

    public native void SkinHack(int setting_code);

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
    private AnimatedBottomBar bottomBar;
    private LinearLayout mainContainer;
    private LayoutInflater inflater;
    static myTools m;

    ImageView[] tabs = new ImageView[17];
    LinearLayout[] sections = new LinearLayout[17];

    int[] tabIds = {
            R.id.tab1, R.id.tab2, R.id.tab3, R.id.tab4, R.id.tab5, R.id.tab6,
            R.id.tab7, R.id.tab8, R.id.tab9, R.id.tab10, R.id.tab11, R.id.tab12,
            R.id.tab13, R.id.tab14, R.id.tab15, R.id.tab16, R.id.tab17
    };

    int[] sectionIds = {
            R.id.specialitems, R.id.scope, R.id.arweapons, R.id.LMGweapons, R.id.SMGweapons, R.id.SRweapons,
            R.id.ShotGuns, R.id.Pistols, R.id.Milliweapons, R.id.Throwables, R.id.Ammo, R.id.HelmetBag,
            R.id.consumables, R.id.attachments, R.id.magazines, R.id.grips, R.id.others
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

        @Override
    public void onCreate() {
        super.onCreate();
        
        m = new myTools(this);
        setTheme(m.geInt("myTheme","myTheme",R.style.AppTheme));
        ctx = getApplicationContext();
        InitShowMainView();
        loadbahasa();

        aimKnocked.setChecked(true);
        aimignore.setChecked(true);
        aimby.check(R.id.radioButton2);
        aimby.check(R.id.radioButton9);
        
    }



    private void InitShowMainView() {
        mainView = LayoutInflater.from(this).inflate(R.layout.float_service, null);
        paramsMainView = getparams();
        windowManagerMainView = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManagerMainView.addView(mainView, paramsMainView);
        layout_icon_control_view = mainView.findViewById(R.id.layout_icon_control_view);
        layout_main_view = mainView.findViewById(R.id.layout_main_view);

        ImageView layout_close_main_view = mainView.findViewById(R.id.close_btn);
        layout_close_main_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                layout_main_view.setVisibility(View.GONE);
                layout_icon_control_view.setVisibility(View.VISIBLE);
            }
        });

        LinearLayout layout_view = mainView.findViewById(R.id.layout_view);
        layout_view.setOnTouchListener(onTouchListener());

        initDesign();
        visual(mainView);
        aimbot(mainView);
        items(mainView);
        memory(mainView);
        // car(mainView);
        
        


    }
    
    

    public void initDesign() {
        LinearLayout menu1 = mainView.findViewById(R.id.menuf1);
        LinearLayout menu2 = mainView.findViewById(R.id.menuf2);
        LinearLayout menu3 = mainView.findViewById(R.id.menuf3);
        LinearLayout menu4 = mainView.findViewById(R.id.menuf4);
        LinearLayout menu5 = mainView.findViewById(R.id.menuf5);

        LinearLayout navi1 = mainView.findViewById(R.id.navitems);
        LinearLayout navi2 = mainView.findViewById(R.id.navvehicle);
        LinearLayout menui1 = mainView.findViewById(R.id.items1);
        LinearLayout menui2 = mainView.findViewById(R.id.lyvehicle);
        LinearLayout lefttab = mainView.findViewById(R.id.lefttab);
        View bottomi1 = mainView.findViewById(R.id.bottomi1);
        View bottomi2 = mainView.findViewById(R.id.bottomi2);
        for (int i = 0; i < 17; i++) {
            final int index = i;
            tabs[i] = mainView.findViewById(tabIds[i]);
            sections[i] = mainView.findViewById(sectionIds[i]);

            tabs[i].setOnClickListener(v -> {
                // Deselect all tabs
                for (ImageView tab : tabs) tab.setSelected(false);
                // Hide all sections
                for (LinearLayout section : sections) section.setVisibility(View.GONE);

                // Select clicked tab and show corresponding section
                tabs[index].setSelected(true);
                sections[index].setVisibility(View.VISIBLE);
            });
        }

        // Set default selected tab (tab1)
        tabs[0].setSelected(true);
        sections[0].setVisibility(View.VISIBLE);

        AnimatedBottomBar bottomBar = mainView.findViewById(R.id.esp_tabbar); // Make sure this is correct ID

        bottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, @Nullable AnimatedBottomBar.Tab lastTab, int newIndex, @NonNull AnimatedBottomBar.Tab newTab) {
                // Sab menu GONE karo
                menu1.setVisibility(View.GONE);
                menu2.setVisibility(View.GONE);
                menu3.setVisibility(View.GONE);
                menu4.setVisibility(View.GONE);
                menu5.setVisibility(View.GONE);

                // By default hide item sub-layouts too
                menui1.setVisibility(View.GONE);
                menui2.setVisibility(View.GONE);
                bottomi1.setVisibility(View.GONE);
                bottomi2.setVisibility(View.GONE);

                switch (newTab.getId()) {
                    case R.id.tab_player:
                        menu1.setVisibility(View.VISIBLE);
                        break;
                    case R.id.tab_items:
                        menu2.setVisibility(View.VISIBLE);
                        menui1.setVisibility(View.VISIBLE);
                        bottomi1.setVisibility(View.VISIBLE);
                        lefttab.setVisibility(View.VISIBLE);
                        break;
                    case R.id.tab_vehicles:
                        menu2.setVisibility(View.VISIBLE);
                        menui2.setVisibility(View.VISIBLE);
                        bottomi2.setVisibility(View.VISIBLE);
                        lefttab.setVisibility(View.GONE);
                        break;
                    case R.id.tab_aimbot:
                        menu3.setVisibility(View.VISIBLE);
                        break;
                    case R.id.tab_injector:
                        menu4.setVisibility(View.VISIBLE);
                        break;
                    case R.id.tab_esp_settings:
                        menu5.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabReselected(int index, @NonNull AnimatedBottomBar.Tab tab) {
                // Optional
            }
        });



        // Inner buttons/items logic (Items tab ke andar nav switch)
        navi1.setOnClickListener(v -> {
            menui1.setVisibility(View.VISIBLE);
            menui2.setVisibility(View.GONE);
            bottomi1.setVisibility(View.VISIBLE);
            bottomi2.setVisibility(View.GONE);
        });

        navi2.setOnClickListener(v -> {
            menui1.setVisibility(View.GONE);
            menui2.setVisibility(View.VISIBLE);
            bottomi1.setVisibility(View.GONE);
            bottomi2.setVisibility(View.VISIBLE);
        });
    }



/*
    private void AdapterNavigation() {
        Dapter vpadapter = new Dapter(ctx);
        ViewPager viewPager = mainView.findViewById(R.id.viewPager);
        viewPager.setAdapter(vpadapter);
        viewPager.setOffscreenPageLimit(4);

        View bottom1 = mainView.findViewById(R.id.bottom1);
        View bottom2 = mainView.findViewById(R.id.bottom2);
        View bottom3 = mainView.findViewById(R.id.bottom3);
        View bottom4 = mainView.findViewById(R.id.bottom4);
        LinearLayout visual = mainView.findViewById(R.id.navf1);
        LinearLayout items = mainView.findViewById(R.id.navf2);
        LinearLayout aimbot = mainView.findViewById(R.id.navf3);
        LinearLayout memory = mainView.findViewById(R.id.navf4);

        View.OnClickListener oc = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int aa = v.getId();
                viewPager.setCurrentItem(aa == R.id.navf1 ? 0 : aa == R.id.navf2 ? 1 : aa == R.id.navf3 ? 2 : 3);

            }
        };

        visual.setOnClickListener(oc);
        items.setOnClickListener(oc);
        aimbot.setOnClickListener(oc);
        memory.setOnClickListener(oc);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottom1.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                bottom2.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                bottom3.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
                bottom4.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }
*/


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
        params.preferredRefreshRate = 120f;

        return params;
    }
    
    private int getFlagsType() {
        int LAYOUT_FLAG = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        return LAYOUT_FLAG;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mainView != null) {
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

    public void espvisual(final TextView a, final int b) {
        // Custom toggle flag
        boolean currentState = getConfig((String) a.getText());
        a.setSelected(currentState); // you can use selected state for visuals
        SettingValue(b, currentState);

        // Optional: update UI to reflect current state
        updateTextViewVisual(a, currentState);

        a.setOnClickListener(new View.OnClickListener() {
            boolean isChecked = currentState;

            @Override
            public void onClick(View v) {
                isChecked = !isChecked;
                a.setSelected(isChecked);
                updateTextViewVisual(a, isChecked); // update UI if needed
                setValue(String.valueOf(a.getText()), isChecked);
                SettingValue(b, isChecked);
                //TastyToast.makeText(FloatService.this, "Installing GSM...", TastyToast.LENGTH_LONG, TastyToast.DEFAULT);
            }
        });
    }

    // Optional helper to change visuals (like background/text color)
   private void updateTextViewVisual(TextView view, boolean checked) {
        if (checked) {
            view.setBackgroundResource(R.drawable.float_selected); // your drawable
           // view.setTextColor(Color.GREEN);  // or any color
        } else {
            view.setBackgroundResource(R.drawable.float_unselect);
            //view.setTextColor(Color.RED);
        }
    }



    public void setaim(final Switch a, final int b) {
        a.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                setValue(String.valueOf(a.getText()), a.isChecked());
                SettingAim(b, a.isChecked());
            }
        });
    }

    public void vehicless(final TextView textView) {
        // Set initial state based on config
        boolean isEnabled = getConfig((String) textView.getText());
        textView.setSelected(isEnabled);
        updateToggleBackground(textView, isEnabled);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !textView.isSelected();
                textView.setSelected(newState);
                updateToggleBackground(textView, newState);
                setValue(String.valueOf(textView.getText()), newState);
            }
        });
    }

    private void updateToggleBackground(TextView textView, boolean selected) {
        if (selected) {
            textView.setBackgroundResource(R.drawable.float_selected);   // ON state
        } else {
            textView.setBackgroundResource(R.drawable.float_unselect); // OFF state
        }
    }



    public void itemss(final TextView checkBox) {
        // Set initial selected state based on config
        boolean selected = getConfig(checkBox.getText().toString());
        checkBox.setSelected(selected);

        // Update background or text color based on selection (optional)
        updateTextViewStyle(checkBox, selected);

        // Set click listener to toggle selection
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isSelected = !checkBox.isSelected();
                checkBox.setSelected(isSelected);
                setValue(checkBox.getText().toString(), isSelected);
                updateTextViewStyle(checkBox, isSelected);
            }
        });
    }

    private void updateTextViewStyle(TextView textView, boolean selected) {
        if (selected) {
            textView.setBackgroundResource(R.drawable.float_selected); // your drawable
            // view.setTextColor(Color.GREEN);  // or any color
        } else {
            textView.setBackgroundResource(R.drawable.float_unselect);
            //view.setTextColor(Color.RED);
        }
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

    private void DrawESP() {

    

    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
        if (Shell.rootAccess()) {
            //MainActivity.socket = "su -c " + MainActivity.daemonPath;
            startService(new Intent(this, Overlay.class));
        } else {
          //  MainActivity.socket = MainActivity.daemonPath;
            startService(new Intent(MainActivity.get(), Overlay.class));
        }
    }, 2000);
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
    
    
    private void StopESP() {
        stopService(new Intent(this, Overlay.class));
    }
    
    
    private void anomake() {

    File temp = new File(getFilesDir(), "ano_tmp");

    // delete if exists (file or folder)
    if (temp.exists()) {
        deleteRecursive(temp);
    }

    // recreate as folder
    boolean created = temp.mkdirs();

    if (!created) {
        //Log.e("anomake", "Failed to create ano_tmp folder");
    }
}

private void deleteRecursive(File fileOrDir) {
    if (fileOrDir.isDirectory()) {
        File[] files = fileOrDir.listFiles();
        if (files != null) {
            for (File child : files) {
                deleteRecursive(child);
            }
        }
    }
    fileOrDir.delete();
}




    public void Skin(View view) {
    // SKIN CHECKBOXES
    final CheckBox pharaoh = view.findViewById(R.id.pharaohskin);
    final CheckBox bloodreven = view.findViewById(R.id.bloodreven);
    final CheckBox posreidon = view.findViewById(R.id.posreidon);
    final CheckBox avalache = view.findViewById(R.id.avalache);
    final CheckBox silvanus = view.findViewById(R.id.silvanus);
    final CheckBox iridescense = view.findViewById(R.id.iridescense);
    final CheckBox aracane = view.findViewById(R.id.aracane);

    skinvisual(pharaoh, 1);
    skinvisual(bloodreven, 2);
    skinvisual(posreidon, 3);
    skinvisual(avalache, 4);
    skinvisual(silvanus, 5);
    skinvisual(iridescense, 6);
    skinvisual(aracane, 7);

    // COLOR PICKER VIEWS
    final LinearLayout btnLinePicker = view.findViewById(R.id.colorLinePicker);
    final RelativeLayout linepick = view.findViewById(R.id.lay_line_pick);
    final ColorPickerView linecolorpick = view.findViewById(R.id.color_line_picker);

    // Set initial color
    int savedColor = getEspValue("clrLine", Color.RED);
    setLineColor(Color.alpha(savedColor), Color.red(savedColor), Color.green(savedColor), Color.blue(savedColor));
    linecolorpick.setColor(savedColor, true);

    // Update color on change
    linecolorpick.addOnColorChangedListener(new OnColorChangedListener() {
        @Override
        public void onColorChanged(int newColor) {
            setEspValue("clrLine", newColor);
            setLineColor(Color.alpha(newColor), Color.red(newColor), Color.green(newColor), Color.blue(newColor));
        }
    });

    // Toggle visibility
    btnLinePicker.setOnClickListener(new View.OnClickListener() {
    boolean isVisible = false;

    @Override
    public void onClick(View v) {
        Toast.makeText(v.getContext(), "Clicked!", Toast.LENGTH_LONG).show();
        isVisible = !isVisible;
        linepick.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
});
}

    private void visual(View visual) {
        //final TextView protectiontext = visual.findViewById(R.id.protectiontext);
        //final Switch iscrash = visual.findViewById(R.id.iscrash);
       final Switch isisland = visual.findViewById(R.id.isisland);
        //final Switch isfixed = visual.findViewById(R.id.isfixed);
        drawesp  = visual.findViewById(R.id.isenableesp);
        final LinearLayout menuisland = visual.findViewById(R.id.menuisland);
        //final LinearLayout menuloho = visual.findViewById(R.id.menuloho);
        //final LinearLayout menucrash = visual.findViewById(R.id.menucrash);
        //final ImageView imgfixed = visual.findViewById(R.id.imgfixed);
        final ImageView imgisland = visual.findViewById(R.id.imgisland);
        
        


        drawesp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    @Override
    public void onCheckedChanged(CompoundButton p1, boolean isChecked) {

        if (isChecked) {

            drawesp.setText("Please wait...");

            anomake();

            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> {

                        DrawESP();
                        drawesp.setText("Activated");

                    }, 3000);

        } else {

            StopESP();
            drawesp.setText("Stopped");
        }
    }
});
        //  NONROOT BYPSS

        if (!Sufii) {
            //imgfixed.setBackgroundResource(R.drawable.baseline_lock_24);
          // menuloho.setAlpha(0.6f);
           // isfixed.setEnabled(false);
           // typelogin = "FREE";
        } else {
          //  typelogin = "BLAZEHAX";
           /* isfixed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (bitversi == 64) {
                            if (gameint == 5) {     // BGMI NONROOT BYPSS
                                Exec("/TW " + game + " 005");
                                Exec("/SUFI 200 " + game);

                            } else {                // GLOBAL NONROOT BYPSS
                                Exec("/TW " + game + " 002");
                                Exec("/fix " + game);
                            }
                        } else if (bitversi == 32) {
                            if (gameint == 5) {
                                //  Exec("/TW " + game + " 006", getString(R.string.BYPASS_32_ENABLE));
                            } else {
                                //  Exec("/TW " + game + " 33", getString(R.string.BYPASS_32_ENABLE));
                            }
                        }
                    }
                }
            });

            iscrash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (bitversi == 64) {
                            if (gameint == 5) {     // BGMI NONROOT BYPSS
                                Exec("/TW " + game + " 555");
                                //Exec("/SUFI 200 " + game + "");

                            } else {                // GLOBAL NONROOT BYPSS
                                Exec("/TW " + game + " 000");
                                Exec("/sufi 200" + game);
                            }
                        } else if (bitversi == 32) {
                            if (gameint == 5) {
                                Exec("/TW " + game + " 000");
                            } else {
                                Exec("/TW " + game + " 000");
                            }
                        }
                    }
                }
            });*/
        }


        // ISLAND BYPSS

        if (!Sufii) {
           // imgisland.setBackgroundResource(R.drawable.baseline_lock_24);
           // menuisland.setAlpha(0.6f);
            //isisland.setEnabled(false);
         //   typelogin = "FREE";
        } else {
          //  typelogin = "BLAZEHAX";
            isisland.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
				public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
						if (isChecked) {
						if (!Shell.rootAccess()) {
						runant("bypass 5");
					//	logoText.setVisibility(View.GONE);
						}
					}
						else
						{
							runant("bypass 6");
							//logoText.setVisibility(View.VISIBLE);
						}

					}
				
			});

        }
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


        final RadioButton fps3 = visual.findViewById(R.id.fps60);
        final RadioButton fps4 = visual.findViewById(R.id.fps120);
        final RadioButton fps5 = visual.findViewById(R.id.fps130);
        final RadioButton fps6 = visual.findViewById(R.id.fps144);

        int CheckFps = getFps();
        if (CheckFps == 60) {
            fps3.setChecked(true);
            ESPView.sleepTime = 1000 / 60;
        } else if (CheckFps == 90) {
            fps4.setChecked(true);
            ESPView.sleepTime = 1000 / 90;
        } else if (CheckFps == 120) {
            fps5.setChecked(true);
            ESPView.sleepTime = 1000 / 120;
        }  else if (CheckFps == 144) {
            fps6.setChecked(true);
            ESPView.sleepTime = 1000 / 144;
        } else {
            fps3.setChecked(true);
            ESPView.sleepTime = 1000 / 60;
        }


        fps3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fps4.setChecked(false);
                    fps5.setChecked(false);
                    setFps(60);
                    ESPView.ChangeFps(60);
                }
            }
        });

        fps4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fps3.setChecked(false);
                    fps5.setChecked(false);
                    setFps(90);
                    ESPView.ChangeFps(90);
                }
            }
        });

        fps5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fps3.setChecked(false);
                    fps4.setChecked(false);
                    setFps(120);
                    ESPView.ChangeFps(120);
                }
            }
        });

        SharedPreferences sharedPreferences = visual.getContext().getSharedPreferences("espValue", Context.MODE_PRIVATE);
        String bypassmode = sharedPreferences.getString("bypassmode", "manual");

        if (Shell.rootAccess()) {
            //visual.findViewById(R.id.protectiontext).setVisibility(View.GONE);
           // visual.findViewById(R.id.menucrash).setVisibility(View.GONE);
            //visual.findViewById(R.id.menuloho).setVisibility(View.GONE);

        } else {
            if (bypassmode.equals("automatic")) {
               // visual.findViewById(R.id.protectiontext).setVisibility(View.GONE);
               // visual.findViewById(R.id.menuloho).setVisibility(View.GONE);
            } else {
               // visual.findViewById(R.id.protectiontext).setVisibility(View.VISIBLE);
                //visual.findViewById(R.id.menuloho).setVisibility(View.VISIBLE);
            }
            //visual.findViewById(R.id.menucrash).setVisibility(View.GONE);
        }


        final TextView isLine = visual.findViewById(R.id.isline);
        //isLine.setChecked(true);
        espvisual(isLine, 2);

        final TextView isbox = visual.findViewById(R.id.isBox);
        //isbox.setChecked(true);
        espvisual(isbox, 3);

        final TextView isskeleton = visual.findViewById(R.id.isskeleton);
        //isskeleton.setChecked(true);
        espvisual(isskeleton, 4);

        final TextView isdistance = visual.findViewById(R.id.isdistance);
        //isdistance.setChecked(true);
        espvisual(isdistance, 5);

        final TextView ishealth = visual.findViewById(R.id.ishealth);
        //ishealth.setChecked(true);
        espvisual(ishealth, 6);

        final TextView isname = visual.findViewById(R.id.isName);
        //isname.setChecked(true);
        espvisual(isname, 7);

        final TextView ishead = visual.findViewById(R.id.ishead);
        //ishead.setChecked(true);
        espvisual(ishead, 8);

        final TextView isalert = visual.findViewById(R.id.isalert);
        //isalert.setChecked(true);
        espvisual(isalert, 9);

        final TextView isweapon = visual.findViewById(R.id.isweapon);
        //isweapon.setChecked(true);
        espvisual(isweapon, 10);

        final TextView isthrowables = visual.findViewById(R.id.isthrowables);
        //isthrowables.setChecked(true);
        espvisual(isthrowables, 11);


        final TextView isnobot = visual.findViewById(R.id.isnobot);
        //isnobot.setChecked(false);
        espvisual(isnobot, 15);

        final TextView isweaponicon = visual.findViewById(R.id.isweaponicon);
        //isweaponicon.setChecked(true);
        espvisual(isweaponicon, 16);


        islootitems = visual.findViewById(R.id.islootitems);
        espvisual(islootitems, 17);

        final TextView isradar = visual.findViewById(R.id.isradar);
        espvisual(isradar, 18);

        final TextView playerid = visual.findViewById(R.id.playerid);
        espvisual(playerid, 19);

        final TextView playernation = visual.findViewById(R.id.playernation);
        espvisual(playernation, 20);

        final TextView isteamid = visual.findViewById(R.id.isteamid);
        espvisual(isteamid, 21);

        final TextView isFightMode = visual.findViewById(R.id.isfightmode);
        espvisual(isFightMode, 22);
        isFightMode.setOnClickListener(new View.OnClickListener() {
            boolean isSelected = false;

            @Override
            public void onClick(View v) {
                if (!isSelected) {
                    StartFightModeFloat();
                    isFightMode.setBackgroundResource(R.drawable.float_selected);
                } else {
                    StopFightModeFloat();
                    isFightMode.setBackgroundResource(R.drawable.float_unselect);
                }
                isSelected = !isSelected;
            }
        });






    }

    public static void enableESP(Context context) {
        Intent intent = new Intent(context, FloatService.class);
        intent.setAction("ENABLE_ESP");
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if ("ENABLE_ESP".equals(action)) {
                drawesp.setChecked(true);
            } else if ("UPDATE_LOOT_ITEMS".equals(action)) {
                boolean isFightModeOn = intent.getBooleanExtra("isFightModeOn", false);
                if (islootitems != null) {
                    islootitems.setSelected(!isFightModeOn);
                }
            }
        }
        return START_STICKY;
    }



    private void items(View items) {
        LinearLayout menui1 = items.findViewById(R.id.items1);
        LinearLayout menui2 = items.findViewById(R.id.lyvehicle);
        View bottomi1 = items.findViewById(R.id.bottomi1);
        View bottomi2 = items.findViewById(R.id.bottomi2);
        // LinearLayout navi1 = items.findViewById(R.id.navitems);


     /*   navi1.setOnClickListener(v -> {
            menui1.setVisibility(View.VISIBLE);
            menui2.setVisibility(View.GONE);
            bottomi1.setVisibility(View.VISIBLE);
            bottomi2.setVisibility(View.GONE);
        });

        navi2.setOnClickListener(v -> {
            menui1.setVisibility(View.GONE);
            menui2.setVisibility(View.VISIBLE);
            bottomi1.setVisibility(View.GONE);
            bottomi2.setVisibility(View.VISIBLE);
        });*/

        final TextView lootbox = items.findViewById(R.id.lootbox);
        espvisual(lootbox, 14);


        final TextView M416 = items.findViewById(R.id.m416);
        itemss(M416);

        final TextView QBZ = items.findViewById(R.id.QBZ);
        itemss(QBZ);

        final TextView SCARL = items.findViewById(R.id.SCARL);
        itemss(SCARL);

        final TextView AKM = items.findViewById(R.id.AKM);
        itemss(AKM);

        final TextView M16A4 = items.findViewById(R.id.M16A4);
        itemss(M16A4);

        final TextView AUG = items.findViewById(R.id.AUG);
        itemss(AUG);



        final TextView Groza = items.findViewById(R.id.Groza);
        itemss(Groza);

        final TextView MK47 = items.findViewById(R.id.MK47);
        itemss(MK47);

        final TextView M762 = items.findViewById(R.id.M762);
        itemss(M762);

        final TextView G36C = items.findViewById(R.id.G36C);
        itemss(G36C);

        final TextView DP28 = items.findViewById(R.id.DP28);
        itemss(DP28);

        final TextView MG3 = items.findViewById(R.id.MG3);
        itemss(MG3);

        final TextView M249 = items.findViewById(R.id.M249);
        itemss(M249);



        final TextView FAMAS = items.findViewById(R.id.FAMAS);
        itemss(FAMAS);


        final TextView HoneyBadger = items.findViewById(R.id.HoneyBadger);
        itemss(HoneyBadger);


        final TextView AC32 = items.findViewById(R.id.AC32);
        itemss(AC32);


        //SMG

        final TextView UMP = items.findViewById(R.id.UMP);
        itemss(UMP);

        final TextView bizon = items.findViewById(R.id.bizon);
        itemss(bizon);

        final TextView MP5K = items.findViewById(R.id.MP5K);
        itemss(MP5K);

        final TextView TommyGun = items.findViewById(R.id.TommyGun);
        itemss(TommyGun);

        final TextView vector = items.findViewById(R.id.vector);
        itemss(vector);

        final TextView P90 = items.findViewById(R.id.P90);
        itemss(P90);

        final TextView UZI = items.findViewById(R.id.UZI);
        itemss(UZI);


        //Snipers

        final TextView AWM = items.findViewById(R.id.AWM);
        itemss(AWM);

        final TextView QBU = items.findViewById(R.id.QBU);
        itemss(QBU);

        final TextView Kar98k = items.findViewById(R.id.Kar98k);
        itemss(Kar98k);

        final TextView M24 = items.findViewById(R.id.M24);
        itemss(M24);

        final TextView SLR = items.findViewById(R.id.SLR);
        itemss(SLR);

        final TextView SKS = items.findViewById(R.id.SKS);
        itemss(SKS);

        final TextView MK14 = items.findViewById(R.id.MK14);
        itemss(MK14);

        final TextView Mini14 = items.findViewById(R.id.Mini14);
        itemss(Mini14);

        final TextView Mosin = items.findViewById(R.id.Mosin);
        itemss(Mosin);

        final TextView VSS = items.findViewById(R.id.VSS);
        itemss(VSS);

        final TextView AMR = items.findViewById(R.id.AMR);
        itemss(AMR);

        final TextView Win94 = items.findViewById(R.id.Win94);
        itemss(Win94);

        final TextView MK12 = items.findViewById(R.id.MK12);
        itemss(MK12);

        //Scopes

        final TextView x2 = items.findViewById(R.id.x2);
        itemss(x2);

        final TextView x3 = items.findViewById(R.id.x3);
        itemss(x3);

        final TextView x4 = items.findViewById(R.id.x4);
        itemss(x4);

        final TextView x6 = items.findViewById(R.id.x6);
        itemss(x6);

        final TextView x8 = items.findViewById(R.id.x8);
        itemss(x8);

        final TextView canted = items.findViewById(R.id.canted);
        itemss(canted);

        final TextView hollow = items.findViewById(R.id.hollow);
        itemss(hollow);

        final TextView reddot = items.findViewById(R.id.reddot);
        itemss(reddot);

        //Armor

        final TextView bag1 = items.findViewById(R.id.bag1);
        itemss(bag1);

        final TextView bag2 = items.findViewById(R.id.bag2);
        itemss(bag2);

        final TextView bag3 = items.findViewById(R.id.bag3);
        itemss(bag3);

        final TextView helmet1 = items.findViewById(R.id.helmet1);
        itemss(helmet1);

        final TextView helmet2 = items.findViewById(R.id.helmet2);
        itemss(helmet2);

        final TextView helmet3 = items.findViewById(R.id.helmet3);
        itemss(helmet3);

        final TextView vest1 = items.findViewById(R.id.vest1);
        itemss(vest1);

        final TextView vest2 = items.findViewById(R.id.vest2);
        itemss(vest2);

        final TextView vest3 = items.findViewById(R.id.vest3);
        itemss(vest3);

        //Ammo
        final TextView a9 = items.findViewById(R.id.a9);
        itemss(a9);

        final TextView a7 = items.findViewById(R.id.a7);
        itemss(a7);

        final TextView a5 = items.findViewById(R.id.a5);
        itemss(a5);

        final TextView a300 = items.findViewById(R.id.a300);
        itemss(a300);

        final TextView a45 = items.findViewById(R.id.a45);
        itemss(a45);

        final TextView Arrow = items.findViewById(R.id.arrow);
        itemss(Arrow);

        final TextView BMG50 = items.findViewById(R.id.BMG50);
        itemss(BMG50);

        final TextView a12 = items.findViewById(R.id.a12);
        itemss(a12);

        //Shotgun
        final TextView DBS = items.findViewById(R.id.DBS);
        itemss(DBS);

        final TextView NS2000 = items.findViewById(R.id.NS2000);
        itemss(NS2000);

        final TextView S686 = items.findViewById(R.id.S686);
        itemss(S686);

        final TextView sawed = items.findViewById(R.id.sawed);
        itemss(sawed);

        final TextView M1014 = items.findViewById(R.id.M1014);
        itemss(M1014);

        final TextView S1897 = items.findViewById(R.id.S1897);
        itemss(S1897);

        final TextView S12K = items.findViewById(R.id.S12K);
        itemss(S12K);

        //Throwables
        final TextView grenade = items.findViewById(R.id.grenade);
        itemss(grenade);

        final TextView molotov = items.findViewById(R.id.molotov);
        itemss(molotov);

        final TextView stun = items.findViewById(R.id.stun);
        itemss(stun);

        final TextView smoke = items.findViewById(R.id.smoke);
        itemss(smoke);

        //Medics

        final TextView painkiller = items.findViewById(R.id.painkiller);
        itemss(painkiller);

        final TextView medkit = items.findViewById(R.id.medkit);
        itemss(medkit);

        final TextView firstaid = items.findViewById(R.id.firstaid);
        itemss(firstaid);

        final TextView bandage = items.findViewById(R.id.bandage);
        itemss(bandage);

        final TextView injection = items.findViewById(R.id.injection);
        itemss(injection);

        final TextView energydrink = items.findViewById(R.id.energydrink);
        itemss(energydrink);

        //Handy
        final TextView Pan = items.findViewById(R.id.Pan);
        itemss(Pan);

        final TextView Crowbar = items.findViewById(R.id.Crowbar);
        itemss(Crowbar);

        final TextView Sickle = items.findViewById(R.id.Sickle);
        itemss(Sickle);

        final TextView Machete = items.findViewById(R.id.Machete);
        itemss(Machete);

        final TextView Crossbow = items.findViewById(R.id.Crossbow);
        itemss(Crossbow);

        final TextView Explosive = items.findViewById(R.id.Explosive);
        itemss(Explosive);

        //Pistols

        final TextView Desert = items.findViewById(R.id.Desert);
        itemss(Desert);

        final TextView P92 = items.findViewById(R.id.P92);
        itemss(P92);

        final TextView R45 = items.findViewById(R.id.R45);
        itemss(R45);

        final TextView P18C = items.findViewById(R.id.P18C);
        itemss(P18C);

        final TextView P1911 = items.findViewById(R.id.P1911);
        itemss(P1911);

        final TextView R1895 = items.findViewById(R.id.R1895);
        itemss(R1895);

        final TextView Scorpion = items.findViewById(R.id.Scorpion);
        itemss(Scorpion);

        //Other
        final TextView CheekPad = items.findViewById(R.id.CheekPad);
        itemss(CheekPad);

        final TextView Choke = items.findViewById(R.id.Choke);
        itemss(Choke);

        final TextView CompensatorSMG = items.findViewById(R.id.CompensatorSMG);
        itemss(CompensatorSMG);


        final TextView FlashHiderSMG = items.findViewById(R.id.FlashHiderSMG);
        itemss(FlashHiderSMG);


        final TextView FlashHiderAr = items.findViewById(R.id.FlashHiderAr);
        itemss(FlashHiderAr);

        final TextView ArCompensator = items.findViewById(R.id.ArCompensator);
        itemss(ArCompensator);

        final TextView TacticalStock = items.findViewById(R.id.TacticalStock);
        itemss(TacticalStock);

        final TextView Duckbill = items.findViewById(R.id.Duckbill);
        itemss(Duckbill);

        final TextView FlashHiderSniper = items.findViewById(R.id.FlashHiderSniper);
        itemss(FlashHiderSniper);

        final TextView SuppressorSMG = items.findViewById(R.id.SuppressorSMG);
        itemss(SuppressorSMG);

        final TextView HalfGrip = items.findViewById(R.id.HalfGrip);
        itemss(HalfGrip);

        final TextView StockMicroUZI = items.findViewById(R.id.StockMicroUZI);
        itemss(StockMicroUZI);

        final TextView SuppressorSniper = items.findViewById(R.id.SuppressorSniper);
        itemss(SuppressorSniper);

        final TextView SuppressorAr = items.findViewById(R.id.SuppressorAr);
        itemss(SuppressorAr);

        final TextView SniperCompensator = items.findViewById(R.id.SniperCompensator);
        itemss(SniperCompensator);

        final TextView ExQdSniper = items.findViewById(R.id.ExQdSniper);
        itemss(ExQdSniper);

        final TextView QdSMG = items.findViewById(R.id.QdSMG);
        itemss(QdSMG);

        final TextView ExSMG = items.findViewById(R.id.ExSMG);
        itemss(ExSMG);

        final TextView QdSniper = items.findViewById(R.id.QdSniper);
        itemss(QdSniper);

        final TextView ExSniper = items.findViewById(R.id.ExSniper);
        itemss(ExSniper);

        final TextView ExAr = items.findViewById(R.id.ExAr);
        itemss(ExAr);

        final TextView ExQdAr = items.findViewById(R.id.ExQdAr);
        itemss(ExQdAr);

        final TextView QdAr = items.findViewById(R.id.QdAr);
        itemss(QdAr);

        final TextView ExQdSMG = items.findViewById(R.id.ExQdSMG);
        itemss(ExQdSMG);

        final TextView QuiverCrossBow = items.findViewById(R.id.QuiverCrossBow);
        itemss(QuiverCrossBow);

        final TextView BulletLoop = items.findViewById(R.id.BulletLoop);
        itemss(BulletLoop);

        final TextView ThumbGrip = items.findViewById(R.id.ThumbGrip);
        itemss(ThumbGrip);

        final TextView LaserSight = items.findViewById(R.id.LaserSight);
        itemss(LaserSight);

        final TextView AngledGrip = items.findViewById(R.id.AngledGrip);
        itemss(AngledGrip);

        final TextView LightGrip = items.findViewById(R.id.LightGrip);
        itemss(LightGrip);

        final TextView VerticalGrip = items.findViewById(R.id.VerticalGrip);
        itemss(VerticalGrip);

        final TextView GasCan = items.findViewById(R.id.GasCan);
        itemss(GasCan);

        //Vehicle
        final TextView UTV = items.findViewById(R.id.UTV);
        //UTV.setChecked(true);
        vehicless(UTV);

        final TextView Buggy = items.findViewById(R.id.Buggy);
        //Buggy.setChecked(true);
        vehicless(Buggy);

        final TextView UAZ = items.findViewById(R.id.UAZ);
        //UAZ.setChecked(true);
        vehicless(UAZ);

        final TextView Trike = items.findViewById(R.id.Trike);
        //Trike.setChecked(true);
        vehicless(Trike);

        final TextView Bike = items.findViewById(R.id.Bike);
        //Bike.setChecked(true);
        vehicless(Bike);

        final TextView Dacia = items.findViewById(R.id.Dacia);
        //Dacia.setChecked(true);
        vehicless(Dacia);

        final TextView Jet = items.findViewById(R.id.Jet);
        //Jet.setChecked(true);
        vehicless(Jet);

        final TextView Boat = items.findViewById(R.id.Boat);
        //Boat.setChecked(true);
        vehicless(Boat);

        final TextView Scooter = items.findViewById(R.id.Scooter);
        //Scooter.setChecked(true);
        vehicless(Scooter);

        final TextView Bus = items.findViewById(R.id.Bus);
        //Bus.setChecked(true);
        vehicless(Bus);

        final TextView Mirado = items.findViewById(R.id.Mirado);
        //Mirado.setChecked(true);
        vehicless(Mirado);

        final TextView Rony = items.findViewById(R.id.Rony);
       // Rony.setChecked(true);
        vehicless(Rony);

        final TextView Snowbike = items.findViewById(R.id.Snowbike);
        //Snowbike.setChecked(true);
        vehicless(Snowbike);

        final TextView Snowmobile = items.findViewById(R.id.Snowmobile);
       // Snowmobile.setChecked(true);
        vehicless(Snowmobile);

        final TextView Tempo = items.findViewById(R.id.Tempo);
        ///Tempo.setChecked(true);
        vehicless(Tempo);

        final TextView Truck = items.findViewById(R.id.Truck);
        //Truck.setChecked(true);
        vehicless(Truck);

        final TextView MonsterTruck = items.findViewById(R.id.MonsterTruck);
       // MonsterTruck.setChecked(true);
        vehicless(MonsterTruck);

        final TextView BRDM = items.findViewById(R.id.BRDM);
        //BRDM.setChecked(true);
        vehicless(BRDM);

        final TextView ATV = items.findViewById(R.id.ATV);
        //ATV.setChecked(true);
        vehicless(ATV);

        final TextView LadaNiva = items.findViewById(R.id.LadaNiva);
        //LadaNiva.setChecked(true);
        vehicless(LadaNiva);

        final TextView Motorglider = items.findViewById(R.id.Motorglider);
        //Motorglider.setChecked(true);
        vehicless(Motorglider);

        final TextView CoupeRB = items.findViewById(R.id.CoupeRB);
        //CoupeRB.setChecked(true);
        vehicless(CoupeRB);


        //Special
        final TextView Crate = items.findViewById(R.id.Crate);
        itemss(Crate);

        final TextView Airdrop = items.findViewById(R.id.Airdrop);
        itemss(Airdrop);

        final TextView DropPlane = items.findViewById(R.id.DropPlane);
        itemss(DropPlane);

        final TextView FlareGun = items.findViewById(R.id.FlareGun);
        itemss(FlareGun);

 /*       final LinearLayout checkall = mainView.findViewById(R.id.itemscheckall);
        final LinearLayout noneall = mainView.findViewById(R.id.itemsblockall);
        final LinearLayout checkallv = mainView.findViewById(R.id.mobilscheckall);
        final LinearLayout noneallv = mainView.findViewById(R.id.mobilsblockall);

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


            Crate.setChecked(true);
            Airdrop.setChecked(true);
            DropPlane.setChecked(true);
            CheekPad.setChecked(true);
            lootbox.setChecked(true);
            Choke.setChecked(true);



            canted.setChecked(true);
            reddot.setChecked(true);
            hollow.setChecked(true);
            x2.setChecked(true);
            x3.setChecked(true);
            x4.setChecked(true);
            x6.setChecked(true);
            x8.setChecked(true);


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

            Crate.setChecked(false);
            Airdrop.setChecked(false);
            DropPlane.setChecked(false);
            CheekPad.setChecked(false);
            lootbox.setChecked(false);
            Choke.setChecked(false);



            canted.setChecked(false);
            reddot.setChecked(false);
            hollow.setChecked(false);
            x2.setChecked(false);
            x3.setChecked(false);
            x4.setChecked(false);
            x6.setChecked(false);
            x8.setChecked(false);


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
        });*/
    }

    private void aimbot(View aimbot) {
        TextView menutextaimtouch = aimbot.findViewById(R.id.texttouch);
        TextView aimpre = aimbot.findViewById(R.id.aimpre);
        LinearLayout aimsec = aimbot.findViewById(R.id.aimsec);
        LinearLayout menurotation = aimbot.findViewById(R.id.rotationmenu);
        LinearLayout aimspeedmenu = aimbot.findViewById(R.id.aimspeedmenu);
        LinearLayout recoilmenu = aimbot.findViewById(R.id.recoilmenu);
        LinearLayout recoilmenus2 = aimbot.findViewById(R.id.recoilmenus2);
        LinearLayout smoothnessmenu = aimbot.findViewById(R.id.smoothnessmenu);
        RadioButton touchsimulation = aimbot.findViewById(R.id.touchsimulation);
        RadioButton bullettrack = aimbot.findViewById(R.id.bullettrack);
        RadioButton aimbottt = aimbot.findViewById(R.id.aimbot);
        final LinearLayout touchLocationmenu = aimbot.findViewById(R.id.touchlocationmenu);
        final LinearLayout touchsizemenu = aimbot.findViewById(R.id.touchsizemenu);
        final LinearLayout posXmenu = aimbot.findViewById(R.id.posXmenu);
        final LinearLayout posYmenu = aimbot.findViewById(R.id.posYmenu);

        if (!modestatus) {
            aimbottt.setVisibility(View.GONE);
            bullettrack.setVisibility(View.VISIBLE);
        } else {
            aimbottt.setVisibility(View.VISIBLE);
            bullettrack.setVisibility(View.VISIBLE);
        }


            
            aimpre.setVisibility(View.GONE);
            aimsec.setVisibility(View.VISIBLE);
            menurotation.setAlpha(1.0f);
            aimspeedmenu.setAlpha(1.0f);
            recoilmenu.setAlpha(1.0f);
            recoilmenus2.setAlpha(1.0f);
            smoothnessmenu.setAlpha(1.0f);
            touchsimulation.setEnabled(true);
            bullettrack.setEnabled(true);
            aimbottt.setEnabled(true);
            touchsimulation.setAlpha(1.0f);
            bullettrack.setAlpha(1.0f);
            aimbottt.setAlpha(1.0f);
            touchLocationmenu.setAlpha(1.0f);
            touchsizemenu.setAlpha(1.0f);
            posXmenu.setAlpha(1.0f);
            posYmenu.setAlpha(1.0f);

        

        RadioGroup aimgrup = aimbot.findViewById(R.id.grupaim);
        aimgrup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.disableaim:
                        StopAimBulletFloat();
                        StopAimFloat();
                        StopAimTouch();
                        menutextaimtouch.setVisibility(View.GONE);
                        menurotation.setVisibility(View.GONE);
                        aimspeedmenu.setVisibility(View.GONE);
                        smoothnessmenu.setVisibility(View.GONE);
                        break;

                    case R.id.aimbot:
                      StartAimFloat();
                        StopAimBulletFloat();
                        StopAimTouch();
                        menutextaimtouch.setVisibility(View.GONE);
                        menurotation.setVisibility(View.GONE);
                        aimspeedmenu.setVisibility(View.GONE);
                        smoothnessmenu.setVisibility(View.GONE);
                        touchLocationmenu.setVisibility(View.GONE);
                        touchsizemenu.setVisibility(View.GONE);
                        recoilmenu.setVisibility(View.VISIBLE);
                        posXmenu.setVisibility(View.GONE);
                        posYmenu.setVisibility(View.GONE);
                        break;

                    case R.id.touchsimulation:
                     StartAimTouch();
                        StopAimBulletFloat();
                        StopAimFloat();
                        menutextaimtouch.setVisibility(View.VISIBLE);
                        menurotation.setVisibility(View.VISIBLE);
                        aimspeedmenu.setVisibility(View.VISIBLE);
                        smoothnessmenu.setVisibility(View.VISIBLE);
                        touchLocationmenu.setVisibility(View.VISIBLE);
                        touchsizemenu.setVisibility(View.VISIBLE);
                        recoilmenu.setVisibility(View.VISIBLE);
                        recoilmenus2.setVisibility(View.VISIBLE);
                        posXmenu.setVisibility(View.VISIBLE);
                        posYmenu.setVisibility(View.VISIBLE);
                        break;

                    case R.id.bullettrack:
                      StartAimBulletFloat();
                        StopAimFloat();
                        StopAimTouch();
                        menutextaimtouch.setVisibility(View.GONE);
                        menurotation.setVisibility(View.GONE);
                        aimspeedmenu.setVisibility(View.GONE);
                        smoothnessmenu.setVisibility(View.GONE);
                        touchLocationmenu.setVisibility(View.GONE);
                        touchsizemenu.setVisibility(View.GONE);
                        recoilmenu.setVisibility(View.GONE);
                        recoilmenus2.setVisibility(View.GONE);
                        posXmenu.setVisibility(View.GONE);
                        posYmenu.setVisibility(View.GONE);
                        break;
                }
            }
        });


        aimKnocked = aimbot.findViewById(R.id.aimknocked);
        setaim(aimKnocked, 3);

        aimignore = aimbot.findViewById(R.id.aimignorebot);
        setaim(aimignore, 4);

        final Switch changerotation = aimbot.findViewById(R.id.rotationscren);
        setaim(changerotation, 5);

        final Switch touchlocation = aimbot.findViewById(R.id.touchlocation);
        setaim(touchlocation, 6);

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

        final SeekBar AimSpeedSize = aimbot.findViewById(R.id.aimingspeed);
        final TextView AimSpeedText = aimbot.findViewById(R.id.aimingspeedtext);
        setupSeekBar(AimSpeedSize, AimSpeedText, getAimSpeed(), new Runnable() {
            @Override
            public void run() {
                AimingSpeed(AimSpeedSize.getProgress());
                setAimSpeed(AimSpeedSize.getProgress());
            }
        });

        final SeekBar SmoothSize = aimbot.findViewById(R.id.Smoothness);
        final TextView SmoothText = aimbot.findViewById(R.id.smoothtext);
        setupSeekBar(SmoothSize, SmoothText, getSmoothness(), new Runnable() {
            @Override
            public void run() {
                Smoothness(SmoothSize.getProgress());
                setSmoothness(SmoothSize.getProgress());
            }
        });

        final SeekBar touchsize = mainView.findViewById(R.id.touchsize);
        final TextView touchsizetext = mainView.findViewById(R.id.touchsizetext);
        setupSeekBar(touchsize, touchsizetext, getTouchSize(), new Runnable() {
            @Override
            public void run() {
                TouchSize(touchsize.getProgress());
                setTouchSize(touchsize.getProgress());
            }
        });

        final SeekBar touchPosX = mainView.findViewById(R.id.touchPosX);
        final TextView touchPosXtext = mainView.findViewById(R.id.touchPosXtext);
        setupSeekBar(touchPosX, touchPosXtext, getTouchPosX(), new Runnable() {
            @Override
            public void run() {
                TouchPosX(touchPosX.getProgress());
                setTouchPosX(touchPosX.getProgress());
            }
        });

        final SeekBar touchPosY = mainView.findViewById(R.id.touchPosY);
        final TextView touchPosYtext = mainView.findViewById(R.id.touchPosYtext);
        setupSeekBar(touchPosY, touchPosYtext, getTouchPosY(), new Runnable() {
            @Override
            public void run() {
                TouchPosY(touchPosY.getProgress());
                setTouchPosY(touchPosY.getProgress());
            }
        });


        aimby = aimbot.findViewById(R.id.aimby);
        aimby.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimby.getCheckedRadioButtonId();
                RadioButton btn = aimbot.findViewById(chkdId);
                AimBy(Integer.parseInt(btn.getTag().toString()));
            }
        });

        final RadioGroup aimwhen = aimbot.findViewById(R.id.aimwhen);
        aimwhen.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimwhen.getCheckedRadioButtonId();
                RadioButton btn = aimbot.findViewById(chkdId);
                AimWhen(Integer.parseInt(btn.getTag().toString()));
            }
        });

        final RadioGroup aimbotmode = aimbot.findViewById(R.id.aimbotmode);
        aimbotmode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int chkdId = aimbotmode.getCheckedRadioButtonId();
                RadioButton btn = aimbot.findViewById(chkdId);
                Target(Integer.parseInt(btn.getTag().toString()));
            }
        });
    }

    private void memory(View memory) {
        final Switch less = memory.findViewById(R.id.isreducerecoil);
        memory(less, 1);
        final Switch Cross = memory.findViewById(R.id.issmallcross);
        memory(Cross, 2);
        final Switch amms = memory.findViewById(R.id.isaimlock);
        memory(amms, 3);
        final Switch ismagic = memory.findViewById(R.id.ismagichead);
        final Switch ishitx = memory.findViewById(R.id.ishitx);
        final SeekBar wideviewSeekBar = memory.findViewById(R.id.rangewide);
        final TextView wideviewText = memory.findViewById(R.id.rangetextwide);
        final TextView aimpresdk = memory.findViewById(R.id.aimpresdk);
        LinearLayout memsec = memory.findViewById(R.id.memsec);

        if (Sufii) {
            typelogin = "BLAZEHAX";
            aimpresdk.setVisibility(View.GONE);
            memsec.setVisibility(View.VISIBLE);
        } else {
            typelogin = "FREE";
            aimpresdk.setVisibility(View.VISIBLE);
            memsec.setVisibility(View.GONE);
            less.setEnabled(false);
            Cross.setEnabled(false);
            amms.setEnabled(false);
            ismagic.setEnabled(false);
            ishitx.setEnabled(false);
            wideviewSeekBar.setEnabled(false);
            less.setAlpha(0.0f);
            Cross.setAlpha(0.0f);
            amms.setAlpha(0.0f);
            ismagic.setAlpha(0.0f);
            ishitx.setAlpha(0.0f);
        }


        // MAGIC BULLET


        ismagic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                  //  Exec("/SUFI 500 " + USERKEY + " " + game + " 44");
                } else {
                    // Exec("/sufi 400 "+USERKEY+" "+game+" 001", getString(R.string.magic_launch_success));
                }
            }
        });


        ishitx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                //    Exec("/TANHA " + USERKEY + game);
                } else {
                    // Exec("/SUFI 400 "+USERKEY+" "+game+" 001", getString(R.string.hitx_launch_success));
                }
            }
        });


        setupSeekBar(wideviewSeekBar, wideviewText, getwideview(), new Runnable() {
            @Override
            public void run() {
                WideView(wideviewSeekBar.getProgress());
                getwideview(wideviewSeekBar.getProgress());
            }
        });
    }


  /*  public class Dapter extends PagerAdapter {
        LayoutInflater inflater;
        Context context;

        public Dapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

       *//* @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = null;
            view = inflater.inflate(position == 0 ? R.layout.esp_visual : position == 1 ? R.layout.esp_inventory : position == 2 ? R.layout.esp_aimbot : R.layout.esp_memory, null);
            ViewPager viewPager = (ViewPager) container;
            viewPager.addView(view);
            if (position == 0) {
                visual(view);
            } else if (position == 1) {
                items(view);
            } else if (position == 2) {
                aimbot(view);
            } else if (position == 3) {
                memory(view);
            }
            return view;
        }*//*
    }*/
}
