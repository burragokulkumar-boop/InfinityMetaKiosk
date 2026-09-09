package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private Dialog kioskDialog;

    /*
     * True = kiosk should be active.
     *
     * When the user presses Exit Kiosk, this becomes false.
     * This prevents onResume() from immediately starting
     * Lock Task again.
     */
    private boolean kioskEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.rgb(103, 184, 213)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        );

        devicePolicyManager =
                (DevicePolicyManager)
                        getSystemService(
                                Context.DEVICE_POLICY_SERVICE
                        );

        adminComponent =
                new ComponentName(
                        this,
                        KioskDeviceAdminReceiver.class
                );

        hideSystemBars();

        setContentView(
                new HomeView(this)
        );

        /*
         * Configure Lock Task only when this application
         * has actually become Device Owner.
         *
         * This prevents crashes when the app is installed
         * normally without Device Owner privileges.
         */
        configureKiosk();

        /*
         * Start kiosk mode.
         */
        enterKiosk();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();

        /*
         * Do NOT automatically re-enter kiosk after the
         * user has deliberately pressed Exit Kiosk.
         */
        if (kioskEnabled) {
            enterKiosk();
        }
    }

    /*
     * Configure Android Lock Task policy.
     */
    private void configureKiosk() {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && devicePolicyManager != null
                    && devicePolicyManager.isDeviceOwnerApp(
                            getPackageName()
                    )) {

                /*
                 * Allow both this kiosk application and
                 * the real Infinity Meta application.
                 *
                 * This is important because Infinity Meta
                 * is a separate application.
                 */
                devicePolicyManager.setLockTaskPackages(
                        adminComponent,
                        new String[]{
                                getPackageName(),
                                INFINITY_META_PACKAGE
                        }
                );

                /*
                 * On Android 9/API 28 and newer, disable
                 * extra system features while locked.
                 *
                 * The kiosk can still exit through our
                 * own Exit Kiosk button.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                    devicePolicyManager.setLockTaskFeatures(
                            adminComponent,
                            DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    );
                }
            }

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Kiosk policy is not configured yet",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception ignored) {
        }
    }

    /*
     * Hide Android navigation/status bars.
     *
     * Uses the older system UI flags deliberately so that
     * this file does not depend on WindowInsetsController.
     */
    private void hideSystemBars() {

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
    }

    /*
     * Enter Android Lock Task mode.
     *
     * If Device Owner has not been configured yet,
     * this safely does nothing instead of crashing.
     */
    private void enterKiosk() {

        if (!kioskEnabled) {
            return;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (devicePolicyManager != null
                        && devicePolicyManager.isDeviceOwnerApp(
                                getPackageName()
                        )) {

                    startLockTask();
                }
            }

        } catch (SecurityException ignored) {

        } catch (IllegalStateException ignored) {
        }
    }

    /*
     * Exit Android Lock Task mode.
     */
    private void exitKiosk() {

        kioskEnabled = false;

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                stopLockTask();
            }

        } catch (Exception ignored) {
        }

        hideSystemBars();

        Toast.makeText(
                this,
                "Kiosk mode exited",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onBackPressed() {
        /*
         * Back is deliberately ignored while the kiosk
         * is active.
         */
        if (kioskEnabled) {
            return;
        }

        super.onBackPressed();
    }

    private int dp(float value) {

        return (int) (
                value
                        * getResources()
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

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(
                Gravity.CENTER_VERTICAL
        );

        return view;
    }

    private GradientDrawable rounded(
            int radius,
            int color
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    /*
     * Kiosk information menu.
     */
    private void showKioskMenu() {

        if (kioskDialog != null
                && kioskDialog.isShowing()) {

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
                        28,
                        Color.WHITE
                )
        );

        TextView title =
                label(
                        "Kiosk",
                        20,
                        Color.rgb(
                                65,
                                65,
                                65
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
                v -> openSettings()
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
                        this,
                        "Support",
                        Toast.LENGTH_SHORT
                ).show()
        );

        menuRow(
                box,
                "Open source",
                v -> Toast.makeText(
                        this,
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
                                105,
                                105,
                                105
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
                        "Installed date\n260909",
                        16,
                        Color.rgb(
                                105,
                                105,
                                105
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
                                55,
                                55,
                                55
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
                v -> {

                    if (kioskDialog != null) {
                        kioskDialog.dismiss();
                    }
                }
        );

        box.addView(
                done,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        kioskDialog.setContentView(box);

        Window window =
                kioskDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        kioskDialog.setCanceledOnTouchOutside(true);

        kioskDialog.show();

        window =
                kioskDialog.getWindow();

        if (window != null) {

            int width =
                    Math.min(
                            dp(610),
                            (int)
                                    (
                                            getResources()
                                                    .getDisplayMetrics()
                                                    .widthPixels
                                                    * 0.78f
                                    )
                    );

            window.setLayout(
                    width,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void divider(
            LinearLayout box
    ) {

        View line =
                new View(this);

        line.setBackgroundColor(
                Color.rgb(
                        220,
                        220,
                        220
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
                                90,
                                90,
                                90
                        )
                );

        TextView right =
                label(
                        "View",
                        17,
                        Color.rgb(
                                115,
                                135,
                                205
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

    private void confirmExit() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Exit Kiosk"
                )
                .setMessage(
                        "Are you sure you want to exit kiosk mode?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Exit",
                        (dialog, which) ->
                                exitKiosk()
                )
                .show();
    }

    private void openSettings() {

        try {

            startActivity(
                    new Intent(
                            Settings.ACTION_SETTINGS
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Settings unavailable",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * Launch the real Infinity Meta application.
     */
    private void openInfinityMeta() {

        try {

            Intent launchIntent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    INFINITY_META_PACKAGE
                            );

            if (launchIntent == null) {

                Toast.makeText(
                        this,
                        "Infinity Meta is not installed",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(
                    launchIntent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open Infinity Meta",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * Home screen / wallpaper.
     */
    private class HomeView extends View {

        private final Bitmap wallpaper;

        HomeView(Context context) {

            super(context);

            wallpaper =
                    BitmapFactory.decodeResource
