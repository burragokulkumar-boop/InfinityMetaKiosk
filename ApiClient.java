package com.infinitymeta.kiosk;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    /*
     * CHANGE THIS LATER
     */
    private static final String SERVER_URL =
            "https://YOUR-SERVER-DOMAIN/api";

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface Callback {

        void onSuccess(String response);

        void onError(String error);
    }

    public void registerDevice(
            JSONObject data,
            Callback callback
    ) {
        post(
                "/devices/register",
                data,
                callback
        );
    }

    public void getCommands(
            String deviceId,
            Callback callback
    ) {

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "deviceId",
                    deviceId
            );

            post(
                    "/commands/poll",
                    data,
                    callback
            );

        } catch (Exception e) {

            callback.onError(
                    e.getMessage()
            );
        }
    }

    public void acknowledgeCommand(
            String deviceId,
            String commandId,
            String status,
            String message,
            Callback callback
    ) {

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "deviceId",
                    deviceId
            );

            data.put(
                    "commandId",
                    commandId
            );

            data.put(
                    "status",
                    status
            );

            data.put(
                    "message",
                    message
            );

            post(
                    "/commands/ack",
                    data,
                    callback
            );

        } catch (Exception e) {

            callback.onError(
                    e.getMessage()
            );
        }
    }

    private void post(
            String endpoint,
            JSONObject body,
            Callback callback
    ) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                        SERVER_URL + endpoint
                );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "POST"
                );

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        15000
                );

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                byte[] bytes =
                        body.toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                );

                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(bytes);
                }

                int code =
                        connection.getResponseCode();

                InputStream stream =
                        code >= 200 &&
                        code < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String response =
                        readStream(stream);

                if (code >= 200 &&
                        code < 300) {

                    mainHandler.post(() ->
                            callback.onSuccess(
                                    response
                            )
                    );

                } else {

                    mainHandler.post(() ->
                            callback.onError(
                                    "HTTP " +
                                    code +
                                    ": " +
                                    response
                            )
                    );
                }

            } catch (Exception e) {

                mainHandler.post(() ->
                        callback.onError(
                                e.getMessage()
                        )
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String readStream(
            InputStream stream
    ) throws Exception {

        if (stream == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while ((line =
                reader.readLine()) != null) {

            result.append(line);
        }

        reader.close();

        return result.toString();
    }
}
