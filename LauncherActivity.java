package com.infinitymeta.kiosk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends Activity {
    private static final String KIOSK_PREFS = "kiosk_state";
    private static final String KIOSK_DISABLED_KEY = "kiosk_disabled_after_exit";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getSharedPreferences(KIOSK_PREFS, MODE_PRIVATE)
                .edit().putBoolean(KIOSK_DISABLED_KEY, false).apply();

        try {
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, MainActivity.class),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, KioskBootReceiver.class),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
