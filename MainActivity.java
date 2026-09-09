package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

public class MainActivity extends Activity {

    private static final String KIOSK_PACKAGE =
            "com.infinitymeta.kiosk";

    private static final String INFINITY_LEARN_PACKAGE =
            "apps.infinitylearn.lms";

    private Dialog kioskDialog;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        devicePolicyManager =
                (DevicePolicyManager)
                        getSystemService(
                                Context.DEVICE_POLICY_SERVICE
                        );

        deviceAdminComponent =
                KioskDeviceAdminReceiver
                        .getComponentName(this);

        getWindow().setStatusBarColor(
                Color.rgb(103, 184, 213)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_NOTHING
        );

        hideSystemBars();

        setContentView(new HomeView(this));

        configureKiosk();

        enterKiosk();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();

        configureKiosk();

        /*
         * Re-enter kiosk when returning to this app.
         */
        enterKiosk();
    }

    private void configureKiosk() {

        if (devicePolicyManager == null) {
            return;
        }

        try {

            if (devicePolicyManager.isDeviceOwnerApp(
                    getPackageName()
            )) {

                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.LOLLIPOP) {

                    devicePolicyManager.setLockTaskPackages(
                            deviceAdminComponent,
                            new String[]{
                                    KIOSK_PACKAGE,
                                    INFINITY_LEARN_PACKAGE
                            }
                    );
                }
            }

        } catch (Exception ignored) {
        }
    }

    private void hideSystemBars() {

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
    }

    private void enterKiosk() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        try {

            if (devicePolicyManager != null
                    && devicePolicyManager.isDeviceOwnerApp(
                            getPackageName()
                    )) {

                startLockTask();
            }

        } catch (Exception ignored) {
        }
    }

    private void exitKiosk() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                stopLockTask();
            }

        } catch (Exception ignored) {
        }

        /*
         * Leave the kiosk activity visible after stopping
         * Lock Task. The tablet itself is no longer locked.
         */
    }

    @Override
    public void onBackPressed() {
        /*
         * Back is intentionally disabled while this
         * kiosk activity is running.
         */
    }

    private int dp(float value) {

        return (int) (
                value *
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
        g.setCornerRadius(dp(radius));

        return g;
    }

    private void showKioskMenu() {

        if (kioskDialog != null
                && kioskDialog.isShowing()) {

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
                v -> kioskDialog.dismiss()
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

            int width = Math.min(
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
                    WindowManager.LayoutParams
                            .WRAP_CONTENT
            );
        }
    }

    private void divider(
            LinearLayout box
    ) {

        View line = new View(this);

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

        row.setOnClickListener(action);

        box.addView(row);
    }

    private void confirmExit() {

        new AlertDialog.Builder(this)

                .setTitle("Exit Kiosk")

                .setMessage(
                        "Are you sure you want to exit kiosk mode?"
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .setPositiveButton(
                        "Exit",
                        (dialog, which) -> {

                            if (kioskDialog != null
                                    && kioskDialog.isShowing()) {

                                kioskDialog.dismiss();
                            }

                            exitKiosk();
                        }
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

    private void openInfinityLearn() {

        try {

            Intent launchIntent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    INFINITY_LEARN_PACKAGE
                            );

            if (launchIntent == null) {

                Toast.makeText(
                        this,
                        "Infinity Learn is not installed",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(launchIntent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open Infinity Learn",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private class HomeView extends View {

        private final android.graphics.Bitmap wallpaper;

        HomeView(Context context) {

            super(context);

            wallpaper =
                    android.graphics.BitmapFactory
                            .decodeResource(
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

            android.graphics.Paint paint =
                    new android.graphics.Paint(
                            android.graphics.Paint
                                    .FILTER_BITMAP_FLAG
                    );

            if (wallpaper != null) {

                canvas.drawBitmap(
                        wallpaper,
                        null,
                        new android.graphics.Rect(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        ),
                        paint
                );
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction()
                    == MotionEvent.ACTION_UP) {

                float x = event.getX();
                float y = event.getY();

                /*
                 * Bottom-left i button.
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
                 * Infinity Meta logo.
                 * Opens Infinity Learn.
                 */
                if (
                        x < getWidth() * 0.32f
                                &&
                        y < getHeight() * 0.30f
                ) {

                    openInfinityLearn();

                    return true;
                }
            }

            return true;
        }
    }
}
