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

            /*
             * If the user intentionally exited kiosk mode, do not
             * relaunch the kiosk automatically after reboot.
             */
            boolean kioskDisabled =
                    context.getSharedPreferences(
                            "kiosk_state",
                            Context.MODE_PRIVATE
                    ).getBoolean(
                            "kiosk_disabled_after_exit",
                            false
                    );

            if (kioskDisabled) {
                return;
            }

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
