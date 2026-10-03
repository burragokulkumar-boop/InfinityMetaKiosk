package com.infinitymeta.kiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class KioskBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            Intent kioskIntent =
                    new Intent(context, MainActivity.class);

            /*
             * Mark this as a boot launch so a previous intentional
             * Exit Kiosk state is respected after reboot.
             */
            kioskIntent.putExtra("BOOT_START", true);

            kioskIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            context.startActivity(kioskIntent);
        }
    }
}
