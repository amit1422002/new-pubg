package lauresprojects.com.recorder.libhelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import net.lingala.zip4j.ZipFile;
import lauresprojects.com.recorder.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadZip extends AsyncTask<String, Integer, Boolean> {

    private final Context context;
    private final Runnable onCompleteCallback;


    private Dialog progressDialog;

    private native String PASSJKPAPA();

    public DownloadZip(Context context, Runnable onCompleteCallback) {
        this.context = context;
        this.onCompleteCallback = onCompleteCallback;
        showProgressDialog();
    }

    private void showProgressDialog() {
        progressDialog = new Dialog(context);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.dialog_lottie_progress);
        progressDialog.setCancelable(false);

        Window window = progressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(R.color.background));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
        }

        progressDialog.show();
    }


    @Override
    protected Boolean doInBackground(String... strings) {
        String downloadUrl = strings[1];
        String fileName = "assets.zip";
        File pathBase = context.getFilesDir();

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            int lengthOfFile = connection.getContentLength();

            InputStream input = connection.getInputStream();
            File outputZip = new File(pathBase, fileName);
            OutputStream output = new FileOutputStream(outputZip);

            byte[] data = new byte[4096];
            int total = 0, count;
            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress((total * 100) / lengthOfFile);
                output.write(data, 0, count);
            }

            output.close();
            input.close();

            return outputZip.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (success) {
            String zipPath = new File(context.getFilesDir(), "assets.zip").getAbsolutePath();
            String outputDir = context.getFilesDir().getAbsolutePath();
            String password = PASSJKPAPA();

            if (unzipEncrypted(zipPath, outputDir, password)) {
                new File(zipPath).delete();
                //Toast.makeText(context, "Loader setup successful!", Toast.LENGTH_LONG).show();
                if (onCompleteCallback != null) {
                    onCompleteCallback.run();
                }
            } else {
                Toast.makeText(context, "Failed to extract ZIP. Check ZIP and password.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "Download failed. Check internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean unzipEncrypted(String zipPath, String outputDir, String password) {
        try {
            ZipFile zipFile = new ZipFile(zipPath, password.toCharArray());
            zipFile.extractAll(outputDir);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
