package com.infinitymeta.kiosk;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class KioskDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(
            Context context,
            Intent intent
    ) {
        Toast.makeText(
                context,
                "Infinity Meta Kiosk administrator enabled",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDisabled(
            Context context,
            Intent intent
    ) {
        Toast.makeText(
                context,
                "Infinity Meta Kiosk administrator disabled",
                Toast.LENGTH_SHORT
        ).show();
    }
}
