package lauresprojects.com.recorder.floating;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;


import java.io.*;
import java.lang.Process;


import android.annotation.SuppressLint;
import lauresprojects.com.recorder.activity.MainActivity;
import lauresprojects.com.recorder.activity.GamePidHelper;
import lauresprojects.com.recorder.utils.FPrefs;

public class Overlay extends Service {
  
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    WindowManager windowManager;
   static Process process;
    View mainView;
    ESPView overlayView;
    
    public FPrefs getPref() {
        return FPrefs.with(this);
    }
    

   @SuppressLint("StaticFieldLeak")
    private static Overlay Instance;

	static Context ctx;
    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();
        ctx=this;
        windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        overlayView = new ESPView(ctx);
        DrawCanvas();
        // Start BEFORE Instance=this — Start() only launches getReady/sock64 when Instance==null.
        // Setting Instance first made Start a no-op → sock64 never ran → ESP dead.
        Start(ctx, 0, 1);
        Instance = this;
    }

    @Override
    public void onDestroy() {
		super.onDestroy();
		Close();

        if(overlayView != null)
        {
            ((WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE)).removeView(overlayView);
            overlayView = null;
        }
        Instance = null;
		if (process != null) {
			process.destroy();
			process = null;
		}
    }

    public static void Start(final Context context,final int gametype,final int bit) {

        if (Instance == null) {
            Thread t=new Thread(new Runnable() {
					@Override
					public void run(){
						getReady(gametype);

					}
				});
            t.start();

            Thread t2=new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							// Give getReady time to Bind+Listen before sock64 Connect
							Thread.sleep(150);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartDaemon(context,bit);
					}
				});
            t2.start();

        }
    }
    static native boolean getReady(int nameofgame);

	public static void StartDaemon(final Context context,int bit){
		try {
			GamePidHelper.refreshPidFile(context);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		android.util.Log.i("RBASED", "StartDaemon launching sock64 path=" +
				(MainActivity.daemonPath != null ? MainActivity.daemonPath : MainActivity.socket));
		Shell(context, MainActivity.daemonPath != null ? MainActivity.daemonPath : MainActivity.socket);
	}

    public static void Stop(Context context) {
        Intent intent = new Intent(context, Overlay.class);
        context.stopService(intent);

        Intent floatLogo = new Intent(context, FloatService.class);
        context.stopService(floatLogo);



    }

    private native void Close();
	public static boolean getConfig(String key){
        SharedPreferences sp=ctx.getSharedPreferences("espValue",Context.MODE_PRIVATE);
        return  sp.getBoolean(key,false);
    }
    private void DrawCanvas() {
    int LAYOUT_FLAG;
    // Use correct overlay type depending on Android version
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
    }

    // Base layout params for the overlay
    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,  // initial x (can adjust later)
            getNavigationBarHeight(),  // initial y
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.RGBA_8888
    );

    // Handle display cutouts (notches)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
    }

    // Base position for overlay
    params.gravity = Gravity.TOP | Gravity.START;
    params.x = 0;
    params.y = 0;

    // Apply "fake recorder" modifications only if recording is active
    if (MainActivity.Record) {
        HideRecorder.setFakeRecorderWindowLayoutParams(params);
        // HideRecorder can adjust gravity, size, or other params safely here
    }

    // Add the overlay to the window manager
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    windowManager.addView(overlayView, params);
}


    public static native void DrawOn(ESPView espView, Canvas canvas);
    private int getNavigationBarHeight() {
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && !hasMenuKey) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

	public static void Shell(Context context, String daemonPath) {
		if (daemonPath == null || daemonPath.isEmpty()) {
			return;
		}
		try {
			java.util.Map<String, String> env = new java.util.HashMap<>(System.getenv());
			GamePidHelper.applySockPidEnv(env, context);
			ProcessBuilder pb = new ProcessBuilder(daemonPath);
			pb.environment().putAll(env);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			process = p;
			// sock64 is long-running; do not waitFor on the ESP thread
			new Thread(() -> {
				try {
					p.waitFor();
				} catch (InterruptedException ignored) {
				}
			}, "sock64-wait").start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** @deprecated use {@link #Shell(Context, String)} */
	public static void Shell(String str) {
		Shell(ctx, str);
	}
}

