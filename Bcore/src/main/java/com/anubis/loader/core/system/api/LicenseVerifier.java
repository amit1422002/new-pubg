package com.anubis.loader.core.system.api;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @deprecated Use app module {@code com.anubis.license.LicenseManager} instead.
 */
@Deprecated
public class LicenseVerifier {

    private static final String DATABASE_URL =
            "https://anubisloader-default-rtdb.firebaseio.com";

    public interface Callback {
        void onResult(boolean isValid, String message);
    }

    public static void verify(final String licenseKey, final Callback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String key = licenseKey == null ? "" : licenseKey.trim().toUpperCase();
                    URL url = new URL(DATABASE_URL + "/license/" + key + ".json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream(), StandardCharsets.UTF_8));
                    String line, response = "";
                    while ((line = br.readLine()) != null) response += line;
                    br.close();
                    return response;
                } catch (Exception e) {
                    return "{\"error\":\"" + e.getMessage() + "\"}";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    if (result == null || result.equals("null")) {
                        callback.onResult(false, "Invalid key");
                        return;
                    }
                    JSONObject json = new JSONObject(result);
                    if (!json.optBoolean("active", false)) {
                        callback.onResult(false, "Inactive key");
                        return;
                    }
                    long expiry = json.optLong("expiry", 0L);
                    if (expiry > 0 && System.currentTimeMillis() > expiry) {
                        callback.onResult(false, "Expired");
                        return;
                    }
                    callback.onResult(true, "License OK");
                } catch (Exception e) {
                    callback.onResult(false, e.getMessage());
                }
            }
        }.execute();
    }
}
