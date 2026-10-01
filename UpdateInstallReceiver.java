package com.infinitymeta.kiosk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

public class UpdateInstallReceiver extends BroadcastReceiver {

    public static final String ACTION_INSTALL_RESULT =
            "com.infinitymeta.kiosk.UPDATE_INSTALL_RESULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        int status =
                intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                );

        String message =
                intent.getStringExtra(
                        PackageInstaller.EXTRA_STATUS_MESSAGE
                );

        if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(
                    context,
                    "Infinity Meta Kiosk updated successfully",
                    Toast.LENGTH_LONG
            ).show();

            Intent main =
                    new Intent(context, MainActivity.class);

            main.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            context.startActivity(main);
            return;
        }

        if (status ==
                PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation =
                    intent.getParcelableExtra(
                            Intent.EXTRA_INTENT
                    );

            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            }

            Toast.makeText(
                    context,
                    "Android requires update approval on this device",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(
                context,
                "Kiosk update failed: " +
                        (message == null ? "unknown error" : message),
                Toast.LENGTH_LONG
        ).show();
    }
}
