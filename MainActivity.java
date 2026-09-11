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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

    /*
     * Package name of the Infinity Learn application.
     */
    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private Dialog kioskDialog;

    /*
     * Once Exit Kiosk is pressed, this prevents onResume()
     * from hiding the system bars or restarting kiosk behavior.
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
         * Configure Lock Task only when this application
         * is actually Device Owner.
         */
        configureAndEnterKiosk();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Do not restore kiosk UI after the user has
         * deliberately exited kiosk mode.
         */
        if (!kioskExitRequested) {
            hideSystemBars();
        }
    }

    /**
     * Configure Lock Task and enter kiosk mode.
     */
    private void configureAndEnterKiosk() {

        if (devicePolicyManager == null ||
                adminComponent == null) {
            return;
        }

        try {

            /*
             * The application must be Device Owner.
             */
            if (!devicePolicyManager.isDeviceOwnerApp(
                    getPackageName()
            )) {
                return;
            }

            /*
             * Allow our kiosk application and Infinity Learn
             * to operate while Lock Task is active.
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
             * Enter Lock Task after the allowlist is configured.
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

        } catch (Exception ignored) {
            /*
             * Never crash if kiosk configuration fails.
             */
        }
    }

    /**
     * Exit Lock Task mode and return the tablet to normal use.
     *
     * Device Owner is NOT removed.
     */
    private void exitKiosk() {

        /*
         * Tell onResume() not to put the app back into
         * kiosk presentation mode.
         */
        kioskExitRequested = true;

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                stopLockTask();
            }

        } catch (Exception ignored) {
        }

        /*
         * Restore normal Android system bars.
         */
        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {

                Window window = getWindow();

                WindowInsetsController controller =
                        window.getInsetsController();

                if (controller != null) {

                    controller.show(
                            WindowInsets.Type.statusBars()
                                    | WindowInsets.Type.navigationBars()
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

        Toast.makeText(
                this,
                "Kiosk mode exited",
                Toast.LENGTH_SHORT
        ).show();

        /*
         * Remove this kiosk activity from the recent-task stack
         * and return control to the normal Android launcher.
         */
        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                finishAndRemoveTask();

            } else {

                finish();
            }

        } catch (Exception ignored) {

            finish();
        }
    }

    /**
     * Hide Android system bars while kiosk mode is active.
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
     * Consume Back while this kiosk activity is active.
     *
     * The user can use the Exit Kiosk option instead.
     */
    @Override
    public void onBackPressed() {
        /*
         * Intentionally empty.
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

    /**
     * Show the kiosk information menu.
     */
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

    /**
     * Settings are not launched while Lock Task is active.
     * Exit Kiosk first, then the tablet can be used normally.
     */
    private void openSettings() {

        Toast.makeText(
                this,
                "Exit Kiosk first to use Android Settings",
                Toast.LENGTH_LONG
        ).show();
    }

    /**
     * Launch the actual Infinity Learn application.
     */
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
     * Main kiosk wallpaper view.
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
             * Expected file:
             *
             * app/src/main/res/drawable/infinity_wallpaper.jpg
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

                /*
                 * Safe fallback if wallpaper cannot be loaded.
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
                 * Bottom-left information button.
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
                 * Infinity Learn logo area.
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
