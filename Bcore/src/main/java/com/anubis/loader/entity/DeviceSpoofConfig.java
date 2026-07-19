package com.anubis.loader.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Per-cloned-app device identifier overrides. Empty fields get a stable generated value when enabled.
 */
public class DeviceSpoofConfig implements Parcelable {

    public boolean enabled;
    public String androidId;
    public String imei;
    public String imsi;
    public String serial;
    public String wifiMac;
    public String model;
    public String brand;

    public DeviceSpoofConfig() {
    }

    public DeviceSpoofConfig(boolean enabled, String androidId, String imei, String imsi, String serial,
                             String wifiMac, String model, String brand) {
        this.enabled = enabled;
        this.androidId = androidId;
        this.imei = imei;
        this.imsi = imsi;
        this.serial = serial;
        this.wifiMac = wifiMac;
        this.model = model;
        this.brand = brand;
    }

    protected DeviceSpoofConfig(Parcel in) {
        enabled = in.readByte() != 0;
        androidId = in.readString();
        imei = in.readString();
        imsi = in.readString();
        serial = in.readString();
        wifiMac = in.readString();
        model = in.readString();
        brand = in.readString();
    }

    public static final Creator<DeviceSpoofConfig> CREATOR = new Creator<DeviceSpoofConfig>() {
        @Override
        public DeviceSpoofConfig createFromParcel(Parcel in) {
            return new DeviceSpoofConfig(in);
        }

        @Override
        public DeviceSpoofConfig[] newArray(int size) {
            return new DeviceSpoofConfig[size];
        }
    };

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("enabled", enabled);
        o.put("androidId", nullToEmpty(androidId));
        o.put("imei", nullToEmpty(imei));
        o.put("imsi", nullToEmpty(imsi));
        o.put("serial", nullToEmpty(serial));
        o.put("wifiMac", nullToEmpty(wifiMac));
        o.put("model", nullToEmpty(model));
        o.put("brand", nullToEmpty(brand));
        return o;
    }

    public static DeviceSpoofConfig fromJson(JSONObject o) throws JSONException {
        DeviceSpoofConfig c = new DeviceSpoofConfig();
        c.enabled = o.optBoolean("enabled", false);
        c.androidId = emptyToNull(o.optString("androidId", ""));
        c.imei = emptyToNull(o.optString("imei", ""));
        c.imsi = emptyToNull(o.optString("imsi", ""));
        c.serial = emptyToNull(o.optString("serial", ""));
        c.wifiMac = emptyToNull(o.optString("wifiMac", ""));
        c.model = emptyToNull(o.optString("model", ""));
        c.brand = emptyToNull(o.optString("brand", ""));
        return c;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return TextUtils.isEmpty(s) ? null : s;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeString(androidId);
        dest.writeString(imei);
        dest.writeString(imsi);
        dest.writeString(serial);
        dest.writeString(wifiMac);
        dest.writeString(model);
        dest.writeString(brand);
    }
}
