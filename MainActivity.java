package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    /*
     * ============================================================
     * Infinity Meta package name
     * ============================================================
     *
     * This is the package name of the separate Infinity Meta app.
     */
    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private static final int BLUE =
            Color.rgb(45, 83, 180);

    private Dialog kioskDialog;


    // ============================================================
    // ACTIVITY
    // ============================================================

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        try {
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

            setContentView(
                    new HomeView(this)
            );

            enterKiosk();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Infinity Meta Kiosk failed to start",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();
        enterKiosk();
    }


    @Override
    protected void onWindowFocusChanged(
            boolean hasFocus
    ) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideSystemBars();
        }
    }


    // ============================================================
    // FULL SCREEN / SYSTEM BARS
    // ============================================================

    private void hideSystemBars() {

        try {

            View decorView =
                    getWindow().getDecorView();

            /*
             * Use the older View flags instead of
             * WindowInsetsController.
             *
             * This avoids the compilation problem that
             * occurred in the previous build.
             */

            int flags =
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            decorView.setSystemUiVisibility(flags);

        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // KIOSK MODE
    // ============================================================

    private void enterKiosk() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.LOLLIPOP) {

                startLockTask();
            }

        } catch (Exception ignored) {

            /*
             * Lock Task can fail if the device has not been
             * provisioned as Device Owner / allowlisted.
             *
             * The application itself should still continue.
             */
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
    }


    // ============================================================
    // BACK BUTTON
    // ============================================================

    @Override
    public void onBackPressed() {

        /*
         * Intentionally consume Back.
         *
         * This keeps the kiosk screen from being closed.
         */
    }


    // ============================================================
    // HELPERS
    // ============================================================

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


    // ============================================================
    // KIOSK MENU
    // ============================================================

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


        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // SETTINGS
        // --------------------------------------------------------

        menuRow(
                box,
                "Settings",
                v -> openSettings()
        );


        // --------------------------------------------------------
        // EXIT KIOSK
        // --------------------------------------------------------

        menuRow(
                box,
                "Exit Kiosk",
                v -> confirmExit()
        );


        // --------------------------------------------------------
        // SUPPORT
        // --------------------------------------------------------

        menuRow(
                box,
                "Support",
                v -> Toast.makeText(
                        MainActivity.this,
                        "Support",
                        Toast.LENGTH_SHORT
                ).show()
        );


        // --------------------------------------------------------
        // OPEN SOURCE
        // --------------------------------------------------------

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


        // --------------------------------------------------------
       
