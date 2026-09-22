package com.infinitymeta.kiosk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.UUID;

public class DeviceRegistration {

    private static final String PREFS =
            "infinity_management";

    private static final String DEVICE_ID =
            "device_id";

    private final Context context;
    private final SharedPreferences prefs;

    public DeviceRegistration(Context context) {
        this.context = context.getApplicationContext();

        prefs = this.context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }

    public String getDeviceId() {

        String id = prefs.getString(
                DEVICE_ID,
                null
        );

        if (id == null || id.isEmpty()) {

            id = UUID.randomUUID().toString();

            prefs.edit()
                    .putString(DEVICE_ID, id)
                    .apply();
        }

        return id;
    }

    public String getModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public String getPackageName() {
        return context.getPackageName();
    }

    public String getAppVersion() {

        try {

            return context
                    .getPackageManager()
                    .getPackageInfo(
                            context.getPackageName(),
                            0
                    )
                    .versionName;

        } catch (Exception e) {

            return "unknown";
        }
    }
}
