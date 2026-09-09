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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int BLUE = Color.rgb(45, 83, 180);

    /*
     * REAL Infinity Learn application package.
     *
     * This is the package from:
     * https://play.google.com/store/apps/details?id=apps.infinitylearn.lms
     */
    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private Dialog kioskDialog;

    /*
     * Prevents onResume() from automatically putting the tablet
     * back into kiosk mode after the user deliberately presses
     * "Exit Kiosk".
     */
    private boolean kioskExitRequested = false;

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

        hideSystemBars();

        setContentView(new HomeView(this));

        /*
         * Only enter Lock Task when this application really is
         * Device Owner and the required applications can be
         * allowlisted.
         *
         * This is intentionally protected so the application
         * does NOT crash when Device Owner has not yet been
         * configured.
         */
        configureAndEnterKiosk();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();

        /*
         * Do NOT blindly call startLockTask() here.
         *
         * This is important because pressing "Exit Kiosk" should
         * actually allow the tablet to return to normal operation.
         */
        if (!kioskExitRequested) {
            /*
             * If Lock Task is already active, Android keeps it active.
             * We deliberately do not force it again here.
             */
        }
    }

    /**
     * Configure the Device Owner Lock Task allowlist and then
     * enter kiosk mode when possible.
     */
    private void configureAndEnterKiosk() {

        if (devicePolicyManager == null) {
            return;
        }

        if (adminComponent == null) {
            return;
        }

        try {

            /*
             * Check whether our app is actually Device Owner.
             *
             * This prevents SecurityException from crashing the app
             * on a normal/non-provisioned tablet.
             */
            if (!devicePolicyManager.isDeviceOwnerApp(
                    getPackageName()
            )) {
                return;
            }

            /*
             * Allow both:
             *
             * 1. This kiosk application
             * 2. Infinity Learn
             *
             * to run while Lock Task mode is active.
             */
            List<String> packages =
                    new ArrayList<>();

            packages.add(getPackageName());
            packages.add(INFINITY_META_PACKAGE);

            devicePolicyManager.setLockTaskPackages(
                    adminComponent,
                    packages.toArray(new String[0])
            );

            /*
             * Start Lock Task only after the package allowlist
             * has successfully been configured.
             */
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

        } catch (Exception e) {

            /*
             * Never let kiosk configuration crash the application.
             */
        }
    }

    /**
     * Exit Android Lock Task mode.
     *
     * The Device Owner remains installed. This only stops the
     * current Lock Task session, allowing normal tablet use.
     */
    private void exitKiosk() {

        kioskExitRequested = true;

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                stopLockTask();
            }

        } catch (Exception ignored) {
        }

        Toast.makeText(
                this,
                "Kiosk mode exited",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Hide system bars while the kiosk screen is displayed.
     */
    private void hideSystemBars() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {

                Window window = getWindow();

                WindowInsetsController controller =
                        window.getInsetsController();

                if (controller != null) {

                    controller.hide(
                            WindowInsets.Type.statusBars()
                                    | WindowInsets.Type.navigationBars()
                                    | WindowInsets.Type.systemBars()
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
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        );
            }

        } catch (Exception ignored) {
        }
    }

    /**
     * Prevent Back from leaving the kiosk screen.
     */
    @Override
    public void onBackPressed() {
        /*
         * Intentionally empty.
         *
         * Back is consumed while this activity is being used
         * as the kiosk launcher.
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

        TextView t = new TextView(this);

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

    /**
     * Kiosk information menu.
     */
    private void showKioskMenu() {

        if (kioskDialog != null &&
                kioskDialog.isShowing()) {

            return;
        }

        kioskDialog = new Dialog(this);

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
                        MainActivity.this,
                        "Support",
                        Toast.LENGTH_SHORT
                ).show()
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
                        "Version\n1.0.5",
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
                        "Installed date\n260808",
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

                    if (kioskDialog != null &&
                            kioskDialog.isShowing()) {

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

            w.setLayout(
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

            /*
             * Settings may be restricted by Android while
             * full Lock Task is active.
             *
             * The important exit path remains available
             * through Exit Kiosk.
             */
            Intent intent =
                    new Intent(
                            Settings.ACTION_SETTINGS
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Settings unavailable while kiosk is active",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Launch the actual Infinity Learn application.
     */
    private void openInfinityMeta() {

        try {

            /*
             * First make sure the package is installed.
             */
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

            /*
             * These flags make the existing Infinity Learn
             * task come to the front instead of unnecessarily
             * creating duplicate activities.
             */
            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    /**
     * The main kiosk wallpaper screen.
     */
    private class HomeView extends View {

        private Bitmap wallpaper;

        private final Paint paint =
                new Paint(
                        Paint.FILTER_BITMAP_FLAG
                );

        HomeView(Context context) {

            super(context);

            /*
             * Load the exact wallpaper from:
             *
             * res/drawable/infinity_wallpaper.jpg
             *
             * The fallback prevents a crash if the file is
             * temporarily missing.
             */
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
                android.graphics.Canvas canvas
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

                /*
                 * Safe fallback instead of crashing.
                 */
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
                 * Bottom-left "i" button.
                 *
                 * This corresponds to the i button in
                 * the supplied wallpaper.
                 */
                if (
                        x <
                                getWidth()
                                        * 0.13f
                                &&
                        y >
                                getHeight()
                                        * 0.88f
                ) {

                    showKioskMenu();

                    return true;
                }

                /*
                 * Infinity Meta / Infinity Learn logo.
                 *
                 * This corresponds to the logo positioned
                 * near the upper-left portion of the wallpaper.
                 */
                if (
                        x <
                                getWidth()
                                        * 0.32f
                                &&
                        y <
                                getHeight()
                                        * 0.30f
                ) {

                    openInfinityMeta();

                    return true;
                }
            }

            return true;
        }
    }
}
