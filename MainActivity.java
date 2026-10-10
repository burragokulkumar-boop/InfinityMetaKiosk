package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private static final String EXIT_KIOSK_PIN =
            "0331";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private Dialog kioskDialog;

    private boolean kioskExitRequested = false;

    /*
     * Set after the user successfully exits kiosk mode.
     * This prevents onCreate/onResume from immediately putting
     * the device back into Lock Task mode.
     */
    private static final String KIOSK_PREFS = "kiosk_state";
    private static final String KIOSK_DISABLED_KEY = "kiosk_disabled_after_exit";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        devicePolicyManager =
                (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE
                );

        adminComponent =
                new ComponentName(
                        this,
                        KioskDeviceAdminReceiver.class
                );

        getWindow().setStatusBarColor(
                Color.rgb(103, 184, 213)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        );

        boolean kioskDisabled =
                getSharedPreferences(KIOSK_PREFS, MODE_PRIVATE)
                        .getBoolean(KIOSK_DISABLED_KEY, false);

        /*
         * A normal explicit launch of the app is treated as a request
         * to start kiosk again. Boot launches carry BOOT_START=true,
         * so an intentional Exit remains exited after reboot.
         */
        boolean bootLaunch =
                getIntent() != null
                        && getIntent().getBooleanExtra("BOOT_START", false);

        boolean launcherLaunch = false;
        if (getIntent() != null) {
            launcherLaunch = getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);
        }

        /*
         * Do NOT clear the disabled state on every activity recreation.
         * Android/Samsung can recreate or relaunch the Home activity while
         * we are leaving Lock Task. Clearing the flag here would immediately
         * call startLockTask() again and make Exit Kiosk appear broken.
         *
         * A real launcher tap is an explicit request to open this app again,
         * so only a CATEGORY_LAUNCHER launch re-enables kiosk mode.
         */
        if (launcherLaunch && !bootLaunch) {
            kioskDisabled = false;
            getSharedPreferences(KIOSK_PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KIOSK_DISABLED_KEY, false)
                    .apply();
        }

        if (kioskDisabled) {
            kioskExitRequested = true;
            showSystemBars();
        } else {
            hideSystemBars();
            configureAndEnterKiosk();
        }

        setContentView(new HomeView(this));

        startManagementService();
    }

    private void startManagementService() {
        try {
            Intent managementIntent =
                    new Intent(
                            this,
                            ManagementService.class
                    );

            startService(
                    managementIntent
            );

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!kioskExitRequested) {
            hideSystemBars();
        } else {
            showSystemBars();
        }
    }

    private void configureAndEnterKiosk() {

        if (devicePolicyManager == null ||
                adminComponent == null) {
            return;
        }

        try {

            if (!devicePolicyManager.isDeviceOwnerApp(
                    getPackageName()
            )) {
                return;
            }

            List<String> packages =
                    new ArrayList<>();

            packages.add(getPackageName());
            packages.add(INFINITY_META_PACKAGE);

            devicePolicyManager.setLockTaskPackages(
                    adminComponent,
                    packages.toArray(new String[0])
            );

            /*
             * Enable the Android Home button while keeping
             * Overview/Recent Apps disabled.
             */
            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.P) {

                /*
                 * Keep Home available and allow the top system shade to
                 * reveal time, battery, notifications and quick settings
                 * when the user swipes down from the top edge.
                 */
                devicePolicyManager.setLockTaskFeatures(
                        adminComponent,
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME
                                | DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
                                | DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
                );
            }

            /*
             * Make this application the persistent Home activity.
             * Therefore, pressing Home from Infinity Learn
             * returns to this kiosk screen.
             */
            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                IntentFilter homeFilter =
                        new IntentFilter(
                                Intent.ACTION_MAIN
                        );

                homeFilter.addCategory(
                        Intent.CATEGORY_HOME
                );

                homeFilter.addCategory(
                        Intent.CATEGORY_DEFAULT
                );

                devicePolicyManager
                        .addPersistentPreferredActivity(
                                adminComponent,
                                homeFilter,
                                new ComponentName(
                                        this,
                                        MainActivity.class
                                )
                        );
            }

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                startLockTask();
            }

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Kiosk permission is not configured",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception ignored) {
        }
    }

    private void disableKioskComponents() {
        try {
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, MainActivity.class),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }

        try {
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, KioskBootReceiver.class),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }

        // ManagementCommandReceiver remains enabled for intentional remote
        // ENTER_KIOSK/RESTART_KIOSK commands.
    }

    private void exitKiosk() {

        kioskExitRequested = true;

        // Persist the disabled state before touching Device Owner policy.
        getSharedPreferences(KIOSK_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KIOSK_DISABLED_KEY, true)
                .commit();

        /*
         * The activity itself must leave Lock Task first. Android documents
         * stopLockTask() as the operation that actually ends the current
         * Lock Task session. Do not call setLockTaskFeatures(0) first:
         * on Android 14+ lock-task features and the package allowlist are
         * one policy, so changing that policy during the exit transition
         * can interfere with the session we are trying to stop.
         */
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
            }
        } catch (Exception ignored) {
        }

        /*
         * Now remove the kiosk policies. Removing this package from the
         * allowlist also finishes any remaining locked task belonging to it.
         */
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && devicePolicyManager != null
                    && adminComponent != null) {

                devicePolicyManager.setLockTaskPackages(
                        adminComponent,
                        new String[0]
                );
            }
        } catch (Exception ignored) {
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && devicePolicyManager != null
                    && adminComponent != null) {

                devicePolicyManager
                        .clearPackagePersistentPreferredActivities(
                                adminComponent,
                                getPackageName()
                        );
            }
        } catch (Exception ignored) {
        }

        showSystemBars();

        Toast.makeText(
                this,
                "Kiosk mode exited",
                Toast.LENGTH_SHORT
        ).show();

        /*
         * Resolve a HOME activity other than this kiosk. We deliberately
         * do not use resolveActivity() here because this app is itself a
         * HOME activity and can still be returned while Android is updating
         * the persistent-home policy.
         */
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addCategory(Intent.CATEGORY_DEFAULT);

            List<android.content.pm.ResolveInfo> homes =
                    getPackageManager().queryIntentActivities(
                            homeIntent,
                            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    );

            android.content.pm.ResolveInfo selectedHome = null;

            for (android.content.pm.ResolveInfo candidate : homes) {
                if (candidate == null
                        || candidate.activityInfo == null) {
                    continue;
                }

                if (!getPackageName().equals(
                        candidate.activityInfo.packageName
                )) {
                    selectedHome = candidate;
                    break;
                }
            }

            if (selectedHome != null) {
                ComponentName homeComponent =
                        new ComponentName(
                                selectedHome.activityInfo.packageName,
                                selectedHome.activityInfo.name
                        );

                Intent launchHome =
                        new Intent(Intent.ACTION_MAIN);

                launchHome.addCategory(Intent.CATEGORY_HOME);
                launchHome.addCategory(Intent.CATEGORY_DEFAULT);
                launchHome.setComponent(homeComponent);
                launchHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(launchHome);
            }
        } catch (Exception ignored) {
        }

        // Remove the kiosk activity itself from HOME resolution.
        disableKioskComponents();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        } catch (Exception ignored) {
            finish();
        }
    }

    private void showSystemBars() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {

                Window window = getWindow();

                WindowInsetsController controller =
                        window.getInsetsController();

                if (controller != null) {

                    controller.show(
                            WindowInsets.Type.statusBars()
                                    |
                            WindowInsets.Type.navigationBars()
                    );
                }

            } else {

                getWindow()
                        .getDecorView()
                        .setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        );
            }

        } catch (Exception ignored) {
        }
    }

    /*
     * Hide only the STATUS BAR.
     *
     * The navigation bar is deliberately left visible
     * so Android can show the Home button.
     */
    private void hideSystemBars() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {

                Window window = getWindow();

                WindowInsetsController controller =
                        window.getInsetsController();

                if (controller != null) {

                    /*
                     * True immersive kiosk mode:
                     * hide both the status bar and the navigation bar.
                     * Android will reveal them temporarily when the user
                     * swipes from the system-bar edge.
                     */
                    controller.hide(
                            WindowInsets.Type.statusBars()
                                    | WindowInsets.Type.navigationBars()
                    );

                    controller.setSystemBarsBehavior(
                            WindowInsetsController
                                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }

            } else {

                getWindow()
                        .getDecorView()
                        .setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                        |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        );
            }

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null) {
            String managementAction =
                    intent.getStringExtra("management_action");

            if ("com.infinitymeta.kiosk.EXIT_KIOSK".equals(
                    managementAction
            )) {
                exitKiosk();
            } else if ("com.infinitymeta.kiosk.ENTER_KIOSK".equals(
                    managementAction
            )) {
                getSharedPreferences(KIOSK_PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KIOSK_DISABLED_KEY, false)
                        .apply();
                kioskExitRequested = false;
                hideSystemBars();
                configureAndEnterKiosk();
            } else if ("com.infinitymeta.kiosk.CLEAR_APP_DATA".equals(
                    managementAction
            )) {
                clearInfinityLearnData();
            } else if ("com.infinitymeta.kiosk.UPDATE_APP".equals(
                    managementAction
            )) {
                String updateJson =
                        intent.getStringExtra("update_json");
                if (updateJson != null) {
                    try {
                        org.json.JSONObject update =
                                new org.json.JSONObject(updateJson);
                        UpdateManager.start(
                                this,
                                update.optString("apkUrl", ""),
                                update.optInt("versionCode", -1),
                                update.optString("versionName", ""),
                                update.optString("sha256", "")
                        );
                    } catch (Exception e) {
                        Toast.makeText(
                                this,
                                "Invalid update package information",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        /*
         * Back remains blocked.
         * Home is allowed by Lock Task policy.
         */
    }

    private int dp(float n) {

        return (int) (
                n *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    private TextView label(
            String text,
            float size,
            int color
    ) {

        TextView t =
                new TextView(this);

        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(
                Gravity.CENTER_VERTICAL
        );

        return t;
    }

    private GradientDrawable rounded(
            int radius,
            int color
    ) {

        GradientDrawable g =
                new GradientDrawable();

        g.setColor(color);

        g.setCornerRadius(
                dp(radius)
        );

        return g;
    }

    private void showKioskMenu() {

        if (kioskDialog != null &&
                kioskDialog.isShowing()) {

            return;
        }

        kioskDialog =
                new Dialog(this);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(26),
                dp(14),
                dp(26),
                dp(8)
        );

        box.setBackground(
                rounded(
                        32,
                        Color.WHITE
                )
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            box.setClipToOutline(true);
            box.setElevation(dp(2));
        }

        TextView title =
                label(
                        "Kiosk",
                        20,
                        Color.rgb(
                                45,
                                45,
                                45
                        )
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        divider(box);

        menuRow(
                box,
                "Settings",
                v -> showSettings()
        );

        menuRow(
                box,
                "Exit Kiosk",
                v -> confirmExit()
        );

        menuRow(
                box,
                "Support",
                v -> Toast.makeText(
                        MainActivity.this,
                        "Support",
                        Toast.LENGTH_SHORT
                ).show()
        );

        menuRow(
                box,
                "Clear app data",
                v -> confirmClearAppData()
        );

        menuRow(
                box,
                "Open source",
                v -> Toast.makeText(
                        MainActivity.this,
                        "Open source",
                        Toast.LENGTH_SHORT
                ).show()
        );

        divider(box);

        TextView version =
                label(
                        "Version\n1.0.6",
                        16,
                        Color.rgb(
                                45,
                                45,
                                45
                        )
                );

        version.setPadding(
                0,
                dp(10),
                0,
                0
        );

        box.addView(
                version,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        TextView date =
                label(
                        "Installed date\n260808",
                        16,
                        Color.rgb(
                                45,
                                45,
                                45
                        )
                );

        box.addView(
                date,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        TextView done =
                label(
                        "Done",
                        17,
                        Color.rgb(
                                40,
                                40,
                                40
                        )
                );

        done.setGravity(
                Gravity.CENTER
        );

        done.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        done.setOnClickListener(v -> {

            if (kioskDialog != null &&
                    kioskDialog.isShowing()) {

                kioskDialog.dismiss();
            }
        });

        box.addView(
                done,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        kioskDialog.setContentView(box);

        Window w =
                kioskDialog.getWindow();

        if (w != null) {

            w.setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        kioskDialog.setCanceledOnTouchOutside(
                true
        );

        kioskDialog.show();

        w = kioskDialog.getWindow();

        if (w != null) {

            /*
             * Kiosk UI sizing.
             *
             * Use visual scaling for the actual panel because Dialog
             * WRAP_CONTENT can recalculate its window bounds after
             * setAttributes() on Samsung/Android.
             *
             * 90% width = 5% inward from each side.
             * 105% height = 5% longer vertically.
             */
            int width =
                    Math.min(
                            dp(610),
                            (int) (
                                    getResources()
                                            .getDisplayMetrics()
                                            .widthPixels
                                            * 0.78f
                            )
                    );

            WindowManager.LayoutParams params =
                    w.getAttributes();

            params.width = width;
            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.gravity =
                    Gravity.BOTTOM |
                    Gravity.CENTER_HORIZONTAL;

            params.y = dp(70);

            w.setAttributes(params);

            // Do not scale the panel after layout.
            // Scaling a WRAP_CONTENT dialog can clip the rounded corners.

        }
    }

    private void divider(
            LinearLayout box
    ) {

        View line =
                new View(this);

        line.setBackgroundColor(
                Color.rgb(
                        205,
                        205,
                        205
                )
        );

        box.addView(
                line,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(1)
                )
        );
    }

    private void menuRow(
            LinearLayout box,
            String text,
            View.OnClickListener action
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView left =
                label(
                        text,
                        17,
                        Color.rgb(
                                35,
                                35,
                                35
                        )
                );

        TextView right =
                label(
                        "View",
                        17,
                        Color.rgb(
                                0,
                                0,
                                238
                        )
                );

        right.setGravity(
                Gravity.CENTER
        );

        row.addView(
                left,
                new LinearLayout.LayoutParams(
                        0,
                        dp(68),
                        1
                )
        );

        row.addView(
                right,
                new LinearLayout.LayoutParams(
                        dp(72),
                        dp(68)
                )
        );

        row.setOnClickListener(
                action
        );

        box.addView(row);
    }

    /*
     * SETTINGS POPUP
     */
    private void showSettings() {

        final Dialog settingsDialog =
                new Dialog(this);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(26),
                dp(14),
                dp(26),
                dp(8)
        );

        box.setBackground(
                rounded(
                        28,
                        Color.WHITE
                )
        );

        TextView title =
                label(
                        "Settings",
                        20,
                        Color.rgb(
                                45,
                                45,
                                45
                        )
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        divider(box);

        /*
         * Wi-Fi
         */
        settingsRow(
                box,
                "Wi-Fi",
                "On",
                v -> openWifiSettings()
        );

        /*
         * Auto-rotate
         */
        settingsRow(
                box,
                "Auto-rotate",
                getAutoRotateState(),
                v -> openAutoRotateSettings()
        );

        /*
         * Display
         */
        settingsRow(
                box,
                "Display",
                "",
                v -> openDisplaySettings()
        );

        divider(box);

        TextView done =
                label(
                        "Done",
                        17,
                        Color.rgb(
                                40,
                                40,
                                40
                        )
                );

        done.setGravity(
                Gravity.CENTER
        );

        done.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        done.setOnClickListener(
                v -> settingsDialog.dismiss()
        );

        box.addView(
                done,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(65)
                )
        );

        settingsDialog.setContentView(
                box
        );

        Window w =
                settingsDialog.getWindow();

        if (w != null) {

            w.setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        settingsDialog.show();

        w = settingsDialog.getWindow();

        if (w != null) {

            int width =
                    Math.min(
                            dp(610),
                            (int) (
                                    getResources()
                                            .getDisplayMetrics()
                                            .widthPixels
                                            * 0.78f
                            )
                    );

            WindowManager.LayoutParams params =
                    w.getAttributes();

            params.width = width;
            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.gravity =
                    Gravity.BOTTOM |
                    Gravity.CENTER_HORIZONTAL;

            // Keep the Settings panel slightly above the bottom edge.
            params.y = dp(70);

            w.setAttributes(params);
        }
    }

    private void settingsRow(
            LinearLayout box,
            String name,
            String value,
            View.OnClickListener action
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView text =
                label(
                        name + "\n" + value,
                        17,
                        Color.rgb(
                                35,
                                35,
                                35
                        )
                );

        row.addView(
                text,
                new LinearLayout.LayoutParams(
                        0,
                        dp(82),
                        1
                )
        );

        TextView arrow =
                label(
                        "›",
                        28,
                        Color.rgb(
                                70,
                                70,
                                70
                        )
                );

        arrow.setGravity(
                Gravity.CENTER
        );

        row.addView(
                arrow,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(82)
                )
        );

        row.setOnClickListener(
                action
        );

        box.addView(row);
    }

    private String getAutoRotateState() {

        try {

            int rotation =
                    Settings.System.getInt(
                            getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION,
                            0
                    );

            return rotation == 1
                    ? "On"
                    : "Off";

        } catch (Exception e) {

            return "On";
        }
    }

    private void openWifiSettings() {

        try {

            startActivity(
                    new Intent(
                            Settings.ACTION_WIFI_SETTINGS
                    )
            );

        } catch (Exception e) {

            startActivity(
                    new Intent(
                            Settings.ACTION_SETTINGS
                    )
            );
        }
    }

    private void openAutoRotateSettings() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.JELLY_BEAN_MR1) {

                startActivity(
                        new Intent(
                                Settings.ACTION_DISPLAY_SETTINGS
                        )
                );

            } else {

                openDisplaySettings();
            }

        } catch (Exception e) {

            openDisplaySettings();
        }
    }

    private void openDisplaySettings() {

        try {

            startActivity(
                    new Intent(
                            Settings.ACTION_DISPLAY_SETTINGS
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open Display settings",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * CLEAR INFINITY LEARN APP DATA
     */
    private void confirmClearAppData() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Clear app data"
                )
                .setMessage(
                        "This will clear all data for Infinity Learn on this tablet. Continue?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Clear",
                        (dialog, which) ->
                                clearInfinityLearnData()
                )
                .show();
    }

    private void clearInfinityLearnData() {

        if (devicePolicyManager == null ||
                adminComponent == null) {

            Toast.makeText(
                    this,
                    "Kiosk administration is unavailable",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.P) {

            Toast.makeText(
                    this,
                    "Clear app data requires Android 9 or newer",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            devicePolicyManager.clearApplicationUserData(
                    adminComponent,
                    INFINITY_META_PACKAGE,
                    getMainExecutor(),
                    (packageName, successful) -> {

                        if (successful) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Infinity Learn app data cleared",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to clear Infinity Learn data",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Device Owner permission is required",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to clear app data",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * EXIT KIOSK PIN
     */
    private void confirmExit() {

        /*
         * Custom Exit Kiosk dialog.
         *
         * This intentionally follows the supplied reference:
         * - large white rounded panel
         * - Exit Kiosk title
         * - admin-code description
         * - simple underline code field
         * - User ID / Device name / IMEI / Tenant ID information
         * - bottom Cancel / Exit Kiosk actions
         */
        final Dialog dialog = new Dialog(this);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(28),
                dp(18),
                dp(28),
                dp(8)
        );

        box.setBackground(
                rounded(
                        28,
                        Color.WHITE
                )
        );

        TextView title =
                label(
                        "Exit Kiosk",
                        20,
                        Color.rgb(48, 48, 48)
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER_VERTICAL
        );

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        divider(box);

        TextView description =
                label(
                        "Enter the code provided by your IT admin.",
                        16,
                        Color.rgb(105, 105, 105)
                );

        description.setGravity(
                Gravity.CENTER_VERTICAL
        );

        box.addView(
                description,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        final EditText pinInput =
                new EditText(this);

        pinInput.setHint(
                "Enter code"
        );

        pinInput.setHintTextColor(
                Color.rgb(105, 145, 210)
        );

        pinInput.setTextColor(
                Color.rgb(55, 55, 55)
        );

        pinInput.setTextSize(16);

        pinInput.setSingleLine(true);

        pinInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        pinInput.setPadding(
                0,
                0,
                0,
                0
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pinInput.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(145, 145, 145)
                    )
            );
        }

        box.addView(
                pinInput,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        String deviceName =
                new DeviceRegistration(this)
                        .getDeviceName();

        if (deviceName == null ||
                deviceName.trim().isEmpty()) {
            deviceName = "268030624_Android_1";
        }

        addExitInfoRow(
                box,
                "User ID",
                "268030624"
        );

        addExitInfoRow(
                box,
                "Device name",
                deviceName
        );

        addExitInfoRow(
                box,
                "IMEI",
                "-"
        );

        addExitInfoRow(
                box,
                "Tenant ID",
                "srichaitanya.net"
        );

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttons.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView cancel =
                label(
                        "Cancel",
                        18,
                        Color.rgb(48, 48, 48)
                );

        cancel.setGravity(
                Gravity.CENTER
        );

        cancel.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        cancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        buttons.addView(
                cancel,
                new LinearLayout.LayoutParams(
                        0,
                        dp(64),
                        1
                )
        );

        final TextView exit =
                label(
                        "Exit Kiosk",
                        18,
                        Color.rgb(190, 190, 190)
                );

        exit.setGravity(
                Gravity.CENTER
        );

        exit.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        exit.setEnabled(false);

        exit.setOnClickListener(
                v -> {

                    String enteredPin =
                            pinInput
                                    .getText()
                                    .toString()
                                    .trim();

                    if (EXIT_KIOSK_PIN.equals(
                            enteredPin
                    )) {

                        dialog.dismiss();

                        if (kioskDialog != null &&
                                kioskDialog.isShowing()) {

                            kioskDialog.dismiss();
                        }

                        exitKiosk();

                    } else {

                        pinInput.setError(
                                "Incorrect PIN"
                        );

                        pinInput.requestFocus();
                    }
                }
        );

        buttons.addView(
                exit,
                new LinearLayout.LayoutParams(
                        0,
                        dp(64),
                        1
                )
        );

        box.addView(
                buttons
        );

        pinInput.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        boolean valid =
                                EXIT_KIOSK_PIN.equals(
                                        s.toString()
                                );

                        exit.setEnabled(valid);

                        exit.setTextColor(
                                valid
                                        ? Color.rgb(48, 48, 48)
                                        : Color.rgb(190, 190, 190)
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {
                    }
                }
        );

        dialog.setContentView(box);

        Window firstWindow =
                dialog.getWindow();

        if (firstWindow != null) {
            firstWindow.setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        dialog.setCanceledOnTouchOutside(true);

        dialog.show();

        Window window =
                dialog.getWindow();

        if (window != null) {

            int width =
                    Math.min(
                            dp(780),
                            (int) (
                                    getResources()
                                            .getDisplayMetrics()
                                            .widthPixels
                                            * 0.78f
                            )
                    );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width = width;

            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.gravity =
                    Gravity.BOTTOM |
                    Gravity.CENTER_HORIZONTAL;

            // Keep Exit Kiosk lower, matching the reference dialog position.
            params.y = dp(55);

            window.setAttributes(params);
        }

        if (window != null) {
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );
        }
    }

    private void addExitInfoRow(
            LinearLayout box,
            String titleText,
            String valueText
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView title =
                label(
                        titleText,
                        18,
                        Color.rgb(55, 55, 55)
                );

        title.setGravity(
                Gravity.BOTTOM
        );

        TextView value =
                label(
                        valueText,
                        16,
                        Color.rgb(125, 125, 125)
                );

        value.setGravity(
                Gravity.TOP
        );

        row.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(31)
                )
        );

        row.addView(
                value,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(31)
                )
        );

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(72)
                );

        box.addView(
                row,
                rowParams
        );
    }

    private void openInfinityMeta() {

        try {

            android.content.pm.PackageManager pm =
                    getPackageManager();

            Intent launchIntent =
                    pm.getLaunchIntentForPackage(
                            INFINITY_META_PACKAGE
                    );

            if (launchIntent == null) {

                Toast.makeText(
                        this,
                        "Infinity Learn app is not installed",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(
                    launchIntent
            );

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Infinity Learn is blocked by kiosk mode",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open Infinity Learn",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private class HomeView
            extends View {

        private Bitmap wallpaper;

        private final Paint paint =
                new Paint(
                        Paint.FILTER_BITMAP_FLAG
                );

        HomeView(
                Context context
        ) {

            super(context);

            wallpaper =
                    BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.infinity_wallpaper
                    );

            setFocusable(true);
            setClickable(true);
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {

            super.onDraw(canvas);

            if (wallpaper != null &&
                    !wallpaper.isRecycled()) {

                canvas.drawBitmap(
                        wallpaper,
                        null,
                        new Rect(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        ),
                        paint
                );

            } else {

                canvas.drawColor(
                        Color.rgb(
                                210,
                                240,
                                250
                        )
                );
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event == null) {
                return true;
            }

            if (event.getAction() ==
                    MotionEvent.ACTION_UP) {

                float x =
                        event.getX();

                float y =
                        event.getY();

                /*
                 * Bottom-left i logo
                 */
                if (
                        x < getWidth() * 0.13f
                                &&
                        y > getHeight() * 0.88f
                ) {

                    showKioskMenu();

                    return true;
                }

                /*
                 * Infinity Meta shortcut
                 */
                if (
                        x < getWidth() * 0.32f
                                &&
                        y < getHeight() * 0.30f
                ) {

                    openInfinityMeta();

                    return true;
                }
            }

            return true;
        }
    }
}
