package lauresprojects.com.recorder.Component;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import lauresprojects.com.recorder.R;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.util.Log;

public class DownloadZip extends AsyncTask<String, Integer, String> {

    private Context context;
    private Dialog progressDialog;
    private TextView progressText;
    private ProgressBar progressBar;
    private String fileName;

    public DownloadZip(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new Dialog(context);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setCancelable(false);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(70, 60, 70, 60);

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        progressText = new TextView(context);
        progressText.setText("Downloading... 0%");
        progressText.setGravity(Gravity.CENTER);

        layout.addView(progressBar);
        layout.addView(progressText);

        progressDialog.setContentView(layout);
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {

        try {
            String downloadUrl = params[0];
            fileName = params[1];

            File dir = context.getFilesDir();
            File outputFile = new File(dir, fileName);

            HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "ERR: " + connection.getResponseCode();
            }

            int fileLength = connection.getContentLength();

            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192];
            long total = 0;
            int count;

            while ((count = input.read(buffer)) != -1) {
                total += count;

                if (fileLength > 0) {
                    publishProgress((int) (total * 100 / fileLength));
                }

                output.write(buffer, 0, count);
            }

            output.close();
            input.close();

            // unzip if needed
            if (fileName.endsWith(".zip")) {
                publishProgress(-1);
                unzip(outputFile, dir);
                outputFile.delete();
            }

            return "OK";

        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }

    private void unzip(File zipFile, File targetDirectory) throws Exception {

        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile))
        );

        ZipEntry ze;
        byte[] buffer = new byte[8192];

        while ((ze = zis.getNextEntry()) != null) {

            File file = new File(targetDirectory, ze.getName());

            if (ze.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(file);

                int count;
                while ((count = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }

                fos.close();
            }

            zis.closeEntry();
        }

        zis.close();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        int progress = values[0];

        if (progress == -1) {
            progressText.setText("Extracting...");
        } else {
            progressBar.setProgress(progress);
            progressText.setText("Downloading... " + progress + "%");
        }
    }

    @Override
    protected void onPostExecute(String result) {

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
    }
}