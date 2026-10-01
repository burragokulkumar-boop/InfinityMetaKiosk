package com.infinitymeta.kiosk;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class UpdateManager {

    private static final String TAG = "InfinityMetaUpdate";
    private static final String APK_FILE = "infinitymeta-update.apk";

    private UpdateManager() {}

    public static void start(
            Context context,
            String apkUrl,
            int expectedVersionCode,
            String expectedVersionName,
            String expectedSha256
    ) {
        if (apkUrl == null || apkUrl.trim().isEmpty()) {
            show(context, "Update failed: APK URL is missing");
            return;
        }

        if (!apkUrl.startsWith("https://")) {
            show(context, "Update failed: HTTPS is required");
            return;
        }

        new Thread(() -> {
            File apk = null;
            try {
                apk = download(context, apkUrl);
                verifySha256(apk, expectedSha256);

                PackageManager pm = context.getPackageManager();
                android.content.pm.PackageInfo info =
                        pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);

                if (info == null || info.applicationInfo == null) {
                    throw new Exception("Downloaded file is not a valid APK");
                }

                if (!context.getPackageName().equals(info.packageName)) {
                    throw new Exception("APK package does not match kiosk package");
                }

                long downloadedVersionCode =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                ? info.getLongVersionCode()
                                : info.versionCode;

                if (downloadedVersionCode <= getCurrentVersionCode(context)) {
                    throw new Exception("Downloaded APK is not newer than the installed version");
                }

                if (expectedVersionCode > 0 &&
                        downloadedVersionCode != expectedVersionCode) {
                    throw new Exception("APK version code does not match update metadata");
                }

                if (expectedVersionName != null &&
                        !expectedVersionName.isEmpty() &&
                        !expectedVersionName.equals(info.versionName)) {
                    throw new Exception("APK version name does not match update metadata");
                }

                install(context, apk);
            } catch (Exception e) {
                String message = e.getMessage();
                show(context, "Update failed: " +
                        (message == null ? e.getClass().getSimpleName() : message));
                if (apk != null) {
                    //noinspection ResultOfMethodCallIgnored
                    apk.delete();
                }
            }
        }, "InfinityMeta-Update").start();
    }

    private static long getCurrentVersionCode(Context context) throws Exception {
        android.content.pm.PackageInfo info =
                context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? info.getLongVersionCode()
                : info.versionCode;
    }

    private static File download(Context context, String apkUrl) throws Exception {
        URL url = new URL(apkUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive");

        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("APK download HTTP " + code);
            }

            File apk = new File(context.getCacheDir(), APK_FILE);
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(apk)) {

                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }

            if (!apk.isFile() || apk.length() == 0) {
                throw new Exception("Downloaded APK is empty");
            }

            return apk;
        } finally {
            connection.disconnect();
        }
    }

    private static void verifySha256(File file, String expected) throws Exception {
        if (expected == null || expected.trim().isEmpty()) {
            throw new Exception("SHA-256 is missing from update metadata");
        }

        String wanted = expected.trim().toLowerCase();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        StringBuilder actual = new StringBuilder();
        for (byte b : digest.digest()) {
            actual.append(String.format("%02x", b));
        }

        if (!actual.toString().equals(wanted)) {
            throw new Exception("APK SHA-256 verification failed");
        }
    }

    private static void install(Context context, File apk) throws Exception {
        PackageInstaller installer =
                context.getPackageManager().getPackageInstaller();

        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        params.setAppPackageName(context.getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(
                    PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }

        int sessionId = installer.createSession(params);
        PackageInstaller.Session session = installer.openSession(sessionId);

        try {
            try (InputStream input = new FileInputStream(apk);
                 java.io.OutputStream output =
                         session.openWrite("base.apk", 0, apk.length())) {

                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                session.fsync(output);
            }

            Intent intent =
                    new Intent(context, UpdateInstallReceiver.class)
                            .setAction(UpdateInstallReceiver.ACTION_INSTALL_RESULT)
                            .setPackage(context.getPackageName());

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            sessionId,
                            intent,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    ? PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_MUTABLE
                                    : PendingIntent.FLAG_UPDATE_CURRENT);

            session.commit(pendingIntent.getIntentSender());
        } finally {
            session.close();
        }

        show(context, "Kiosk update downloaded. Installing…");
    }

    private static void show(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(
                        context.getApplicationContext(),
                        message,
                        Toast.LENGTH_LONG
                ).show()
        );
    }
}
