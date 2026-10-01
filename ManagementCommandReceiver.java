package com.infinitymeta.kiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ManagementCommandReceiver
        extends BroadcastReceiver {

    public static final String ACTION_COMMAND =
            "com.infinitymeta.kiosk.MANAGEMENT_COMMAND";

    public static final String EXTRA_COMMAND =
            "command";

    public static final String EXTRA_MESSAGE =
            "message";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        if (intent == null) {
            return;
        }

        String command =
                intent.getStringExtra(
                        EXTRA_COMMAND
                );

        if (command == null) {
            return;
        }

        switch (command) {

            case "PING":

                Toast.makeText(
                        context,
                        "Management connection OK",
                        Toast.LENGTH_SHORT
                ).show();

                break;

            case "SHOW_MESSAGE":

                String message =
                        intent.getStringExtra(
                                EXTRA_MESSAGE
                        );

                Toast.makeText(
                        context,
                        message == null
                                ? ""
                                : message,
                        Toast.LENGTH_LONG
                ).show();

                break;

            case "ENTER_KIOSK":
            case "RESTART_KIOSK":

                Intent main =
                        new Intent(
                                context,
                                MainActivity.class
                        );

                main.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                context.startActivity(main);

                break;

            case "CLEAR_APP_DATA":

                Intent clearData =
                        new Intent(
                                context,
                                MainActivity.class
                        );

                clearData.putExtra(
                        "management_action",
                        "com.infinitymeta.kiosk.CLEAR_APP_DATA"
                );

                clearData.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                context.startActivity(clearData);

                break;

            case "REMOVE_KIOSK_APP":

                Intent remove =
                        new Intent(
                                context,
                                MainActivity.class
                        );

                remove.putExtra(
                        "management_action",
                        "com.infinitymeta.kiosk.REMOVE_KIOSK_APP"
                );

                remove.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                context.startActivity(remove);

                break;

            case "EXIT_KIOSK":

                Intent exit =
                        new Intent(
                                context,
                                MainActivity.class
                        );

                exit.putExtra(
                        "management_action",
                        "com.infinitymeta.kiosk.EXIT_KIOSK"
                );

                exit.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                context.startActivity(exit);

                break;

            default:

                Toast.makeText(
                        context,
                        "Unknown command: " + command,
                        Toast.LENGTH_SHORT
                ).show();

                break;
        }
    }
}
