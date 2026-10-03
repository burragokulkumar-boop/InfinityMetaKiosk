package com.infinitymeta.kiosk;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

public class ManagementService extends Service {

    private static final long POLL_INTERVAL =
            30_000L;

    /*
     * Registration is also the device heartbeat. Retrying it is important:
     * the first request can fail while Wi-Fi/DNS/Render is waking up.
     */
    private static final long REGISTRATION_INTERVAL =
            30_000L;

    private DeviceRegistration registration;
    private ApiClient apiClient;

    private boolean running = true;
    private boolean registered = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable pollRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!running) {
                        return;
                    }

                    pollCommands();

                    handler.postDelayed(
                            this,
                            POLL_INTERVAL
                    );
                }
            };

    @Override
    public void onCreate() {

        super.onCreate();

        registration =
                new DeviceRegistration(this);

        apiClient =
                new ApiClient();

        registerDevice();

        handler.post(pollRunnable);
    }

    private void registerDevice() {

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "deviceId",
                    registration.getDeviceId()
            );

            data.put(
                    "deviceName",
                    registration.getDeviceName()
            );

            data.put(
                    "model",
                    registration.getModel()
            );

            data.put(
                    "androidVersion",
                    registration.getAndroidVersion()
            );

            data.put(
                    "sdk",
                    registration.getSdkVersion()
            );

            data.put(
                    "packageName",
                    registration.getPackageName()
            );

            data.put(
                    "appVersion",
                    registration.getAppVersion()
            );

            apiClient.registerDevice(
                    data,
                    new ApiClient.Callback() {

                        @Override
                        public void onSuccess(
                                String response
                        ) {
                            registered = true;
                        }

                        @Override
                        public void onError(
                                String error
                        ) {
                            registered = false;
                            scheduleRegistrationRetry();
                        }
                    }
            );

        } catch (Exception ignored) {
        }
    }

    private void scheduleRegistrationRetry() {

        if (!running) {
            return;
        }

        handler.postDelayed(
                this::registerDevice,
                REGISTRATION_INTERVAL
        );
    }

    private void pollCommands() {

        /*
         * If registration failed or the server restarted and forgot this
         * device, do not just keep polling a 404 forever. Re-register first.
         */
        if (!registered) {
            registerDevice();
            return;
        }

        apiClient.getCommands(
                registration.getDeviceId(),
                new ApiClient.Callback() {

                    @Override
                    public void onSuccess(
                            String response
                    ) {

                        registered = true;
                        processCommands(response);
                    }

                    @Override
                    public void onError(
                            String error
                    ) {
                        /*
                         * A 404 means the server restarted and lost the
                         * in-memory device record. Re-register immediately
                         * on the next cycle.
                         */
                        if (error != null && error.contains("HTTP 404")) {
                            registered = false;
                        }
                    }
                }
        );
    }

    private void processCommands(
            String response
    ) {

        try {

            JSONObject root =
                    new JSONObject(response);

            JSONArray commands =
                    root.optJSONArray(
                            "commands"
                    );

            if (commands == null) {
                return;
            }

            for (int i = 0;
                 i < commands.length();
                 i++) {

                executeCommand(
                        commands.getJSONObject(i)
                );
            }

        } catch (Exception ignored) {
        }
    }

    private void executeCommand(
            JSONObject command
    ) {

        try {

            String commandId =
                    command.optString(
                            "id"
                    );

            String type =
                    command.optString(
                            "type"
                    );

            Intent intent =
                    new Intent(
                            this,
                            ManagementCommandReceiver.class
                    );

            intent.setAction(
                    ManagementCommandReceiver
                            .ACTION_COMMAND
            );

            intent.putExtra(
                    ManagementCommandReceiver
                            .EXTRA_COMMAND,
                    type
            );

            intent.putExtra(
                    ManagementCommandReceiver
                            .EXTRA_MESSAGE,
                    command.optString(
                            "message",
                            ""
                    )
            );

            sendBroadcast(intent);

            apiClient.acknowledgeCommand(
                    registration.getDeviceId(),
                    commandId,
                    "SUCCESS",
                    "Command accepted",
                    new ApiClient.Callback() {

                        @Override
                        public void onSuccess(
                                String response
                        ) {
                        }

                        @Override
                        public void onError(
                                String error
                        ) {
                        }
                    }
            );

        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        running = false;

        handler.removeCallbacks(
                pollRunnable
        );

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
